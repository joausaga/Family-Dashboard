package com.joausaga.familydashboard.data.repository

import com.joausaga.familydashboard.data.Content
import com.joausaga.familydashboard.data.cache.Cached
import com.joausaga.familydashboard.data.cache.CachedResource
import com.joausaga.familydashboard.data.cache.DiskCache
import kotlinx.coroutines.flow.StateFlow
import java.time.LocalDate

/** Eventos, ocurrencias recurrentes y tareas de Google, con copia local si falla la red. */
class ContentRepository(
    private val google: GoogleWorkspaceRepository,
    cache: DiskCache
) {

    private val resource = CachedResource<Content>(cache, "content", Content::class.java, MAX_AGE_MS)

    val content: StateFlow<Cached<Content>?> = resource.data

    fun isStale(maxAgeMs: Long = MAX_AGE_MS) = resource.isStale(maxAgeMs)

    suspend fun refresh(token: String, today: LocalDate): Result<Content> = resource.refresh {
        google.content(token, today)
    }

    fun clear() = resource.clear()

    companion object {
        const val MAX_AGE_MS = 5 * 60_000L
    }
}

internal fun bearer(token: String) = "Bearer $token"
