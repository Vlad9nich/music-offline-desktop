package com.yaneodex.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Watch Dogs 2 HUD — pure black/white pixel terminal + red signal accents.
 */
object Wd2 {
    val Bg = Color(0xFF000000)
    val BgElevated = Color(0xFF080808)
    val Panel = Color(0xFF0C0C0C)
    val PanelRaised = Color(0xFF141414)
    val Line = Color(0xFFFFFFFF)
    val LineDim = Color(0x40FFFFFF)
    val LineFaint = Color(0x18FFFFFF)
    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFC8C8C8)
    val Muted = Color(0xFF7A7A7A)
    val Accent = Color(0xFFFF1A1A)
    val AccentDim = Color(0xFF990000)
    val AccentSoft = Color(0x33FF1A1A)
    val Ok = Color(0xFFFFFFFF) // live = white pulse in pure B/W HUD
    val Warn = Color(0xFFFF6666)
}

private val Palette = darkColorScheme(
    primary = Wd2.Accent,
    onPrimary = Wd2.Text,
    secondary = Wd2.Text,
    onSecondary = Wd2.Bg,
    tertiary = Wd2.Warn,
    background = Wd2.Bg,
    surface = Wd2.Panel,
    surfaceVariant = Wd2.PanelRaised,
    onBackground = Wd2.Text,
    onSurface = Wd2.Text,
    outline = Wd2.LineDim,
)

private val Type = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 38.sp,
        letterSpacing = 2.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = 1.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 2.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 2.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        letterSpacing = 1.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 14.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.6.sp,
    ),
)

@Composable
fun YaNeoDexDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, typography = Type, content = content)
}
