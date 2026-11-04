package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ShadowHuntDarkColorScheme = darkColorScheme(
    primary = NeonRed,
    onPrimary = Color.White,
    primaryContainer = NeonRedDark,
    onPrimaryContainer = Color.White,
    secondary = ElectricBlue,
    onSecondary = Color.Black,
    secondaryContainer = ElectricBlueDark,
    onSecondaryContainer = Color.White,
    tertiary = NeonPurple,
    onTertiary = Color.White,
    tertiaryContainer = NeonPurpleDark,
    onTertiaryContainer = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = NeonRed,
    onError = Color.White
)

@Composable
fun ShadowHuntTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ShadowHuntDarkColorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias for template
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    ShadowHuntTheme(content = content)
}
