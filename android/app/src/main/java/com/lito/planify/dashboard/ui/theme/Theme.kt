package com.lito.planify.dashboard.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lito.planify.dashboard.R

// Medidas en píxeles del panel (1920×1200): la app usa Density(1f), así que 1 dp = 1 sp = 1 px.

object Palette {
    val Background = Color(0xFFF7EEE2)
    val Rail = Color(0xFFF0E3D2)
    val Card = Color(0xFFFFFDF9)
    val CardBorder = Color(0xFFEFE2D0)
    val Line = Color(0xFFEADBC6)
    val Ink = Color(0xFF221D18)
    val InkSoft = Color(0xFF6E6358)
    val InkMuted = Color(0xFFA6998A)
    /** Títulos de sección (SectionLabel). */
    val Accent = Color(0xFFC8243B)
    val Rain = Color(0xFF2F80ED)
    val Warm = Color(0xFFF5A623)
    val Night = Color(0xFF5B6B9A)
    val Highlight = Color(0xFFF6ECDD)
    val Offline = Color(0xFFB5A99A)
    val Online = Color(0xFF3BA55C)
    /** Identidad visual exclusiva para seguimiento de TV; Online queda reservado para tareas/conectividad. */
    val Television = Color(0xFF6659A6)
    val TelevisionSoft = Color(0xFFECE8F7)

    // Semánticos (antes hex sueltos en TodayScreen)
    val Border = Color(0xFFE8DAC6)
    val Divider = Color(0xFFEADBC6)
    val Success = Color(0xFF1F7A43)
    val SuccessSoft = Color(0xFFE3F3E6)
    val TaskInk = Color(0xFF3A3128)
    val TodayTint = Color(0xFFFFF3EE)
    val TodayBorder = Color(0xFFF1C7BD)
    val AccentSoft = Color(0xFFFBDDD8)
    val AmberInk = Color(0xFF9A5B00)
    val AmberSoft = Color(0xFFFFEBCB)
    val SwitchOff = Color(0xFF968978)
    val ScheduleNight = Color(0xFF5B6B9A)
    val RainOnDark = Color(0xFF9FD3FF)
    // Identidad de TV: héroe (encendida / resto) y escala de intensidad del mapa de calor
    val TelevisionDeep = Color(0xFF47397F)
    val TelevisionBright = Color(0xFF7B6CCF)
    val TelevisionDim = Color(0xFF4B4666)
    val TelevisionDimEnd = Color(0xFF6A6588)
    val TelevisionOn = Color(0xFF8BE3A8)
    val TvHeat = listOf(Color(0xFFEFEAF6), Color(0xFFD9D1F0), Color(0xFFB7ABE3), Color(0xFF8E7FCF), Color(0xFF5F4FAE))
    val EmptyIcon = Color(0xFFCFC3B2)
}

object Radius {
    val Small = 14.dp
    val Chip = 16.dp
    val Column = 20.dp
    val Pill = 24.dp
    val Card = 28.dp
    val Panel = 36.dp
}

/** Escala de espaciado en múltiplos de 4/8 (px del panel). */
object Space {
    val XS = 4.dp
    val S = 8.dp
    val M = 16.dp
    val L = 24.dp
    val XL = 32.dp
}

val Geist = FontFamily(
    Font(R.font.geist_thin, FontWeight.Thin),
    Font(R.font.geist_extralight, FontWeight.ExtraLight),
    Font(R.font.geist_light, FontWeight.Light),
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold),
    Font(R.font.geist_bold, FontWeight.Bold)
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium),
    Font(R.font.geist_mono_bold, FontWeight.Bold)
)

object Type {
    /** Etiquetas de sección en mayúsculas: SIGUIENTE, PRÓXIMAS HORAS… */
    val Label = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, letterSpacing = 2.5.sp, color = Palette.Accent)
    val Mono = TextStyle(fontFamily = GeistMono, fontSize = 24.sp, color = Palette.InkSoft)
    val Clock = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Light, fontSize = 196.sp, lineHeight = 196.sp, letterSpacing = (-8).sp, color = Palette.Ink)
    val Huge = TextStyle(fontFamily = Geist, fontWeight = FontWeight.SemiBold, fontSize = 140.sp, lineHeight = 140.sp, letterSpacing = (-5).sp, color = Palette.Ink)
    val Title = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 46.sp, lineHeight = 54.sp, color = Palette.Ink)
    val Large = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 34.sp, lineHeight = 42.sp, color = Palette.Ink)
    val Body = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 29.sp, lineHeight = 37.sp, color = Palette.Ink)
    val Small = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Normal, fontSize = 24.sp, lineHeight = 30.sp, color = Palette.InkSoft)
    val Tiny = TextStyle(fontFamily = Geist, fontWeight = FontWeight.Medium, fontSize = 20.sp, lineHeight = 24.sp, color = Palette.InkSoft)
}
