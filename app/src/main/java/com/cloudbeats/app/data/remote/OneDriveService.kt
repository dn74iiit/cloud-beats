package com.cloudbeats.app.data.remote

import com.cloudbeats.app.auth.AuthManager
import com.cloudbeats.app.data.local.entities.SongEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class PlaylistFile(
    val oneDriveId: String,
    val name: String,
    val oneDrivePath: String
)

data class SyncResult(
    val songs: List<SongEntity>,
    val playlists: List<PlaylistFile>
)

/**
 * Service for interacting with OneDrive via Microsoft Graph API.
 * Handles listing audio files, fetching download URLs, and file metadata.
 */
@Singleton
class OneDriveService @Inject constructor(
    private val authManager: AuthManager,
    private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val GRAPH_BASE_URL = "https://graph.microsoft.com/v1.0"

        /** Audio file extensions we support */
        val AUDIO_EXTENSIONS = setOf(
            "mp3", "aac", "flac", "ogg", "wav", "m4a", "wma", "opus"
        )
        val PLAYLIST_EXTENSIONS = setOf("m3u", "m3u8", "pls", "wpl")
    }

    /**
     * List all audio files in the specified OneDrive folder.
     * @param folderPath The folder path in OneDrive (e.g., "/spotify_downloads")
     * @return List of SongEntity objects with metadata from OneDrive
     */
    suspend fun listAudioFiles(
        folderPath: String,
        onDebugLog: ((String) -> Unit)? = null
    ): Result<SyncResult> =
        withContext(Dispatchers.IO) {
            try {
                val token = authManager.accessToken.value
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val songs = mutableListOf<SongEntity>()
                val playlists = mutableListOf<PlaylistFile>()
                val foldersToScan = mutableListOf(folderPath)

                var totalFoldersScanned = 0
                
                while (foldersToScan.isNotEmpty()) {
                    val currentFolder = foldersToScan.removeAt(0)
                    totalFoldersScanned++
                    onDebugLog?.invoke("Scanning folder $totalFoldersScanned: $currentFolder (found ${songs.size} songs so far)")
                    
                    var nextLink: String? = buildFolderUrl(currentFolder)

                    // Handle pagination for the current folder
                    while (nextLink != null) {
                        val request = Request.Builder()
                            .url(nextLink)
                            .addHeader("Authorization", "Bearer $token")
                            .build()

                        val response = okHttpClient.newCall(request).execute()
                        if (!response.isSuccessful) {
                            if (response.code == 404) {
                                // If the very first (root) folder doesn't exist, show an error
                                if (currentFolder == folderPath) {
                                    return@withContext Result.failure(Exception("Folder '$folderPath' not found in OneDrive."))
                                }
                                // Otherwise, just skip this subfolder
                                break
                            }
                            return@withContext Result.failure(
                                Exception("API error: ${response.code} ${response.message}")
                            )
                        }

                        val json = JSONObject(response.body?.string() ?: "{}")
                        val items = json.optJSONArray("value") ?: break

                        for (i in 0 until items.length()) {
                            val item = items.getJSONObject(i)
                            
                            // If it's a folder, add it to our scan list
                            if (item.has("folder")) {
                                val folderName = item.getString("name")
                                val newPath = if (currentFolder.endsWith("/")) "$currentFolder$folderName" else "$currentFolder/$folderName"
                                foldersToScan.add(newPath)
                            } else {
                                // Attempt to parse as a song or playlist
                                val song = parseFileItem(item, currentFolder)
                                if (song != null) {
                                    songs.add(song)
                                } else {
                                    val playlist = parsePlaylistItem(item, currentFolder)
                                    if (playlist != null) playlists.add(playlist)
                                }
                            }
                        }

                        // Check for next page of items in this folder
                        nextLink = json.optString("@odata.nextLink", null)
                    }
                }

                Result.success(SyncResult(songs, playlists))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get a temporary download URL for streaming a file.
     * The URL is typically valid for about 1 hour.
     * @param itemId The OneDrive item ID
     * @return The download URL string
     */
    suspend fun getDownloadUrl(itemId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val token = authManager.accessToken.value
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                val url = "$GRAPH_BASE_URL/me/drive/items/$itemId"
                val request = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .build()

                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(
                        Exception("API error: ${response.code}")
                    )
                }

                val json = JSONObject(response.body?.string() ?: "{}")
                val downloadUrl = json.optString("@microsoft.graph.downloadUrl", "")

                if (downloadUrl.isNotEmpty()) {
                    Result.success(downloadUrl)
                } else {
                    Result.failure(Exception("No download URL available"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get string content of a file (useful for reading text-based playlists).
     */
    suspend fun getFileContent(itemId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val urlResult = getDownloadUrl(itemId)
                if (urlResult.isFailure) return@withContext Result.failure(urlResult.exceptionOrNull()!!)
                
                val request = Request.Builder()
                    .url(urlResult.getOrNull()!!)
                    .build()
                
                val response = okHttpClient.newCall(request).execute()
                if (!response.isSuccessful) return@withContext Result.failure(Exception("Failed to download file"))
                
                Result.success(response.body?.string() ?: "")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get file content as a byte stream URL for downloading.
     * @param itemId The OneDrive item ID
     * @return The content download URL
     */
    suspend fun getContentUrl(itemId: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val token = authManager.accessToken.value
                    ?: return@withContext Result.failure(Exception("Not authenticated"))

                // The /content endpoint redirects to the actual download URL
                Result.success("$GRAPH_BASE_URL/me/drive/items/$itemId/content")
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Build the Graph API URL for listing children of a folder.
     */
    private fun buildFolderUrl(folderPath: String): String {
        val encodedPath = folderPath.trimStart('/').split('/').joinToString("/") { segment ->
            java.net.URLEncoder.encode(segment, "UTF-8").replace("+", "%20")
        }
        return "$GRAPH_BASE_URL/me/drive/root:/$encodedPath:/children?" +
                "\$select=id,name,size,file,folder,audio,lastModifiedDateTime,parentReference&" +
                "\$expand=thumbnails&" +
                "\$top=200"
    }

    /**
     * Parse a Graph API DriveItem JSON into a SongEntity.
     * Returns null if the item is not a supported audio file.
     */
    private fun parseFileItem(item: JSONObject, folderPath: String): SongEntity? {
        // Skip folders
        if (!item.has("file")) return null

        val name = item.getString("name")
        val extension = name.substringAfterLast('.', "").lowercase()

        // Filter to supported audio files only
        if (extension !in AUDIO_EXTENSIONS) return null

        val id = item.getString("id")
        val size = item.optLong("size", 0L)
        val mimeType = item.optJSONObject("file")?.optString("mimeType", "audio/mpeg") ?: "audio/mpeg"

        // Extract audio metadata if available (ID3 tags parsed by OneDrive)
        val audio = item.optJSONObject("audio")
        val title = audio?.optString("title", "")?.takeIf { it.isNotBlank() }
            ?: name.substringBeforeLast('.')
        val artist = audio?.optString("artist", "")?.takeIf { it.isNotBlank() }
            ?: "Unknown Artist"
        val album = audio?.optString("album", "")?.takeIf { it.isNotBlank() } ?: ""
        val duration = audio?.optLong("duration", 0L)?.times(1000) ?: 0L // Graph returns seconds

        // Extract album art thumbnail if available
        val albumArtUrl = item.optJSONArray("thumbnails")
            ?.optJSONObject(0)
            ?.optJSONObject("medium")
            ?.optString("url")
            ?.takeIf { it.isNotBlank() }

        return SongEntity(
            oneDriveId = id,
            title = title,
            artist = artist,
            album = album,
            duration = duration,
            fileSize = size,
            oneDrivePath = "$folderPath/$name",
            albumArtUrl = albumArtUrl,
            mimeType = mimeType,
            fileExtension = extension,
            lastSynced = System.currentTimeMillis()
        )
    }

    private fun parsePlaylistItem(item: JSONObject, folderPath: String): PlaylistFile? {
        if (!item.has("file")) return null
        val name = item.getString("name")
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension !in PLAYLIST_EXTENSIONS) return null
        return PlaylistFile(
            oneDriveId = item.getString("id"),
            name = name.substringBeforeLast('.'),
            oneDrivePath = "$folderPath/$name"
        )
    }
}
