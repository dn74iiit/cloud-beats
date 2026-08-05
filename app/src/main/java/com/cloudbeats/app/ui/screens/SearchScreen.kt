package com.cloudbeats.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.cloudbeats.app.ui.components.SongItem
import com.cloudbeats.app.ui.theme.Purple60
import com.cloudbeats.app.ui.viewmodels.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    currentSongId: String?,
    onSongClick: () -> Unit = {}
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    
    var selectedSongIds by remember { mutableStateOf(setOf<String>()) }
    var showPlaylistDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 16.dp)
    ) {
        if (selectedSongIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { selectedSongIds = emptySet() }) {
                    Icon(Icons.Default.Close, "Clear selection")
                }
                Text(
                    text = "${selectedSongIds.size} Selected",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { showPlaylistDialog = true }) {
                    Icon(Icons.Default.PlaylistAdd, "Add to Playlist")
                }
            }
        } else {
            // Search field
            OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.search(it) },
            placeholder = { Text("Search songs, artists, albums...") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple60,
                cursorColor = Purple60
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (searchQuery.isBlank()) {
            // Show recent plays or all songs when no search
            Text(
                text = "All Songs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = songs,
                    key = { it.oneDriveId }
                ) { song ->
                    SongItem(
                        song = song,
                        isCurrentlyPlaying = song.oneDriveId == currentSongId,
                        isSelected = selectedSongIds.contains(song.oneDriveId),
                        onClick = {
                            if (selectedSongIds.isNotEmpty()) {
                                selectedSongIds = if (selectedSongIds.contains(song.oneDriveId)) {
                                    selectedSongIds - song.oneDriveId
                                } else {
                                    selectedSongIds + song.oneDriveId
                                }
                            } else {
                                viewModel.playSong(song, songs)
                                onSongClick()
                            }
                        },
                        onLongClick = {
                            selectedSongIds = if (selectedSongIds.contains(song.oneDriveId)) {
                                selectedSongIds - song.oneDriveId
                            } else {
                                selectedSongIds + song.oneDriveId
                            }
                        }
                    )
                }
            }
        } else {
            // Show search results
            Text(
                text = "${searchResults.size} results",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                items(
                    items = searchResults,
                    key = { it.oneDriveId }
                ) { song ->
                    SongItem(
                        song = song,
                        isCurrentlyPlaying = song.oneDriveId == currentSongId,
                        isSelected = selectedSongIds.contains(song.oneDriveId),
                        onClick = {
                            if (selectedSongIds.isNotEmpty()) {
                                selectedSongIds = if (selectedSongIds.contains(song.oneDriveId)) {
                                    selectedSongIds - song.oneDriveId
                                } else {
                                    selectedSongIds + song.oneDriveId
                                }
                            } else {
                                viewModel.playSong(song, searchResults)
                                onSongClick()
                            }
                        },
                        onLongClick = {
                            selectedSongIds = if (selectedSongIds.contains(song.oneDriveId)) {
                                selectedSongIds - song.oneDriveId
                            } else {
                                selectedSongIds + song.oneDriveId
                            }
                        }
                    )
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
}
