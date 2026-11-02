package com.joausaga.familydashboard.data.backup

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.joausaga.familydashboard.data.CalendarVisibilitySelection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.InputStream
import java.security.MessageDigest

class BackupCodecTest {
    private val codec = BackupCodec { backupTime.toEpochMilli() }

    @Test fun completeBackupRoundTripsWithAllImportedMetadataAndNoPackageBinding() {
        val document = backupDocument()
        val bytes = codec.encode(document)
        assertEquals(document, codec.decode(bytes))
        val text = bytes.toString(Charsets.UTF_8)
        assertTrue(text.contains("family-dashboard-backup"))
        assertTrue(text.contains("legacy-sheet"))
        assertFalse(text.contains("com.lito.planify.dashboard"))
        assertFalse(text.contains("com.joausaga.familydashboard"))
        assertFalse(text.contains("google_connected"))
        assertFalse(text.contains("\"token\""))
        assertFalse(text.contains("backup_restore_journal"))
    }

    @Test fun emptyHistoryNullLocationAndAutomaticBrightnessAreExplicitlyPreserved() {
        val document = backupDocument().copy(tvHistory = null, configuration = backupConfig().copy(location = null, brightness = null, calendarSelections = emptyMap(), taskListSelections = emptyMap()))
        assertEquals(document, codec.decode(codec.encode(document)))
    }

    @Test fun corruptedOrTruncatedCopyIsRejected() {
        val bytes = codec.encode(backupDocument())
        val root = JsonParser.parseString(bytes.toString(Charsets.UTF_8)).asJsonObject
        root.getAsJsonObject("configuration").addProperty("brightness", 0.75)
        rejects(root.toString().toByteArray())
        rejects(bytes.copyOf(bytes.size / 2))
        rejects("not json".toByteArray())
    }

    @Test fun wrongFormatAndFutureVersionAreRejectedEvenWithConsistentChecksum() {
        rejects(edit { it.addProperty("format", "another-application") })
        rejects(edit { it.addProperty("version", 2) })
    }

    @Test fun incompletePrimitiveFieldsAndCoercedStringsAreRejected() {
        rejects(edit { it.getAsJsonObject("configuration").getAsJsonObject("schedule").remove("enabled") })
        rejects(edit { it.getAsJsonObject("configuration").getAsJsonObject("schedule").addProperty("enabled", "true") })
        rejects(edit { it.getAsJsonObject("configuration").addProperty("calendarTaskRefreshMinutes", "10") })
        rejects(edit { it.getAsJsonObject("configuration").addProperty("calendarTaskRefreshMinutes", 4294967306L) })
        rejects(edit { it.getAsJsonObject("configuration").add("location", com.google.gson.JsonNull.INSTANCE); it.getAsJsonObject("configuration").remove("schedule") })
    }

    @Test fun invalidConfigurationDoesNotDecode() {
        rejects(edit { it.getAsJsonObject("configuration").addProperty("brightness", 2.0) })
        rejects(edit { it.getAsJsonObject("configuration").addProperty("calendarTaskRefreshMinutes", 7) })
        rejects(edit { it.getAsJsonObject("configuration").getAsJsonObject("location").addProperty("latitude", 150) })
        rejects(edit { it.getAsJsonObject("configuration").getAsJsonObject("location").addProperty("timezone", "not-a-zone") })
        rejects(edit { it.getAsJsonObject("configuration").getAsJsonObject("tvMonitorSettings").addProperty("ipAddress", "not-an-ip") })
        rejects(edit { it.getAsJsonObject("configuration").getAsJsonObject("schedule").addProperty("weekdayOff", 420) })
    }

    @Test fun selectionKeysCannotWriteCredentialsAndVisibleMustBeKnown() {
        val badKey = backupDocument().copy(configuration = backupConfig().copy(calendarSelections = mapOf("token" to CalendarVisibilitySelection(emptySet(), emptySet()))))
        assertTrue(runCatching { codec.encode(badKey) }.isFailure)
        val badIds = backupDocument().copy(configuration = backupConfig().copy(calendarSelections = mapOf("calendar_visibility_YQ" to CalendarVisibilitySelection(setOf("known"), setOf("unknown")))))
        assertTrue(runCatching { codec.encode(badIds) }.isFailure)
    }

    @Test fun inconsistentIntervalsUnknownStatesAndFutureHistoryAreRejected() {
        rejects(edit { it.getAsJsonObject("tvHistory").getAsJsonArray("intervals")[0].asJsonObject.addProperty("endMillis", backupTime.toEpochMilli()) })
        rejects(edit { it.getAsJsonObject("tvHistory").getAsJsonArray("transitions")[0].asJsonObject.addProperty("state", "UNKNOWN") })
        rejects(edit { it.getAsJsonObject("tvHistory").getAsJsonArray("transitions")[0].asJsonObject.addProperty("atMillis", Long.MAX_VALUE) })
    }

    @Test fun sizeLimitIsEnforcedWhileReadingWithoutTrustingProviderLength() {
        val input = object : InputStream() {
            var remaining = BackupCodec.MAX_BYTES.toLong() + 1
            override fun read(): Int = if (remaining-- > 0) 0 else -1
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (remaining == 0L) return -1
                val count = minOf(length.toLong(), remaining).toInt()
                remaining -= count
                return count
            }
        }
        assertTrue(runCatching { codec.read(input) }.isFailure)
    }

    private fun rejects(bytes: ByteArray) = assertTrue(runCatching { codec.decode(bytes) }.isFailure)

    private fun edit(change: (JsonObject) -> Unit): ByteArray {
        val root = JsonParser.parseString(codec.encode(backupDocument()).toString(Charsets.UTF_8)).asJsonObject
        root.remove("sha256")
        change(root)
        val digest = MessageDigest.getInstance("SHA-256").digest(root.toString().toByteArray()).joinToString("") { "%02x".format(it.toInt() and 255) }
        root.addProperty("sha256", digest)
        return root.toString().toByteArray()
    }
}
