package com.lito.planify.dashboard.data.repository

import com.lito.planify.dashboard.data.cache.DiskCache
import com.lito.planify.dashboard.data.isUnauthorized
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.LocalDate

class CalendarRepositoryTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val api = FakeGoogleSource()
    private var clock = 1_000_000L
    private val monday = LocalDate.of(2026, 10, 12)

    private fun repository() = CalendarRepository(api.repository, DiskCache(folder.root), now = { clock })

    @Test
    fun downloadedWeekIsAvailableAfterRestart() = runBlocking {
        repository().refresh("token", monday)
        val week = repository().weeks.value[monday]
        assertEquals("Evento", week?.events?.single()?.title)
        assertEquals("Rutina", week?.sessions?.single()?.title)
    }

    @Test
    fun offlineKeepsTheCachedWeek() = runBlocking {
        val repository = repository()
        repository.refresh("token", monday)
        clock += CalendarRepository.MAX_AGE_MS
        assertTrue(repository.isStale(monday))
        api.failure = IOException("sin red")
        assertTrue(repository.refresh("token", monday).isFailure)
        assertEquals(1, repository.weeks.value[monday]?.events?.size)
    }

    @Test
    fun calendarWeekUsesSelectedMaxAge() = runBlocking {
        val repository = repository()
        repository.refresh("token", monday)
        clock += 6 * 60_000L

        assertTrue(repository.isStale(monday, 5 * 60_000L))
        assertTrue(!repository.isStale(monday, 10 * 60_000L))
    }

    @Test
    fun expiredSessionIsReportedAsUnauthorized() = runBlocking {
        api.status = 401
        val error = repository().refresh("token", monday).exceptionOrNull()
        assertTrue(error?.isUnauthorized == true)
    }

    @Test
    fun keepsOnlyTheMostRecentlyDownloadedWeeks() = runBlocking {
        val repository = repository()
        val mondays = (0L until CalendarRepository.KEEP + 3L).map { monday.plusWeeks(it) }
        mondays.forEach {
            clock += 1_000
            repository.refresh("token", it)
        }
        val kept = mondays.takeLast(CalendarRepository.KEEP).toSet()
        assertEquals(kept, repository.weeks.value.keys)
        assertEquals(kept, repository().weeks.value.keys)
    }

    @Test
    fun clearForgetsEverything() = runBlocking {
        val repository = repository()
        repository.refresh("token", monday)
        repository.clear()
        assertTrue(repository.weeks.value.isEmpty())
        assertTrue(repository().weeks.value.isEmpty())
    }
}
