package com.lito.planify.dashboard.ui

import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsLogicTest {
    private fun min(m: Long) = m * 60_000L

    @Test
    fun scheduleTimeNeedsAtLeastFifteenMinutesBetweenOnAndOff() {
        assertEquals(null, scheduleTimeError(editingOn = true, minutes = 480, on = 420, off = 1380))
        assertEquals(null, scheduleTimeError(editingOn = false, minutes = 1200, on = 420, off = 1380))
        assertEquals(null, scheduleTimeError(editingOn = true, minutes = 1365, on = 420, off = 1380))
        assertTrue(scheduleTimeError(editingOn = true, minutes = 1370, on = 420, off = 1380) != null)
        assertTrue(scheduleTimeError(editingOn = true, minutes = 1380, on = 420, off = 1380) != null)
        assertTrue(scheduleTimeError(editingOn = false, minutes = 0, on = 420, off = 1380) != null)
        assertTrue(scheduleTimeError(editingOn = false, minutes = 430, on = 420, off = 1380) != null)
    }

    @Test
    fun timeLabelUsesTwelveHourClock() {
        assertEquals("7:00 AM", scheduleTimeLabel(420))
        assertEquals("11:45 PM", scheduleTimeLabel(1425))
        assertEquals("12:00 AM", scheduleTimeLabel(0))
        assertEquals("12:30 PM", scheduleTimeLabel(750))
        assertEquals("1:05 PM", scheduleTimeLabel(785))
        assertEquals("11:59 AM", scheduleTimeLabel(719))
    }

    @Test
    fun freshnessUsesThreeTimesTheExpectedFrequency() {
        assertTrue(serviceIsFresh(min(14), 5))
        assertTrue(serviceIsFresh(min(15), 5))
        assertFalse(serviceIsFresh(min(16), 5))
        assertFalse(serviceIsFresh(null, 5))
        assertFalse(serviceIsFresh(-1, 5))
    }

    @Test
    fun testFailuresAreReadable() {
        assertEquals("La TV no respondió a tiempo.", tvTestFailureMessage(SocketTimeoutException()))
        assertEquals("No se pudo conectar con la TV. Comprueba la IP y la red.", tvTestFailureMessage(ConnectException("x")))
        assertEquals("La dirección IP de la TV no es válida.", tvTestFailureMessage(IllegalArgumentException("La dirección IP de la TV no es válida.")))
        assertEquals("La TV respondió HTTP 500", tvTestFailureMessage(IOException("La TV respondió HTTP 500")))
        assertEquals("No se pudo contactar la TV.", tvTestFailureMessage(RuntimeException()))
    }

    @Test
    fun activeItemsComeFirstKeepingOrder() {
        val items = listOf("a", "b", "c", "d", "e")
        assertEquals(listOf("b", "d", "a", "c", "e"), activeFirst(items) { it == "b" || it == "d" })
        assertEquals(items, activeFirst(items) { true })
        assertEquals(items, activeFirst(items) { false })
        assertEquals(emptyList<String>(), activeFirst(emptyList<String>()) { true })
    }
}
