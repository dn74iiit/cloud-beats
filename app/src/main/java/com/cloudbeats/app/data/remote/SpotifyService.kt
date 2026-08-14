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
        private const val BACKEND_URL = "http://10.0.2.2:5000"
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
}
