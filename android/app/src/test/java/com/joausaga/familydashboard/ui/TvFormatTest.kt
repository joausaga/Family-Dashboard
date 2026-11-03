package com.joausaga.familydashboard.ui

import com.joausaga.familydashboard.data.TvPeriodComparison
import org.junit.Assert.assertEquals
import org.junit.Test

class TvFormatTest {
    private fun min(m: Long) = m * 60_000L

    @Test
    fun durationPartsSplitHoursAndMinutes() {
        assertEquals(listOf("1" to "h", "50" to "min"), tvDurationParts(min(110)))
        assertEquals(listOf("2" to "h"), tvDurationParts(min(120)))
        assertEquals(listOf("45" to "min"), tvDurationParts(min(45)))
        assertEquals(listOf("0" to "min"), tvDurationParts(0))
        assertEquals(listOf("10" to "h", "59" to "min"), tvDurationParts(min(659)))
    }

    @Test
    fun heatmapDurationLabelsAreCompactAndHideZeroUsage() {
        assertEquals("", compactHeatDuration(0))
        assertEquals("<1m", compactHeatDuration(59_999))
        assertEquals("1m", compactHeatDuration(min(1)))
        assertEquals("59m", compactHeatDuration(min(59)))
        assertEquals("1h", compactHeatDuration(min(60)))
        assertEquals("1h5", compactHeatDuration(min(65)))
        assertEquals("8h23", compactHeatDuration(min(8 * 60 + 23)))
    }

    @Test
    fun hourlyBarLabelsAlwaysShowConsumption() {
        assertEquals("", compactHourDuration(0))
        assertEquals("<1m", compactHourDuration(59_999))
        assertEquals("46m", compactHourDuration(min(46)))
        assertEquals("1h", compactHourDuration(min(60)))
    }

    @Test
    fun agoLabelCoversRanges() {
        assertEquals("hace unos segundos", tvAgoLabel(30_000))
        assertEquals("hace 5 min", tvAgoLabel(min(5)))
        assertEquals("hace 2 h", tvAgoLabel(min(120)))
        assertEquals("hace 2 h 10 min", tvAgoLabel(min(130)))
        assertEquals("hace 3 d", tvAgoLabel(min(3 * 24 * 60 + 5)))
    }

    @Test
    fun vsAverageLabelHandlesAllCases() {
        assertEquals("Sin historial para comparar", tvVsAverageLabel(min(60), 0))
        assertEquals("Igual que el promedio", tvVsAverageLabel(min(60), min(60) + 20_000))
        assertEquals("1 h 33 min sobre el promedio", tvVsAverageLabel(min(110), min(17)))
        assertEquals("30 min bajo el promedio", tvVsAverageLabel(min(30), min(60)))
    }

    @Test
    fun comparisonTextHandlesDirectionAndMissingData() {
        assertEquals(0 to "Sin uso en ambos periodos", tvComparisonText(TvPeriodComparison(0, 0), "la semana pasada"))
        assertEquals(0 to "Sin datos del periodo anterior", tvComparisonText(TvPeriodComparison(min(10), 0), "el mes pasado"))
        assertEquals(1 to "30 min más que la semana pasada", tvComparisonText(TvPeriodComparison(min(90), min(60)), "la semana pasada"))
        assertEquals(-1 to "1 h menos que el mes pasado", tvComparisonText(TvPeriodComparison(min(60), min(120)), "el mes pasado"))
        assertEquals(0 to "Igual que el mes pasado", tvComparisonText(TvPeriodComparison(min(60), min(60) + 10_000), "el mes pasado"))
    }
}
