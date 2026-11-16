package com.example.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.model.PrayerName
import com.example.domain.PrayerTimeCalculator
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Calendar
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * WorkManager worker that delivers a localized notification when a daily prayer time arrives
 * based on the user's geographic location, then schedules the next day's occurrence.
 */
class PrayerNotificationWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(applicationContext)
        val settings = prefs.settingsFlow.first()
        if (!settings.prayerNotificationsEnabled) {
            return Result.success()
        }

        val prayerNameRaw = inputData.getString(KEY_PRAYER_NAME) ?: return Result.failure()
        val prayer = runCatching { PrayerName.valueOf(prayerNameRaw) }.getOrNull()
            ?: return Result.failure()

        val coords = PrayerWorkScheduler.resolveLatestCoordinatesOrDefault(applicationContext, settings)
        val cal = Calendar.getInstance()
        val times = PrayerTimeCalculator.calculateForDate(
            year = cal.get(Calendar.YEAR),
            month1To12 = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            latitude = coords.first,
            longitude = coords.second,
            method = settings.calculationMethod,
            madhab = settings.asrMadhab,
        )
        val formattedTime = times.timeFor(prayer)

        ReminderScheduler.showImmediateNotification(
            context = applicationContext,
            title = "حان الآن موعد صلاة ${prayer.arabicName} ($formattedTime)",
            body = "حيّ على الصلاة — حسب موقعك في ${settings.cityName}",
            isPrayer = true,
            useAdhan = settings.adhanSoundEnabled,
            notifId = PRAYER_NOTIF_BASE_ID + prayer.ordinal,
        )

        // Schedule tomorrow's work for this prayer using the same geographic coordinates
        PrayerWorkScheduler.scheduleSinglePrayer(
            context = applicationContext,
            prayer = prayer,
            settings = settings.copy(latitude = coords.first, longitude = coords.second),
            forceTomorrow = true,
        )

        return Result.success()
    }

    companion object {
        const val KEY_PRAYER_NAME = "key_prayer_name"
        const val KEY_LATITUDE = "key_latitude"
        const val KEY_LONGITUDE = "key_longitude"
        const val KEY_CITY_NAME = "key_city_name"
        const val PRAYER_NOTIF_BASE_ID = 100
    }
}

/**
 * Periodic WorkManager sync job that refreshes daily prayer notification timings
 * whenever the date changes or the user's coarse location updates.
 */
class DailyPrayerWorkSyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = UserPreferencesRepository(applicationContext)
        val settings = prefs.settingsFlow.first()
        if (!settings.prayerNotificationsEnabled) {
            PrayerWorkScheduler.cancelAllPrayers(applicationContext)
            return Result.success()
        }

        val coords = PrayerWorkScheduler.resolveLatestCoordinatesOrDefault(applicationContext, settings)
        if (coords.first != settings.latitude || coords.second != settings.longitude) {
            prefs.updateLocation(settings.cityName, coords.first, coords.second)
        }
        PrayerWorkScheduler.scheduleAllPrayers(
            context = applicationContext,
            settings = settings.copy(latitude = coords.first, longitude = coords.second),
        )
        return Result.success()
    }
}

object PrayerWorkScheduler {
    const val TAG_PRAYER_WORK = "prayer_notifications"
    const val UNIQUE_DAILY_SYNC_WORK = "daily_prayer_work_sync"

    fun uniqueWorkNameFor(prayer: PrayerName): String = "prayer_notify_${prayer.name}"

    fun scheduleAllPrayers(context: Context, settings: UserSettings) {
        NotificationChannels.createAll(context)
        if (!settings.prayerNotificationsEnabled) {
            cancelAllPrayers(context)
            return
        }

        PrayerName.entries.forEach { prayer ->
            scheduleSinglePrayer(
                context = context,
                prayer = prayer,
                settings = settings,
                forceTomorrow = false,
            )
        }

        val dailySyncRequest = PeriodicWorkRequestBuilder<DailyPrayerWorkSyncWorker>(
            24,
            TimeUnit.HOURS,
        )
            .setInitialDelay(24, TimeUnit.HOURS)
            .addTag(TAG_PRAYER_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_DAILY_SYNC_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailySyncRequest,
        )
    }

    fun scheduleSinglePrayer(
        context: Context,
        prayer: PrayerName,
        settings: UserSettings,
        forceTomorrow: Boolean = false,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val delayMillis = computeDelayToNextOccurrenceMillis(
            prayer = prayer,
            settings = settings,
            nowMillis = nowMillis,
            forceTomorrow = forceTomorrow,
        )

        val input = Data.Builder()
            .putString(PrayerNotificationWorker.KEY_PRAYER_NAME, prayer.name)
            .putDouble(PrayerNotificationWorker.KEY_LATITUDE, settings.latitude)
            .putDouble(PrayerNotificationWorker.KEY_LONGITUDE, settings.longitude)
            .putString(PrayerNotificationWorker.KEY_CITY_NAME, settings.cityName)
            .build()

        val request = OneTimeWorkRequestBuilder<PrayerNotificationWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(input)
            .addTag(TAG_PRAYER_WORK)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            uniqueWorkNameFor(prayer),
            ExistingWorkPolicy.REPLACE,
            request,
        )
    }

    fun cancelAllPrayers(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelAllWorkByTag(TAG_PRAYER_WORK)
        wm.cancelUniqueWork(UNIQUE_DAILY_SYNC_WORK)
    }

    fun computeDelayToNextOccurrenceMillis(
        prayer: PrayerName,
        settings: UserSettings,
        nowMillis: Long = System.currentTimeMillis(),
        forceTomorrow: Boolean = false,
    ): Long {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        if (forceTomorrow) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        var times = PrayerTimeCalculator.calculateForDate(
            year = cal.get(Calendar.YEAR),
            month1To12 = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            latitude = settings.latitude,
            longitude = settings.longitude,
            method = settings.calculationMethod,
            madhab = settings.asrMadhab,
        )
        var mins = times.minutesFor(prayer)
        var targetCal = (cal.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, mins / 60)
            set(Calendar.MINUTE, mins % 60)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!forceTomorrow && targetCal.timeInMillis <= nowMillis + 5_000L) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            times = PrayerTimeCalculator.calculateForDate(
                year = cal.get(Calendar.YEAR),
                month1To12 = cal.get(Calendar.MONTH) + 1,
                day = cal.get(Calendar.DAY_OF_MONTH),
                latitude = settings.latitude,
                longitude = settings.longitude,
                method = settings.calculationMethod,
                madhab = settings.asrMadhab,
            )
            mins = times.minutesFor(prayer)
            targetCal = (cal.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, mins / 60)
                set(Calendar.MINUTE, mins % 60)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        return (targetCal.timeInMillis - nowMillis).coerceAtLeast(1_000L)
    }

    @SuppressLint("MissingPermission")
    suspend fun resolveLatestCoordinatesOrDefault(
        context: Context,
        settings: UserSettings,
    ): Pair<Double, Double> {
        val hasCoarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        if (!hasCoarse) {
            return settings.latitude to settings.longitude
        }
        val location = withTimeoutOrNull(2_000L) {
            runCatching {
                val client = LocationServices.getFusedLocationProviderClient(context)
                suspendCancellableCoroutine { cont ->
                    client.lastLocation
                        .addOnSuccessListener { loc ->
                            if (cont.isActive) cont.resume(loc)
                        }
                        .addOnFailureListener {
                            if (cont.isActive) cont.resume(null)
                        }
                }
            }.getOrNull()
        }
        return if (location != null) {
            location.latitude to location.longitude
        } else {
            settings.latitude to settings.longitude
        }
    }
}
