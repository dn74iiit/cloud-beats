package com.cloudbeats.app.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table linking playlists to songs (many-to-many relationship).
 * Uses foreign keys for referential integrity and cascading deletes.
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songId"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SongEntity::class,
            parentColumns = ["oneDriveId"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("playlistId"),
        Index("songId")
    ]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: String,
    /** Sort order within the playlist (for drag-to-reorder) */
    val sortOrder: Int = 0
)
