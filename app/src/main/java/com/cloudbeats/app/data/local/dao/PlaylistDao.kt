package com.cloudbeats.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.cloudbeats.app.data.local.entities.PlaylistEntity
import com.cloudbeats.app.data.local.entities.PlaylistSongCrossRef
import com.cloudbeats.app.data.local.entities.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for playlist operations.
 */
@Dao
interface PlaylistDao {

    // ── Read Operations ──

    /** Get all playlists ordered by most recently updated */
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    /** Get a playlist with all its songs */
    @Transaction
    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?>

    /** Get all playlists with their songs */
    @Transaction
    @Query("SELECT * FROM playlists ORDER BY updatedAt DESC")
    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>>

    /** Get the number of songs in a playlist */
    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    fun getPlaylistSongCount(playlistId: Long): Flow<Int>

    /** Check if a song is in a specific playlist */
    @Query("SELECT COUNT(*) > 0 FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun isSongInPlaylist(playlistId: Long, songId: String): Boolean

    /** Get all playlist IDs that contain a specific song */
    @Query("SELECT playlistId FROM playlist_songs WHERE songId = :songId")
    fun getPlaylistsForSong(songId: String): Flow<List<Long>>

    // ── Write Operations ──

    /** Create a new playlist, returns the generated ID */
    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    /** Update playlist details */
    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    /** Rename a playlist */
    @Query("UPDATE playlists SET name = :newName, updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun renamePlaylist(
        playlistId: Long,
        newName: String,
        timestamp: Long = System.currentTimeMillis()
    )

    /** Delete a playlist (cascade will remove cross-refs) */
    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    /** Add a song to a playlist */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    /** Add multiple songs to a playlist */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongsToPlaylist(crossRefs: List<PlaylistSongCrossRef>)

    /** Remove a song from a playlist */
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String)

    /** Remove all songs from a playlist */
    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    suspend fun clearPlaylist(playlistId: Long)

    /** Update the sort order of a song in a playlist (for reordering) */
    @Query("UPDATE playlist_songs SET sortOrder = :newOrder WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun updateSortOrder(playlistId: Long, songId: String, newOrder: Int)

    /** Touch the updatedAt timestamp of a playlist */
    @Query("UPDATE playlists SET updatedAt = :timestamp WHERE id = :playlistId")
    suspend fun touchPlaylist(playlistId: Long, timestamp: Long = System.currentTimeMillis())

    /** Get a playlist by exact name to prevent duplicates */
    @Query("SELECT * FROM playlists WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?
}
