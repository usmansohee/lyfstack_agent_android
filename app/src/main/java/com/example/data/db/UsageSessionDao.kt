package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UsageSession
import kotlinx.coroutines.flow.Flow

data class CategoryStat(
    val category: String,
    val activeSeconds: Long,
    val sessionCount: Int
)

data class TopAppStat(
    val applicationName: String,
    val processName: String,
    val category: String,
    val activeSeconds: Long,
    val sessionCount: Int
)

@Dao
interface UsageSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: UsageSession)

    @Update
    suspend fun updateSession(session: UsageSession)

    @Query("SELECT * FROM usage_sessions WHERE isOpen = 1 LIMIT 1")
    suspend fun getOpenSession(): UsageSession?

    @Query("UPDATE usage_sessions SET isOpen = 0, endedAt = :endedIso, endTimeEpoch = :endedEpoch WHERE isOpen = 1")
    suspend fun closeOrphanSessions(endedIso: String, endedEpoch: Long)

    @Query("SELECT * FROM usage_sessions ORDER BY startTimeEpoch DESC")
    fun getAllSessionsFlow(): Flow<List<UsageSession>>

    @Query("SELECT * FROM usage_sessions WHERE startTimeEpoch >= :startTimeEpoch ORDER BY startTimeEpoch DESC")
    fun getSessionsSinceFlow(startTimeEpoch: Long): Flow<List<UsageSession>>

    @Query("SELECT * FROM usage_sessions WHERE startTimeEpoch >= :startTimeEpoch ORDER BY startTimeEpoch DESC")
    suspend fun getSessionsSince(startTimeEpoch: Long): List<UsageSession>

    @Query("SELECT * FROM usage_sessions WHERE startTimeEpoch >= :startEpoch AND startTimeEpoch <= :endEpoch ORDER BY startTimeEpoch DESC")
    suspend fun getSessionsInRange(startEpoch: Long, endEpoch: Long): List<UsageSession>

    @Query("SELECT * FROM usage_sessions WHERE isSynced = 0 ORDER BY startTimeEpoch ASC")
    suspend fun getPendingSessions(): List<UsageSession>

    @Query("SELECT COUNT(*) FROM usage_sessions WHERE isSynced = 0")
    fun getPendingCountFlow(): Flow<Int>

    @Query("UPDATE usage_sessions SET isSynced = 1, syncedAt = :syncedAt WHERE id IN (:sessionIds)")
    suspend fun markSessionsSynced(sessionIds: List<String>, syncedAt: Long)

    @Query("SELECT COUNT(*) FROM usage_sessions WHERE startTimeEpoch >= :startEpoch")
    fun getSessionCountSinceFlow(startEpoch: Long): Flow<Int>

    @Query("SELECT category, SUM(activeDurationSeconds) as activeSeconds, COUNT(*) as sessionCount FROM usage_sessions WHERE startTimeEpoch >= :startEpoch GROUP BY category ORDER BY activeSeconds DESC")
    fun getCategoryStatsSinceFlow(startEpoch: Long): Flow<List<CategoryStat>>

    @Query("SELECT applicationName, processName, category, SUM(activeDurationSeconds) as activeSeconds, COUNT(*) as sessionCount FROM usage_sessions WHERE startTimeEpoch >= :startEpoch GROUP BY processName ORDER BY activeSeconds DESC LIMIT :limit")
    fun getTopAppsSinceFlow(startEpoch: Long, limit: Int = 4): Flow<List<TopAppStat>>

    @Query("DELETE FROM usage_sessions")
    suspend fun deleteAll()
}
