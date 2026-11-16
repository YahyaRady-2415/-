package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.UserSettings
import com.example.data.model.AchievementBadge
import com.example.data.model.AsrMadhab
import com.example.data.model.CalculationMethod
import com.example.data.model.CityPresets
import com.example.ui.components.CelActionButton
import com.example.ui.components.CelCard
import com.example.ui.components.CelProgressBar
import com.example.ui.components.CelSectionHeader
import com.example.ui.theme.LocalCelColors
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MoreSettingsScreen(
    settings: UserSettings,
    achievements: List<AchievementBadge>,
    locationStatusMessage: String?,
    onToggleDarkMode: (Boolean) -> Unit,
    onToggleSnowfall: (Boolean) -> Unit,
    onSelectCityPreset: (String, Double, Double, CalculationMethod) -> Unit,
    onUpdateCalculationMethod: (CalculationMethod) -> Unit,
    onUpdateAsrMadhab: (AsrMadhab) -> Unit,
    onUpdatePrayerNotifications: (Boolean, Boolean) -> Unit,
    onUpdateReminder: (String, Boolean, Int, Int) -> Unit,
    onSendTestNotification: () -> Unit,
    onRefreshGpsLocation: () -> Unit,
    onExportBackupJson: suspend () -> String,
) {
    val cel = LocalCelColors.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var editingReminder by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) onSendTestNotification()
        }
    )

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (granted) onRefreshGpsLocation()
        }
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Achievements Section
        item {
            CelCard(
                borderColor = cel.goldAccent,
                testTag = "achievements_section_card",
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_achievement_crest),
                            contentDescription = "شعار الإنجازات",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(60.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "الإنجازات والشارات",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = cel.textPrimary,
                            )
                            val unlocked = achievements.count { it.isUnlocked }
                            Text(
                                text = "شارات الثبات المفتوحة: $unlocked من ${achievements.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = cel.goldAccent,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        achievements.forEach { badge ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = cel.cardSurfaceAlt,
                                border = BorderStroke(
                                    2.dp,
                                    if (badge.isUnlocked) cel.goldAccent else cel.cardBorder.copy(alpha = 0.4f)
                                ),
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (badge.isUnlocked) Icons.Filled.Verified else Icons.Filled.EmojiEvents,
                                                contentDescription = null,
                                                tint = if (badge.isUnlocked) cel.goldAccent else cel.textSecondary,
                                                modifier = Modifier.size(22.dp),
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Text(
                                                text = badge.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = cel.textPrimary,
                                            )
                                        }
                                        Text(
                                            text = badge.requirementLabel,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (badge.isUnlocked) cel.onTimeColor else cel.primaryAccent,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = badge.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = cel.textSecondary,
                                    )
                                    Spacer(Modifier.height(6.dp))
                                    CelProgressBar(
                                        progress = badge.progress,
                                        fillColor = if (badge.isUnlocked) cel.goldAccent else cel.primaryAccent,
                                        height = 8.dp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Reminders & Local Notifications Settings
        item {
            CelCard(testTag = "notifications_settings_card") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CelSectionHeader(
                        title = "التنبيهات والإشعارات المحلية",
                        subtitle = "تعمل محلياً بالكامل دون اتصال بخادم خارجي",
                    )

                    SettingSwitchRow(
                        title = "إشعارات مواقيت الصلاة (WorkManager)",
                        subtitle = "تنبيه تلقائي عند دخول وقت كل صلاة حسب موقعك (${settings.cityName})",
                        checked = settings.prayerNotificationsEnabled,
                        onCheckedChange = { onUpdatePrayerNotifications(it, settings.adhanSoundEnabled) },
                    )
                    SettingSwitchRow(
                        title = "صوت تنبيه الأذان",
                        subtitle = "تفعيل النغمة الصوتية لإشعارات الصلاة",
                        checked = settings.adhanSoundEnabled,
                        onCheckedChange = { onUpdatePrayerNotifications(settings.prayerNotificationsEnabled, it) },
                    )

                    ReminderRow(
                        title = "أذكار الصباح",
                        enabled = settings.morningAdhkarReminderEnabled,
                        hour = settings.morningAdhkarHour,
                        minute = settings.morningAdhkarMinute,
                        onToggle = { onUpdateReminder("MORNING", it, settings.morningAdhkarHour, settings.morningAdhkarMinute) },
                        onPickTime = { editingReminder = Triple("MORNING", settings.morningAdhkarHour, settings.morningAdhkarMinute) },
                    )
                    ReminderRow(
                        title = "أذكار المساء",
                        enabled = settings.eveningAdhkarReminderEnabled,
                        hour = settings.eveningAdhkarHour,
                        minute = settings.eveningAdhkarMinute,
                        onToggle = { onUpdateReminder("EVENING", it, settings.eveningAdhkarHour, settings.eveningAdhkarMinute) },
                        onPickTime = { editingReminder = Triple("EVENING", settings.eveningAdhkarHour, settings.eveningAdhkarMinute) },
                    )
                    ReminderRow(
                        title = "وِرد القرآن اليومي",
                        enabled = settings.quranWirdReminderEnabled,
                        hour = settings.quranWirdHour,
                        minute = settings.quranWirdMinute,
                        onToggle = { onUpdateReminder("QURAN", it, settings.quranWirdHour, settings.quranWirdMinute) },
                        onPickTime = { editingReminder = Triple("QURAN", settings.quranWirdHour, settings.quranWirdMinute) },
                    )
                    ReminderRow(
                        title = "قيام الليل والوتر",
                        enabled = settings.qiyamReminderEnabled,
                        hour = settings.qiyamHour,
                        minute = settings.qiyamMinute,
                        onToggle = { onUpdateReminder("QIYAM", it, settings.qiyamHour, settings.qiyamMinute) },
                        onPickTime = { editingReminder = Triple("QIYAM", settings.qiyamHour, settings.qiyamMinute) },
                    )
                    ReminderRow(
                        title = "صلاة الضحى",
                        enabled = settings.duhaReminderEnabled,
                        hour = settings.duhaHour,
                        minute = settings.duhaMinute,
                        onToggle = { onUpdateReminder("DUHA", it, settings.duhaHour, settings.duhaMinute) },
                        onPickTime = { editingReminder = Triple("DUHA", settings.duhaHour, settings.duhaMinute) },
                    )

                    CelActionButton(
                        text = "إرسال إشعار تجريبي",
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                onSendTestNotification()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        testTag = "test_notification_btn",
                        leadingIcon = {
                            Icon(Icons.Filled.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                    )
                }
            }
        }

        // Location & Prayer Calculation Method
        item {
            CelCard(testTag = "location_method_card") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CelSectionHeader(
                        title = "الموقع وطريقة حساب المواقيت",
                        subtitle = "الحالي: ${settings.cityName} (${String.format(Locale.US, "%.2f, %.2f", settings.latitude, settings.longitude)})",
                    )

                    CelActionButton(
                        text = "تحديد الموقع تلقائياً (GPS)",
                        onClick = {
                            locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                    if (!locationStatusMessage.isNullOrBlank()) {
                        Text(locationStatusMessage, style = MaterialTheme.typography.labelMedium, color = cel.primaryAccent)
                    }

                    Text("أو اختر مدينة:", style = MaterialTheme.typography.labelLarge, color = cel.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CityPresets.list.forEach { city ->
                            FilterChip(
                                selected = settings.cityName == city.arabicName,
                                onClick = { onSelectCityPreset(city.arabicName, city.latitude, city.longitude, city.method) },
                                label = { Text(city.arabicName) },
                            )
                        }
                    }

                    Text("طريقة الحساب الفلكية:", style = MaterialTheme.typography.labelLarge, color = cel.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CalculationMethod.entries.forEach { method ->
                            FilterChip(
                                selected = settings.calculationMethod == method,
                                onClick = { onUpdateCalculationMethod(method) },
                                label = { Text(method.arabicName) },
                            )
                        }
                    }

                    Text("مذهب صلاة العصر:", style = MaterialTheme.typography.labelLarge, color = cel.textPrimary)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AsrMadhab.entries.forEach { madhab ->
                            FilterChip(
                                selected = settings.asrMadhab == madhab,
                                onClick = { onUpdateAsrMadhab(madhab) },
                                label = { Text(madhab.arabicName) },
                            )
                        }
                    }
                }
            }
        }

        // Theme, Winter Snowfall & Local Backup
        item {
            CelCard(testTag = "appearance_backup_card") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CelSectionHeader(
                        title = "المظهر والأجواء الشتوية والنسخ الاحتياطي",
                    )
                    SettingSwitchRow(
                        title = "الوضع الليلي القطبي",
                        subtitle = "كحلي عميق مع لمسات جليدية متوهجة (أو نهاري ثلجي أبيض)",
                        checked = settings.isDarkMode,
                        onCheckedChange = onToggleDarkMode,
                    )
                    SettingSwitchRow(
                        title = "تساقط الثلج المتحرك في الخلفية",
                        subtitle = "بلورات ثلجية خفيفة بأسلوب الأجواء الشتوية",
                        checked = settings.snowfallEnabled,
                        onCheckedChange = onToggleSnowfall,
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = cel.secondaryAccent, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "تُحفظ جميع عباداتك محلياً بأمان (Offline-first).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cel.textSecondary,
                        )
                    }

                    CelActionButton(
                        text = "تصدير نسخة احتياطية من التقدّم (JSON)",
                        onClick = {
                            scope.launch {
                                val json = onExportBackupJson()
                                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "نسخة طريق الجنة الاحتياطية")
                                    putExtra(Intent.EXTRA_TEXT, json)
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "حفظ أو مشاركة النسخة الاحتياطية"))
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = cel.secondaryAccent,
                        leadingIcon = { Icon(Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    editingReminder?.let { (kind, h, m) ->
        val pickerState = rememberTimePickerState(initialHour = h, initialMinute = m, is24Hour = true)
        AlertDialog(
            onDismissRequest = { editingReminder = null },
            title = { Text("ضبط توقيت التنبيه") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = pickerState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onUpdateReminder(kind, true, pickerState.hour, pickerState.minute)
                    editingReminder = null
                }) { Text("تأكيد") }
            },
            dismissButton = {
                TextButton(onClick = { editingReminder = null }) { Text("إلغاء") }
            },
        )
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val cel = LocalCelColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = cel.textPrimary)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cel.textSecondary)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ReminderRow(
    title: String,
    enabled: Boolean,
    hour: Int,
    minute: Int,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit,
) {
    val cel = LocalCelColors.current
    val timeStr = String.format(Locale.US, "%02d:%02d", hour, minute)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = cel.cardSurfaceAlt,
        border = BorderStroke(1.5.dp, cel.cardBorder.copy(alpha = 0.5f)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = cel.textPrimary)
                Text(
                    text = "التوقيت: $timeStr (اضغط لتعديل الساعة)",
                    style = MaterialTheme.typography.labelSmall,
                    color = cel.primaryAccent,
                    modifier = Modifier.clickable(onClick = onPickTime),
                )
            }
            Switch(checked = enabled, onCheckedChange = onToggle)
        }
    }
}
