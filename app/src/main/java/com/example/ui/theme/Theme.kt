package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ChessXDarkColorScheme = darkColorScheme(
    primary = ImperialGold,
    onPrimary = ObsidianBg,
    primaryContainer = ImperialGoldDark,
    onPrimaryContainer = ImperialGoldLight,
    secondary = ImperialGoldLight,
    onSecondary = ObsidianBg,
    secondaryContainer = ObsidianSurfaceVariant,
    onSecondaryContainer = ParchmentText,
    tertiary = CyanHolo,
    onTertiary = ObsidianBg,
    background = ObsidianBg,
    onBackground = ParchmentText,
    surface = ObsidianSurface,
    onSurface = ParchmentText,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = MutedSandText,
    outline = ObsidianCardBorder,
    error = CrimsonCheck,
    onError = ParchmentText
)

@Composable
fun ChessXTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ChessXDarkColorScheme,
        typography = ChessXTypography,
        content = content
    )
}
