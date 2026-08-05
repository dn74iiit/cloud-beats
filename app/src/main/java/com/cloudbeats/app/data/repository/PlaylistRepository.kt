package com.cloudbeats.app.data.repository

import com.cloudbeats.app.data.local.dao.PlaylistDao
import com.cloudbeats.app.data.local.entities.PlaylistEntity
import com.cloudbeats.app.data.local.entities.PlaylistSongCrossRef
import com.cloudbeats.app.data.local.entities.PlaylistWithSongs
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for playlist operations.
 * All data lives locally in Room — playlists are not synced to OneDrive.
 */
@Singleton
class PlaylistRepository @Inject constructor(
    private val playlistDao: PlaylistDao
) {
    fun getAllPlaylists(): Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    fun getAllPlaylistsWithSongs(): Flow<List<PlaylistWithSongs>> =
        playlistDao.getAllPlaylistsWithSongs()

    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> =
        playlistDao.getPlaylistWithSongs(playlistId)

    fun getPlaylistSongCount(playlistId: Long): Flow<Int> =
        playlistDao.getPlaylistSongCount(playlistId)

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.createPlaylist(
            PlaylistEntity(name = name)
        )
    }

    suspend fun renamePlaylist(playlistId: Long, newName: String) {
        playlistDao.renamePlaylist(playlistId, newName)
    }

    suspend fun deletePlaylist(playlistId: Long) {
        playlistDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
        playlistDao.addSongToPlaylist(
            PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
        )
        playlistDao.touchPlaylist(playlistId)
    }

    suspend fun addSongsToPlaylist(playlistId: Long, songIds: List<String>) {
        val crossRefs = songIds.mapIndexed { index, songId ->
            PlaylistSongCrossRef(
                playlistId = playlistId,
                songId = songId,
                sortOrder = index
            )
        }
        playlistDao.addSongsToPlaylist(crossRefs)
        playlistDao.touchPlaylist(playlistId)
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
        playlistDao.touchPlaylist(playlistId)
    }

    suspend fun isSongInPlaylist(playlistId: Long, songId: String): Boolean {
        return playlistDao.isSongInPlaylist(playlistId, songId)
    }

    fun getPlaylistsForSong(songId: String): Flow<List<Long>> {
        return playlistDao.getPlaylistsForSong(songId)
    }
}
