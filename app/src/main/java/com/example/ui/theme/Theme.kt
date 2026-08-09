package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFE5E7EB),
    onPrimary = Color(0xFF111827),
    primaryContainer = Color(0xFF374151),
    onPrimaryContainer = Color(0xFFF3F4F6),
    secondary = Color(0xFF9CA3AF),
    onSecondary = Color(0xFF111827),
    tertiary = Color(0xFF60A5FA),
    onTertiary = Color(0xFF0B1220),
    background = Color(0xFF0B0F14),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF151B23),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF24303D),
    onSurfaceVariant = Color(0xFFD1D5DB),
    // Lifted above background so cards don't melt into the page
    surfaceContainerLowest = Color(0xFF1A222C),
    surfaceContainerLow = Color(0xFF1E2733),
    surfaceContainer = Color(0xFF24303D),
    surfaceContainerHigh = Color(0xFF2C3A49),
    surfaceContainerHighest = Color(0xFF364656),
    outline = Color(0xFF6B7280),
    outlineVariant = Color(0xFF374151),
    error = StatusErrorRed,
    onError = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Ink800,
    onPrimary = Color.White,
    primaryContainer = Slate200,
    onPrimaryContainer = Ink900,
    secondary = Slate600,
    onSecondary = Color.White,
    tertiary = GreenSync,
    onTertiary = Color.White,
    background = PageBackground,
    onBackground = Ink900,
    surface = SurfaceContainerLowest,
    onSurface = Ink900,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = Slate600,
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = Slate300,
    outlineVariant = Color(0xFFE5E7EB),
    error = StatusErrorRed,
    onError = Color.White
)

@Composable
fun LyfStackTheme(
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

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = LyfShapes,
        content = content
    )
}
