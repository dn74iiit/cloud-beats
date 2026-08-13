package com.cloudbeats.app.player

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.cloudbeats.app.data.repository.MusicRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject/**
 * Background media playback service using Media3.
 *
 * This service keeps music playing when the app is in the background,
 * handles system media controls (lock screen, notification, Bluetooth),
 * and manages audio focus properly.
 */
@AndroidEntryPoint
class CloudBeatsService : MediaSessionService() {

    @Inject
    lateinit var musicRepository: MusicRepository

    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()

        // Configure data source for custom cloudbeats:// URIs
        val defaultDataSourceFactory = DefaultDataSource.Factory(this)
        val resolvingDataSourceFactory = ResolvingDataSource.Factory(
            defaultDataSourceFactory,
            ResolvingDataSource.Resolver { dataSpec ->
                if (dataSpec.uri.scheme == "cloudbeats") {
                    val oneDriveId = dataSpec.uri.host
                    if (oneDriveId != null) {
                        val url = runBlocking {
                            val song = musicRepository.getSongById(oneDriveId)
                            if (song != null) {
                                musicRepository.getStreamingUrl(song).getOrElse { "" }
                            } else {
                                ""
                            }
                        }
                        if (url.isNotEmpty()) {
                            return@Resolver dataSpec.buildUpon().setUri(url).build()
                        }
                    }
                }
                dataSpec
            }
        )
        
        val mediaSourceFactory = DefaultMediaSourceFactory(this)
            .setDataSourceFactory(resolvingDataSourceFactory)

        // Configure ExoPlayer with audio-optimized settings
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(mediaSourceFactory)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .setHandleAudioBecomingNoisy(true) // Pause when headphones disconnected
            .setWakeMode(C.WAKE_MODE_NETWORK) // Keep CPU + WiFi during streaming
            .build()

        // Create media session
        mediaSession = MediaSession.Builder(this, player!!)
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player ?: run {
            stopSelf()
            return
        }
        // Stop the service if the player is not playing
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        player = null
        super.onDestroy()
    }
}
