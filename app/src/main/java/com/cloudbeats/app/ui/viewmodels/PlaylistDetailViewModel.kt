package com.cloudbeats.app.ui.viewmodels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbeats.app.data.local.entities.PlaylistWithSongs
import com.cloudbeats.app.data.repository.PlaylistRepository
import com.cloudbeats.app.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val playlistId: Long = checkNotNull(savedStateHandle["playlistId"])

    val playlist: StateFlow<PlaylistWithSongs?> =
        playlistRepository.getPlaylistWithSongs(playlistId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun playSong(songId: String) {
        val currentPlaylist = playlist.value ?: return
        val startIndex = currentPlaylist.songs.indexOfFirst { it.oneDriveId == songId }
        if (startIndex != -1) {
            playbackManager.playSongs(currentPlaylist.songs, startIndex)
        }
    }

    fun removeSong(songId: String) {
        viewModelScope.launch {
            playlistRepository.removeSongFromPlaylist(playlistId, songId)
        }
    }
}
