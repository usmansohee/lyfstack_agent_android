package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.LyfStackApplication
import com.example.data.db.CategoryStat
import com.example.data.db.TopAppStat
import com.example.data.model.CategoryOverride
import com.example.data.model.SyncRange
import com.example.data.model.UsageSession
import com.example.data.model.WsConnectionState
import com.example.data.repository.AgentSettings
import com.example.data.repository.RangeStats
import com.example.network.LyfStackWebSocketClient
import com.example.network.SyncManager
import com.example.service.SyncWorker
import com.example.service.TrackingService
import com.example.service.TrackingStatus
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as LyfStackApplication
    private val sessionRepo = app.sessionRepository
    private val settingsRepo = app.settingsRepository
    val syncManager = SyncManager(sessionRepo, settingsRepo)
    val wsClient = LyfStackWebSocketClient(settingsRepo, syncManager, viewModelScope)

    val selectedRange = MutableStateFlow(SyncRange.TODAY)
    val historySearchQuery = MutableStateFlow("")
    val historyCategoryFilter = MutableStateFlow("All")

    val isSyncing = MutableStateFlow(false)
    val syncMessage = MutableStateFlow<String?>(null)

    val trackingStatus: StateFlow<TrackingStatus> = TrackingService.trackingStatus
    val wsConnectionState: StateFlow<WsConnectionState> = wsClient.connectionState

    val settings: StateFlow<AgentSettings> = settingsRepo.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AgentSettings(
            deviceId = "loading...",
            firstInstalledAt = System.currentTimeMillis(),
            firstSyncAt = null,
            lastSyncAt = null,
            isTrackingPaused = false,
            autoSyncEnabled = true,
            autoSyncIntervalMinutes = 15,
            syncEndpointUrl = "https://api.lyfstack.app/api/v1/device-activity/sync",
            wsEnabled = false,
            wsUrl = "wss://api.lyfstack.app/device-connection",
            wsToken = "",
            ignoreListRaw = ""
        )
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val rangeStats: StateFlow<RangeStats> = selectedRange.flatMapLatest { range ->
        sessionRepo.getRangeStatsFlow(range)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RangeStats(0, 0, 0, 0, 0, 0, 0)
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val topApps: StateFlow<List<TopAppStat>> = selectedRange.flatMapLatest { range ->
        sessionRepo.getTopAppsFlow(range, 4)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val categoryStats: StateFlow<List<CategoryStat>> = selectedRange.flatMapLatest { range ->
        sessionRepo.getCategoryStatsFlow(range)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredSessions: StateFlow<List<UsageSession>> = combine(
        selectedRange.flatMapLatest { range -> sessionRepo.getSessionsForRangeFlow(range) },
        historySearchQuery,
        historyCategoryFilter
    ) { sessions, query, category ->
        sessions.filter { s ->
            val matchesQuery = query.isBlank() || s.applicationName.contains(query, ignoreCase = true) || s.processName.contains(query, ignoreCase = true)
            val matchesCat = category == "All" || s.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCat
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categoryOverrides: StateFlow<List<CategoryOverride>> = sessionRepo.categoryOverridesFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        wsClient.start()
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { s ->
                SyncWorker.scheduleAutoSync(getApplication(), s.autoSyncIntervalMinutes, s.autoSyncEnabled)
            }
        }
    }

    fun setRange(range: SyncRange) {
        selectedRange.value = range
    }

    fun toggleTrackingPause() {
        viewModelScope.launch {
            val current = settings.value.isTrackingPaused
            settingsRepo.setTrackingPaused(!current)
        }
    }

    fun triggerSync(range: SyncRange = selectedRange.value) {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = "Syncing..."
            val result = syncManager.performSync(range)
            isSyncing.value = false
            if (result.isSuccess) {
                val count = result.getOrDefault(0)
                syncMessage.value = "Synced $count sessions"
            } else {
                syncMessage.value = "Sync failed: ${result.exceptionOrNull()?.message}"
            }
        }
    }

    fun saveSettings(
        autoSyncEnabled: Boolean,
        intervalMinutes: Int,
        syncUrl: String,
        wsEnabled: Boolean,
        wsUrl: String,
        wsToken: String,
        ignoreListRaw: String
    ) {
        viewModelScope.launch {
            settingsRepo.updateSyncSettings(
                autoSyncEnabled = autoSyncEnabled,
                intervalMinutes = intervalMinutes,
                syncEndpointUrl = syncUrl,
                wsEnabled = wsEnabled,
                wsUrl = wsUrl,
                wsToken = wsToken,
                ignoreListRaw = ignoreListRaw
            )
            Toast.makeText(getApplication(), "Settings Saved", Toast.LENGTH_SHORT).show()
        }
    }

    fun addCategoryOverride(packageName: String, category: String) {
        viewModelScope.launch {
            if (packageName.isNotBlank() && category.isNotBlank()) {
                sessionRepo.setCategoryOverride(packageName.trim(), category.trim())
            }
        }
    }

    fun removeCategoryOverride(packageName: String) {
        viewModelScope.launch {
            sessionRepo.removeCategoryOverride(packageName)
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val sessions = filteredSessions.value
            val sb = StringBuilder()
            sb.append("ID,App Name,Package,Category,Started,Ended,Active (s),Idle (s),State,Is Open,Is Synced\n")
            sessions.forEach { s ->
                sb.append("\"${s.id}\",\"${s.applicationName}\",\"${s.processName}\",\"${s.category}\",\"${s.startedAt}\",\"${s.endedAt ?: ""}\",${s.activeDurationSeconds},${s.idleDurationSeconds},\"${s.lastState}\",${s.isOpen},${s.isSynced}\n")
            }
            shareFile(context, sb.toString(), "lyfstack_sessions.csv", "text/csv")
        }
    }

    fun exportJson(context: Context) {
        viewModelScope.launch {
            val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
            val listType = com.squareup.moshi.Types.newParameterizedType(List::class.java, UsageSession::class.java)
            val json = moshi.adapter<List<UsageSession>>(listType).indent("  ").toJson(filteredSessions.value)
            shareFile(context, json, "lyfstack_sessions.json", "application/json")
        }
    }

    private fun shareFile(context: Context, content: String, filename: String, mimeType: String) {
        try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) exportDir.mkdirs()
            val file = File(exportDir, filename)
            file.writeText(content)

            val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Export LyfStack History"))
        } catch (e: Exception) {
            Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        fun formatDuration(seconds: Long): String {
            if (seconds <= 0) return "0s"
            val hrs = seconds / 3600
            val mins = (seconds % 3600) / 60
            val secs = seconds % 60
            return when {
                hrs > 0 -> "${hrs}h ${mins}m"
                mins > 0 -> "${mins}m ${secs}s"
                else -> "${secs}s"
            }
        }

        fun formatTimestamp(epochMs: Long?): String {
            if (epochMs == null || epochMs <= 0) return "Never"
            val sdf = SimpleDateFormat("MMM d, yyyy · HH:mm", Locale.getDefault())
            return sdf.format(Date(epochMs))
        }
    }
}
