package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.local.CustomCategoryEntity
import com.example.data.local.CustomNaflRoutineEntity
import com.example.data.local.NaflLogEntity
import com.example.data.local.PrayerLogEntity
import com.example.data.local.UserSettings
import com.example.data.model.NaflType
import com.example.data.model.PrayerMode
import com.example.data.model.PrayerName
import com.example.data.model.PrayerStatus
import com.example.data.model.PrayerTimesDay
import com.example.data.model.RoutineFrequency
import com.example.data.model.RoutineTimeOfDay
import com.example.data.model.WeekDays
import com.example.sensors.CompassReading
import com.example.ui.components.CelActionButton
import com.example.ui.components.CelCard
import com.example.ui.components.CelSectionHeader
import com.example.ui.theme.LocalCelColors
import com.example.ui.viewmodel.NawafilSummaryStats
import com.example.ui.viewmodel.PrayerSummaryStats

@Composable
fun PrayersScreen(
    initialTab: Int,
    settings: UserSettings,
    todayIso: String,
    selectedDateIso: String,
    prayerTimes: PrayerTimesDay,
    allPrayerLogs: List<PrayerLogEntity>,
    prayerStats: PrayerSummaryStats,
    allNaflLogs: List<NaflLogEntity>,
    nawafilStats: NawafilSummaryStats,
    customNaflRoutines: List<CustomNaflRoutineEntity>,
    customCategories: List<CustomCategoryEntity>,
    compassReading: CompassReading,
    onSelectDate: (String) -> Unit,
    onUpdatePrayer: (String, PrayerName, PrayerStatus, PrayerMode) -> Unit,
    onTogglePrayerCompletion: (String, PrayerName) -> Unit,
    onToggleNafl: (String, String, String, Int, String, Boolean, Boolean) -> Unit,
    onAddCustomNaflRoutine: (String, Int, String, RoutineFrequency, Int, RoutineTimeOfDay, String, Boolean) -> Unit,
    onDeleteCustomNaflRoutine: (Long) -> Unit,
    onRequestLocationUpdate: () -> Unit,
) {
    val cel = LocalCelColors.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab.coerceIn(0, 3)) }
    var showAddRoutineDialog by remember { mutableStateOf(false) }

    val tabs = listOf("الفرائض", "النوافل", "التقويم والإحصاء", "القبلة")

    Column(modifier = Modifier.fillMaxSize()) {
        // Cel Sub-Navigation Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            tabs.forEachIndexed { idx, label ->
                val active = selectedTab == idx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedTab = idx }
                        .testTag("prayers_tab_$idx"),
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) cel.primaryAccent else cel.cardSurface,
                    border = BorderStroke(2.dp, cel.cardBorder),
                ) {
                    Box(
                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (active) {
                                if (cel.isDark) Color(0xFF040D1A) else Color.White
                            } else cel.textPrimary,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (selectedTab) {
                0 -> {
                    // Streak & Selected Day Banner
                    item {
                        CelCard(
                            containerColor = cel.cardSurfaceAlt,
                            borderColor = cel.goldAccent,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(
                                        text = if (selectedDateIso == todayIso) "صلوات اليوم" else "سجل يوم $selectedDateIso",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = cel.textPrimary,
                                    )
                                    Text(
                                        text = "${settings.cityName} • ${settings.calculationMethod.arabicName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cel.textSecondary,
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Filled.LocalFireDepartment,
                                            contentDescription = null,
                                            tint = cel.goldAccent,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            text = "${prayerStats.streakAllDays} يوم متتالٍ",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = cel.goldAccent,
                                        )
                                    }
                                    Text(
                                        text = "في وقتها: ${prayerStats.streakOnTimeDays} يوم",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = cel.onTimeColor,
                                    )
                                }
                            }
                        }
                    }

                    val dayMap = allPrayerLogs.filter { it.dateIso == selectedDateIso }.associateBy { it.prayerName }
                    items(PrayerName.entries) { prayer ->
                        val log = dayMap[prayer.name]
                        val status = runCatching {
                            PrayerStatus.valueOf(log?.status ?: PrayerStatus.PENDING.name)
                        }.getOrDefault(PrayerStatus.PENDING)
                        val mode = runCatching {
                            PrayerMode.valueOf(log?.mode ?: PrayerMode.SOLO.name)
                        }.getOrDefault(PrayerMode.SOLO)

                        CelCard(
                            borderColor = when (status) {
                                PrayerStatus.ON_TIME -> cel.onTimeColor
                                PrayerStatus.LATE -> cel.lateColor
                                PrayerStatus.QADA -> cel.qadaColor
                                PrayerStatus.MISSED -> cel.missedColor
                                PrayerStatus.PENDING -> cel.cardBorder
                            },
                            testTag = "prayer_card_${prayer.name.lowercase()}",
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        val isCompleted = log?.isCompleted ?: status.isCompleted
                                        IconButton(
                                            onClick = { onTogglePrayerCompletion(selectedDateIso, prayer) },
                                            modifier = Modifier.testTag("toggle_prayer_${prayer.name.lowercase()}"),
                                        ) {
                                            Icon(
                                                imageVector = if (isCompleted) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                                contentDescription = if (isCompleted) "إلغاء إتمام صلاة ${prayer.arabicName}" else "تأكيد إتمام صلاة ${prayer.arabicName}",
                                                tint = if (isCompleted) cel.onTimeColor else cel.textSecondary,
                                            )
                                        }
                                        Spacer(Modifier.width(4.dp))
                                        Column {
                                            Text(
                                                text = "صلاة ${prayer.arabicName}",
                                                style = MaterialTheme.typography.titleLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = cel.textPrimary,
                                            )
                                            Text(
                                                text = "الموعد: ${prayerTimes.timeFor(prayer)}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = cel.primaryAccent,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }
                                    }

                                    // Solo vs Congregation Toggle
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        PrayerMode.entries.forEach { m ->
                                            val isSelected = mode == m
                                            Surface(
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { onUpdatePrayer(selectedDateIso, prayer, status, m) },
                                                shape = RoundedCornerShape(8.dp),
                                                color = if (isSelected) cel.primaryAccent else cel.cardSurfaceAlt,
                                                border = BorderStroke(1.5.dp, cel.cardBorder),
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                ) {
                                                    Icon(
                                                        imageVector = if (m == PrayerMode.CONGREGATION) Icons.Filled.Groups else Icons.Filled.Person,
                                                        contentDescription = m.arabicLabel,
                                                        tint = if (isSelected) {
                                                            if (cel.isDark) Color(0xFF040D1A) else Color.White
                                                        } else cel.textSecondary,
                                                        modifier = Modifier.size(16.dp),
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(
                                                        text = m.arabicLabel,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (isSelected) {
                                                            if (cel.isDark) Color(0xFF040D1A) else Color.White
                                                        } else cel.textPrimary,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                                Spacer(Modifier.height(10.dp))
                                // Status selector buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    listOf(
                                        PrayerStatus.ON_TIME to cel.onTimeColor,
                                        PrayerStatus.LATE to cel.lateColor,
                                        PrayerStatus.QADA to cel.qadaColor,
                                        PrayerStatus.MISSED to cel.missedColor,
                                    ).forEach { (st, stColor) ->
                                        val selected = status == st
                                        Surface(
                                            modifier = Modifier
                                                .weight(1f)
                                                .minimumInteractiveComponentSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { onUpdatePrayer(selectedDateIso, prayer, st, mode) },
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) stColor else cel.cardSurfaceAlt,
                                            border = BorderStroke(1.5.dp, if (selected) cel.cardBorder else cel.cardBorder.copy(alpha = 0.4f)),
                                        ) {
                                            Box(
                                                modifier = Modifier.padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = st.arabicLabel,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (selected) Color(0xFF040D1A) else cel.textPrimary,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Nawafil + Custom Personal Optional Prayer Routines
                    item {
                        CelCard(
                            containerColor = cel.cardSurfaceAlt,
                            borderColor = cel.secondaryAccent,
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "النوافل والسنن الرواتب",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = cel.textPrimary,
                                    )
                                    Text(
                                        text = "إحصائيات مستقلة • ${nawafilStats.todayTotalRakaat} ركعة اليوم",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = cel.secondaryAccent,
                                    )
                                }
                                CelActionButton(
                                    text = "روتين نافلة",
                                    onClick = { showAddRoutineDialog = true },
                                    testTag = "add_custom_nafl_button",
                                    leadingIcon = {
                                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    },
                                )
                            }
                        }
                    }

                    val naflMap = allNaflLogs.filter { it.dateIso == selectedDateIso }.associateBy { it.naflKey }

                    // Custom Personal Optional Prayer Routines Section
                    if (customNaflRoutines.isNotEmpty()) {
                        item {
                            CelSectionHeader(
                                title = "روتينك الشخصي للنوافل",
                                subtitle = "نوافل مخصصة بتكرارها ووقتها وتصنيفها",
                            )
                        }
                        items(customNaflRoutines, key = { "custom_nafl_${it.id}" }) { routine ->
                            val logKey = "custom_${routine.id}"
                            val isDone = naflMap[logKey]?.completed == true
                            val freqLabel = runCatching { RoutineFrequency.valueOf(routine.frequency).arabicLabel }.getOrDefault("يومياً")
                            val timeLabel = runCatching { RoutineTimeOfDay.valueOf(routine.timeOfDay).arabicLabel }.getOrDefault("أي وقت")
                            CelCard(
                                borderColor = if (isDone) cel.onTimeColor else cel.secondaryAccent,
                                onClick = {
                                    onToggleNafl(
                                        selectedDateIso, logKey, routine.title, routine.rakaatCount,
                                        routine.categoryName, true, !isDone
                                    )
                                },
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = routine.title,
                                            tint = if (isDone) cel.onTimeColor else cel.primaryAccent,
                                            modifier = Modifier.size(28.dp),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = routine.title,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = cel.textPrimary,
                                            )
                                            Text(
                                                text = "${routine.rakaatCount} ركعة • $freqLabel • $timeLabel • [${routine.categoryName}]",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = cel.secondaryAccent,
                                            )
                                            if (routine.notes.isNotBlank()) {
                                                Text(
                                                    text = routine.notes,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = cel.textSecondary,
                                                )
                                            }
                                        }
                                    }
                                    IconButton(onClick = { onDeleteCustomNaflRoutine(routine.id) }) {
                                        Icon(
                                            imageVector = Icons.Filled.DeleteOutline,
                                            contentDescription = "حذف الروتين",
                                            tint = cel.missedColor,
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Standard Nawafil grouped by category
                    val grouped = NaflType.entries.groupBy { it.categoryLabel }
                    grouped.forEach { (category, list) ->
                        item {
                            CelSectionHeader(title = category)
                        }
                        items(list, key = { it.key }) { nafl ->
                            val isDone = naflMap[nafl.key]?.completed == true
                            CelCard(
                                borderColor = if (isDone) cel.onTimeColor else cel.cardBorder,
                                onClick = {
                                    onToggleNafl(
                                        selectedDateIso, nafl.key, nafl.arabicTitle,
                                        nafl.defaultRakaat, nafl.categoryLabel, false, !isDone
                                    )
                                },
                                testTag = "nafl_item_${nafl.key}",
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isDone) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = nafl.arabicTitle,
                                            tint = if (isDone) cel.onTimeColor else cel.primaryAccent,
                                            modifier = Modifier.size(28.dp),
                                        )
                                        Spacer(Modifier.width(12.dp))
                                        Text(
                                            text = nafl.arabicTitle,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = cel.textPrimary,
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = cel.cardSurfaceAlt,
                                        border = BorderStroke(1.5.dp, cel.cardBorder),
                                    ) {
                                        Text(
                                            text = "${nafl.defaultRakaat} ركعة",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = cel.primaryAccent,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    item {
                        CalendarAndStatsView(
                            selectedDateIso = selectedDateIso,
                            allPrayerLogs = allPrayerLogs,
                            prayerStats = prayerStats,
                            nawafilStats = nawafilStats,
                            onSelectDate = { d ->
                                onSelectDate(d)
                            },
                        )
                    }
                }

                3 -> {
                    item {
                        QiblaCompassView(
                            settings = settings,
                            compassReading = compassReading,
                            onRequestLocationUpdate = onRequestLocationUpdate,
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showAddRoutineDialog) {
        AddCustomNaflDialog(
            existingCategories = (NaflType.entries.map { it.categoryLabel } +
                customCategories.filter { it.kind == "NAWAFIL" }.map { it.name }).distinct(),
            onDismiss = { showAddRoutineDialog = false },
            onSave = { title, rakaat, cat, freq, mask, time, notes, isNewCat ->
                onAddCustomNaflRoutine(title, rakaat, cat, freq, mask, time, notes, isNewCat)
                showAddRoutineDialog = false
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomNaflDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, Int, String, RoutineFrequency, Int, RoutineTimeOfDay, String, Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var rakaat by remember { mutableIntStateOf(2) }
    var selectedCategory by remember { mutableStateOf(existingCategories.firstOrNull() ?: "نوافل مخصصة") }
    var createNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(RoutineFrequency.DAILY) }
    var daysMask by remember { mutableIntStateOf(0b1111111) }
    var timeOfDay by remember { mutableStateOf(RoutineTimeOfDay.QIYAM) }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة روتين نافلة شخصي", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("اسم النافلة (مثال: ركعتا الشكر)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_nafl_title_input"),
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("عدد الركعات: $rakaat", style = MaterialTheme.typography.titleMedium)
                    Row {
                        TextButton(onClick = { if (rakaat > 1) rakaat-- }) { Text("-1") }
                        TextButton(onClick = { rakaat += 2 }) { Text("+2") }
                    }
                }
                Text("التكرار", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoutineFrequency.entries.forEach { f ->
                        FilterChip(
                            selected = frequency == f,
                            onClick = { frequency = f },
                            label = { Text(f.arabicLabel) },
                        )
                    }
                }
                if (frequency == RoutineFrequency.SPECIFIC_DAYS) {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        WeekDays.all.forEach { d ->
                            val checked = (daysMask and d.bitMask) != 0
                            FilterChip(
                                selected = checked,
                                onClick = { daysMask = daysMask xor d.bitMask },
                                label = { Text(d.shortArabic) },
                            )
                        }
                    }
                }
                Text("وقت الأداء", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RoutineTimeOfDay.entries.forEach { t ->
                        FilterChip(
                            selected = timeOfDay == t,
                            onClick = { timeOfDay = t },
                            label = { Text(t.arabicLabel) },
                        )
                    }
                }
                Text("التصنيف", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    existingCategories.forEach { cat ->
                        FilterChip(
                            selected = !createNewCategory && selectedCategory == cat,
                            onClick = {
                                createNewCategory = false
                                selectedCategory = cat
                            },
                            label = { Text(cat) },
                        )
                    }
                    FilterChip(
                        selected = createNewCategory,
                        onClick = { createNewCategory = true },
                        label = { Text("+ تصنيف جديد") },
                    )
                }
                if (createNewCategory) {
                    OutlinedTextField(
                        value = newCategoryName,
                        onValueChange = { newCategoryName = it },
                        label = { Text("اسم التصنيف الجديد") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("ملاحظات / نية (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && (!createNewCategory || newCategoryName.isNotBlank()),
                onClick = {
                    val finalCat = if (createNewCategory) newCategoryName else selectedCategory
                    onSave(title, rakaat, finalCat, frequency, daysMask, timeOfDay, notes, createNewCategory)
                },
                modifier = Modifier.testTag("save_custom_nafl_btn"),
            ) {
                Text("حفظ")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
