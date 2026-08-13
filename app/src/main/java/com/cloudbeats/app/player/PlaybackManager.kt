package com.cloudbeats.app.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cloudbeats.app.data.local.entities.SongEntity
import com.cloudbeats.app.data.repository.MusicRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level playback controller that bridges the UI with the MediaSessionService.
 * Manages the play queue, shuffle, repeat, and streaming URL acquisition.
 */
@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val musicRepository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    // ── Playback State ──

    private val _currentSong = MutableStateFlow<SongEntity?>(null)
    val currentSong: StateFlow<SongEntity?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(Player.REPEAT_MODE_OFF)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    // ── Queue ──

    private val _queue = MutableStateFlow<List<SongEntity>>(emptyList())
    val queue: StateFlow<List<SongEntity>> = _queue.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    /** Original queue order (before shuffle) */
    private var originalQueue: List<SongEntity> = emptyList()

    /**
     * Initialize the MediaController connection to the CloudBeatsService.
     * Must be called during app startup.
     */
    fun initialize() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, CloudBeatsService::class.java)
        )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            controller = controllerFuture?.get()
            setupPlayerListener()
            startPositionUpdater()
        }, MoreExecutors.directExecutor())
    }

    /**
     * Listen to player state changes and update our StateFlows.
     */
    private fun setupPlayerListener() {
        controller?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                _isBuffering.value = state == Player.STATE_BUFFERING

                if (state == Player.STATE_READY) {
                    _duration.value = controller?.duration ?: 0L
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val index = controller?.currentMediaItemIndex ?: -1
                if (index != -1 && index in _queue.value.indices) {
                    _currentIndex.value = index
                    val song = _queue.value[index]
                    _currentSong.value = song
                    
                    // Record play in database
                    scope.launch(Dispatchers.IO) {
                        musicRepository.recordPlay(song.oneDriveId)
                    }

                    // Pre-fetch next song URL
                    prefetchNextSong(index)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _shuffleEnabled.value = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(mode: Int) {
                _repeatMode.value = mode
            }
        })
    }

    /**
     * Periodically update the current playback position.
     */
    private fun startPositionUpdater() {
        scope.launch {
            while (true) {
                controller?.let { ctrl ->
                    if (ctrl.isPlaying) {
                        _currentPosition.value = ctrl.currentPosition
                    }
                }
                delay(250) // Update 4 times per second
            }
        }
    }

    // ── Playback Controls ──

    /**
     * Play a list of songs starting from the given index.
     * To prevent freezing, we only prepare the first song immediately.
     */
    fun playSongs(songs: List<SongEntity>, startIndex: Int = 0) {
        scope.launch {
            originalQueue = songs
            _queue.value = songs
            _currentIndex.value = startIndex
            
            val firstSong = songs.getOrNull(startIndex)
            _currentSong.value = firstSong

            if (songs.isNotEmpty()) {
                val mediaItems = songs.map { createMediaItem(it) }
                controller?.apply {
                    setMediaItems(mediaItems, startIndex, 0L)
                    prepare()
                    play()
                }
            }
        }
    }

    /**
     * Create a MediaItem for a song, using custom cloudbeats:// URI for dynamic resolution.
     */
    private fun createMediaItem(song: SongEntity): MediaItem {
        val uri = if (song.isDownloaded && song.localPath != null) {
            song.localPath
        } else {
            "cloudbeats://${song.oneDriveId}"
        }

        return MediaItem.Builder()
            .setMediaId(song.oneDriveId)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .setArtworkUri(song.albumArtUrl?.let { android.net.Uri.parse(it) })
                    .build()
            )
            .build()
    }

    /**
     * Pre-fetch the streaming URL for the next song to eliminate delay.
     */
    private fun prefetchNextSong(currentIndex: Int) {
        val nextIndex = currentIndex + 1
        if (nextIndex in _queue.value.indices) {
            scope.launch(Dispatchers.IO) {
                musicRepository.prefetchStreamingUrl(_queue.value[nextIndex])
            }
        }
    }

    fun play() { controller?.play() }
    fun pause() { controller?.pause() }

    fun playPause() {
        controller?.let { ctrl ->
            if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun skipToNext() {
        controller?.seekToNextMediaItem()
    }
    
    fun skipToPrevious() {
        if (_currentPosition.value > 3000) {
            seekTo(0)
            return
        }
        controller?.seekToPreviousMediaItem()
    }

    private fun playQueueItem(index: Int) {
        if (index in _queue.value.indices) {
            controller?.seekToDefaultPosition(index)
            controller?.play()
        }
    }

    fun toggleShuffle() {
        controller?.let { ctrl ->
            ctrl.shuffleModeEnabled = !ctrl.shuffleModeEnabled
        }
    }

    fun cycleRepeatMode() {
        controller?.let { ctrl ->
            ctrl.repeatMode = when (ctrl.repeatMode) {
                Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
                Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                else -> Player.REPEAT_MODE_OFF
            }
        }
    }

    /**
     * Add a song to the end of the current queue.
     */
    fun addToQueue(song: SongEntity) {
        scope.launch {
            val currentQueue = _queue.value.toMutableList()
            currentQueue.add(song)
            _queue.value = currentQueue

            val mediaItem = createMediaItem(song)
            controller?.addMediaItem(mediaItem)
        }
    }

    /**
     * Remove a song from the queue at the given index.
     */
    fun removeFromQueue(index: Int) {
        if (index in _queue.value.indices) {
            val currentQueue = _queue.value.toMutableList()
            currentQueue.removeAt(index)
            _queue.value = currentQueue
            controller?.removeMediaItem(index)
        }
    }

    /**
     * Stop playback and clear the queue.
     */
    fun stop() {
        controller?.apply {
            stop()
            clearMediaItems()
        }
        _queue.value = emptyList()
        _currentSong.value = null
        _currentIndex.value = -1
        _isPlaying.value = false
    }

    /**
     * Release the controller. Call when the app is being destroyed.
     */
    fun release() {
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controller = null
        controllerFuture = null
    }
}
