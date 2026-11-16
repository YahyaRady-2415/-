package com.example.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.model.PrayerName
import com.example.domain.PrayerTimeCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

object NotificationChannels {
    const val CHANNEL_PRAYERS = "tariq_prayers_channel"
    const val CHANNEL_PRAYERS_ADHAN = "tariq_prayers_adhan_channel"
    const val CHANNEL_ADHKAR = "tariq_adhkar_channel"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val prayerChannel = NotificationChannel(
            CHANNEL_PRAYERS,
            context.getString(R.string.notification_channel_prayers),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات دخول أوقات الصلوات الخمس"
        }

        val adhanTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val prayerAdhanChannel = NotificationChannel(
            CHANNEL_PRAYERS_ADHAN,
            "مواقيت الصلاة مع صوت التنبيه",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "تنبيهات الصلاة مع نغمة منبه صوتية"
            setSound(
                adhanTone,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val adhkarChannel = NotificationChannel(
            CHANNEL_ADHKAR,
            context.getString(R.string.notification_channel_adhkar),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "تذكير بأذكار الصباح والمساء والورد القرآني والنوافل"
        }

        manager.createNotificationChannel(prayerChannel)
        manager.createNotificationChannel(prayerAdhanChannel)
        manager.createNotificationChannel(adhkarChannel)
    }
}

object ReminderScheduler {
    private const val EXTRA_TITLE = "extra_title"
    private const val EXTRA_BODY = "extra_body"
    private const val EXTRA_IS_PRAYER = "extra_is_prayer"
    private const val EXTRA_USE_ADHAN = "extra_use_adhan"
    private const val EXTRA_NOTIF_ID = "extra_notif_id"

    fun rescheduleAll(context: Context, settings: UserSettings) {
        NotificationChannels.createAll(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel legacy prayer alarms and use WorkManager for location-aware prayer notifications
        PrayerName.entries.forEachIndexed { idx, _ ->
            cancel(context, alarmManager, 100 + idx)
        }
        PrayerWorkScheduler.scheduleAllPrayers(context, settings)

        scheduleOrCancel(
            context, alarmManager, 201,
            settings.morningAdhkarReminderEnabled,
            settings.morningAdhkarHour, settings.morningAdhkarMinute,
            "أذكار الصباح", "ابدأ يومك بحصن المسلم وأذكار الصباح المباركة",
        )
        scheduleOrCancel(
            context, alarmManager, 202,
            settings.eveningAdhkarReminderEnabled,
            settings.eveningAdhkarHour, settings.eveningAdhkarMinute,
            "أذكار المساء", "لا تنسَ وردك من أذكار المساء لحفظ الله ورعايته",
        )
        scheduleOrCancel(
            context, alarmManager, 203,
            settings.quranWirdReminderEnabled,
            settings.quranWirdHour, settings.quranWirdMinute,
            "وِرد القرآن الكريم", "موعد تلاوة وردك اليومي من كتاب الله",
        )
        scheduleOrCancel(
            context, alarmManager, 204,
            settings.qiyamReminderEnabled,
            settings.qiyamHour, settings.qiyamMinute,
            "قيام الليل والوتر", "أفضل الصلاة بعد الفريضة صلاة الليل — ركعات يسيرة بأجر عظيم",
        )
        scheduleOrCancel(
            context, alarmManager, 205,
            settings.duhaReminderEnabled,
            settings.duhaHour, settings.duhaMinute,
            "صلاة الضحى", "صلاة الأوابين — ركعتا الضحى صدقة عن كل مفاصل الجسد",
        )
    }

    private fun scheduleOrCancel(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        enabled: Boolean,
        hour: Int,
        minute: Int,
        title: String,
        body: String,
    ) {
        if (enabled) {
            scheduleAtHourMinute(context, alarmManager, requestCode, hour, minute, title, body, false, false)
        } else {
            cancel(context, alarmManager, requestCode)
        }
    }

    private fun scheduleAtHourMinute(
        context: Context,
        alarmManager: AlarmManager,
        requestCode: Int,
        hour: Int,
        minute: Int,
        title: String,
        body: String,
        isPrayer: Boolean,
        useAdhan: Boolean,
    ) {
        val triggerCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis() + 5_000L) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TITLE, title)
            putExtra(EXTRA_BODY, body)
            putExtra(EXTRA_IS_PRAYER, isPrayer)
            putExtra(EXTRA_USE_ADHAN, useAdhan)
            putExtra(EXTRA_NOTIF_ID, requestCode)
        }
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pi)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerCal.timeInMillis, pi)
        }
    }

    private fun cancel(context: Context, alarmManager: AlarmManager, requestCode: Int) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.cancel(pi)
    }

    fun showImmediateNotification(
        context: Context,
        title: String,
        body: String,
        isPrayer: Boolean = false,
        useAdhan: Boolean = false,
        notifId: Int = 999,
    ) {
        NotificationChannels.createAll(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val channelId = when {
            isPrayer && useAdhan -> NotificationChannels.CHANNEL_PRAYERS_ADHAN
            isPrayer -> NotificationChannels.CHANNEL_PRAYERS
            else -> NotificationChannels.CHANNEL_ADHKAR
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            notifId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingOpen)
            .setAutoCancel(true)
            .setPriority(if (isPrayer) NotificationCompat.PRIORITY_HIGH else NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(notifId, notification)
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("extra_title") ?: "تذكير طريق الجنة"
        val body = intent.getStringExtra("extra_body") ?: "حافظ على وردك اليومي من الطاعات"
        val isPrayer = intent.getBooleanExtra("extra_is_prayer", false)
        val useAdhan = intent.getBooleanExtra("extra_use_adhan", false)
        val notifId = intent.getIntExtra("extra_notif_id", 100)

        ReminderScheduler.showImmediateNotification(context, title, body, isPrayer, useAdhan, notifId)

        // Reschedule for tomorrow
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            runCatching {
                val settings = UserPreferencesRepository(context).settingsFlow.first()
                ReminderScheduler.rescheduleAll(context, settings)
            }
            pendingResult.finish()
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val settings = UserPreferencesRepository(context).settingsFlow.first()
                    ReminderScheduler.rescheduleAll(context, settings)
                }
                pendingResult.finish()
            }
        }
    }
}
