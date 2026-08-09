package com.example.network

import android.os.Build
import com.example.data.model.SessionPayload
import com.example.data.model.SyncMeta
import com.example.data.model.SyncPayload
import com.example.data.model.SyncRange
import com.example.data.repository.SessionRepository
import com.example.data.repository.SettingsRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

class SyncManager(
    private val sessionRepository: SessionRepository,
    private val settingsRepository: SettingsRepository
) {

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private fun getRetrofit(baseUrl: String): LyfStackSyncService {
        val rootUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(rootUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(LyfStackSyncService::class.java)
    }

    suspend fun performSync(
        range: SyncRange = SyncRange.SINCE_LAST,
        customFromIso: String? = null,
        customToIso: String? = null
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val settings = settingsRepository.getSettings()
            val sessionsToSync = if (range == SyncRange.SINCE_LAST) {
                sessionRepository.getPendingSessions()
            } else {
                sessionRepository.getSessionsForRange(range)
            }

            if (sessionsToSync.isEmpty()) {
                settingsRepository.recordSyncSuccess()
                return@withContext Result.success(0)
            }

            val deviceName = "${Build.MANUFACTURER.replaceFirstChar { it.uppercase() }} ${Build.MODEL}"
            val exportedAtIso = formatIso8601(System.currentTimeMillis())

            val sessionPayloads = sessionsToSync.map { s ->
                SessionPayload(
                    id = s.id,
                    applicationName = s.applicationName,
                    processName = s.processName,
                    processId = s.processId,
                    startedAt = s.startedAt,
                    endedAt = s.endedAt,
                    activeDurationSeconds = s.activeDurationSeconds,
                    idleDurationSeconds = s.idleDurationSeconds,
                    lastState = s.lastState,
                    isOpen = s.isOpen,
                    category = s.category
                )
            }

            val payload = SyncPayload(
                source = "LyfStack.Agent.Android",
                deviceId = settings.deviceId,
                device = deviceName,
                platform = "android",
                exportedAt = exportedAtIso,
                aggregation = "usage_sessions",
                sync = SyncMeta(
                    range = range.value,
                    from = customFromIso,
                    to = customToIso,
                    pendingOnly = range == SyncRange.SINCE_LAST
                ),
                sessionCount = sessionPayloads.size,
                sessions = sessionPayloads
            )

            val service = getRetrofit(extractBaseUrl(settings.syncEndpointUrl))
            val response = service.syncActivity(
                url = settings.syncEndpointUrl,
                range = range.value,
                from = customFromIso,
                to = customToIso,
                payload = payload
            )

            if (response.isSuccessful) {
                val now = System.currentTimeMillis()
                val sessionIds = sessionsToSync.map { it.id }
                sessionRepository.markSessionsSynced(sessionIds, now)
                settingsRepository.recordSyncSuccess(now)
                Result.success(sessionPayloads.size)
            } else {
                Result.failure(Exception("Sync failed: HTTP ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun extractBaseUrl(fullUrl: String): String {
        return try {
            val uri = java.net.URI(fullUrl)
            "${uri.scheme}://${uri.host}${if (uri.port != -1) ":${uri.port}" else ""}/"
        } catch (e: Exception) {
            "https://api.lyfstack.app/"
        }
    }

    companion object {
        fun formatIso8601(epochMs: Long): String {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            return sdf.format(Date(epochMs))
        }
    }
}
