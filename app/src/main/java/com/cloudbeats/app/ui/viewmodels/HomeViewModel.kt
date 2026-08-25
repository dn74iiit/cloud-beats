package com.cloudbeats.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbeats.app.data.local.entities.PlaylistEntity
import com.cloudbeats.app.data.local.entities.SongEntity
import com.cloudbeats.app.data.repository.MusicRepository
import com.cloudbeats.app.data.repository.PlaylistRepository
import com.cloudbeats.app.data.remote.SpotifyService
import com.cloudbeats.app.download.DownloadManager
import com.cloudbeats.app.download.DownloadState
import com.cloudbeats.app.player.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Sorting options for the song list.
 */
enum class SortOption {
    TITLE, ARTIST, ALBUM, DATE_ADDED, MOST_PLAYED
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playbackManager: PlaybackManager,
    private val downloadManager: DownloadManager,
    private val spotifyService: SpotifyService
) : ViewModel() {

    // ── Songs List ──

    private val _sortOption = MutableStateFlow(SortOption.TITLE)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val songs: StateFlow<List<SongEntity>> = _sortOption
        .kotlinx.coroutines.flow.flatMapLatest { option ->
            when (option) {
                SortOption.TITLE -> musicRepository.getAllSongs()
                SortOption.ARTIST -> musicRepository.getAllSongsByArtist()
                SortOption.ALBUM -> musicRepository.getAllSongsByAlbum()
                SortOption.DATE_ADDED -> musicRepository.getAllSongsByDateAdded()
                SortOption.MOST_PLAYED -> musicRepository.getMostPlayedSongs()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val songCount: StateFlow<Int> = musicRepository.getSongCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val recentlyPlayed: StateFlow<List<SongEntity>> = musicRepository.getRecentlyPlayed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isSyncing: StateFlow<Boolean> = musicRepository.isSyncing
    val syncError: StateFlow<String?> = musicRepository.syncError
    val debugLog: StateFlow<String> = musicRepository.debugLog

    val activeDownloads: StateFlow<Map<String, DownloadState>> = downloadManager.activeDownloads

    // ── Search ──

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<SongEntity>>(emptyList())
    val searchResults: StateFlow<List<SongEntity>> = _searchResults.asStateFlow()
    
    // -- Playlists --
    val playlists: StateFlow<List<PlaylistEntity>> = playlistRepository.getAllPlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addSongsToPlaylist(playlistId: Long, songIds: List<String>) {
        viewModelScope.launch {
            playlistRepository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    fun createPlaylist(name: String, songIds: List<String>) {
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(name)
            playlistRepository.addSongsToPlaylist(playlistId, songIds)
        }
    }

    // ── OneDrive folder path (user configurable) ──
    private val _folderPath = MutableStateFlow("spotify_downloads")
    val folderPath: StateFlow<String> = _folderPath.asStateFlow()

    init {
        syncLibrary()
    }

    fun syncLibrary() {
        viewModelScope.launch {
            musicRepository.syncLocalMusic()
            musicRepository.syncFromOneDrive(_folderPath.value)
        }
    }

    fun setFolderPath(path: String) {
        _folderPath.value = path
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun search(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        viewModelScope.launch {
            musicRepository.searchSongs(query).collect { results ->
                _searchResults.value = results
            }
        }
    }

    fun playSong(song: SongEntity, allSongs: List<SongEntity>) {
        val index = allSongs.indexOf(song).takeIf { it >= 0 } ?: 0
        playbackManager.playSongs(allSongs, index)
    }

    fun downloadSong(song: SongEntity) {
        viewModelScope.launch {
            downloadManager.downloadSong(song)
        }
    }

    fun deleteDownload(song: SongEntity) {
        viewModelScope.launch {
            downloadManager.deleteDownload(song)
        }
    }

    fun toggleFavorite(songId: String) {
        viewModelScope.launch {
            musicRepository.toggleFavorite(songId)
        }
    }

    // ── Spotify Downloader ──

    private val _spotifyDownloadStatus = MutableStateFlow<String?>(null)
    val spotifyDownloadStatus: StateFlow<String?> = _spotifyDownloadStatus.asStateFlow()

    private val _isDownloadingSpotify = MutableStateFlow(false)
    val isDownloadingSpotify: StateFlow<Boolean> = _isDownloadingSpotify.asStateFlow()

    fun downloadFromSpotify(url: String) {
        if (url.isBlank()) return
        viewModelScope.launch {
            _isDownloadingSpotify.value = true
            _spotifyDownloadStatus.value = "Sending request to backend..."
            
            val result = spotifyService.downloadSpotifyLink(url)
            
            result.onSuccess {
                _spotifyDownloadStatus.value = "Success: Download initiated! Syncing library..."
                syncLibrary()
            }.onFailure {
                _spotifyDownloadStatus.value = "Error: ${it.message}"
            }
            
            _isDownloadingSpotify.value = false
        }
    }

    fun clearSpotifyDownloadStatus() {
        _spotifyDownloadStatus.value = null
    }
}
