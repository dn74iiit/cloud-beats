package com.cloudbeats.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.cloudbeats.app.data.local.entities.SongEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for song operations.
 * All queries return Flow for reactive UI updates.
 */
@Dao
interface SongDao {

    // ── Read Operations ──

    /** Get all songs ordered by title */
    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    /** Get all songs ordered by artist */
    @Query("SELECT * FROM songs ORDER BY artist ASC, title ASC")
    fun getAllSongsByArtist(): Flow<List<SongEntity>>

    /** Get all songs ordered by album */
    @Query("SELECT * FROM songs ORDER BY album ASC, title ASC")
    fun getAllSongsByAlbum(): Flow<List<SongEntity>>

    /** Get all songs ordered by date synced (newest first) */
    @Query("SELECT * FROM songs ORDER BY lastSynced DESC")
    fun getAllSongsByDateAdded(): Flow<List<SongEntity>>

    /** Get all songs ordered by play count (most played first) */
    @Query("SELECT * FROM songs ORDER BY playCount DESC")
    fun getMostPlayedSongs(): Flow<List<SongEntity>>

    /** Get recently played songs */
    @Query("SELECT * FROM songs WHERE lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 50): Flow<List<SongEntity>>

    /** Get favorite songs */
    @Query("SELECT * FROM songs WHERE isFavorite = 1 ORDER BY title ASC")
    fun getFavoriteSongs(): Flow<List<SongEntity>>

    /** Get downloaded songs (available offline) */
    @Query("SELECT * FROM songs WHERE localPath IS NOT NULL ORDER BY title ASC")
    fun getDownloadedSongs(): Flow<List<SongEntity>>

    /** Search songs by title, artist, or album */
    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
        OR artist LIKE '%' || :query || '%' 
        OR album LIKE '%' || :query || '%' 
        ORDER BY title ASC
    """)
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("""
        SELECT * FROM songs 
        WHERE title LIKE '%' || :query || '%' 
        OR artist LIKE '%' || :query || '%' 
        OR album LIKE '%' || :query || '%' 
        ORDER BY title ASC
    """)
    suspend fun searchSongsSync(query: String): List<SongEntity>

    /** Get a single song by its OneDrive ID */
    @Query("SELECT * FROM songs WHERE oneDriveId = :id")
    suspend fun getSongById(id: String): SongEntity?

    /** Get total count of songs */
    @Query("SELECT COUNT(*) FROM songs")
    fun getSongCount(): Flow<Int>

    /** Get total size of downloaded files */
    @Query("SELECT COALESCE(SUM(fileSize), 0) FROM songs WHERE localPath IS NOT NULL")
    fun getDownloadedSize(): Flow<Long>

    /** Get all unique artists */
    @Query("SELECT DISTINCT artist FROM songs ORDER BY artist ASC")
    fun getAllArtists(): Flow<List<String>>

    /** Get all unique albums */
    @Query("SELECT DISTINCT album FROM songs WHERE album != '' ORDER BY album ASC")
    fun getAllAlbums(): Flow<List<String>>

    // ── Write Operations ──

    /** Insert or update songs (used during sync) */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(songs: List<SongEntity>)

    /** Insert a single song */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: SongEntity)

    /** Update a song */
    @Update
    suspend fun update(song: SongEntity)

    /** Mark a song as downloaded with its local file path */
    @Query("UPDATE songs SET localPath = :localPath WHERE oneDriveId = :songId")
    suspend fun setDownloadPath(songId: String, localPath: String)

    /** Clear the download path (mark as not downloaded) */
    @Query("UPDATE songs SET localPath = NULL WHERE oneDriveId = :songId")
    suspend fun clearDownloadPath(songId: String)

    /** Increment play count and update last played timestamp */
    @Query("""
        UPDATE songs 
        SET playCount = playCount + 1, lastPlayedAt = :timestamp 
        WHERE oneDriveId = :songId
    """)
    suspend fun recordPlay(songId: String, timestamp: Long = System.currentTimeMillis())

    /** Toggle favorite status */
    @Query("UPDATE songs SET isFavorite = NOT isFavorite WHERE oneDriveId = :songId")
    suspend fun toggleFavorite(songId: String)

    /** Delete a song by ID */
    @Query("DELETE FROM songs WHERE oneDriveId = :songId")
    suspend fun delete(songId: String)

    /** Delete all songs (used during full resync) */
    @Query("DELETE FROM songs")
    suspend fun deleteAll()

    /** Delete songs not in the given list of IDs (removes songs deleted from OneDrive) */
    @Query("DELETE FROM songs WHERE oneDriveId NOT IN (:validIds)")
    suspend fun deleteNotIn(validIds: List<String>)
}
