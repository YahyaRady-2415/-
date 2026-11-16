package com.example

import android.app.Application
import com.example.data.local.UserPreferencesRepository
import com.example.notifications.NotificationChannels
import com.example.notifications.PrayerWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class TariqAlJannahApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.createAll(this)
        appScope.launch {
            runCatching {
                val settings = UserPreferencesRepository(this@TariqAlJannahApp).settingsFlow.first()
                PrayerWorkScheduler.scheduleAllPrayers(this@TariqAlJannahApp, settings)
            }
        }
    }
}
