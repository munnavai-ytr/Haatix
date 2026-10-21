package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AirTouchDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004F58),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = NeonTeal,
    onSecondary = Color(0xFF003830),
    secondaryContainer = Color(0xFF005146),
    onSecondaryContainer = Color(0xFF73F8DF),
    tertiary = NeonAmber,
    onTertiary = Color(0xFF432C00),
    background = DarkNavyBackground,
    onBackground = TextPrimary,
    surface = DarkNavySurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkNavyCard,
    onSurfaceVariant = TextSecondary,
    outline = DarkNavyCardBorder
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AirTouchDarkColorScheme,
        typography = Typography,
        content = content
    )
}
