package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.local.PrayerLogEntity
import com.example.data.local.UserSettings
import com.example.data.model.PrayerStatus
import com.example.domain.HijriCalendarHelper
import com.example.domain.PrayerTimeCalculator
import com.example.sensors.CompassReading
import com.example.ui.components.CelActionButton
import com.example.ui.components.CelCard
import com.example.ui.components.CelProgressBar
import com.example.ui.components.CelSectionHeader
import com.example.ui.theme.LocalCelColors
import com.example.ui.viewmodel.NawafilSummaryStats
import com.example.ui.viewmodel.PrayerSummaryStats
import java.util.Calendar
import kotlin.math.abs

@Composable
fun CalendarAndStatsView(
    selectedDateIso: String,
    allPrayerLogs: List<PrayerLogEntity>,
    prayerStats: PrayerSummaryStats,
    nawafilStats: NawafilSummaryStats,
    onSelectDate: (String) -> Unit,
) {
    val cel = LocalCelColors.current
    val now = remember { Calendar.getInstance() }
    var viewYear by remember { mutableIntStateOf(now.get(Calendar.YEAR)) }
    var viewMonth by remember { mutableIntStateOf(now.get(Calendar.MONTH) + 1) }

    val (firstOffset, cells) = remember(viewYear, viewMonth) {
        HijriCalendarHelper.buildMonthCells(viewYear, viewMonth)
    }
    val logsByDate = remember(allPrayerLogs) { allPrayerLogs.groupBy { it.dateIso } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Integrated Hijri + Gregorian Monthly Calendar
        CelCard(testTag = "calendar_card") {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {
                        if (viewMonth == 1) {
                            viewMonth = 12; viewYear--
                        } else viewMonth--
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "الشهر السابق",
                            tint = cel.primaryAccent,
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = HijriCalendarHelper.gregorianMonthTitle(viewYear, viewMonth),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cel.textPrimary,
                        )
                        val midHijri = HijriCalendarHelper.toHijri(viewYear, viewMonth, 15)
                        Text(
                            text = "${midHijri.monthNameArabic} ${midHijri.year} هـ",
                            style = MaterialTheme.typography.labelMedium,
                            color = cel.primaryAccent,
                        )
                    }
                    IconButton(onClick = {
                        if (viewMonth == 12) {
                            viewMonth = 1; viewYear++
                        } else viewMonth++
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "الشهر التالي",
                            tint = cel.primaryAccent,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Week days header (Sat -> Fri)
                Row(modifier = Modifier.fillMaxWidth()) {
                    listOf("س", "ح", "ن", "ث", "ر", "خ", "ج").forEach { d ->
                        Text(
                            text = d,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = cel.textSecondary,
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))

                val totalSlots = firstOffset + cells.size
                val rows = (totalSlots + 6) / 7
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (r in 0 until rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            for (c in 0 until 7) {
                                val index = r * 7 + c - firstOffset
                                if (index < 0 || index >= cells.size) {
                                    Spacer(modifier = Modifier.weight(1f).height(48.dp))
                                } else {
                                    val cell = cells[index]
                                    val dayLogs = logsByDate[cell.dateIso].orEmpty()
                                    val completedCount = dayLogs.count {
                                        it.status == PrayerStatus.ON_TIME.name ||
                                            it.status == PrayerStatus.LATE.name ||
                                            it.status == PrayerStatus.QADA.name
                                    }
                                    val dotColor = when {
                                        completedCount == 5 -> cel.onTimeColor
                                        completedCount > 0 -> cel.lateColor
                                        else -> Color.Transparent
                                    }
                                    val isSelected = cell.dateIso == selectedDateIso
                                    Surface(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(48.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onSelectDate(cell.dateIso) },
                                        shape = RoundedCornerShape(8.dp),
                                        color = when {
                                            isSelected -> cel.primaryAccent
                                            cell.isToday -> cel.cardSurfaceAlt
                                            else -> cel.cardSurface
                                        },
                                        border = BorderStroke(
                                            width = if (isSelected || cell.isToday) 2.dp else 1.dp,
                                            color = if (isSelected) cel.cardBorder else cel.cardBorder.copy(alpha = 0.35f),
                                        ),
                                    ) {
                                        val fg = if (isSelected) {
                                            if (cel.isDark) Color(0xFF040D1A) else Color.White
                                        } else cel.textPrimary
                                        Column(
                                            modifier = Modifier.fillMaxSize().padding(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.SpaceEvenly,
                                        ) {
                                            Text(
                                                text = "${cell.gregorianDay}",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = fg,
                                            )
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            ) {
                                                Text(
                                                    text = "${cell.hijriDay}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = fg.copy(alpha = 0.78f),
                                                )
                                                if (dotColor != Color.Transparent) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(6.dp)
                                                            .background(dotColor, CircleShape)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Fard Prayer Statistics (Strictly separated from Nawafil)
        CelCard(testTag = "fard_stats_card") {
            Column {
                CelSectionHeader(
                    title = "إحصائيات الفرائض",
                    subtitle = "نسب الالتزام الأسبوعية والشهرية والسنوية",
                )
                Spacer(Modifier.height(14.dp))
                StatRowBar("التزام الأسبوع (7 أيام)", prayerStats.weeklyPercent, cel.primaryAccent)
                Spacer(Modifier.height(10.dp))
                StatRowBar("التزام الشهر (30 يوماً)", prayerStats.monthlyPercent, cel.secondaryAccent)
                Spacer(Modifier.height(10.dp))
                StatRowBar("التزام السنة (365 يوماً)", prayerStats.yearlyPercent, cel.auroraGlow)
                Spacer(Modifier.height(10.dp))
                StatRowBar("نسبة صلاة الجماعة", prayerStats.congregationPercent, cel.goldAccent)
                Spacer(Modifier.height(14.dp))
                // 7-day visual cel bar chart
                Text(
                    text = "مخطط الصلوات خلال آخر 7 أيام",
                    style = MaterialTheme.typography.labelLarge,
                    color = cel.textPrimary,
                )
                Spacer(Modifier.height(8.dp))
                val last7 = remember { HijriCalendarHelper.lastNDatesIso(7) }
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                ) {
                    val barW = (size.width / 7f) * 0.56f
                    val gap = (size.width / 7f)
                    last7.forEachIndexed { idx, dIso ->
                        val count = logsByDate[dIso]?.count {
                            it.status == PrayerStatus.ON_TIME.name ||
                                it.status == PrayerStatus.LATE.name ||
                                it.status == PrayerStatus.QADA.name
                        } ?: 0
                        val frac = count / 5f
                        val barH = (size.height - 16.dp.toPx()) * frac
                        val x = idx * gap + (gap - barW) / 2f
                        val y = size.height - barH - 4.dp.toPx()
                        // Track
                        drawRect(
                            color = cel.cardSurfaceAlt,
                            topLeft = Offset(x, 8.dp.toPx()),
                            size = Size(barW, size.height - 12.dp.toPx()),
                        )
                        // Fill
                        if (barH > 0f) {
                            drawRect(
                                color = if (count == 5) cel.onTimeColor else cel.primaryAccent,
                                topLeft = Offset(x, y),
                                size = Size(barW, barH),
                            )
                        }
                        drawRect(
                            color = cel.cardBorder,
                            topLeft = Offset(x, 8.dp.toPx()),
                            size = Size(barW, size.height - 12.dp.toPx()),
                            style = Stroke(width = 1.5.dp.toPx()),
                        )
                    }
                }
            }
        }

        // Separate Nawafil Statistics Card
        CelCard(
            borderColor = cel.secondaryAccent,
            testTag = "nawafil_stats_card",
        ) {
            Column {
                CelSectionHeader(
                    title = "إحصائيات النوافل (منفصلة)",
                    subtitle = "تُحتسب النوافل والرواتب بشكل مستقل عن الفرائض",
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatMiniBox(
                        modifier = Modifier.weight(1f),
                        value = "${nawafilStats.todayTotalRakaat}",
                        label = "ركعة اليوم",
                        accent = cel.secondaryAccent,
                    )
                    StatMiniBox(
                        modifier = Modifier.weight(1f),
                        value = "${nawafilStats.weeklySessions}",
                        label = "نافلة هذا الأسبوع",
                        accent = cel.primaryAccent,
                    )
                    StatMiniBox(
                        modifier = Modifier.weight(1f),
                        value = "${nawafilStats.totalRakaatAllTime}",
                        label = "إجمالي الركعات",
                        accent = cel.goldAccent,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRowBar(label: String, percent: Int, color: Color) {
    val cel = LocalCelColors.current
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium, color = cel.textPrimary)
            Text(
                "$percent%",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
        Spacer(Modifier.height(4.dp))
        CelProgressBar(progress = percent / 100f, fillColor = color, height = 12.dp)
    }
}

@Composable
private fun StatMiniBox(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accent: Color,
) {
    val cel = LocalCelColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = cel.cardSurfaceAlt,
        border = BorderStroke(2.dp, cel.cardBorder),
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
            Text(label, style = MaterialTheme.typography.labelSmall, color = cel.textSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun QiblaCompassView(
    settings: UserSettings,
    compassReading: CompassReading,
    onRequestLocationUpdate: () -> Unit,
) {
    val cel = LocalCelColors.current
    val qiblaBearing = remember(settings.latitude, settings.longitude) {
        PrayerTimeCalculator.calculateQiblaBearing(settings.latitude, settings.longitude).toFloat()
    }
    val distanceKm = remember(settings.latitude, settings.longitude) {
        PrayerTimeCalculator.distanceToKaabaKm(settings.latitude, settings.longitude)
    }

    val deltaToQibla = remember(compassReading.azimuthDegrees, qiblaBearing) {
        var d = qiblaBearing - compassReading.azimuthDegrees
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        d
    }
    val isAligned = compassReading.hasSensor && abs(deltaToQibla) <= 6f

    CelCard(
        borderColor = if (isAligned) cel.onTimeColor else cel.primaryAccent,
        testTag = "qibla_compass_card",
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            CelSectionHeader(
                title = "بوصلة القبلة",
                subtitle = "${settings.cityName} • المسافة إلى الكعبة $distanceKm كم",
            )
            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.44f

                    // Outer cel-shaded compass ring
                    drawCircle(
                        color = cel.cardSurfaceAlt,
                        radius = radius,
                        center = center,
                    )
                    drawCircle(
                        color = if (isAligned) cel.onTimeColor else cel.cardBorder,
                        radius = radius,
                        center = center,
                        style = Stroke(width = 4.dp.toPx()),
                    )

                    // Rotate dial opposite to device azimuth so North points to true north
                    val dialRotation = if (compassReading.hasSensor) -compassReading.azimuthDegrees else 0f
                    rotate(dialRotation, pivot = center) {
                        // 8-pointed Islamic star reticle inside compass
                        val starSide = radius * 1.15f
                        drawRect(
                            color = cel.primaryAccent.copy(alpha = 0.25f),
                            topLeft = Offset(center.x - starSide / 2, center.y - starSide / 2),
                            size = Size(starSide, starSide),
                            style = Stroke(width = 1.8.dp.toPx()),
                        )
                        rotate(45f, pivot = center) {
                            drawRect(
                                color = cel.primaryAccent.copy(alpha = 0.25f),
                                topLeft = Offset(center.x - starSide / 2, center.y - starSide / 2),
                                size = Size(starSide, starSide),
                                style = Stroke(width = 1.8.dp.toPx()),
                            )
                        }

                        // Degree ticks
                        for (deg in 0 until 360 step 15) {
                            rotate(deg.toFloat(), pivot = center) {
                                val tickLen = if (deg % 90 == 0) 14.dp.toPx() else 7.dp.toPx()
                                drawLine(
                                    color = if (deg == 0) cel.missedColor else cel.textSecondary,
                                    start = Offset(center.x, center.y - radius + 4.dp.toPx()),
                                    end = Offset(center.x, center.y - radius + 4.dp.toPx() + tickLen),
                                    strokeWidth = if (deg % 90 == 0) 3.dp.toPx() else 1.5.dp.toPx(),
                                    cap = StrokeCap.Round,
                                )
                            }
                        }

                        // Qibla directional pointer arrow on dial
                        rotate(qiblaBearing, pivot = center) {
                            val arrowPath = Path().apply {
                                moveTo(center.x, center.y - radius + 12.dp.toPx())
                                lineTo(center.x - 14.dp.toPx(), center.y - 16.dp.toPx())
                                lineTo(center.x, center.y - 28.dp.toPx())
                                lineTo(center.x + 14.dp.toPx(), center.y - 16.dp.toPx())
                                close()
                            }
                            drawPath(
                                path = arrowPath,
                                color = if (isAligned) cel.onTimeColor else cel.goldAccent,
                            )
                            drawPath(
                                path = arrowPath,
                                color = cel.cardBorder,
                                style = Stroke(width = 2.dp.toPx()),
                            )
                        }
                    }

                    // Center Kaaba pivot square
                    val kSize = 22.dp.toPx()
                    drawRect(
                        color = Color(0xFF0A0F1A),
                        topLeft = Offset(center.x - kSize / 2, center.y - kSize / 2),
                        size = Size(kSize, kSize),
                    )
                    drawRect(
                        color = cel.goldAccent,
                        topLeft = Offset(center.x - kSize / 2, center.y - kSize / 2),
                        size = Size(kSize, kSize),
                        style = Stroke(width = 2.5.dp.toPx()),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "زاوية القبلة: ${qiblaBearing.toInt()}° من الشمال الجغرافي",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (isAligned) cel.onTimeColor else cel.primaryAccent,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    !compassReading.hasSensor -> "حساس البوصلة غير متوفر على هذا الجهاز؛ وجّه الهاتف نحو ${qiblaBearing.toInt()}° من الشمال."
                    isAligned -> "أنت الآن متجه نحو القبلة المشرفة!"
                    deltaToQibla > 0 -> "أدر الهاتف لليمين بمقدار ${abs(deltaToQibla).toInt()}°"
                    else -> "أدر الهاتف لليسار بمقدار ${abs(deltaToQibla).toInt()}°"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = cel.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(14.dp))
            CelActionButton(
                text = "تحديث الموقع الجغرافي",
                onClick = onRequestLocationUpdate,
                testTag = "qibla_refresh_location_btn",
                leadingIcon = {
                    Icon(Icons.Filled.MyLocation, contentDescription = null, modifier = Modifier.size(18.dp))
                },
            )
        }
    }
}
