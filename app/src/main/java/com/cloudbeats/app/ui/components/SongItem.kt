package com.cloudbeats.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.OfflinePin
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cloudbeats.app.data.local.entities.SongEntity
import com.cloudbeats.app.download.DownloadState
import com.cloudbeats.app.ui.theme.CloudBadge
import com.cloudbeats.app.ui.theme.Cyan60
import com.cloudbeats.app.ui.theme.DarkSurfaceElevated
import com.cloudbeats.app.ui.theme.DownloadedBadge
import com.cloudbeats.app.ui.theme.Purple60
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

/**
 * Reusable song list item with album art placeholder, metadata,
 * download status, and context menu.
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun SongItem(
    song: SongEntity,
    isCurrentlyPlaying: Boolean = false,
    downloadState: DownloadState? = null,
    onClick: () -> Unit,
    onDownloadClick: () -> Unit = {},
    onFavoriteClick: () -> Unit = {},
    onAddToPlaylistClick: () -> Unit = {},
    isSelected: Boolean = false,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else if (isCurrentlyPlaying) Purple60.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DarkSurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUrl != null) {
                AsyncImage(
                    model = song.albumArtUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrentlyPlaying) Purple60 else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Song info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrentlyPlaying) Purple60 else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Download status icon
                Icon(
                    imageVector = if (song.isDownloaded) Icons.Default.OfflinePin else Icons.Default.Cloud,
                    contentDescription = if (song.isDownloaded) "Downloaded" else "Cloud",
                    tint = if (song.isDownloaded) DownloadedBadge else CloudBadge,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${song.artist} • ${song.durationFormatted}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Download progress bar
            if (downloadState is DownloadState.Downloading) {
                LinearProgressIndicator(
                    progress = { downloadState.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    color = Cyan60,
                    trackColor = DarkSurfaceElevated,
                )
            }
        }

        // File size
        Text(
            text = song.fileSizeFormatted,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // More options
        Box {
            IconButton(onClick = { showMenu = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(if (song.isDownloaded) "Remove Download" else "Download") },
                    onClick = {
                        showMenu = false
                        onDownloadClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (song.isDownloaded) Icons.Default.OfflinePin else Icons.Default.CloudDownload,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(if (song.isFavorite) "Remove from Favorites" else "Add to Favorites") },
                    onClick = {
                        showMenu = false
                        onFavoriteClick()
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text("Add to Playlist") },
                    onClick = {
                        showMenu = false
                        onAddToPlaylistClick()
                    }
                )
            }
        }
    }
}
