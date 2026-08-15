package com.cloudbeats.app.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cloudbeats.app.data.remote.OnlineSong
import com.cloudbeats.app.data.remote.SpotifyService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnlineSearchViewModel @Inject constructor(
    private val spotifyService: SpotifyService
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<OnlineSong>>(emptyList())
    val searchResults: StateFlow<List<OnlineSong>> = _searchResults.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _downloadStatus = MutableStateFlow<String?>(null)
    val downloadStatus = _downloadStatus.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun searchOnline() {
        val query = _searchQuery.value.trim()
        if (query.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            val result = spotifyService.searchOnline(query)
            
            result.onSuccess { songs ->
                _searchResults.value = songs
            }.onFailure { error ->
                _errorMessage.value = error.message ?: "Failed to search online"
            }
            
            _isLoading.value = false
        }
    }

    fun downloadSong(song: OnlineSong) {
        viewModelScope.launch {
            _downloadStatus.value = "Downloading ${song.title}..."
            
            val result = spotifyService.downloadSpotifyLink(song.url)
            
            result.onSuccess {
                _downloadStatus.value = "Successfully started download for ${song.title}"
            }.onFailure { error ->
                _downloadStatus.value = "Failed to download ${song.title}: ${error.message}"
            }
        }
    }
    
    fun clearDownloadStatus() {
        _downloadStatus.value = null
    }
}
