package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

enum class SyncRange(val value: String, val displayName: String, val shortLabel: String = displayName) {
    SINCE_LAST("since_last", "Unsynced Data Only", "Unsynced only"),
    TODAY("today", "Today"),
    WEEK("week", "This Week", "This week"),
    MONTH("month", "This Month", "This month"),
    YEAR("year", "This Year", "This year"),
    ALL("all", "All Time", "All time"),
    CUSTOM("custom", "Custom Range", "Custom");

    companion object {
        fun fromString(str: String?): SyncRange {
            if (str == null) return SINCE_LAST
            return entries.firstOrNull { 
                it.value.equals(str, ignoreCase = true) || it.name.equals(str, ignoreCase = true) 
            } ?: SINCE_LAST
        }
    }
}

@JsonClass(generateAdapter = true)
data class SyncMeta(
    @Json(name = "range") val range: String,
    @Json(name = "from") val from: String? = null,
    @Json(name = "to") val to: String? = null,
    @Json(name = "pendingOnly") val pendingOnly: Boolean = true
)

@JsonClass(generateAdapter = true)
data class SessionPayload(
    @Json(name = "id") val id: String,
    @Json(name = "applicationName") val applicationName: String,
    @Json(name = "processName") val processName: String,
    @Json(name = "processId") val processId: Int? = null,
    @Json(name = "startedAt") val startedAt: String,
    @Json(name = "endedAt") val endedAt: String?,
    @Json(name = "activeDurationSeconds") val activeDurationSeconds: Long,
    @Json(name = "idleDurationSeconds") val idleDurationSeconds: Long,
    @Json(name = "lastState") val lastState: String,
    @Json(name = "isOpen") val isOpen: Boolean,
    @Json(name = "category") val category: String? = null
)

@JsonClass(generateAdapter = true)
data class SyncPayload(
    @Json(name = "source") val source: String = "LyfStack.Agent.Android",
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "device") val device: String,
    @Json(name = "platform") val platform: String = "android",
    @Json(name = "exportedAt") val exportedAt: String,
    @Json(name = "aggregation") val aggregation: String = "usage_sessions",
    @Json(name = "sync") val sync: SyncMeta,
    @Json(name = "sessionCount") val sessionCount: Int,
    @Json(name = "sessions") val sessions: List<SessionPayload>
)

@JsonClass(generateAdapter = true)
data class SyncResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "message") val message: String? = null,
    @Json(name = "syncedCount") val syncedCount: Int? = null
)

// WebSocket Frame Models
@JsonClass(generateAdapter = true)
data class WsHelloFrame(
    @Json(name = "type") val type: String = "HELLO",
    @Json(name = "deviceId") val deviceId: String,
    @Json(name = "device") val device: String,
    @Json(name = "platform") val platform: String = "android",
    @Json(name = "agentVersion") val agentVersion: String = "1.0.0"
)

@JsonClass(generateAdapter = true)
data class WsIncomingFrame(
    @Json(name = "type") val type: String,
    @Json(name = "range") val range: String? = null,
    @Json(name = "from") val from: String? = null,
    @Json(name = "to") val to: String? = null,
    @Json(name = "requestId") val requestId: String? = null
)

@JsonClass(generateAdapter = true)
data class WsOutgoingFrame(
    @Json(name = "type") val type: String,
    @Json(name = "requestId") val requestId: String? = null,
    @Json(name = "success") val success: Boolean? = null,
    @Json(name = "sessionCount") val sessionCount: Int? = null,
    @Json(name = "isTrackingActive") val isTrackingActive: Boolean? = null,
    @Json(name = "message") val message: String? = null
)

enum class WsConnectionState(val label: String) {
    OFF("Off"),
    CONNECTING("Connecting"),
    ONLINE("Online"),
    RECONNECTING("Reconnecting")
}
