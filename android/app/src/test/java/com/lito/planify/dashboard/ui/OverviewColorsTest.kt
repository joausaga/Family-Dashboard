package com.lito.planify.dashboard.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OverviewColorsTest {
    private fun at(hour: Int, minute: Int = 0) = overviewColors(LocalTime.of(hour, minute))

    @Test fun morningMatchesReferenceAndNightIsStable() {
        assertEquals(Color(0xFF91C8FA), at(7).start)
        assertEquals(at(7), at(12))
        assertEquals(at(21), at(0))
        assertEquals(at(0), at(6))
        assertEquals(at(0), at(23, 59))
        assertTrue(at(7).start.luminance() > at(18).start.luminance() * 4)
    }

    @Test fun afternoonDarkensAndDawnLightensContinuously() {
        for (minute in 720 until 1260) {
            val a = overviewColors(LocalTime.ofSecondOfDay(minute * 60L))
            val b = overviewColors(LocalTime.ofSecondOfDay((minute + 1) * 60L))
            val brightnessA = androidx.compose.ui.graphics.lerp(a.start, a.end, 0.5f).luminance()
            val brightnessB = androidx.compose.ui.graphics.lerp(b.start, b.end, 0.5f).luminance()
            // Color conversion and packed channels introduce sub-perceptual rounding.
            assertTrue("Darkens at $minute: $brightnessA -> $brightnessB", brightnessB <= brightnessA + 0.002f)
            assertTrue("No jumps at $minute", kotlin.math.abs(b.start.luminance() - a.start.luminance()) < 0.01f)
        }
        for (minute in 360 until 420) {
            val a = overviewColors(LocalTime.ofSecondOfDay(minute * 60L))
            val b = overviewColors(LocalTime.ofSecondOfDay((minute + 1) * 60L))
            assertTrue(androidx.compose.ui.graphics.lerp(b.start, b.end, 0.5f).luminance() + 0.002f >= androidx.compose.ui.graphics.lerp(a.start, a.end, 0.5f).luminance())
        }
    }

    @Test fun contentRemainsLegibleEveryMinuteIncludingForecastSurfaces() {
        for (minute in 0 until 1440) {
            val colors = overviewColors(LocalTime.ofSecondOfDay(minute * 60L))
            for (background in listOf(colors.start, colors.end)) {
                assertTrue("Content at $minute", overviewContrast(colors.content, background) >= 4.5f)
                assertTrue("Forecast at $minute", overviewContrast(colors.forecastContent, colors.forecastSurface.compositeOver(background)) >= 4.5f)
            }
        }
    }
}
