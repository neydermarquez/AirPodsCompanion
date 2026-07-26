package com.example.airpodscompanion.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Cyan, onPrimary = Navy950, primaryContainer = Navy700, onPrimaryContainer = ColorTokens.DarkText,
    secondary = Teal, secondaryContainer = Color(0xFF123B3D), background = Navy950, surface = Navy900,
    surfaceVariant = Color(0xFF1A3043), onBackground = ColorTokens.DarkText, onSurface = ColorTokens.DarkText,
    onSurfaceVariant = Color(0xFFB2C4D5), outline = Color(0xFF476176)
)

private val LightColors = lightColorScheme(
    primary = ElectricBlue, onPrimary = Color.White, primaryContainer = Color(0xFFDCEEFF), onPrimaryContainer = Ink,
    secondary = Teal, secondaryContainer = Color(0xFFD8F4EE), background = Ice, surface = Color.White,
    surfaceVariant = Color(0xFFE5EEF6), onBackground = Ink, onSurface = Ink, onSurfaceVariant = Slate,
    outline = Color(0xFFB6C8D8)
)

private object ColorTokens { val DarkText = Color(0xFFF2F7FC) }

@Composable
fun AirPodsCompanionTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, typography = Typography, content = content)
}
