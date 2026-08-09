package com.example.data.repository

import com.example.data.db.CategoryOverrideDao
import com.example.data.db.CategoryStat
import com.example.data.db.TopAppStat
import com.example.data.db.UsageSessionDao
import com.example.data.model.CategoryOverride
import com.example.data.model.SyncRange
import com.example.data.model.UsageSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar

data class RangeStats(
    val activeSeconds: Long,
    val idleSeconds: Long,
    val totalTrackedSeconds: Long,
    val focusPercentage: Int, // (active / tracked) * 100
    val sessionCount: Int,
    val uniqueAppsCount: Int,
    val pendingSyncCount: Int
)

class SessionRepository(
    private val sessionDao: UsageSessionDao,
    private val overrideDao: CategoryOverrideDao
) {

    fun getAllSessionsFlow(): Flow<List<UsageSession>> = sessionDao.getAllSessionsFlow()

    fun getPendingCountFlow(): Flow<Int> = sessionDao.getPendingCountFlow()

    val categoryOverridesFlow: Flow<List<CategoryOverride>> = overrideDao.getAllOverridesFlow()

    suspend fun getCategoryOverrideMap(): Map<String, String> {
        return overrideDao.getAllOverrides().associate { it.packageName to it.category }
    }

    suspend fun setCategoryOverride(packageName: String, category: String) {
        overrideDao.setOverride(CategoryOverride(packageName = packageName, category = category))
    }

    suspend fun removeCategoryOverride(packageName: String) {
        overrideDao.removeOverride(packageName)
    }

    suspend fun getOpenSession(): UsageSession? = sessionDao.getOpenSession()

    suspend fun insertOrUpdateSession(session: UsageSession) = sessionDao.insertSession(session)

    suspend fun closeOrphanSessions(endedIso: String, endedEpoch: Long) {
        sessionDao.closeOrphanSessions(endedIso, endedEpoch)
    }

    suspend fun getPendingSessions(): List<UsageSession> = sessionDao.getPendingSessions()

    suspend fun markSessionsSynced(sessionIds: List<String>, syncedAt: Long) {
        if (sessionIds.isNotEmpty()) {
            sessionDao.markSessionsSynced(sessionIds, syncedAt)
        }
    }

    suspend fun getSessionsForRange(range: SyncRange, customStart: Long? = null, customEnd: Long? = null): List<UsageSession> {
        val startEpoch = calculateStartEpoch(range, customStart)
        val endEpoch = customEnd ?: System.currentTimeMillis()
        return if (range == SyncRange.ALL) {
            sessionDao.getSessionsSince(0)
        } else {
            sessionDao.getSessionsInRange(startEpoch, endEpoch)
        }
    }

    fun getSessionsForRangeFlow(range: SyncRange, customStart: Long? = null): Flow<List<UsageSession>> {
        val startEpoch = calculateStartEpoch(range, customStart)
        return sessionDao.getSessionsSinceFlow(startEpoch)
    }

    fun getCategoryStatsFlow(range: SyncRange): Flow<List<CategoryStat>> {
        val startEpoch = calculateStartEpoch(range)
        return sessionDao.getCategoryStatsSinceFlow(startEpoch)
    }

    fun getTopAppsFlow(range: SyncRange, limit: Int = 4): Flow<List<TopAppStat>> {
        val startEpoch = calculateStartEpoch(range)
        return sessionDao.getTopAppsSinceFlow(startEpoch, limit)
    }

    fun getRangeStatsFlow(range: SyncRange): Flow<RangeStats> {
        val startEpoch = calculateStartEpoch(range)
        return sessionDao.getSessionsSinceFlow(startEpoch).map { sessions ->
            val activeSec = sessions.sumOf { it.activeDurationSeconds }
            val idleSec = sessions.sumOf { it.idleDurationSeconds }
            val trackedSec = activeSec + idleSec
            val focusPct = if (trackedSec > 0) ((activeSec.toDouble() / trackedSec.toDouble()) * 100).toInt() else 0
            val uniqueApps = sessions.map { it.processName }.distinct().size
            val pendingSync = sessions.count { !it.isSynced }

            RangeStats(
                activeSeconds = activeSec,
                idleSeconds = idleSec,
                totalTrackedSeconds = trackedSec,
                focusPercentage = focusPct.coerceIn(0, 100),
                sessionCount = sessions.size,
                uniqueAppsCount = uniqueApps,
                pendingSyncCount = pendingSync
            )
        }
    }

    companion object {
        fun calculateStartEpoch(range: SyncRange, customStart: Long? = null): Long {
            if (range == SyncRange.CUSTOM && customStart != null) return customStart
            val cal = Calendar.getInstance()
            when (range) {
                SyncRange.SINCE_LAST -> {
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                }
                SyncRange.TODAY -> {
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                SyncRange.WEEK -> {
                    cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                SyncRange.MONTH -> {
                    cal.set(Calendar.DAY_OF_MONTH, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                SyncRange.YEAR -> {
                    cal.set(Calendar.DAY_OF_YEAR, 1)
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                }
                SyncRange.ALL -> return 0L
                SyncRange.CUSTOM -> return customStart ?: 0L
            }
            return cal.timeInMillis
        }
    }
}
