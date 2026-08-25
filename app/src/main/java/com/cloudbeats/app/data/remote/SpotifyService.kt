package com.cloudbeats.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SpotifyService @Inject constructor(
    private val okHttpClient: OkHttpClient
) {
    companion object {
        // Change this to the IP of your local PC if testing on a real device (e.g., http://192.168.1.5:5000)
        // 10.0.2.2 is the localhost alias for the Android Emulator
        private const val BACKEND_URL = "https://cloudbeats-api.onrender.com"
    }

    /**
     * Send a Spotify URL to the local backend to download and upload to OneDrive.
     */
    suspend fun downloadSpotifyLink(spotifyUrl: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val jsonPayload = JSONObject().apply {
                    put("url", spotifyUrl)
                }

                val requestBody = jsonPayload.toString()
                    .toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url("$BACKEND_URL/download")
                    .post(requestBody)
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    Result.success("Download started successfully on backend.")
                } else {
                    Result.failure(Exception("Backend error: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Search for songs online using the backend.
     */
    suspend fun searchOnline(query: String): Result<List<OnlineSong>> =
        withContext(Dispatchers.IO) {
            try {
                val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
                val request = Request.Builder()
                    .url("https://itunes.apple.com/search?term=$encodedQuery&media=music&limit=20")
                    .get()
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body?.string() ?: ""
                    val jsonResponse = JSONObject(responseBody)
                    val resultsArray = jsonResponse.optJSONArray("results")
                    
                    val songs = mutableListOf<OnlineSong>()
                    if (resultsArray != null) {
                        for (i in 0 until resultsArray.length()) {
                            val obj = resultsArray.getJSONObject(i)
                            val title = obj.optString("trackName", obj.optString("collectionName", "Unknown Title"))
                            val artist = obj.optString("artistName", "Unknown Artist")
                            // Get a higher quality album art (600x600 instead of 100x100)
                            val albumArt = obj.optString("artworkUrl100", "").replace("100x100bb", "600x600bb")
                            
                            // Prevent duplicates in the results
                            if (songs.none { it.title == title && it.artist == artist }) {
                                songs.add(
                                    OnlineSong(
                                        title = title,
                                        artist = artist,
                                        albumArt = albumArt,
                                        url = "ytsearch1: $artist $title audio"
                                    )
                                )
                            }
                        }
                    }
                    Result.success(songs)
                } else {
                    Result.failure(Exception("Backend error: ${response.code}"))
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}

data class OnlineSong(
    val title: String,
    val artist: String,
    val albumArt: String,
    val url: String
)
