package com.lito.planify.dashboard.data.repository

import com.lito.planify.dashboard.data.TvDisplayStatus
import com.lito.planify.dashboard.data.TvPowerState
import com.lito.planify.dashboard.data.TvStatusProbe
import com.lito.planify.dashboard.data.TvTransition
import com.lito.planify.dashboard.data.TvUsageCalculator
import com.lito.planify.dashboard.data.TvUsageHistory
import com.lito.planify.dashboard.data.cache.DiskCache
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.Instant

class TvLocalUsageRepositoryTest {
    @Test fun preservesImportedHistoryAndMetadataAfterRestartAndLocalPolling() = runBlocking {
        val directory = Files.createTempDirectory("family-dashboard-tv-history").toFile()
        try {
            val cache = DiskCache(File(directory, "cache"))
            val now = Instant.parse("2026-11-02T12:00:00Z")
            val imported = listOf(
                TvTransition(now.minusSeconds(7200).toEpochMilli(), TvPowerState.ON, "IFTTT", 1),
                TvTransition(now.minusSeconds(3600).toEpochMilli(), TvPowerState.OFF, "IFTTT", 2)
            )
            val history = TvUsageCalculator.history(imported, now, true,
                sourceRowCounts = mapOf("legacy-sheet" to 3), sourceOrders = mapOf("legacy-sheet" to 1L))
            val type = com.google.gson.reflect.TypeToken.get(TvUsageHistory::class.java).type
            cache.write("tv_usage_history", com.lito.planify.dashboard.data.cache.Cached(history, now.toEpochMilli()), type)
            val updated = TvLocalUsageRepository(TvStatusProbe { false }, cache)
                .observe("192.168.1.20", now).getOrThrow()
            assertEquals(imported, updated.transitions.take(2))
            assertEquals(history.intervals, updated.intervals)
            assertEquals(history.sourceRowCounts, updated.sourceRowCounts)
            assertEquals(history.sourceOrders, updated.sourceOrders)
            assertEquals(updated, TvLocalUsageRepository(TvStatusProbe { false }, cache).history.value?.value)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun recordsOnAndInferredOffOnceAndRestoresTheHistory() = runBlocking {
        val directory = Files.createTempDirectory("planify-tv-local").toFile()
        try {
            var on = true
            val probe = TvStatusProbe { if (on) true else throw IOException("timeout") }
            val cache = DiskCache(File(directory, "cache"))
            val repo = TvLocalUsageRepository(probe, cache)
            val start = Instant.parse("2026-10-29T12:00:00Z")
            repo.observe("192.168.1.20", start)
            on = false
            val offAt = start.plusSeconds(60)
            val firstOff = repo.observe("192.168.1.20", offAt).getOrThrow()
            val repeatedOff = repo.observe("192.168.1.20", offAt.plusSeconds(60)).getOrThrow()

            assertEquals(2, firstOff.transitions.size)
            assertEquals(TvPowerState.OFF, firstOff.transitions.last().state)
            assertTrue(firstOff.transitions.last().inferred)
            assertEquals(2, repeatedOff.transitions.size)
            assertEquals(TvDisplayStatus.OFF_INFERRED,
                TvUsageCalculator.summarize(repeatedOff, offAt.plusSeconds(60), java.time.ZoneId.of("UTC")).status)
            assertEquals(2, TvLocalUsageRepository(TvStatusProbe { true }, cache).history.value?.value?.transitions?.size)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun closesLegacyOpenSessionAtTheStaleLimitBeforeStartingLocalObservations() = runBlocking {
        val directory = Files.createTempDirectory("planify-tv-boundary").toFile()
        try {
            val cache = DiskCache(File(directory, "cache"))
            val at = Instant.parse("2026-10-29T12:00:00Z")
            val legacyStart = at.minusSeconds(24 * 60 * 60L)
            val legacy = TvUsageCalculator.history(
                listOf(TvTransition(legacyStart.toEpochMilli(), TvPowerState.ON, "IFTTT", 1)),
                legacyStart, true
            )
            cache.write("tv_usage_history", com.lito.planify.dashboard.data.cache.Cached(legacy, legacyStart.toEpochMilli()),
                com.google.gson.reflect.TypeToken.get(TvUsageHistory::class.java).type)
            val result = TvLocalUsageRepository(TvStatusProbe { true }, cache).observe("192.168.1.20", at).getOrThrow()

            assertEquals(2, result.intervals.size)
            assertEquals(legacyStart.plusMillis(TvUsageCalculator.STALE_AFTER_MILLIS).toEpochMilli(), result.intervals.first().endMillis)
            assertEquals(at.toEpochMilli(), result.intervals.last().startMillis)
        } finally {
            directory.deleteRecursively()
        }
    }
}
