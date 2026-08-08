package com.nandan.calorimeterapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val AppColorScheme = darkColorScheme(
    primary = Emerald,
    onPrimary = Background,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Emerald,
    secondary = AccentBlue,
    onSecondary = Background,
    secondaryContainer = Color(0xFF0D2040),
    onSecondaryContainer = AccentBlue,
    tertiary = AccentPurple,
    background = Background,
    onBackground = OnBackground,
    surface = SurfaceCard,
    onSurface = OnBackground,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = OnSurfaceMuted,
    outline = BorderColor,
    error = AccentRed,
    onError = Background,
)

@Composable
fun CalorimeterAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content,
    )
}