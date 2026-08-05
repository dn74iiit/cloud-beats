package com.cloudbeats.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cloudbeats.app.auth.AuthManager
import com.cloudbeats.app.auth.AuthState
import com.cloudbeats.app.ui.theme.GradientEnd
import com.cloudbeats.app.ui.theme.GradientMid
import com.cloudbeats.app.ui.theme.GradientStart
import com.cloudbeats.app.ui.theme.Purple60
import com.cloudbeats.app.ui.theme.TextSecondary

/**
 * Sign-in screen shown when the user is not authenticated.
 * Displays the app branding and a Microsoft sign-in button.
 */
@Composable
fun SignInScreen(
    authState: AuthState,
    authManager: AuthManager
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(GradientStart, GradientMid, GradientEnd)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App icon
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = Purple60,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // App name
            Text(
                text = "CloudBeats",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Stream your music from OneDrive",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            when (authState) {
                is AuthState.Loading -> {
                    CircularProgressIndicator(color = Purple60)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Checking account...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
                is AuthState.SignedOut -> {
                    Button(
                        onClick = { authManager.signIn(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple60),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Sign in with Microsoft",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                is AuthState.Error -> {
                    Text(
                        text = authState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { authManager.signIn(context) },
                        colors = ButtonDefaults.buttonColors(containerColor = Purple60),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = "Try Again",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                else -> {} // SignedIn handled in MainActivity
            }
        }
    }
}
