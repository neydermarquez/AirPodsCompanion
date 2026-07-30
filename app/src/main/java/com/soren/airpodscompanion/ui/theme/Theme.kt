package com.soren.airpodscompanion.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = ElectricBlue, onPrimary = Color.White, primaryContainer = Color(0xFFDCEEFF), onPrimaryContainer = Ink,
    secondary = Teal, secondaryContainer = Color(0xFFD8F4EE), background = Ice, surface = Color.White,
    surfaceVariant = Color(0xFFE5EEF6), onBackground = Ink, onSurface = Ink, onSurfaceVariant = Slate,
    outline = Color(0xFF9FB4C6)
)

@Composable
fun AirPodsCompanionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = LightColors, typography = Typography, content = content)
}
