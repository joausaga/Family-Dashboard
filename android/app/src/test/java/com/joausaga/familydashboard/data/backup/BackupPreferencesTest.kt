package com.joausaga.familydashboard.data.backup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupPreferencesTest {
    @Test fun snapshotAndReplacementPreserveOnlyTheAllowedSettings() {
        val config = backupConfig()
        val values = BackupPreferences.replacementValues(config).toMutableMap()
        values.putAll(mapOf("token" to "private-token", "email" to "private-email", "google_connected" to true,
            "last_valid_time" to 123L, "backup_restore_journal" to "private-journal", "future_setting" to "keep"))
        val snapshot = BackupPreferences.snapshot(values)
        assertEquals(config, snapshot)
        val bytes = BackupCodec { backupTime.toEpochMilli() }.encode(backupDocument().copy(configuration = snapshot))
        assertFalse(bytes.toString(Charsets.UTF_8).contains("private-"))
        val replacement = config.copy(brightness = null, calendarSelections = emptyMap(), taskListSelections = emptyMap())
        BackupPreferences.keysToReplace(values.keys).forEach(values::remove)
        BackupPreferences.replacementValues(replacement).forEach { (key, value) -> if (value != null) values[key] = value else values.remove(key) }
        assertEquals("private-token", values["token"])
        assertEquals(true, values["google_connected"])
        assertEquals(123L, values["last_valid_time"])
        assertEquals("private-journal", values["backup_restore_journal"])
        assertEquals("keep", values["future_setting"])
        assertEquals(replacement, BackupPreferences.snapshot(values))
    }

    @Test fun accountSelectionsKeepIndependentKeysAndUnknownPreferencesAreNotRemoved() {
        val keys = setOf("calendar_visibility_YQ", "task_list_visibility_Yg", "token", "google_connected", "another_key")
        val changed = BackupPreferences.keysToReplace(keys)
        assertTrue(changed.containsAll(setOf("calendar_visibility_YQ", "task_list_visibility_Yg")))
        assertFalse("token" in changed)
        assertFalse("google_connected" in changed)
        assertFalse("another_key" in changed)
    }
}
