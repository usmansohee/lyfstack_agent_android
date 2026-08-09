package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.example.data.db.AppDatabase
import com.example.data.repository.SessionRepository
import com.example.data.repository.SettingsRepository

class LyfStackApplication : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var sessionRepository: SessionRepository
        private set

    lateinit var settingsRepository: SettingsRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AppDatabase.getDatabase(this)
        sessionRepository = SessionRepository(database.usageSessionDao(), database.categoryOverrideDao())
        settingsRepository = SettingsRepository(this)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "LyfStack Activity Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "LyfStack background app usage tracking service notification"
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "lyfstack_tracking_channel"
        const val NOTIFICATION_ID = 1001

        lateinit var instance: LyfStackApplication
            private set
    }
}
