package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

enum class ThemeMode {
    LIGHT, DARK, AMOLED
}

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = NeonPink,
    tertiary = OrbitViolet,
    background = ObsidianBackground,
    surface = SurfaceCoal,
    onPrimary = AmoledBackground,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val AmoledDarkColorScheme = darkColorScheme(
    primary = CyberCyan,
    secondary = NeonPink,
    tertiary = OrbitViolet,
    background = AmoledBackground,
    surface = AmoledBackground,
    onPrimary = AmoledBackground,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite
)

private val CyberLightColorScheme = lightColorScheme(
    primary = OrbitViolet,
    secondary = NeonPink,
    tertiary = CyberCyan,
    background = PremiumLightBackground,
    surface = SurfaceLight,
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onBackground = TextDark,
    onSurface = TextDark
)

@Composable
fun TbmMusicTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.LIGHT -> CyberLightColorScheme
        ThemeMode.DARK -> CyberDarkColorScheme
        ThemeMode.AMOLED -> AmoledDarkColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val isLight = themeMode == ThemeMode.LIGHT
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = isLight
                isAppearanceLightNavigationBars = isLight
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
