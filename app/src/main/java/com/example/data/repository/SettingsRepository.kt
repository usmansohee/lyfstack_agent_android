package com.example.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "lyfstack_settings")

data class AgentSettings(
    val deviceId: String,
    val firstInstalledAt: Long,
    val firstSyncAt: Long?,
    val lastSyncAt: Long?,
    val isTrackingPaused: Boolean,
    val autoSyncEnabled: Boolean,
    val autoSyncIntervalMinutes: Int,
    val syncEndpointUrl: String,
    val wsEnabled: Boolean,
    val wsUrl: String,
    val wsToken: String,
    val ignoreListRaw: String
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val DEVICE_ID = stringPreferencesKey("device_id")
        val FIRST_INSTALLED_AT = longPreferencesKey("first_installed_at")
        val FIRST_SYNC_AT = longPreferencesKey("first_sync_at")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
        val IS_TRACKING_PAUSED = booleanPreferencesKey("is_tracking_paused")
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val AUTO_SYNC_INTERVAL_MINUTES = intPreferencesKey("auto_sync_interval_minutes")
        val SYNC_ENDPOINT_URL = stringPreferencesKey("sync_endpoint_url")
        val WS_ENABLED = booleanPreferencesKey("ws_enabled")
        val WS_URL = stringPreferencesKey("ws_url")
        val WS_TOKEN = stringPreferencesKey("ws_token")
        val IGNORE_LIST_RAW = stringPreferencesKey("ignore_list_raw")
    }

    val settingsFlow: Flow<AgentSettings> = context.dataStore.data.map { prefs ->
        val deviceId = prefs[Keys.DEVICE_ID] ?: UUID.randomUUID().toString().also { newId ->
            // Note: will be saved asynchronously on first access
        }
        val firstInstalledAt = prefs[Keys.FIRST_INSTALLED_AT] ?: System.currentTimeMillis()

        AgentSettings(
            deviceId = deviceId,
            firstInstalledAt = firstInstalledAt,
            firstSyncAt = prefs[Keys.FIRST_SYNC_AT],
            lastSyncAt = prefs[Keys.LAST_SYNC_AT],
            isTrackingPaused = prefs[Keys.IS_TRACKING_PAUSED] ?: false,
            autoSyncEnabled = prefs[Keys.AUTO_SYNC_ENABLED] ?: true,
            autoSyncIntervalMinutes = prefs[Keys.AUTO_SYNC_INTERVAL_MINUTES] ?: 15,
            syncEndpointUrl = prefs[Keys.SYNC_ENDPOINT_URL] ?: "https://api.lyfstack.app/api/v1/device-activity/sync",
            wsEnabled = prefs[Keys.WS_ENABLED] ?: false,
            wsUrl = prefs[Keys.WS_URL] ?: "wss://api.lyfstack.app/device-connection",
            wsToken = prefs[Keys.WS_TOKEN] ?: "",
            ignoreListRaw = prefs[Keys.IGNORE_LIST_RAW] ?: "com.android.systemui\ncom.google.android.apps.nexuslauncher"
        )
    }

    suspend fun getSettings(): AgentSettings {
        ensureInitialized()
        return settingsFlow.first()
    }

    private suspend fun ensureInitialized() {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.DEVICE_ID] == null) {
                prefs[Keys.DEVICE_ID] = UUID.randomUUID().toString()
            }
            if (prefs[Keys.FIRST_INSTALLED_AT] == null) {
                prefs[Keys.FIRST_INSTALLED_AT] = System.currentTimeMillis()
            }
        }
    }

    suspend fun setTrackingPaused(paused: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.IS_TRACKING_PAUSED] = paused
        }
    }

    suspend fun updateSyncSettings(
        autoSyncEnabled: Boolean,
        intervalMinutes: Int,
        syncEndpointUrl: String,
        wsEnabled: Boolean,
        wsUrl: String,
        wsToken: String,
        ignoreListRaw: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[Keys.AUTO_SYNC_ENABLED] = autoSyncEnabled
            prefs[Keys.AUTO_SYNC_INTERVAL_MINUTES] = intervalMinutes
            prefs[Keys.SYNC_ENDPOINT_URL] = syncEndpointUrl
            prefs[Keys.WS_ENABLED] = wsEnabled
            prefs[Keys.WS_URL] = wsUrl
            prefs[Keys.WS_TOKEN] = wsToken
            prefs[Keys.IGNORE_LIST_RAW] = ignoreListRaw
        }
    }

    suspend fun recordSyncSuccess(timestamp: Long = System.currentTimeMillis()) {
        context.dataStore.edit { prefs ->
            if (prefs[Keys.FIRST_SYNC_AT] == null) {
                prefs[Keys.FIRST_SYNC_AT] = timestamp
            }
            prefs[Keys.LAST_SYNC_AT] = timestamp
        }
    }
}
