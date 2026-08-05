package com.cloudbeats.app.download

import android.content.Context
import android.os.Environment
import com.cloudbeats.app.data.local.entities.SongEntity
import com.cloudbeats.app.data.remote.OneDriveService
import com.cloudbeats.app.data.repository.MusicRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages downloading songs from OneDrive for offline playback.
 * Downloads are stored in the app's external files directory.
 */
@Singleton
class DownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val oneDriveService: OneDriveService,
    private val musicRepository: MusicRepository,
    private val okHttpClient: OkHttpClient
) {
    /** Active download states keyed by song OneDrive ID */
    private val _activeDownloads = MutableStateFlow<Map<String, DownloadState>>(emptyMap())
    val activeDownloads: StateFlow<Map<String, DownloadState>> = _activeDownloads.asStateFlow()

    /** Base directory for downloaded music files */
    private val downloadDir: File
        get() = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            ?: File(context.filesDir, "music")

    /**
     * Download a song for offline playback.
     */
    suspend fun downloadSong(song: SongEntity): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                // Update state to downloading
                updateDownloadState(song.oneDriveId, DownloadState.Downloading(0f))

                // Get download URL
                val urlResult = oneDriveService.getDownloadUrl(song.oneDriveId)
                val downloadUrl = urlResult.getOrElse {
                    updateDownloadState(song.oneDriveId, DownloadState.Error(it.message ?: "Failed to get URL"))
                    return@withContext Result.failure(it)
                }

                // Create file
                val fileName = "${song.oneDriveId}.${song.fileExtension}"
                val file = File(downloadDir, fileName)
                downloadDir.mkdirs()

                // Download with progress
                val request = Request.Builder().url(downloadUrl).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    val error = "Download failed: ${response.code}"
                    updateDownloadState(song.oneDriveId, DownloadState.Error(error))
                    return@withContext Result.failure(Exception(error))
                }

                val body = response.body ?: run {
                    val error = "Empty response body"
                    updateDownloadState(song.oneDriveId, DownloadState.Error(error))
                    return@withContext Result.failure(Exception(error))
                }

                val totalBytes = body.contentLength()
                var downloadedBytes = 0L

                FileOutputStream(file).use { output ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            output.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead

                            if (totalBytes > 0) {
                                val progress = downloadedBytes.toFloat() / totalBytes
                                updateDownloadState(song.oneDriveId, DownloadState.Downloading(progress))
                            }
                        }
                    }
                }

                // Update database with local path
                val localPath = file.absolutePath
                musicRepository.setDownloadPath(song.oneDriveId, localPath)

                // Mark as completed
                updateDownloadState(song.oneDriveId, DownloadState.Completed)

                // Remove from active downloads after a delay
                kotlinx.coroutines.delay(2000)
                removeDownloadState(song.oneDriveId)

                Result.success(localPath)
            } catch (e: Exception) {
                updateDownloadState(song.oneDriveId, DownloadState.Error(e.message ?: "Download failed"))
                Result.failure(e)
            }
        }

    /**
     * Delete a downloaded song file and clear the database reference.
     */
    suspend fun deleteDownload(song: SongEntity) = withContext(Dispatchers.IO) {
        song.localPath?.let { path ->
            File(path).delete()
        }
        musicRepository.clearDownloadPath(song.oneDriveId)
    }

    /**
     * Delete all downloaded files and clear all download paths.
     */
    suspend fun deleteAllDownloads() = withContext(Dispatchers.IO) {
        downloadDir.listFiles()?.forEach { it.delete() }
        // The database paths will be cleared on next sync
    }

    /**
     * Get the total size of downloaded files in bytes.
     */
    suspend fun getTotalDownloadedSize(): Long = withContext(Dispatchers.IO) {
        downloadDir.listFiles()?.sumOf { it.length() } ?: 0L
    }

    private fun updateDownloadState(songId: String, state: DownloadState) {
        _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
            put(songId, state)
        }
    }

    private fun removeDownloadState(songId: String) {
        _activeDownloads.value = _activeDownloads.value.toMutableMap().apply {
            remove(songId)
        }
    }
}

/**
 * Represents the state of a song download.
 */
sealed class DownloadState {
    data class Downloading(val progress: Float) : DownloadState()
    data object Completed : DownloadState()
    data class Error(val message: String) : DownloadState()
}
