package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.AcUnit
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.NaflLogEntity
import com.example.data.local.PrayerLogEntity
import com.example.data.local.QuranDailyLogEntity
import com.example.data.local.UserSettings
import com.example.data.model.DailyWisdom
import com.example.data.model.PrayerMode
import com.example.data.model.PrayerName
import com.example.data.model.PrayerStatus
import com.example.data.model.PrayerTimesDay
import com.example.data.model.QuranSurah
import com.example.domain.HijriDateInfo
import com.example.ui.components.CelCard
import com.example.ui.components.CelProgressBar
import com.example.ui.components.CelSectionHeader
import com.example.ui.components.RubElHizbStar
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.FinnishBlue
import com.example.ui.theme.LocalCelColors
import com.example.ui.viewmodel.PrayerSummaryStats
import java.util.Calendar

@Composable
fun DashboardScreen(
    settings: UserSettings,
    currentHijriDate: HijriDateInfo,
    todayHeader: Pair<String, String>,
    todayIso: String,
    prayerTimes: PrayerTimesDay,
    prayerLogs: List<PrayerLogEntity>,
    prayerStats: PrayerSummaryStats,
    naflLogs: List<NaflLogEntity>,
    quranLogs: List<QuranDailyLogEntity>,
    dhikrProgressToday: List<DhikrProgressEntity>,
    surahs: List<QuranSurah>,
    dailyWisdom: DailyWisdom,
    onToggleTheme: () -> Unit,
    onToggleSnow: () -> Unit,
    onQuickMarkPrayer: (PrayerName, PrayerStatus, PrayerMode) -> Unit,
    onNavigateToPrayers: (Int) -> Unit,
    onNavigateToQuran: () -> Unit,
    onNavigateToAdhkar: () -> Unit,
    onNavigateToMore: () -> Unit,
) {
    val cel = LocalCelColors.current
    val todayPrayerMap = remember(prayerLogs, todayIso) {
        prayerLogs.filter { it.dateIso == todayIso }.associateBy { it.prayerName }
    }
    val todayQuran = remember(quranLogs, todayIso) {
        quranLogs.firstOrNull { it.dateIso == todayIso }
    }
    val todayNaflCompleted = remember(naflLogs, todayIso) {
        naflLogs.filter { it.dateIso == todayIso && it.completed }
    }
    val completedDhikrCount = remember(dhikrProgressToday) {
        dhikrProgressToday.count { it.completed }
    }

    val nextPrayer = remember(prayerTimes) {
        val cal = Calendar.getInstance()
        val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        PrayerName.entries.firstOrNull { prayerTimes.minutesFor(it) > nowMins } ?: PrayerName.FAJR
    }
    val remainingLabel = remember(prayerTimes, nextPrayer) {
        val cal = Calendar.getInstance()
        val nowMins = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        var diff = prayerTimes.minutesFor(nextPrayer) - nowMins
        if (diff <= 0) diff += 24 * 60
        val h = diff / 60
        val m = diff % 60
        if (h > 0) "بعد $h س و $m د" else "بعد $m دقيقة"
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Top Brand Bar with Theme & Snowfall toggles
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RubElHizbStar(modifier = Modifier.size(34.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "طريق الجنة",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = cel.textPrimary,
                        )
                        Text(
                            text = "رفيق العبادات اليومية",
                            style = MaterialTheme.typography.labelMedium,
                            color = cel.textSecondary,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = cel.cardSurface,
                        border = BorderStroke(2.dp, cel.cardBorder),
                    ) {
                        IconButton(
                            onClick = onToggleSnow,
                            modifier = Modifier.testTag("toggle_snow_button"),
                        ) {
                            Icon(
                                imageVector = if (settings.snowfallEnabled) Icons.Filled.AcUnit else Icons.Outlined.AcUnit,
                                contentDescription = "تبديل تساقط الثلج",
                                tint = if (settings.snowfallEnabled) cel.primaryAccent else cel.textSecondary,
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = cel.cardSurface,
                        border = BorderStroke(2.dp, cel.cardBorder),
                    ) {
                        IconButton(
                            onClick = onToggleTheme,
                            modifier = Modifier.testTag("toggle_theme_button"),
                        ) {
                            Icon(
                                imageVector = if (settings.isDarkMode) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                contentDescription = "تبديل الوضع الليلي والنهاري",
                                tint = cel.goldAccent,
                            )
                        }
                    }
                }
            }
        }

        // Elegant Hijri Date Header Card (Material 3 Typography)
        item {
            CelCard(
                containerColor = cel.cardSurface,
                borderColor = cel.cardBorder,
                accentStripeColor = cel.goldAccent,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                onClick = { onNavigateToPrayers(2) },
                testTag = "dashboard_hijri_date_header",
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "التاريخ الهجري اليوم",
                                style = MaterialTheme.typography.labelMedium,
                                color = cel.textSecondary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = currentHijriDate.fullFormattedArabic,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = cel.primaryAccent,
                            )
                        }
                        Surface(
                            shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                            color = cel.cardSurfaceAlt,
                            border = BorderStroke(1.5.dp, cel.goldAccent),
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CalendarMonth,
                                    contentDescription = "عرض التقويم الهجري",
                                    tint = cel.goldAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${currentHijriDate.day} ${currentHijriDate.monthNameArabic}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cel.textPrimary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(
                        thickness = 1.dp,
                        color = cel.cardBorder.copy(alpha = 0.25f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "الموافق ${currentHijriDate.gregorianFormattedArabic.ifEmpty { todayHeader.second }}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cel.textSecondary,
                        )
                        Text(
                            text = "${currentHijriDate.year} هـ",
                            style = MaterialTheme.typography.labelLarge,
                            color = cel.goldAccent,
                        )
                    }
                }
            }
        }

        // Hero Banner with Winter Mosque Illustration + Next Prayer
        item {
            CelCard(
                contentPadding = PaddingValues(0.dp),
                borderColor = cel.primaryAccent,
                testTag = "dashboard_hero_card",
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(155.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_winter_mosque_banner),
                            contentDescription = "مشهد المسجد الشتوي تحت الشفق القطبي",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            FinnishBlue.copy(alpha = 0.92f),
                                        )
                                    )
                                )
                        )
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF00E5FF),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "${settings.cityName} • ${todayHeader.second}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = Color.White,
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "الصلاة القادمة: ${nextPrayer.arabicName}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                )
                            }
                            Surface(
                                shape = CutCornerShape(topStart = 10.dp, bottomEnd = 10.dp),
                                color = Color(0xFF00E5FF),
                                border = BorderStroke(2.dp, Color.White),
                            ) {
                                Column(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        text = prayerTimes.timeFor(nextPrayer),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF040D1A),
                                    )
                                    Text(
                                        text = remainingLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF040D1A),
                                    )
                                }
                            }
                        }
                    }

                    // 5 Daily Prayers Quick Strip
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "الصلوات الخمس اليوم (اضغط للتسجيل السريع)",
                                style = MaterialTheme.typography.labelLarge,
                                color = cel.textPrimary,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.LocalFireDepartment,
                                    contentDescription = "سلسلة الأيام",
                                    tint = cel.goldAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = "${prayerStats.streakAllDays} يوم",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cel.goldAccent,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PrayerName.entries.forEach { prayer ->
                                val log = todayPrayerMap[prayer.name]
                                val status = runCatching {
                                    PrayerStatus.valueOf(log?.status ?: PrayerStatus.PENDING.name)
                                }.getOrDefault(PrayerStatus.PENDING)
                                val chipBg = when (status) {
                                    PrayerStatus.ON_TIME -> cel.onTimeColor
                                    PrayerStatus.LATE -> cel.lateColor
                                    PrayerStatus.QADA -> cel.qadaColor
                                    PrayerStatus.MISSED -> cel.missedColor
                                    PrayerStatus.PENDING -> cel.cardSurfaceAlt
                                }
                                val chipText = if (status == PrayerStatus.PENDING) cel.textPrimary else Color(0xFF040D1A)

                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            val nextStatus = when (status) {
                                                PrayerStatus.PENDING -> PrayerStatus.ON_TIME
                                                PrayerStatus.ON_TIME -> PrayerStatus.LATE
                                                PrayerStatus.LATE -> PrayerStatus.QADA
                                                PrayerStatus.QADA -> PrayerStatus.MISSED
                                                PrayerStatus.MISSED -> PrayerStatus.PENDING
                                            }
                                            onQuickMarkPrayer(prayer, nextStatus, PrayerMode.CONGREGATION)
                                        }
                                        .testTag("dash_prayer_${prayer.name.lowercase()}"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = chipBg,
                                    border = BorderStroke(
                                        2.dp,
                                        if (prayer == nextPrayer && status == PrayerStatus.PENDING) cel.primaryAccent else cel.cardBorder
                                    ),
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                    ) {
                                        Text(
                                            text = prayer.arabicName,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = chipText,
                                        )
                                        Text(
                                            text = prayerTimes.timeFor(prayer),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = chipText.copy(alpha = 0.9f),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Strict 2-column Summary Cards: Quran Wird & Adhkar/Nawafil
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val pagesRead = todayQuran?.pagesRead ?: 0
                val targetPages = settings.dailyTargetPages.coerceAtLeast(1)
                val quranProgress = (pagesRead.toFloat() / targetPages.toFloat()).coerceIn(0f, 1f)
                val bookmarkSurah = surahs.firstOrNull { it.number == settings.lastSurah }?.arabicName ?: "الفاتحة"

                CelCard(
                    modifier = Modifier.weight(1f),
                    accentStripeColor = cel.primaryAccent,
                    onClick = onNavigateToQuran,
                    testTag = "dash_quran_card",
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.MenuBook,
                                contentDescription = null,
                                tint = cel.primaryAccent,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "وِرد القرآن",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cel.textPrimary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "$pagesRead / $targetPages صفحة",
                            style = MaterialTheme.typography.headlineMedium,
                            color = cel.primaryAccent,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(6.dp))
                        CelProgressBar(progress = quranProgress, fillColor = cel.primaryAccent)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "العلامة: $bookmarkSurah (${settings.lastAyah})",
                            style = MaterialTheme.typography.labelSmall,
                            color = cel.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                CelCard(
                    modifier = Modifier.weight(1f),
                    accentStripeColor = cel.secondaryAccent,
                    onClick = onNavigateToAdhkar,
                    testTag = "dash_adhkar_card",
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = cel.secondaryAccent,
                                modifier = Modifier.size(22.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "الأذكار والنوافل",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cel.textPrimary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "$completedDhikrCount ذكر مُنجز",
                            style = MaterialTheme.typography.titleLarge,
                            color = cel.secondaryAccent,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "النوافل اليوم: ${todayNaflCompleted.size} (${todayNaflCompleted.sumOf { it.rakaat }} ركعة)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = cel.textSecondary,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "التزام الأسبوع: ${prayerStats.weeklyPercent}%",
                            style = MaterialTheme.typography.labelMedium,
                            color = cel.goldAccent,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }

        // Quick Tools Row: Qibla, Hijri Calendar, Nawafil, Achievements
        item {
            CelSectionHeader(
                title = "اختصارات سريعة",
                subtitle = "انتقال مباشر لكل أقسام العبادة",
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "القبلة",
                    icon = Icons.Filled.Explore,
                    accent = cel.primaryAccent,
                    testTag = "quick_qibla",
                    onClick = { onNavigateToPrayers(3) },
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "النوافل",
                    icon = Icons.Filled.Mosque,
                    accent = cel.secondaryAccent,
                    testTag = "quick_nawafil",
                    onClick = { onNavigateToPrayers(1) },
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "التقويم",
                    icon = Icons.Filled.CalendarMonth,
                    accent = cel.auroraGlow,
                    testTag = "quick_calendar",
                    onClick = { onNavigateToPrayers(2) },
                )
                QuickActionTile(
                    modifier = Modifier.weight(1f),
                    title = "الشارات",
                    icon = Icons.Filled.EmojiEvents,
                    accent = cel.goldAccent,
                    testTag = "quick_badges",
                    onClick = onNavigateToMore,
                )
            }
        }

        // Daily Hadith / Dua Card
        item {
            CelCard(
                containerColor = cel.cardSurfaceAlt,
                borderColor = cel.goldAccent,
                testTag = "daily_wisdom_card",
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Book,
                                contentDescription = null,
                                tint = cel.goldAccent,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (dailyWisdom.isHadith) "حديث اليوم" else "دعاء اليوم",
                                style = MaterialTheme.typography.labelLarge,
                                color = cel.goldAccent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        Text(
                            text = dailyWisdom.title,
                            style = MaterialTheme.typography.labelMedium,
                            color = cel.textSecondary,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = dailyWisdom.text,
                        fontFamily = AmiriFontFamily,
                        fontSize = 20.sp,
                        lineHeight = 32.sp,
                        color = cel.textPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = dailyWisdom.source,
                        style = MaterialTheme.typography.labelSmall,
                        color = cel.primaryAccent,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun QuickActionTile(
    modifier: Modifier = Modifier,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    testTag: String,
    onClick: () -> Unit,
) {
    val cel = LocalCelColors.current
    CelCard(
        modifier = modifier,
        borderColor = cel.cardBorder,
        contentPadding = PaddingValues(vertical = 12.dp, horizontal = 4.dp),
        onClick = onClick,
        testTag = testTag,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accent,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = cel.textPrimary,
                maxLines = 1,
            )
        }
    }
}
