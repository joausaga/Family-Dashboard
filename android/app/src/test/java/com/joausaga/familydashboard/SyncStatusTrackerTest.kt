package com.joausaga.familydashboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncStatusTrackerTest {
    @Test
    fun laterSuccessfulRequestClearsPreviousFailure() {
        val tracker = SyncStatusTracker()
        val failedRequest = tracker.begin()
        assertEquals(true, tracker.complete(failedRequest, succeeded = false))

        val successfulRequest = tracker.begin()
        assertEquals(false, tracker.complete(successfulRequest, succeeded = true))
    }

    @Test
    fun lateFailureFromOlderRequestCannotReplaceNewerSuccess() {
        val tracker = SyncStatusTracker()
        val olderRequest = tracker.begin()
        val newerRequest = tracker.begin()

        assertEquals(false, tracker.complete(newerRequest, succeeded = true))
        assertNull(tracker.complete(olderRequest, succeeded = false))
    }

    @Test
    fun failedLatestRequestSetsSyncError() {
        val tracker = SyncStatusTracker()
        val request = tracker.begin()

        assertTrue(tracker.complete(request, succeeded = false) == true)
    }
}
