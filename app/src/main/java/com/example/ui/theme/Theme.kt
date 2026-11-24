package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColorScheme = lightColorScheme(
    primary = InkDark,
    onPrimary = Color.White,
    primaryContainer = LavenderSoft,
    onPrimaryContainer = LavenderInk,
    secondary = Lavender,
    onSecondary = Color.White,
    secondaryContainer = BlushPinkSoft,
    onSecondaryContainer = BlushPinkInk,
    tertiary = Peach,
    tertiaryContainer = PeachSoft,
    onTertiaryContainer = PeachInk,
    background = WarmCreamBg,
    onBackground = InkDark,
    surface = CardWhite,
    onSurface = InkDark,
    surfaceVariant = SubtleLight,
    onSurfaceVariant = MutedLight,
    outline = BorderLight,
    error = ErrorRedLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Lavender,
    onPrimary = SoftDarkBg,
    primaryContainer = LavenderDarkBg,
    onPrimaryContainer = LavenderDarkText,
    secondary = BlushPink,
    onSecondary = SoftDarkBg,
    secondaryContainer = BlushPinkDarkBg,
    onSecondaryContainer = BlushPinkSoft,
    tertiary = Peach,
    tertiaryContainer = PeachDarkBg,
    onTertiaryContainer = PeachSoft,
    background = SoftDarkBg,
    onBackground = InkLight,
    surface = CardDark,
    onSurface = InkLight,
    surfaceVariant = SubtleDark,
    onSurfaceVariant = MutedDark,
    outline = BorderDark,
    error = ErrorRedDark
)

val DuoDeskTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp
    )
)

val DuoDeskShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun DuoDeskTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkDuoDeskPalette else LightDuoDeskPalette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDuoDeskPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = DuoDeskTypography,
            shapes = DuoDeskShapes,
            content = content
        )
    }
}
