package com.joausaga.familydashboard.data.repository

import com.joausaga.familydashboard.data.cache.DiskCache
import com.joausaga.familydashboard.data.isUnauthorized
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.time.LocalDate

class ContentRepositoryTest {
    @get:Rule val folder = TemporaryFolder()
    private val api = FakeGoogleSource()
    private fun repository() = ContentRepository(api.repository, DiskCache(folder.root))
    private val today = LocalDate.of(2026, 11, 2)

    @Test fun googleContentSurvivesRestartAndOfflineFailure() = runBlocking {
        val original = repository().refresh("token", today).getOrThrow()
        assertEquals("Evento", original.events.single().title)
        assertEquals("Tarea", original.tasks.single().title)
        val restarted = repository()
        assertEquals(original, restarted.content.value?.value)
        api.failure = IOException("sin red")
        assertTrue(restarted.refresh("token", today).isFailure)
        assertEquals(original, restarted.content.value?.value)
    }

    @Test fun unauthorizedDoesNotDiscardCachedContent() = runBlocking {
        val repo = repository()
        val original = repo.refresh("token", today).getOrThrow()
        api.status = 401
        assertTrue(repo.refresh("token", today).exceptionOrNull()?.isUnauthorized == true)
        assertEquals(original, repo.content.value?.value)
    }

    @Test fun clearRemovesGoogleContentFromDisk() = runBlocking {
        val repo = repository()
        repo.refresh("token", today).getOrThrow()
        repo.clear()
        assertEquals(null, repo.content.value)
        assertEquals(null, repository().content.value)
    }
}
