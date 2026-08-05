package com.cloudbeats.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.media3.common.Player
import com.cloudbeats.app.data.local.entities.SongEntity
import com.cloudbeats.app.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackManager: PlaybackManager
) : ViewModel() {

    val currentSong: StateFlow<SongEntity?> = playbackManager.currentSong
    val isPlaying: StateFlow<Boolean> = playbackManager.isPlaying
    val currentPosition: StateFlow<Long> = playbackManager.currentPosition
    val duration: StateFlow<Long> = playbackManager.duration
    val shuffleEnabled: StateFlow<Boolean> = playbackManager.shuffleEnabled
    val repeatMode: StateFlow<Int> = playbackManager.repeatMode
    val isBuffering: StateFlow<Boolean> = playbackManager.isBuffering
    val queue: StateFlow<List<SongEntity>> = playbackManager.queue
    val currentIndex: StateFlow<Int> = playbackManager.currentIndex

    fun playPause() = playbackManager.playPause()
    fun skipToNext() = playbackManager.skipToNext()
    fun skipToPrevious() = playbackManager.skipToPrevious()
    fun seekTo(positionMs: Long) = playbackManager.seekTo(positionMs)
    fun toggleShuffle() = playbackManager.toggleShuffle()
    fun cycleRepeatMode() = playbackManager.cycleRepeatMode()

    /** Get the repeat mode icon description */
    fun getRepeatModeLabel(mode: Int): String = when (mode) {
        Player.REPEAT_MODE_OFF -> "Repeat Off"
        Player.REPEAT_MODE_ALL -> "Repeat All"
        Player.REPEAT_MODE_ONE -> "Repeat One"
        else -> "Repeat Off"
    }
}
