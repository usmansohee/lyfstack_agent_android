package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "usage_sessions",
    indices = [
        Index(value = ["isSynced"]),
        Index(value = ["startTimeEpoch"]),
        Index(value = ["processName"])
    ]
)
data class UsageSession(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val applicationName: String,
    val processName: String,
    val processId: Int? = null,
    val startedAt: String, // ISO-8601
    val endedAt: String? = null, // ISO-8601
    val activeDurationSeconds: Long = 0,
    val idleDurationSeconds: Long = 0,
    val lastState: String = "Active", // "Active" | "Idle"
    val isOpen: Boolean = true,
    val category: String = AppCategory.OTHER.displayName,
    val isSynced: Boolean = false,
    val syncedAt: Long? = null,
    val startTimeEpoch: Long = System.currentTimeMillis(),
    val endTimeEpoch: Long = System.currentTimeMillis()
)
