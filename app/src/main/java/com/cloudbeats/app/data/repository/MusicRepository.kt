package com.cloudbeats.app.data.repository

import com.cloudbeats.app.data.local.dao.SongDao
import com.cloudbeats.app.data.local.entities.SongEntity
import com.cloudbeats.app.data.remote.OneDriveService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for music data. Acts as the single source of truth.
 * UI reads from Room (offline-first), syncs from OneDrive in background.
 */
@Singleton
class MusicRepository @Inject constructor(
    private val songDao: SongDao,
    private val oneDriveService: OneDriveService,
    private val playlistRepository: PlaylistRepository,
    @ApplicationContext private val context: Context
) {
    /** Whether a sync operation is in progress */
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    /** Last sync error message, null if last sync was successful */
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    private val _debugLog = MutableStateFlow<String>("Ready to sync")
    val debugLog: StateFlow<String> = _debugLog.asStateFlow()

    /** Cache of download URLs with expiry timestamps */
    private val urlCache = mutableMapOf<String, CachedUrl>()

    // ── Read Operations (all from Room) ──

    fun getAllSongs(): Flow<List<SongEntity>> = songDao.getAllSongs()
    fun getAllSongsByArtist(): Flow<List<SongEntity>> = songDao.getAllSongsByArtist()
    fun getAllSongsByAlbum(): Flow<List<SongEntity>> = songDao.getAllSongsByAlbum()
    fun getAllSongsByDateAdded(): Flow<List<SongEntity>> = songDao.getAllSongsByDateAdded()
    fun getMostPlayedSongs(): Flow<List<SongEntity>> = songDao.getMostPlayedSongs()
    fun getRecentlyPlayed(): Flow<List<SongEntity>> = songDao.getRecentlyPlayed()
    fun getFavoriteSongs(): Flow<List<SongEntity>> = songDao.getFavoriteSongs()
    fun getDownloadedSongs(): Flow<List<SongEntity>> = songDao.getDownloadedSongs()
    fun searchSongs(query: String): Flow<List<SongEntity>> = songDao.searchSongs(query)
    fun getSongCount(): Flow<Int> = songDao.getSongCount()
    fun getDownloadedSize(): Flow<Long> = songDao.getDownloadedSize()
    fun getAllArtists(): Flow<List<String>> = songDao.getAllArtists()
    fun getAllAlbums(): Flow<List<String>> = songDao.getAllAlbums()

    suspend fun getSongById(id: String): SongEntity? = songDao.getSongById(id)

    // ── Sync Operations ──

    /**
     * Sync music files from OneDrive to the local Room database.
     * @param folderPath The OneDrive folder to scan
     */
    suspend fun syncFromOneDrive(folderPath: String) {
        if (_isSyncing.value) return

        _isSyncing.value = true
        _syncError.value = null
        _debugLog.value = "Starting sync for folder: $folderPath"

        try {
            val result = oneDriveService.listAudioFiles(folderPath, onDebugLog = {
                _debugLog.value = it
            })
            result.fold(
                onSuccess = { syncResult ->
                    val remoteSongs = syncResult.songs
                    val remotePlaylists = syncResult.playlists
                    _debugLog.value = "Found ${remoteSongs.size} songs in OneDrive. Saving to database..."
                    
                    // Preserve local-only data (localPath, playCount, favorites, etc.)
                    val mergedSongs = remoteSongs.map { remoteSong ->
                        val existingSong = songDao.getSongById(remoteSong.oneDriveId)
                        if (existingSong != null) {
                            remoteSong.copy(
                                localPath = existingSong.localPath,
                                playCount = existingSong.playCount,
                                lastPlayedAt = existingSong.lastPlayedAt,
                                isFavorite = existingSong.isFavorite,
                                albumArtUrl = existingSong.albumArtUrl
                            )
                        } else {
                            remoteSong
                        }
                    }

                    // Upsert all songs
                    songDao.insertAll(mergedSongs)
                    _debugLog.value = "Database save complete. Validating..."

                    // Remove songs that no longer exist in OneDrive
                    val validIds = remoteSongs.map { it.oneDriveId }
                    if (validIds.isNotEmpty()) {
                        songDao.deleteNotIn(validIds)
                    } else {
                        songDao.deleteAllOneDriveSongs()
                    }

                    // Clean up orphaned downloaded files to fix app size bloat
                    val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                        ?: java.io.File(context.filesDir, "music")
                    
                    // Create .nomedia so Android MediaStore ignores these files (prevents duplicates)
                    java.io.File(downloadDir, ".nomedia").createNewFile()
                    
                    downloadDir.listFiles()?.forEach { file ->
                        val fileNameWithoutExt = file.nameWithoutExtension
                        if (file.name != ".nomedia" && !validIds.contains(fileNameWithoutExt)) {
                            file.delete()
                        }
                    }

                    // Process Playlists
                    if (remotePlaylists.isNotEmpty()) {
                        _debugLog.value = "Processing ${remotePlaylists.size} playlists..."
                        for (playlist in remotePlaylists) {
                            val contentResult = oneDriveService.getFileContent(playlist.oneDriveId)
                            if (contentResult.isSuccess) {
                                val content = contentResult.getOrNull()!!
                                val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }
                                
                                val playlistId = playlistRepository.createPlaylist(playlist.name)
                                val songIds = mutableListOf<String>()
                                
                                for (line in lines) {
                                    val rawFilename = line.replace("\\", "/").substringAfterLast("/").trim()
                                    val decodedFilename = try { 
                                        java.net.URLDecoder.decode(rawFilename, "UTF-8") 
                                    } catch(e: Exception) { 
                                        rawFilename 
                                    }
                                    
                                    val matchedSong = remoteSongs.find { 
                                        it.oneDrivePath.endsWith(decodedFilename, ignoreCase = true) ||
                                        it.oneDrivePath.endsWith(rawFilename, ignoreCase = true)
                                    }
                                    
                                    if (matchedSong != null) {
                                        songIds.add(matchedSong.oneDriveId)
                                    }
                                }
                                
                                playlistRepository.clearPlaylist(playlistId)
                                if (songIds.isNotEmpty()) {
                                    playlistRepository.addSongsToPlaylist(playlistId, songIds)
                                }
                            }
                        }
                    }

                    _debugLog.value = "Sync fully complete! (${remoteSongs.size} songs)"
                },
                onFailure = { error ->
                    _syncError.value = error.message ?: "Sync failed"
                    _debugLog.value = "Sync failed: ${error.message}"
                }
            )
        } catch (e: Exception) {
            _syncError.value = e.message ?: "Sync failed"
            _debugLog.value = "Exception during sync: ${e.message}"
        } finally {
            _isSyncing.value = false
        }
    }

    /**
     * Sync local music from the device's MediaStore.
     */
    suspend fun syncLocalMusic() = withContext(Dispatchers.IO) {
        _isSyncing.value = true
        _debugLog.value = "Scanning local music..."
        
        try {
            val localSongs = mutableListOf<SongEntity>()
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.MIME_TYPE,
                MediaStore.Audio.Media.DATE_MODIFIED
            )
            
            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
            
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                null
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: ""
                    val duration = cursor.getLong(durationCol)
                    val size = cursor.getLong(sizeCol)
                    val data = cursor.getString(dataCol) ?: ""
                    val mimeType = cursor.getString(mimeCol) ?: "audio/mpeg"
                    val dateModified = cursor.getLong(dateCol) * 1000L

                    // Ignore files downloaded by the app itself to prevent duplicates
                    if (data.isNotEmpty() && !data.contains(context.packageName)) {
                        val extension = data.substringAfterLast('.', "").lowercase()
                        val oneDriveId = "local_$id"
                        val existingSong = songDao.getSongById(oneDriveId)
                        
                        val song = SongEntity(
                            oneDriveId = oneDriveId,
                            title = title,
                            artist = artist,
                            album = album,
                            duration = duration,
                            fileSize = size,
                            oneDrivePath = "Local Library",
                            localPath = data,
                            albumArtUrl = "content://media/external/audio/media/$id",
                            mimeType = mimeType,
                            fileExtension = extension,
                            dateModified = dateModified,
                            lastSynced = System.currentTimeMillis(),
                            playCount = existingSong?.playCount ?: 0,
                            lastPlayedAt = existingSong?.lastPlayedAt,
                            isFavorite = existingSong?.isFavorite ?: false
                        )
                        localSongs.add(song)
                    }
                }
            }
            
            _debugLog.value = "Found ${localSongs.size} local songs. Saving..."
            songDao.insertAll(localSongs)
            
            val validLocalIds = localSongs.map { it.oneDriveId }
            if (validLocalIds.isNotEmpty()) {
                songDao.deleteLocalNotIn(validLocalIds)
            } else {
                songDao.deleteAllLocalSongs()
            }
            
        } catch (e: Exception) {
            _syncError.value = e.message ?: "Local sync failed"
            _debugLog.value = "Local sync error: ${e.message}"
        } finally {
            _isSyncing.value = false
        }
    }

    // ── Playback URL Management ──

    /**
     * Get a streaming URL for a song. Uses cached URL if still valid,
     * otherwise fetches a fresh one from OneDrive.
     */
    suspend fun getStreamingUrl(song: SongEntity): Result<String> {
        // If song is downloaded, use local file
        if (song.isDownloaded && song.localPath != null) {
            return Result.success(song.localPath)
        }

        // Check URL cache (URLs are valid for ~1 hour, we refresh at 50 min)
        val cached = urlCache[song.oneDriveId]
        if (cached != null && !cached.isExpired()) {
            return Result.success(cached.url)
        }

        // Fetch fresh URL
        val result = oneDriveService.getDownloadUrl(song.oneDriveId)
        result.onSuccess { url ->
            urlCache[song.oneDriveId] = CachedUrl(url)
        }

        return result
    }

    /**
     * Pre-fetch the streaming URL for the next song in the queue.
     * Called while the current song is playing to eliminate delay.
     */
    suspend fun prefetchStreamingUrl(song: SongEntity) {
        if (song.isDownloaded) return

        val cached = urlCache[song.oneDriveId]
        if (cached != null && !cached.isExpired()) return

        oneDriveService.getDownloadUrl(song.oneDriveId).onSuccess { url ->
            urlCache[song.oneDriveId] = CachedUrl(url)
        }
    }

    // ── Song Metadata Updates ──

    suspend fun recordPlay(songId: String) = songDao.recordPlay(songId)
    suspend fun toggleFavorite(songId: String) = songDao.toggleFavorite(songId)
    suspend fun setDownloadPath(songId: String, path: String) = songDao.setDownloadPath(songId, path)
    suspend fun clearDownloadPath(songId: String) = songDao.clearDownloadPath(songId)

    /**
     * Cached download URL with expiry tracking.
     * OneDrive URLs are valid for ~1 hour; we refresh at 50 minutes.
     */
    private data class CachedUrl(
        val url: String,
        val fetchedAt: Long = System.currentTimeMillis()
    ) {
        fun isExpired(): Boolean {
            val fiftyMinutes = 50 * 60 * 1000L
            return System.currentTimeMillis() - fetchedAt > fiftyMinutes
        }
    }
}
