package com.example.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core DuoDesk Warm Cream & Soft Dark Tokens
val WarmCreamBg = Color(0xFFFAF7F2)
val SoftDarkBg = Color(0xFF16161D)

val CardWhite = Color(0xFFFFFFFF)
val CardDark = Color(0xFF1F1F29)

val SubtleLight = Color(0xFFFAF7F2)
val SubtleDark = Color(0xFF272733)

val NavSurfaceLight = Color(0xFFFFFFFF)
val NavSurfaceDark = Color(0xFF1B1B24)

val BorderLight = Color(0xFFEAE4DC)
val BorderDark = Color(0xFF2E2D3B)
val InputBorderDark = Color(0xFF343343)

val DividerLight = Color(0xFFF2EDE6)

val InkDark = Color(0xFF18181F)
val InkLight = Color(0xFFF5F3EE)

val MutedLight = Color(0xFF6E6A75)
val MutedDark = Color(0xFFA19CA8)
val TertiaryMuted = Color(0xFF9A94A3)

// Signature Pastels & Accents
val Lavender = Color(0xFF9B82F3)
val LavenderSoft = Color(0xFFE6DFFB)
val LavenderInk = Color(0xFF5B469C)
val LavenderDarkBg = Color(0xFF2D283E)
val LavenderDarkText = Color(0xFFD5C9FA)

val BlushPink = Color(0xFFE684A7)
val BlushPinkSoft = Color(0xFFF8D8E4)
val BlushPinkInk = Color(0xFF8E3B5B)
val BlushPinkDarkBg = Color(0xFF3A2632)

val Peach = Color(0xFFF2A36B)
val PeachSoft = Color(0xFFFCE6D4)
val PeachInk = Color(0xFF9A5528)
val PeachDarkBg = Color(0xFF332A24)

val Sage = Color(0xFF5BB992)
val SageSoft = Color(0xFFDCF4EA)
val SageInk = Color(0xFF246A50)

val ErrorRedLight = Color(0xFFDC2626)
val ErrorRedDark = Color(0xFFF87171)
val PriorityMedLight = Color(0xFFD97706)
val PriorityLowDot = Color(0xFF6C8FF5)

@Immutable
data class DuoDeskPalette(
    val isDark: Boolean,
    val background: Color,
    val card: Color,
    val subtleSurface: Color,
    val navSurface: Color,
    val border: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val lavender: Color,
    val lavenderSoft: Color,
    val lavenderInk: Color,
    val pink: Color,
    val pinkSoft: Color,
    val pinkInk: Color,
    val peach: Color,
    val peachSoft: Color,
    val peachInk: Color,
    val sage: Color,
    val sageSoft: Color,
    val sageInk: Color,
    val error: Color,
    val heroStart: Color,
    val heroMid: Color,
    val heroEnd: Color,
    val sharedHeaderStart: Color,
    val sharedHeaderEnd: Color,
    val primaryButtonBg: Color,
    val primaryButtonText: Color
)

val LightDuoDeskPalette = DuoDeskPalette(
    isDark = false,
    background = WarmCreamBg,
    card = CardWhite,
    subtleSurface = SubtleLight,
    navSurface = NavSurfaceLight,
    border = BorderLight,
    divider = DividerLight,
    textPrimary = InkDark,
    textSecondary = MutedLight,
    textTertiary = TertiaryMuted,
    lavender = Lavender,
    lavenderSoft = LavenderSoft,
    lavenderInk = LavenderInk,
    pink = BlushPink,
    pinkSoft = BlushPinkSoft,
    pinkInk = BlushPinkInk,
    peach = Peach,
    peachSoft = PeachSoft,
    peachInk = PeachInk,
    sage = Sage,
    sageSoft = SageSoft,
    sageInk = SageInk,
    error = ErrorRedLight,
    heroStart = Color(0xFFFBE3EC),
    heroMid = Color(0xFFECE6FC),
    heroEnd = Color(0xFFFDECE0),
    sharedHeaderStart = Color(0xFFFFFFFF),
    sharedHeaderEnd = Color(0xFFF0EBFC),
    primaryButtonBg = InkDark,
    primaryButtonText = Color.White
)

val DarkDuoDeskPalette = DuoDeskPalette(
    isDark = true,
    background = SoftDarkBg,
    card = CardDark,
    subtleSurface = SubtleDark,
    navSurface = NavSurfaceDark,
    border = BorderDark,
    divider = BorderDark,
    textPrimary = InkLight,
    textSecondary = MutedDark,
    textTertiary = TertiaryMuted,
    lavender = Lavender,
    lavenderSoft = LavenderDarkBg,
    lavenderInk = LavenderDarkText,
    pink = BlushPink,
    pinkSoft = BlushPinkDarkBg,
    pinkInk = BlushPinkSoft,
    peach = Peach,
    peachSoft = PeachDarkBg,
    peachInk = PeachSoft,
    sage = Sage,
    sageSoft = Color(0xFF1F382E),
    sageInk = Color(0xFF8BE2BE),
    error = ErrorRedDark,
    heroStart = Color(0xFF2C2334),
    heroMid = Color(0xFF262338),
    heroEnd = Color(0xFF2E2623),
    sharedHeaderStart = CardDark,
    sharedHeaderEnd = Color(0xFF272338),
    primaryButtonBg = Lavender,
    primaryButtonText = SoftDarkBg
)

val LocalDuoDeskPalette = staticCompositionLocalOf { LightDuoDeskPalette }
