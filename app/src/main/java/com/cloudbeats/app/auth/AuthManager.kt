package com.cloudbeats.app.auth

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.net.SocketTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages Microsoft account authentication using a custom OAuth 2.0 implementation
 * that utilizes the public `rclone` Client ID. This bypasses the need for the user
 * to register their own Azure application.
 */
@Singleton
class AuthManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient
) {
    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _accessToken = MutableStateFlow<String?>(null)
    val accessToken: StateFlow<String?> = _accessToken.asStateFlow()

    private val prefs = context.getSharedPreferences("auth_prefs", Context.MODE_PRIVATE)
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var serverSocket: ServerSocket? = null

    companion object {
        private const val TAG = "AuthManager"
        // Using Microsoft Graph PowerShell public Client ID to bypass Azure registration
        private const val CLIENT_ID = "14d82eec-204b-4c2f-b7e8-296a70dab67e"
        private const val REDIRECT_URI = "http://localhost:53682/"
        private const val SCOPES = "Files.Read User.Read offline_access"
        private const val AUTH_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize"
        private const val TOKEN_URL = "https://login.microsoftonline.com/common/oauth2/v2.0/token"
        private const val PORT = 53682

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_USER_NAME = "user_name"
    }

    /**
     * Initialize auth state from SharedPreferences.
     */
    fun initialize() {
        val savedToken = prefs.getString(KEY_ACCESS_TOKEN, null)
        val savedRefreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        val savedName = prefs.getString(KEY_USER_NAME, "User") ?: "User"

        if (savedToken != null && savedRefreshToken != null) {
            _accessToken.value = savedToken
            _authState.value = AuthState.SignedIn(savedName, "", "")
            // Proactively refresh the token
            refreshToken()
        } else {
            _authState.value = AuthState.SignedOut
        }
    }

    /**
     * Initiate interactive sign-in flow.
     * Launches browser and starts local socket to catch the redirect.
     */
    fun signIn(context: Context) {
        _authState.value = AuthState.Loading

        val authUri = Uri.parse(AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", CLIENT_ID)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", REDIRECT_URI)
            .appendQueryParameter("scope", SCOPES)
            .build()

        // Launch browser
        val intent = Intent(Intent.ACTION_VIEW, authUri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        
        try {
            context.startActivity(intent)
            startLocalServer()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch browser", e)
            _authState.value = AuthState.Error("No web browser found on device.")
        }
    }

    /**
     * Starts a temporary local web server on port 53682 to intercept the OAuth callback.
     */
    private fun startLocalServer() {
        scope.launch {
            try {
                // Close any existing socket just in case
                serverSocket?.close()
                serverSocket = ServerSocket(PORT).apply {
                    soTimeout = 120000 // 2 minute timeout
                }

                Log.d(TAG, "Waiting for OAuth callback on port $PORT...")
                var code: String? = null
                var error: String? = null
                
                // Loop to handle potential multiple requests (like favicon.ico)
                while (code == null && error == null) {
                    val socket = serverSocket!!.accept()
                    Log.d(TAG, "Received connection")

                    val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val requestLine: String? = reader.readLine()
                    
                    if (requestLine != null && requestLine.startsWith("GET ")) {
                        val parts = requestLine.split(" ")
                        if (parts.size >= 2) {
                            val path = parts[1]
                            
                            // Ignore favicon requests
                            if (path.contains("favicon.ico")) {
                                socket.close()
                                continue
                            }

                            val uri = Uri.parse("http://localhost$path")
                            code = uri.getQueryParameter("code")
                            error = uri.getQueryParameter("error") ?: uri.getQueryParameter("error_description")

                            // Send response to browser
                            val responseHtml = if (code != null) {
                                """
                                HTTP/1.1 200 OK
                                Content-Type: text/html
                                Connection: close
                                
                                <html>
                                <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
                                    <h1 style="color: #4CAF50;">Authentication Successful!</h1>
                                    <p>You can close this window and return to CloudBeats.</p>
                                    <script>window.close();</script>
                                </body>
                                </html>
                                """.trimIndent()
                            } else {
                                """
                                HTTP/1.1 400 Bad Request
                                Content-Type: text/html
                                Connection: close
                                
                                <html>
                                <body style="font-family: sans-serif; text-align: center; padding-top: 50px;">
                                    <h1 style="color: #F44336;">Authentication Failed</h1>
                                    <p>${error ?: "Unknown error"}</p>
                                    <p>Please close this window and try again.</p>
                                </body>
                                </html>
                                """.trimIndent()
                            }
                            
                            socket.getOutputStream().write(responseHtml.toByteArray())
                            socket.getOutputStream().flush()
                        }
                    }
                    socket.close()
                }

                serverSocket?.close()

                if (code != null) {
                    exchangeCodeForToken(code)
                } else {
                    _authState.value = AuthState.Error("Failed to retrieve authorization code: ${error ?: "Unknown reason"}")
                }

            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "OAuth timeout", e)
                _authState.value = AuthState.Error("Sign in timed out. Please try again.")
            } catch (e: Exception) {
                Log.e(TAG, "Local server error", e)
                if (e.message?.contains("Socket closed") != true) {
                    _authState.value = AuthState.Error("Local server error: ${e.message}")
                }
            } finally {
                serverSocket?.close()
            }
        }
    }

    /**
     * Exchanges the authorization code for access and refresh tokens.
     */
    private suspend fun exchangeCodeForToken(code: String) {
        val formBody = FormBody.Builder()
            .add("client_id", CLIENT_ID)
            .add("redirect_uri", REDIRECT_URI)
            .add("grant_type", "authorization_code")
            .add("code", code)
            .build()

        val request = Request.Builder()
            .url(TOKEN_URL)
            .post(formBody)
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val json = JSONObject(responseData)
                    val accessToken = json.getString("access_token")
                    val refreshToken = json.getString("refresh_token")
                    
                    saveTokens(accessToken, refreshToken)
                    
                    // Fetch user profile
                    fetchUserProfile(accessToken)
                }
            } else {
                val errorBody = response.body?.string()
                Log.e(TAG, "Token exchange failed: ${response.code} $errorBody")
                _authState.value = AuthState.Error("Failed to acquire token: ${response.code}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error during token exchange", e)
            _authState.value = AuthState.Error("Network error: ${e.message}")
        }
    }

    /**
     * Refreshes the access token using the saved refresh token.
     */
    fun refreshToken() {
        val savedRefreshToken = prefs.getString(KEY_REFRESH_TOKEN, null)
        if (savedRefreshToken == null) {
            _authState.value = AuthState.SignedOut
            return
        }

        scope.launch {
            val formBody = FormBody.Builder()
                .add("client_id", CLIENT_ID)
                .add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "refresh_token")
                .add("refresh_token", savedRefreshToken)
                .build()

            val request = Request.Builder()
                .url(TOKEN_URL)
                .post(formBody)
                .build()

            try {
                val response = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    if (responseData != null) {
                        val json = JSONObject(responseData)
                        val accessToken = json.getString("access_token")
                        // Microsoft might issue a new refresh token
                        val newRefreshToken = json.optString("refresh_token", savedRefreshToken)
                        
                        saveTokens(accessToken, newRefreshToken)
                        
                        val savedName = prefs.getString(KEY_USER_NAME, "User") ?: "User"
                        _accessToken.value = accessToken
                        _authState.value = AuthState.SignedIn(savedName, "", "")
                    }
                } else {
                    Log.e(TAG, "Token refresh failed: ${response.code}")
                    // Refresh token is likely expired or revoked
                    signOut()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error during token refresh", e)
                // Don't sign out immediately on network error, keep current state
                if (_authState.value !is AuthState.SignedIn) {
                    _authState.value = AuthState.Error("Network error: ${e.message}")
                }
            }
        }
    }

    /**
     * Fetches basic user profile info from Microsoft Graph.
     */
    private suspend fun fetchUserProfile(accessToken: String) {
        val request = Request.Builder()
            .url("https://graph.microsoft.com/v1.0/me")
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        try {
            val response = withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute()
            }

            if (response.isSuccessful) {
                val responseData = response.body?.string()
                if (responseData != null) {
                    val json = JSONObject(responseData)
                    val displayName = json.optString("displayName", "User")
                    val email = json.optString("userPrincipalName", "")
                    
                    prefs.edit {
                        putString(KEY_USER_NAME, displayName)
                    }
                    
                    _accessToken.value = accessToken
                    _authState.value = AuthState.SignedIn(displayName, email, "")
                }
            } else {
                // We have the token, so we are signed in, even if profile fetch failed
                val savedName = prefs.getString(KEY_USER_NAME, "User") ?: "User"
                _accessToken.value = accessToken
                _authState.value = AuthState.SignedIn(savedName, "", "")
            }
        } catch (e: Exception) {
            val savedName = prefs.getString(KEY_USER_NAME, "User") ?: "User"
            _accessToken.value = accessToken
            _authState.value = AuthState.SignedIn(savedName, "", "")
        }
    }

    private fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, accessToken)
            putString(KEY_REFRESH_TOKEN, refreshToken)
        }
    }

    /**
     * Sign out the current user and clear cached tokens.
     */
    fun signOut() {
        prefs.edit { clear() }
        _accessToken.value = null
        _authState.value = AuthState.SignedOut
    }
}
