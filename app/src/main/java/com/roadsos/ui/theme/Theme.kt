package com.roadsos.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFF8FAFC),
    onPrimary = Color(0xFF0F172A),
    secondary = Color(0xFF10B981),
    tertiary = Color(0xFFF59E0B),
    background = Color(0xFF090D16),
    surface = Color(0xFF131B2E),
    surfaceVariant = Color(0xFF1E293B),
    error = Color(0xFFEF4444),
    errorContainer = Color(0xFF450A0A),
    onErrorContainer = Color(0xFFFCA5A5),
    outline = Color(0xFF334155)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F172A), // Deep Slate/Black
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF059669), // Safety Emerald Green
    onSecondary = Color(0xFFFFFFFF),
    tertiary = Color(0xFFD97706), // Warning Amber
    background = Color(0xFFF8FAFC), // Clean crisp off-white
    surface = Color(0xFFFFFFFF), // Pure white cards
    surfaceVariant = Color(0xFFF1F5F9), // Card hover/subtle background
    onSurfaceVariant = Color(0xFF475569),
    error = Color(0xFFDC2626), // Vivid Emergency Red
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2), // Soft emergency badge background
    onErrorContainer = Color(0xFF991B1B),
    outline = Color(0xFFE2E8F0) // Subtle card border
)

@Composable
fun RoadSOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
