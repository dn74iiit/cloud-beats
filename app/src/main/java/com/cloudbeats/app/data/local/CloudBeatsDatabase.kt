package com.cloudbeats.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cloudbeats.app.data.local.dao.PlaylistDao
import com.cloudbeats.app.data.local.dao.SongDao
import com.cloudbeats.app.data.local.entities.PlaylistEntity
import com.cloudbeats.app.data.local.entities.PlaylistSongCrossRef
import com.cloudbeats.app.data.local.entities.SongEntity

/**
 * Room database for CloudBeats.
 * Stores song metadata cache and user playlists.
 */
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 1,
    exportSchema = false
)
abstract class CloudBeatsDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
}
