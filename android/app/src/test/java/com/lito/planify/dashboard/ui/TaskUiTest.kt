package com.lito.planify.dashboard.ui

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskUiTest {
    private val today = LocalDate.parse("2026-11-04")

    @Test
    fun countdownCoversRanges() {
        assertEquals("Vencida", taskCountdown("2026-11-03", today))
        assertEquals("Hoy", taskCountdown("2026-11-04T00:00:00.000Z", today))
        assertEquals("Mañana", taskCountdown("2026-11-05", today))
        assertEquals("6 días", taskCountdown("2026-11-10", today))
        assertEquals("Sin fecha", taskCountdown(null, today))
    }

    @Test
    fun toneFollowsDaysUntilDue() {
        assertEquals(DueTone.Overdue, dueTone("2026-11-03", today))
        assertEquals(DueTone.Overdue, dueTone("2026-11-04", today))
        assertEquals(DueTone.Soon, dueTone("2026-11-06", today))
        assertEquals(DueTone.Later, dueTone("2026-11-07", today))
        assertEquals(DueTone.None, dueTone(null, today))
    }

    @Test
    fun sentenceDescribesTheDueDate() {
        assertEquals("Sin fecha de vencimiento", taskDueSentence(null, today))
        assertEquals("Vence hoy, miércoles 4 de noviembre", taskDueSentence("2026-11-04", today))
        assertEquals("Vence mañana, jueves 5 de noviembre", taskDueSentence("2026-11-05", today))
        assertEquals("Vence el lunes 9 de noviembre, en 5 días", taskDueSentence("2026-11-09", today))
        assertEquals("Venció ayer, el martes 3 de noviembre", taskDueSentence("2026-11-03", today))
        assertEquals("Venció el sábado 31 de octubre, hace 4 días", taskDueSentence("2026-10-31", today))
    }

    @Test
    fun iconKeywordsFallBackToGenericIcon() {
        assertEquals(taskIcon("Comprar leche"), taskIcon("pasar al SUPER"))
        assertEquals(taskIcon("algo sin palabras clave"), taskIcon("otra cosa cualquiera"))
    }
}
