package com.lito.planify.dashboard.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.luminance
import java.time.LocalTime

internal data class OverviewColors(val start: Color, val end: Color, val whiteForecast: Boolean = false) {
    val content: Color get() = overviewContent(start, end)
    val forecastSurface: Color
        get() {
            if (!whiteForecast) return Color.White.copy(alpha = 0.20f)
            // Keep a visible blue cell; darken only as much as white text needs.
            return (18..100).asSequence().map { opacity ->
                Color(0xFF182542).copy(alpha = opacity / 100f)
            }.first { surface ->
                minOf(overviewContrast(Color.White, surface.compositeOver(start)),
                    overviewContrast(Color.White, surface.compositeOver(end))) >= 4.5f
            }
        }
    val forecastContent: Color get() = if (whiteForecast) Color.White
        else overviewContent(forecastSurface.compositeOver(start), forecastSurface.compositeOver(end))
}

private val OverviewInk = Color(0xFF020408)
private val Morning = OverviewColors(Color(0xFF91C8FA), Color(0xFFDEEEFF))
private val Afternoon = OverviewColors(Color(0xFF354F7E), Color(0xFF526895))
private val Night = OverviewColors(Color(0xFF182542), Color(0xFF30385F))

/** Linear-light interpolation keeps perceived daylight progression monotonic. */
private fun lerp(from: Color, to: Color, fraction: Float): Color {
    if (fraction <= 0f) return from
    if (fraction >= 1f) return to
    val a = from.convert(ColorSpaces.LinearSrgb)
    val b = to.convert(ColorSpaces.LinearSrgb)
    fun mix(x: Float, y: Float) = x + (y - x) * fraction
    return Color(mix(a.red, b.red), mix(a.green, b.green), mix(a.blue, b.blue), colorSpace = ColorSpaces.LinearSrgb)
}

/** Local wall-clock time only: works offline and without a weather response. */
internal fun overviewColors(time: LocalTime): OverviewColors {
    val minute = time.hour * 60 + time.minute + time.second / 60f
    val whiteForecast = time.hour in 12..20
    fun between(from: OverviewColors, to: OverviewColors, start: Int, end: Int): OverviewColors {
        val fraction = (minute - start) / (end - start)
        // Keep the gradient narrow around the foreground change, so both ends remain legible.
        val middle = OverviewColors(Color(0xFF6987A5), Color(0xFF6987A5))
        val a = if (fraction < 0.5f) from else middle
        val b = if (fraction < 0.5f) middle else to
        val t = if (fraction < 0.5f) fraction * 2 else (fraction - 0.5f) * 2
        return OverviewColors(lerp(a.start, b.start, t), lerp(a.end, b.end, t))
    }
    val colors = when {
        minute < 360 -> Night
        minute < 420 -> between(Night, Morning, 360, 420)
        minute < 720 -> Morning
        minute < 1080 -> between(Morning, Afternoon, 720, 1080)
        minute < 1260 -> OverviewColors(
            lerp(Afternoon.start, Night.start, (minute - 1080) / 180f),
            lerp(Afternoon.end, Night.end, (minute - 1080) / 180f)
        )
        else -> Night
    }
    // Around the light/dark text crossover, reduce only the gradient's spread.
    // A single foreground must contrast against both ends, including the translucent cells.
    val center = lerp(colors.start, colors.end, 0.5f)
    var safe = colors
    repeat(40) {
        val contentContrast = minOf(overviewContrast(safe.content, safe.start), overviewContrast(safe.content, safe.end))
        val forecastContrast = minOf(
            overviewContrast(safe.forecastContent, safe.forecastSurface.compositeOver(safe.start)),
            overviewContrast(safe.forecastContent, safe.forecastSurface.compositeOver(safe.end))
        )
        if (minOf(contentContrast, forecastContrast) >= 4.5f) return safe.copy(whiteForecast = whiteForecast)
        safe = OverviewColors(lerp(safe.start, center, 0.25f), lerp(safe.end, center, 0.25f))
    }
    return OverviewColors(center, center, whiteForecast = whiteForecast)
}

internal fun overviewContrast(a: Color, b: Color): Float =
    (maxOf(a.luminance(), b.luminance()) + 0.05f) / (minOf(a.luminance(), b.luminance()) + 0.05f)

private fun overviewContent(start: Color, end: Color): Color {
    fun worst(color: Color) = minOf(overviewContrast(color, start), overviewContrast(color, end))
    return if (worst(OverviewInk) >= worst(Color.White)) OverviewInk else Color.White
}
