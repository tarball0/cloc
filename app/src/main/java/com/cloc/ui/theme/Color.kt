package com.cloc.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class NothingColors(
    val black: Color = Color(0xFF000000),
    val surface: Color = Color(0xFF111111),
    val surfaceRaised: Color = Color(0xFF1A1A1A),
    val border: Color = Color(0xFF222222),
    val borderVisible: Color = Color(0xFF333333),
    val textDisabled: Color = Color(0xFF666666),
    val textSecondary: Color = Color(0xFF999999),
    val textPrimary: Color = Color(0xFFE8E8E8),
    val textDisplay: Color = Color(0xFFFFFFFF),
    val segmentEmpty: Color = Color(0xFF222222),
    val accent: Color = Color(0xFFD71921),
    val amber: Color = Color(0xFFD4A843),    // warm muted amber, calm on OLED black
)

val DarkNothing = NothingColors()

fun NothingColors.toMaterial(): ColorScheme = darkColorScheme(
    primary = textDisplay, onPrimary = black,
    primaryContainer = surfaceRaised, onPrimaryContainer = textPrimary,
    inversePrimary = black,
    secondary = textSecondary, onSecondary = black,
    secondaryContainer = surfaceRaised, onSecondaryContainer = textPrimary,
    tertiary = textSecondary, onTertiary = black,
    tertiaryContainer = surfaceRaised, onTertiaryContainer = textPrimary,
    background = black, onBackground = textPrimary,
    surface = black, onSurface = textPrimary,
    surfaceVariant = surface, onSurfaceVariant = textSecondary,
    surfaceTint = Color.Transparent,
    surfaceDim = black, surfaceBright = surfaceRaised,
    surfaceContainerLowest = black, surfaceContainerLow = surface,
    surfaceContainer = surfaceRaised, surfaceContainerHigh = surface,
    surfaceContainerHighest = surface,
    inverseSurface = textDisplay, inverseOnSurface = black,
    outline = borderVisible, outlineVariant = border,
    error = accent, onError = Color.White,
    errorContainer = accent.copy(alpha = 0.15f), onErrorContainer = accent,
    scrim = Color.Black.copy(alpha = 0.8f),
)