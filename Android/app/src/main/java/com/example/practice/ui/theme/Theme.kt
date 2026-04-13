package com.example.practice.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    // Primary: Indigo
    primary = PrimaryIndigo,
    onPrimary = Color.White,
    primaryContainer = PrimaryIndigoDeep,
    onPrimaryContainer = Color(0xFFE0E7FF),

    // Secondary: Purple
    secondary = SecondaryPurple,
    onSecondary = Color.White,
    secondaryContainer = SecondaryPurpleDeep,
    onSecondaryContainer = Color(0xFFEDE9FE),

    // Tertiary: Cyan
    tertiary = TertiaryCyan,
    onTertiary = Color.White,
    tertiaryContainer = TertiaryCyanDark,
    onTertiaryContainer = Color(0xFFCFFAFE),

    // Surfaces
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    surfaceContainerHighest = DarkElevated,
    surfaceContainerHigh = DarkCard,
    surfaceContainer = DarkSurface,
    surfaceContainerLow = DarkBackground,

    // Semantic
    error = ErrorCoral,
    onError = Color.White,
    errorContainer = Color(0xFF3B1111),
    onErrorContainer = ErrorCoralLight,

    outline = Color(0xFF334155),              // Slate-700
    outlineVariant = Color(0xFF1E293B),        // Slate-800
    inverseSurface = LightSurface,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = PrimaryIndigoDark,
    scrim = Color(0xCC000000)
)

private val LightColorScheme = lightColorScheme(
    // Primary: Indigo
    primary = PrimaryIndigoDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = PrimaryIndigoDeep,

    // Secondary: Purple
    secondary = SecondaryPurpleDark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEDE9FE),
    onSecondaryContainer = SecondaryPurpleDeep,

    // Tertiary: Cyan
    tertiary = TertiaryCyanDark,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFFAFE),
    onTertiaryContainer = Color(0xFF164E63),

    // Surfaces
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    surfaceContainerHighest = Color(0xFFE2E8F0),
    surfaceContainerHigh = LightCard,
    surfaceContainer = Color(0xFFF8FAFC),
    surfaceContainerLow = Color.White,

    // Semantic
    error = ErrorCoralDark,
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),

    outline = Color(0xFFCBD5E1),              // Slate-300
    outlineVariant = Color(0xFFE2E8F0),        // Slate-200
    inverseSurface = DarkSurface,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = PrimaryIndigo,
    scrim = Color(0x66000000)
)

@Composable
fun PracticeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
