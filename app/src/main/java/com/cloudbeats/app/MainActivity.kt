package com.cloudbeats.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.cloudbeats.app.auth.AuthManager
import com.cloudbeats.app.auth.AuthState
import com.cloudbeats.app.player.PlaybackManager
import com.cloudbeats.app.ui.navigation.MainNavGraph
import com.cloudbeats.app.ui.screens.SignInScreen
import com.cloudbeats.app.ui.theme.CloudBeatsTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single activity for the entire app.
 * Shows sign-in screen when unauthenticated, main nav when signed in.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authManager: AuthManager

    @Inject
    lateinit var playbackManager: PlaybackManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize auth and player
        authManager.initialize()
        playbackManager.initialize()

        setContent {
            CloudBeatsTheme(darkTheme = true) {
                val authState by authManager.authState.collectAsState()

                when (authState) {
                    is AuthState.SignedIn -> {
                        val navController = rememberNavController()
                        MainNavGraph(
                            navController = navController,
                            playbackManager = playbackManager
                        )
                    }
                    else -> {
                        SignInScreen(
                            authState = authState,
                            authManager = authManager
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playbackManager.release()
    }
}
