package com.yaneodex.desktop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Palette = darkColorScheme(
    primary = Color(0xFFA7F46A),
    secondary = Color(0xFFE7C669),
    tertiary = Color(0xFF7CC8FF),
    background = Color(0xFF090A0A),
    surface = Color(0xFF141615),
    onBackground = Color(0xFFF2F5F0),
    onSurface = Color(0xFFF2F5F0),
)

private val Type = androidx.compose.material3.Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Black, fontSize = 52.sp, lineHeight = 56.sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, fontSize = 34.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 22.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 15.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp),
    labelMedium = TextStyle(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.1.sp),
)

@Composable
fun YaNeoDexDesktopTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Palette, typography = Type, content = content)
}
