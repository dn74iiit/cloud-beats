package com.cloudbeats.app.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a song stored in OneDrive.
 * Serves as the local metadata cache — the actual audio file
 * lives in the cloud unless downloaded for offline playback.
 */
@Entity(
    tableName = "songs",
    indices = [
        Index("artist"),
        Index("album"),
        Index("title")
    ]
)
data class SongEntity(
    /** OneDrive item ID — unique identifier from Microsoft Graph */
    @PrimaryKey
    val oneDriveId: String,

    /** Song title (extracted from filename or ID3 tags) */
    val title: String,

    /** Artist name */
    val artist: String,

    /** Album name */
    val album: String = "",

    /** Duration in milliseconds */
    val duration: Long = 0L,

    /** File size in bytes */
    val fileSize: Long = 0L,

    /** Full path in OneDrive (e.g., "/spotify_downloads/song.mp3") */
    val oneDrivePath: String,

    /** Local file path if downloaded for offline playback, null otherwise */
    val localPath: String? = null,

    /** URL for album art thumbnail */
    val albumArtUrl: String? = null,

    /** MIME type (e.g., "audio/mpeg") */
    val mimeType: String = "audio/mpeg",

    /** File extension (e.g., "mp3") */
    val fileExtension: String = "mp3",

    /** Timestamp when this metadata was last synced from OneDrive */
    val lastSynced: Long = System.currentTimeMillis(),

    /** Number of times this song has been played */
    val playCount: Int = 0,

    /** Timestamp of last playback */
    val lastPlayedAt: Long? = null,

    /** Whether this song is marked as favorite */
    val isFavorite: Boolean = false
) {
    /** Whether this song is available for offline playback */
    val isDownloaded: Boolean get() = localPath != null

    /** Human-readable file size */
    val fileSizeFormatted: String
        get() {
            val mb = fileSize / (1024.0 * 1024.0)
            return if (mb >= 1.0) {
                String.format("%.1f MB", mb)
            } else {
                String.format("%.0f KB", fileSize / 1024.0)
            }
        }

    /** Duration formatted as mm:ss */
    val durationFormatted: String
        get() {
            val totalSeconds = duration / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}
