package com.joausaga.familydashboard

/** Tracks the newest started request so a late result cannot overwrite a newer synchronization result. */
internal class SyncStatusTracker {
    private var latestRequestId = 0L

    @Synchronized
    fun begin(): Long = ++latestRequestId

    /** Returns the error state to publish, or null when this result belongs to an older request. */
    @Synchronized
    fun complete(requestId: Long, succeeded: Boolean): Boolean? {
        if (requestId != latestRequestId) return null
        return !succeeded
    }
}
