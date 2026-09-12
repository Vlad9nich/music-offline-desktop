package com.yaneodex.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ctOS shell palette.
 *
 * Same DNA as before — near-black ground, white type, red signal accent — but built in
 * elevation steps instead of white outlines. Depth now comes from surface lightness
 * (base -> panel -> raised -> hover), which is what removes the hard "boxed in" look and
 * lets a single 1dp hairline mean something again.
 */
object Wd2 {
    /** App ground. Slightly above pure black so panels can read as "above" it. */
    val Bg = Color(0xFF0A0A0A)
    val BgElevated = Color(0xFF101010)

    /** Sidebars, rails and cards. */
    val Panel = Color(0xFF121212)
    val PanelRaised = Color(0xFF1C1C1C)
    val PanelHover = Color(0xFF242424)

    /** Hairlines. Deliberately low-contrast: used for separation, not for framing. */
    val Line = Color(0x1FFFFFFF)
    val LineStrong = Color(0x3DFFFFFF)
    val LineDim = Color(0x14FFFFFF)
    val LineFaint = Color(0x0AFFFFFF)

    val Text = Color(0xFFFFFFFF)
    val TextDim = Color(0xFFB3B3B3)
    val Muted = Color(0xFF6A6A6A)

    /** Red signal — reserved for active state and primary actions. */
    val Accent = Color(0xFFFF1A1A)
    val AccentDim = Color(0xFFB31212)
    val AccentSoft = Color(0x1FFF1A1A)

    val Ok = Color(0xFFFFFFFF)
    val Warn = Color(0xFFFF6666)

    /** Filled primary action, e.g. the play button. */
    val AccentText = Color(0xFFFFFFFF)
}

/** Corner radii. Sharp rectangles everywhere was a large part of the "axe-cut" feel. */
object Wd2Radius {
    val sm: Dp = 4.dp
    val md: Dp = 8.dp
    val lg: Dp = 12.dp
    val pill: Dp = 999.dp
}

/**
 * Two families, two jobs: proportional sans for anything the listener reads as language,
 * monospace only for machine-ish metadata (counters, timers, build tags, hotkeys).
 */
object Wd2Fonts {
    val Content: FontFamily = FontFamily.SansSerif
    val Meta: FontFamily = FontFamily.Monospace
}

private val Palette = darkColorScheme(
    primary = Wd2.Accent,
    onPrimary = Wd2.AccentText,
    secondary = Wd2.Text,
    onSecondary = Wd2.Bg,
    tertiary = Wd2.Warn,
    background = Wd2.Bg,
    surface = Wd2.Panel,
    surfaceVariant = Wd2.PanelRaised,
    onBackground = Wd2.Text,
    onSurface = Wd2.Text,
    outline = Wd2.Line,
    outlineVariant = Wd2.LineDim,
)

private val Type = androidx.compose.material3.Typography(
    displayLarge = TextStyle(
        fontFamily = Wd2Fonts.Content,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.4).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Wd2Fonts.Content,
        fontWeight = FontWeight.Bold,
        fontSize = 23.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Wd2Fonts.Content,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Wd2Fonts.Content,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Wd2Fonts.Content,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Wd2Fonts.Content,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Wd2Fonts.Content,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Wd2Fonts.Content,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Wd2Fonts.Meta,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        letterSpacing = 0.6.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Wd2Fonts.Meta,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.8.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Wd2Fonts.Meta,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.9.sp,
    ),
)

@Composable
fun YaNeoDexDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, typography = Type, content = content)
}
