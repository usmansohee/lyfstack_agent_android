package com.example

import com.example.data.model.SyncRange
import com.example.data.repository.SessionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRangeTest {

    @Test
    fun testSyncRangeFromString() {
        assertEquals(SyncRange.SINCE_LAST, SyncRange.fromString("since_last"))
        assertEquals(SyncRange.TODAY, SyncRange.fromString("today"))
        assertEquals(SyncRange.WEEK, SyncRange.fromString("week"))
        assertEquals(SyncRange.MONTH, SyncRange.fromString("month"))
        assertEquals(SyncRange.YEAR, SyncRange.fromString("year"))
        assertEquals(SyncRange.ALL, SyncRange.fromString("all"))
        assertEquals(SyncRange.CUSTOM, SyncRange.fromString("custom"))
        assertEquals(SyncRange.SINCE_LAST, SyncRange.fromString(null))
        assertEquals(SyncRange.SINCE_LAST, SyncRange.fromString("invalid_range"))
    }

    @Test
    fun testCalculateStartEpochAll() {
        val startEpoch = SessionRepository.calculateStartEpoch(SyncRange.ALL)
        assertEquals(0L, startEpoch)
    }

    @Test
    fun testCalculateStartEpochToday() {
        val now = System.currentTimeMillis()
        val startEpoch = SessionRepository.calculateStartEpoch(SyncRange.TODAY)
        assertTrue(startEpoch <= now)
        assertTrue(now - startEpoch <= 86400000L)
    }
}
