package com.cloudbeats.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple60,
    onPrimary = DarkBackground,
    primaryContainer = Purple20,
    onPrimaryContainer = Purple80,
    secondary = Cyan60,
    onSecondary = DarkBackground,
    secondaryContainer = Cyan40,
    onSecondaryContainer = Cyan80,
    tertiary = Cyan40,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    error = Error,
    onError = TextPrimary,
    outline = TextTertiary
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    onPrimary = LightBackground,
    primaryContainer = Purple80,
    onPrimaryContainer = Purple20,
    secondary = Cyan40,
    onSecondary = LightBackground,
    secondaryContainer = Cyan80,
    onSecondaryContainer = Cyan40,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = Error,
    onError = LightBackground,
    outline = TextSecondaryLight
)

@Composable
fun CloudBeatsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CloudBeatsTypography,
        content = content
    )
}
