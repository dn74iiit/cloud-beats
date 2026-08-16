package com.cloudbeats.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.cloudbeats.app.player.PlaybackManager
import com.cloudbeats.app.ui.components.MiniPlayerBar
import com.cloudbeats.app.ui.screens.HomeScreen
import com.cloudbeats.app.ui.screens.NowPlayingScreen
import com.cloudbeats.app.ui.screens.PlaylistDetailScreen
import com.cloudbeats.app.ui.screens.PlaylistsScreen
import com.cloudbeats.app.ui.screens.SearchScreen
import com.cloudbeats.app.ui.screens.SettingsScreen
import com.cloudbeats.app.ui.theme.DarkSurface
import com.cloudbeats.app.ui.theme.Purple60

/**
 * Bottom navigation items.
 */
data class BottomNavItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Routes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Routes.PLAYLISTS, "Playlists", Icons.Filled.QueueMusic, Icons.Outlined.QueueMusic),
    BottomNavItem(Routes.SEARCH, "Search", Icons.Filled.Search, Icons.Outlined.Search),
    BottomNavItem(Routes.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
)

@Composable
fun MainNavGraph(
    navController: NavHostController,
    playbackManager: PlaybackManager
) {
    val currentSong by playbackManager.currentSong.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val currentPosition by playbackManager.currentPosition.collectAsState()
    val duration by playbackManager.duration.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Don't show bottom nav on NowPlaying screen
    val showBottomBar = currentRoute != Routes.NOW_PLAYING

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(Routes.HOME) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = {
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Purple60,
                                selectedTextColor = Purple60,
                                indicatorColor = Purple60.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.HOME,
                modifier = Modifier.fillMaxSize()
            ) {
                composable(Routes.HOME) {
                    HomeScreen(
                        currentSongId = currentSong?.oneDriveId,
                        onSongClick = {
                            navController.navigate(Routes.NOW_PLAYING)
                        }
                    )
                }

                composable(Routes.PLAYLISTS) {
                    PlaylistsScreen(
                        onPlaylistClick = { playlistId ->
                            navController.navigate(Routes.createPlaylistDetailRoute(playlistId))
                        }
                    )
                }

                composable(
                    route = Routes.PLAYLIST_DETAIL,
                    arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
                ) {
                    PlaylistDetailScreen(
                        currentSongId = currentSong?.oneDriveId,
                        onBackClick = { navController.popBackStack() },
                        onSongClick = { navController.navigate(Routes.NOW_PLAYING) }
                    )
                }

                composable(Routes.SEARCH) {
                    SearchScreen(
                        currentSongId = currentSong?.oneDriveId,
                        onSongClick = {
                            navController.navigate(Routes.NOW_PLAYING)
                        },
                        onOnlineSearchClick = {
                            navController.navigate(Routes.ONLINE_SEARCH)
                        }
                    )
                }

                composable(Routes.SETTINGS) {
                    SettingsScreen()
                }

                composable(Routes.NOW_PLAYING) {
                    NowPlayingScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
                
                composable(Routes.ONLINE_SEARCH) {
                    com.cloudbeats.app.ui.screens.OnlineSearchScreen(
                        onBackClick = { navController.popBackStack() }
                    )
                }
            }

            // Mini player (above bottom nav, below content)
            if (showBottomBar) {
                MiniPlayerBar(
                    currentSong = currentSong,
                    isPlaying = isPlaying,
                    progress = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                    onPlayPauseClick = { playbackManager.playPause() },
                    onNextClick = { playbackManager.skipToNext() },
                    onClick = { navController.navigate(Routes.NOW_PLAYING) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
