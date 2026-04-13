package com.example.practice.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════
// NEON OBSIDIAN PALETTE
// ═══════════════════════════════════════════

// Backgrounds
val Background_Deep = Color(0xFF050507)      // True obsidian, for root backgrounds
val Surface_Elevated = Color(0xFF12121A)     // For floating cards or bottom sheets
val Surface_Highlight = Color(0xFF1E1E2A)    // For pressed states

// Neon Accents (The "Signals")
val Neon_Cyan = Color(0xFF00E5FF)            // Primary/Scanning
val Neon_Green = Color(0xFF00FF88)           // Success/Match
val Neon_Red = Color(0xFFFF3366)             // Error/No Match
val Neon_Yellow = Color(0xFFFFD600)          // Warning/Processing

// Typography Colors
val Text_Primary = Color(0xE6FFFFFF)         // 90% opacity white
val Text_Secondary = Color(0xFF8A8A93)

// Legacy Mappings (to prevent build errors from existing usages)
val PrimaryIndigo = Neon_Cyan
val PrimaryIndigoDark = Neon_Cyan
val PrimaryIndigoDeep = Neon_Cyan

val SecondaryPurple = Neon_Cyan
val SecondaryPurpleDark = Neon_Cyan
val SecondaryPurpleDeep = Neon_Cyan

val TertiaryCyan = Neon_Cyan
val TertiaryCyanLight = Neon_Cyan
val TertiaryCyanDark = Neon_Cyan

val DarkBackground = Background_Deep
val DarkSurface = Surface_Elevated
val DarkCard = Surface_Elevated
val DarkSurfaceVariant = Surface_Highlight
val DarkElevated = Surface_Elevated

val LightBackground = Background_Deep
val LightSurface = Surface_Elevated
val LightCard = Surface_Elevated
val LightSurfaceVariant = Surface_Highlight

val SuccessGreen = Neon_Green
val SuccessGreenLight = Neon_Green
val SuccessGreenDark = Neon_Green

val WarningAmber = Neon_Yellow
val WarningAmberLight = Neon_Yellow
val WarningAmberDark = Neon_Yellow

val ErrorCoral = Neon_Red
val ErrorCoralLight = Neon_Red
val ErrorCoralDark = Neon_Red

val GlassSurface = Color(0x1AFFFFFF)
val GlassSurfaceLight = Color(0x33FFFFFF)
val GlassBorder = Color(0x33FFFFFF)
val GlassBorderLight = Color(0x4DFFFFFF)

val TextPrimaryDark = Text_Primary
val TextSecondaryDark = Text_Secondary
val TextTertiaryDark = Text_Secondary

val TextPrimaryLight = Text_Primary
val TextSecondaryLight = Text_Secondary
val TextTertiaryLight = Text_Secondary

val GradientPrimary = listOf(Color(0xFF00B4D8), Neon_Cyan, Color(0xFF48CAE4))
val GradientPurple = listOf(Color(0xFF7B2FF7), Color(0xFFAB47BC), Color(0xFFCE93D8))
val GradientSecondary = listOf(Color(0xFF6366F1), Neon_Cyan, Color(0xFF38BDF8))
val GradientCyan = listOf(Color(0xFF00B4D8), Neon_Cyan, Color(0xFF22D3EE))
val GradientSuccess = listOf(Color(0xFF059669), Neon_Green, Color(0xFF34D399))
val GradientError = listOf(Color(0xFFE11D48), Neon_Red, Color(0xFFFB7185))
val GradientBackground = listOf(Background_Deep, Surface_Elevated, Color(0xFF0D0D14))

val StudentAccent = Neon_Cyan
val ProfessorAccent = Color(0xFF6366F1) // Indigo for professors
val AdminAccent = Color(0xFFE11D48)      // Rose for admins
