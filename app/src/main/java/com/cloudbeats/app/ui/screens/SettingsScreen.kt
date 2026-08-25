package com.cloudbeats.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.cloudbeats.app.BuildConfig
import com.cloudbeats.app.update.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.cloudbeats.app.auth.AuthState
import com.cloudbeats.app.ui.theme.DarkSurfaceVariant
import com.cloudbeats.app.ui.theme.Purple60
import com.cloudbeats.app.ui.viewmodels.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateMessage by remember { mutableStateOf("") }
    var updateUrl by remember { mutableStateOf<String?>(null) }
    var isCheckingUpdate by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineSmall
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Account Section
            SectionHeader("Account")

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (val state = authState) {
                        is AuthState.SignedIn -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    tint = Purple60,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = state.displayName,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = state.email,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { viewModel.signOut() }) {
                                Icon(
                                    imageVector = Icons.Default.Logout,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        is AuthState.SignedOut -> {
                            Text(
                                text = "Not signed in",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        else -> {
                            Text(
                                text = "Loading...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Storage Section
            SectionHeader("Storage")

            SettingItem(
                icon = Icons.Default.Storage,
                title = "Downloaded Songs",
                subtitle = "Manage offline storage"
            )

            SettingItem(
                icon = Icons.Default.Folder,
                title = "OneDrive Folder",
                subtitle = "spotify_downloads"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // About Section
            SectionHeader("About")

            SettingItem(
                icon = Icons.Default.Info,
                title = "Version",
                subtitle = BuildConfig.VERSION_NAME,
                onClick = {
                    Toast.makeText(context, "CloudBeats v${BuildConfig.VERSION_NAME}", Toast.LENGTH_SHORT).show()
                }
            )

            SettingItem(
                icon = Icons.Default.SystemUpdate,
                title = "Check for Updates",
                subtitle = if (isCheckingUpdate) "Checking..." else "Tap to check for new releases",
                onClick = {
                    if (!isCheckingUpdate) {
                        isCheckingUpdate = true
                        coroutineScope.launch {
                            try {
                                val client = OkHttpClient()
                                val request = Request.Builder()
                                    .url("https://api.github.com/repos/dn74iiit/cloud-beats/releases/latest")
                                    .build()
                                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                                if (response.isSuccessful) {
                                    val json = JSONObject(response.body?.string() ?: "{}")
                                    val tagName = json.optString("tag_name", "")
                                    val htmlUrl = json.optString("html_url", "")
                                    
                                    var apkUrl: String? = null
                                    val assets = json.optJSONArray("assets")
                                    if (assets != null) {
                                        for (i in 0 until assets.length()) {
                                            val asset = assets.getJSONObject(i)
                                            if (asset.optString("name").endsWith(".apk")) {
                                                apkUrl = asset.optString("browser_download_url")
                                                break
                                            }
                                        }
                                    }

                                    val currentVersion = BuildConfig.VERSION_NAME
                                    val currentVersionCode = "v$currentVersion"
                                    if (tagName.isNotEmpty() && !tagName.startsWith(currentVersion) && !tagName.startsWith(currentVersionCode)) {
                                        updateMessage = "A new version ($tagName) is available!"
                                        updateUrl = apkUrl ?: htmlUrl
                                    } else {
                                        updateMessage = "You are up to date."
                                        updateUrl = null
                                    }
                                } else {
                                    updateMessage = "Could not check for updates. Make sure the repository name is correct and public."
                                    updateUrl = null
                                }
                            } catch (e: Exception) {
                                updateMessage = "Error checking for updates."
                                updateUrl = null
                            } finally {
                                isCheckingUpdate = false
                                showUpdateDialog = true
                            }
                        }
                    }
                }
            )

            SettingItem(
                icon = Icons.Default.Cloud,
                title = "Powered by",
                subtitle = "Microsoft OneDrive & Graph API"
            )
        }
    }

    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Check") },
            text = { Text(updateMessage) },
            confirmButton = {
                if (updateUrl != null) {
                    TextButton(onClick = {
                        if (updateUrl!!.endsWith(".apk")) {
                            UpdateManager(context).downloadAndInstallUpdate(updateUrl!!)
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(updateUrl))
                            context.startActivity(intent)
                        }
                        showUpdateDialog = false
                    }) {
                        Text("Download")
                    }
                } else {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("OK")
                    }
                }
            },
            dismissButton = {
                if (updateUrl != null) {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = Purple60,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
}
