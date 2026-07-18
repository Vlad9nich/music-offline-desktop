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
 * Watch Dogs 2 / ctOS / KAIRSEC-inspired product theme.
 * Black void, white type, red accent — monospace terminal DNA.
 */
object Wd2 {
    val Bg = Color(0xFF050505)
    val BgElevated = Color(0xFF0A0A0A)
    val Panel = Color(0xFF0E0E0E)
    val PanelRaised = Color(0xFF141414)
    val Line = Color(0xFFFFFFFF)
    val LineDim = Color(0x33FFFFFF)
    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFB0B0B0)
    val Muted = Color(0xFF777777)
    val Accent = Color(0xFFFF0000)
    val Ok = Color(0xFF00FF66)
    val Warn = Color(0xFFFFCC00)
}

private val Palette = darkColorScheme(
    primary = Wd2.Accent,
    onPrimary = Wd2.Text,
    secondary = Wd2.Ok,
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
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = 1.sp,
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 26.sp,
        lineHeight = 30.sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        letterSpacing = 0.5.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        letterSpacing = 1.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.6.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
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
    labelMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.2.sp,
    ),
)

@Composable
fun YaNeoDexDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, typography = Type, content = content)
}
