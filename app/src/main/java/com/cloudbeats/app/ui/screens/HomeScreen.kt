package com.cloudbeats.app.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudbeats.app.ui.components.SongItem
import com.cloudbeats.app.ui.theme.Purple60
import com.cloudbeats.app.ui.viewmodels.HomeViewModel
import com.cloudbeats.app.ui.viewmodels.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    currentSongId: String?,
    onSongClick: () -> Unit = {}
) {
    val songs by viewModel.songs.collectAsState()
    val songCount by viewModel.songCount.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncError by viewModel.syncError.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val activeDownloads by viewModel.activeDownloads.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var showSearch by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showSpotifyDialog by remember { mutableStateOf(false) }
    
    val spotifyDownloadStatus by viewModel.spotifyDownloadStatus.collectAsState()
    val isDownloadingSpotify by viewModel.isDownloadingSpotify.collectAsState()
    
    var selectedSongIds by remember { mutableStateOf(setOf<String>()) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { _ ->
            // Proceed with sync regardless of outcome; if denied, local sync just won't find anything
            viewModel.syncLibrary()
        }
    )

    fun handleSyncClick() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    Scaffold(
        topBar = {
            if (selectedSongIds.isNotEmpty()) {
                TopAppBar(
                    title = { Text("${selectedSongIds.size} Selected") },
                    navigationIcon = {
                        IconButton(onClick = { selectedSongIds = emptySet() }) {
                            Icon(Icons.Default.Close, "Clear selection")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showPlaylistDialog = true }) {
                            Icon(Icons.Default.PlaylistAdd, "Add to Playlist")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            } else {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "CloudBeats",
                                style = MaterialTheme.typography.headlineSmall
                            )
                            if (songCount > 0) {
                                Text(
                                    text = "$songCount songs",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                    actions = {
                    // Search button
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search"
                        )
                    }

                    // Sort button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.Sort,
                                contentDescription = "Sort"
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = when (option) {
                                                SortOption.TITLE -> "Title"
                                                SortOption.ARTIST -> "Artist"
                                                SortOption.ALBUM -> "Album"
                                                SortOption.DATE_ADDED -> "Date Added"
                                                SortOption.MOST_PLAYED -> "Most Played"
                                            },
                                            color = if (option == sortOption) Purple60
                                            else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSortOption(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Spotify Download Button
                    IconButton(onClick = { showSpotifyDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = "Download from Spotify"
                        )
                    }

                    // Sync button
                    IconButton(onClick = { handleSyncClick() }) {
                        if (isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.padding(8.dp),
                                strokeWidth = 2.dp,
                                color = Purple60
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync"
                            )
                        }
                    }
                }
            )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar (expandable)
            AnimatedVisibility(visible = showSearch) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.search(it) },
                    onSearch = { viewModel.search(it) },
                    active = false,
                    onActiveChange = {},
                    placeholder = { Text("Search songs, artists, albums...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {}
            }

            // Sync error
            syncError?.let { error ->
                Text(
                    text = "⚠️ $error",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Song list
            val displaySongs = if (searchQuery.isNotBlank()) searchResults else songs

            if (displaySongs.isEmpty() && !isSyncing) {
                // Empty state
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No results found"
                            else "No songs yet",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different search"
                            else "Pull down to sync from OneDrive",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Debug log to help troubleshoot
                        val debugLog by viewModel.debugLog.collectAsState()
                        Text(
                            text = "DEBUG LOG:\n$debugLog",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(
                        items = displaySongs,
                        key = { it.oneDriveId }
                    ) { song ->
                        SongItem(
                            song = song,
                            isCurrentlyPlaying = song.oneDriveId == currentSongId,
                            downloadState = activeDownloads[song.oneDriveId],
                            isSelected = selectedSongIds.contains(song.oneDriveId),
                            onClick = {
                                if (selectedSongIds.isNotEmpty()) {
                                    selectedSongIds = if (selectedSongIds.contains(song.oneDriveId)) {
                                        selectedSongIds - song.oneDriveId
                                    } else {
                                        selectedSongIds + song.oneDriveId
                                    }
                                } else {
                                    viewModel.playSong(song, displaySongs)
                                    onSongClick()
                                }
                            },
                            onLongClick = {
                                selectedSongIds = if (selectedSongIds.contains(song.oneDriveId)) {
                                    selectedSongIds - song.oneDriveId
                                } else {
                                    selectedSongIds + song.oneDriveId
                                }
                            },
                            onDownloadClick = {
                                if (song.isDownloaded) {
                                    viewModel.deleteDownload(song)
                                } else {
                                    viewModel.downloadSong(song)
                                }
                            },
                            onFavoriteClick = {
                                viewModel.toggleFavorite(song.oneDriveId)
                            },
                            onAddToPlaylistClick = {
                                selectedSongIds = setOf(song.oneDriveId)
                                showPlaylistDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    if (showPlaylistDialog) {
        var newPlaylistName by remember { mutableStateOf("") }
        var isCreatingNew by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showPlaylistDialog = false },
            title = { Text("Add to Playlist") },
            text = {
                Column {
                    if (isCreatingNew) {
                        OutlinedTextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            label = { Text("Playlist Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LazyColumn {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { isCreatingNew = true }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = Purple60)
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("New Playlist", color = Purple60)
                                }
                            }
                            items(playlists) { playlist ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.addSongsToPlaylist(playlist.id, selectedSongIds.toList())
                                            showPlaylistDialog = false
                                            selectedSongIds = emptySet()
                                        }
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(playlist.name)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                if (isCreatingNew) {
                    TextButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                viewModel.createPlaylist(newPlaylistName, selectedSongIds.toList())
                                showPlaylistDialog = false
                                selectedSongIds = emptySet()
                            }
                        }
                    ) {
                        Text("Create & Add")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    if (isCreatingNew) isCreatingNew = false
                    else showPlaylistDialog = false
                }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showSpotifyDialog) {
        var spotifyUrl by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { 
                showSpotifyDialog = false 
                viewModel.clearSpotifyDownloadStatus()
            },
            title = { Text("Download from Spotify") },
            text = {
                Column {
                    Text("Paste a Spotify link below. The backend service will download the song and upload it to your OneDrive.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = spotifyUrl,
                        onValueChange = { spotifyUrl = it },
                        label = { Text("Spotify URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isDownloadingSpotify
                    )

                    spotifyDownloadStatus?.let { status ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = status,
                            color = if (status.startsWith("Error")) MaterialTheme.colorScheme.error else Purple60,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.downloadFromSpotify(spotifyUrl) },
                    enabled = spotifyUrl.isNotBlank() && !isDownloadingSpotify
                ) {
                    if (isDownloadingSpotify) {
                        CircularProgressIndicator(modifier = Modifier.height(16.dp).width(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Download")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showSpotifyDialog = false 
                        viewModel.clearSpotifyDownloadStatus()
                    },
                    enabled = !isDownloadingSpotify
                ) {
                    Text("Close")
                }
            }
        )
    }
}
