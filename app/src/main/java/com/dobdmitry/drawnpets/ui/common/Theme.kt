package com.dobdmitry.drawnpets.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Palette {
    val Sky = Color(0xFF7EC8F0)
    val SkyLight = Color(0xFFD6F1FF)
    val Grass = Color(0xFF6CC24A)
    val GrassDark = Color(0xFF4E9F3A)
    val Sun = Color(0xFFFFD84D)
    val Ink = Color(0xFF3A2A55)
    val Coral = Color(0xFFFF7A59)
    val Pink = Color(0xFFFF8FB8)
    val Mint = Color(0xFF5ED6A8)
    val Lemon = Color(0xFFFFE27A)
    val Water = Color(0xFF3FA7E0)
    val WaterDeep = Color(0xFF1F5FA8)
    val Paper = Color(0xFFFFF8EC)
}

private val colors = lightColorScheme(
    primary = Palette.Coral,
    onPrimary = Color.White,
    secondary = Palette.Mint,
    onSecondary = Palette.Ink,
    tertiary = Palette.Pink,
    background = Palette.Paper,
    surface = Color.White,
    onSurface = Palette.Ink,
    onBackground = Palette.Ink,
    surfaceVariant = Color(0xFFFFF1D6),
)

private val typography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, color = Palette.Ink),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Palette.Ink),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Palette.Ink),
    bodyLarge = TextStyle(fontSize = 17.sp, color = Palette.Ink),
    bodyMedium = TextStyle(fontSize = 15.sp, color = Palette.Ink),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp),
)

@Composable
fun DrawnPetsTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colors, typography = typography, content = content)
}
