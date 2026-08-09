package com.example.service

import android.app.AppOpsManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.Process
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.LyfStackApplication
import com.example.MainActivity
import com.example.R
import com.example.data.model.UsageSession
import com.example.data.repository.CategoryResolver
import com.example.network.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class TrackingStatus(
    val isTrackingActive: Boolean = false,
    val isPaused: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val currentPackage: String = "",
    val currentAppName: String = "",
    val currentCategory: String = "",
    val currentState: String = "Active"
)

class TrackingService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private var trackingJob: Job? = null

    private lateinit var usageStatsManager: UsageStatsManager
    private lateinit var powerManager: PowerManager
    private lateinit var keyguardManager: KeyguardManager

    override fun onCreate() {
        super.onCreate()
        instance = this
        usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        // Close orphan open sessions on startup
        serviceScope.launch {
            val now = System.currentTimeMillis()
            val iso = SyncManager.formatIso8601(now)
            LyfStackApplication.instance.sessionRepository.closeOrphanSessions(iso, now)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val fgsType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
                if (fgsType != 0) {
                    startForeground(LyfStackApplication.NOTIFICATION_ID, createNotification("Initializing LyfStack Agent..."), fgsType)
                } else {
                    startForeground(LyfStackApplication.NOTIFICATION_ID, createNotification("Initializing LyfStack Agent..."))
                }
            } else {
                startForeground(LyfStackApplication.NOTIFICATION_ID, createNotification("Initializing LyfStack Agent..."))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to call startForeground", e)
        }
        startTrackingLoop()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_TOGGLE_PAUSE) {
            serviceScope.launch {
                val current = LyfStackApplication.instance.settingsRepository.getSettings().isTrackingPaused
                LyfStackApplication.instance.settingsRepository.setTrackingPaused(!current)
            }
        }
        return START_STICKY
    }

    private fun startTrackingLoop() {
        trackingJob?.cancel()
        trackingJob = serviceScope.launch {
            while (isActive) {
                try {
                    sampleForegroundApp()
                } catch (e: Exception) {
                    Log.e(TAG, "Error sampling foreground app", e)
                }
                delay(SAMPLE_INTERVAL_MS)
            }
        }
    }

    private suspend fun sampleForegroundApp() {
        val app = LyfStackApplication.instance
        val settings = app.settingsRepository.getSettings()
        val hasPermission = checkUsagePermission(this)

        if (!hasPermission || settings.isTrackingPaused) {
            _trackingStatus.value = TrackingStatus(
                isTrackingActive = false,
                isPaused = settings.isTrackingPaused,
                hasUsagePermission = hasPermission,
                currentPackage = "",
                currentAppName = if (settings.isTrackingPaused) "Tracking Paused" else "Permission Required",
                currentCategory = "None",
                currentState = if (settings.isTrackingPaused) "Paused" else "Idle"
            )
            updateNotification(if (settings.isTrackingPaused) "Tracking is paused" else "Usage Access Permission Required")
            return
        }

        val ignoreList = CategoryResolver.parseIgnoreList(settings.ignoreListRaw)
        val isScreenOn = powerManager.isInteractive
        val isLocked = keyguardManager.isKeyguardLocked

        val state = if (!isScreenOn || isLocked) "Idle" else "Active"
        val fgPackage = getForegroundPackageName()

        if (fgPackage.isBlank() || ignoreList.contains(fgPackage)) {
            _trackingStatus.value = _trackingStatus.value.copy(
                isTrackingActive = true,
                isPaused = false,
                hasUsagePermission = true,
                currentState = state
            )
            return
        }

        val appName = getAppNameFromPackage(fgPackage)
        val overrideMap = app.sessionRepository.getCategoryOverrideMap()
        val resolver = CategoryResolver(overrideMap)
        val category = resolver.resolveCategory(fgPackage)

        _trackingStatus.value = TrackingStatus(
            isTrackingActive = true,
            isPaused = false,
            hasUsagePermission = true,
            currentPackage = fgPackage,
            currentAppName = appName,
            currentCategory = category,
            currentState = state
        )

        updateNotification("Active: $appName ($category)")

        // Session aggregation logic
        val now = System.currentTimeMillis()
        val nowIso = SyncManager.formatIso8601(now)
        val openSession = app.sessionRepository.getOpenSession()

        if (openSession != null) {
            if (openSession.processName == fgPackage && openSession.lastState == state) {
                // Continuation of existing session
                val addedActive = if (state == "Active") (SAMPLE_INTERVAL_MS / 1000) else 0L
                val addedIdle = if (state == "Idle") (SAMPLE_INTERVAL_MS / 1000) else 0L

                val updatedSession = openSession.copy(
                    activeDurationSeconds = openSession.activeDurationSeconds + addedActive,
                    idleDurationSeconds = openSession.idleDurationSeconds + addedIdle,
                    endedAt = nowIso,
                    endTimeEpoch = now,
                    category = category
                )
                app.sessionRepository.insertOrUpdateSession(updatedSession)
            } else {
                // Close old session & create new session
                val closedSession = openSession.copy(
                    isOpen = false,
                    endedAt = nowIso,
                    endTimeEpoch = now
                )
                app.sessionRepository.insertOrUpdateSession(closedSession)

                val newSession = UsageSession(
                    id = UUID.randomUUID().toString(),
                    applicationName = appName,
                    processName = fgPackage,
                    startedAt = nowIso,
                    endedAt = nowIso,
                    activeDurationSeconds = if (state == "Active") (SAMPLE_INTERVAL_MS / 1000) else 0L,
                    idleDurationSeconds = if (state == "Idle") (SAMPLE_INTERVAL_MS / 1000) else 0L,
                    lastState = state,
                    isOpen = true,
                    category = category,
                    isSynced = false,
                    startTimeEpoch = now,
                    endTimeEpoch = now
                )
                app.sessionRepository.insertOrUpdateSession(newSession)
            }
        } else {
            // Create first open session
            val newSession = UsageSession(
                id = UUID.randomUUID().toString(),
                applicationName = appName,
                processName = fgPackage,
                startedAt = nowIso,
                endedAt = nowIso,
                activeDurationSeconds = if (state == "Active") (SAMPLE_INTERVAL_MS / 1000) else 0L,
                idleDurationSeconds = if (state == "Idle") (SAMPLE_INTERVAL_MS / 1000) else 0L,
                lastState = state,
                isOpen = true,
                category = category,
                isSynced = false,
                startTimeEpoch = now,
                endTimeEpoch = now
            )
            app.sessionRepository.insertOrUpdateSession(newSession)
        }
    }

    private fun getForegroundPackageName(): String {
        val now = System.currentTimeMillis()
        val events = usageStatsManager.queryEvents(now - 10000, now)
        val event = UsageEvents.Event()
        var currentPackage = ""
        var latestEventTime = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp > latestEventTime) {
                    latestEventTime = event.timeStamp
                    currentPackage = event.packageName
                }
            }
        }
        return currentPackage
    }

    private fun getAppNameFromPackage(packageName: String): String {
        return try {
            val pm = packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
                .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, LyfStackApplication.NOTIFICATION_CHANNEL_ID)
            .setContentTitle("LyfStack Agent (Android)")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(contentText: String) {
        val notification = createNotification(contentText)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(LyfStackApplication.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {

        serviceScope.cancel()
        instance = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "LyfStackTracking"
        const val SAMPLE_INTERVAL_MS = 5000L
        const val ACTION_TOGGLE_PAUSE = "com.example.ACTION_TOGGLE_PAUSE"

        @Volatile
        var instance: TrackingService? = null
            private set

        private val _trackingStatus = MutableStateFlow(TrackingStatus())
        val trackingStatus: StateFlow<TrackingStatus> = _trackingStatus

        fun checkUsagePermission(context: Context): Boolean {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                appOps.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            return mode == AppOpsManager.MODE_ALLOWED
        }

        fun startService(context: Context) {
            val intent = Intent(context, TrackingService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not start TrackingService foreground service", e)
            }
        }
    }
}
