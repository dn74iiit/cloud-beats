package com.cloudbeats.app.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a user-created playlist.
 */
@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Playlist name */
    val name: String,

    /** Timestamp when the playlist was created */
    val createdAt: Long = System.currentTimeMillis(),

    /** Timestamp when the playlist was last modified */
    val updatedAt: Long = System.currentTimeMillis()
)
