package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CustomCategoryEntity
import com.example.data.local.CustomDhikrEntity
import com.example.data.local.DhikrProgressEntity
import com.example.data.model.HisnChapter
import com.example.data.model.HisnDhikr
import com.example.data.model.RoutineFrequency
import com.example.data.model.RoutineTimeOfDay
import com.example.data.model.WeekDays
import com.example.data.static.HisnAlMuslimExtraData
import com.example.ui.components.CelActionButton
import com.example.ui.components.CelCard
import com.example.ui.components.CelProgressBar
import com.example.ui.components.CelSectionHeader
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.LocalCelColors

private const val FAVORITES_FILTER = "المفضلة"

@Composable
fun AdhkarScreen(
    hisnChapters: List<HisnChapter>,
    progressToday: List<DhikrProgressEntity>,
    customAdhkar: List<CustomDhikrEntity>,
    customCategories: List<CustomCategoryEntity>,
    onIncrementDhikr: (String, Int, String) -> Unit,
    onResetDhikr: (String) -> Unit,
    onToggleFavoriteDhikr: (String) -> Unit,
    onAddCustomDhikr: (String, String, String, Int, String, RoutineFrequency, Int, RoutineTimeOfDay, Boolean) -> Unit,
    onDeleteCustomDhikr: (Long) -> Unit,
) {
    val cel = LocalCelColors.current
    val haptic = LocalHapticFeedback.current
    val progressMap = remember(progressToday) { progressToday.associateBy { it.dhikrKey } }
    var searchQuery by remember { mutableStateOf("") }
    var selectedGroup by remember { mutableStateOf("الكل") }
    var openChapter by remember { mutableStateOf<HisnChapter?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    // Merge Room-backed Hisn al-Muslim chapters + Custom Adhkar chapters (grouped by category)
    val customChapters = remember(customAdhkar) {
        if (customAdhkar.isEmpty()) emptyList()
        else customAdhkar.groupBy { it.categoryName }.map { (catName, list) ->
            HisnChapter(
                id = "custom_cat_$catName",
                title = "أذكار مخصصة: $catName",
                categoryGroup = catName,
                items = list.map { c ->
                    val freq = runCatching { RoutineFrequency.valueOf(c.frequency).arabicLabel }.getOrDefault("يومياً")
                    val tod = runCatching { RoutineTimeOfDay.valueOf(c.timeOfDay).arabicLabel }.getOrDefault("أي وقت")
                    HisnDhikr(
                        key = "custom_dhikr_${c.id}",
                        text = c.phraseText,
                        virtue = c.virtue.ifBlank { "${c.title} • $freq • $tod" },
                        source = "ذكر شخصي مخصص ($freq • $tod)",
                        repeatCount = c.targetCount,
                        isFavorite = c.isFavorite,
                    )
                },
            )
        }
    }

    val allChapters = remember(customChapters, hisnChapters) {
        customChapters + hisnChapters
    }

    val favoriteEntries = remember(allChapters, searchQuery) {
        allChapters.flatMap { ch ->
            ch.items
                .filter { it.isFavorite }
                .filter { d ->
                    searchQuery.isBlank() ||
                        d.text.contains(searchQuery.trim()) ||
                        ch.title.contains(searchQuery.trim())
                }
                .map { d -> ch to d }
        }
    }
    val totalFavoritesCount = remember(allChapters) {
        allChapters.sumOf { ch -> ch.items.count { it.isFavorite } }
    }

    val groups = remember(allChapters, customCategories) {
        listOf("الكل", FAVORITES_FILTER) + allChapters.map { it.categoryGroup }.distinct()
    }

    if (openChapter != null) {
        val refreshedChapter = allChapters.firstOrNull { it.id == openChapter!!.id } ?: openChapter!!
        BackHandler { openChapter = null }
        ChapterReaderView(
            chapter = refreshedChapter,
            progressMap = progressMap,
            customAdhkar = customAdhkar,
            onBack = { openChapter = null },
            onIncrement = onIncrementDhikr,
            onReset = onResetDhikr,
            onToggleFavorite = onToggleFavoriteDhikr,
            onDeleteCustom = onDeleteCustomDhikr,
        )
        return
    }

    val filtered = remember(allChapters, searchQuery, selectedGroup) {
        allChapters.filter { ch ->
            val matchGroup = selectedGroup == "الكل" || ch.categoryGroup == selectedGroup
            val matchSearch = searchQuery.isBlank() ||
                ch.title.contains(searchQuery.trim()) ||
                ch.items.any { it.text.contains(searchQuery.trim()) }
            matchGroup && matchSearch
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            CelCard(
                containerColor = cel.cardSurfaceAlt,
                borderColor = cel.primaryAccent,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "حصن المسلم والأذكار",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cel.textPrimary,
                        )
                        Text(
                            text = "${HisnAlMuslimExtraData.allChapters.size} باباً من الكتاب + أذكارك المخصصة",
                            style = MaterialTheme.typography.labelMedium,
                            color = cel.primaryAccent,
                        )
                    }
                    CelActionButton(
                        text = "ذكر مخصص",
                        onClick = { showAddDialog = true },
                        testTag = "add_custom_dhikr_btn",
                        leadingIcon = {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("ابحث في أبواب حصن المسلم...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("adhkar_search_input"),
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(groups) { group ->
                    val selected = selectedGroup == group
                    val labelText = if (group == FAVORITES_FILTER) {
                        "$FAVORITES_FILTER ($totalFavoritesCount)"
                    } else group
                    FilterChip(
                        selected = selected,
                        onClick = { selectedGroup = group },
                        leadingIcon = if (group == FAVORITES_FILTER) {
                            {
                                Icon(
                                    imageVector = Icons.Filled.Favorite,
                                    contentDescription = null,
                                    tint = if (selected) cel.goldAccent else cel.missedColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        } else null,
                        label = { Text(labelText) },
                        modifier = Modifier.testTag(
                            if (group == FAVORITES_FILTER) "adhkar_filter_favorites" else "adhkar_filter_$group"
                        ),
                    )
                }
            }
        }

        if (selectedGroup == FAVORITES_FILTER) {
            if (favoriteEntries.isEmpty()) {
                item {
                    CelCard(
                        containerColor = cel.cardSurfaceAlt,
                        borderColor = cel.goldAccent,
                        testTag = "favorites_empty_card",
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FavoriteBorder,
                                contentDescription = null,
                                tint = cel.goldAccent,
                                modifier = Modifier.size(36.dp),
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "لا توجد أذكار مفضلة بعد",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = cel.textPrimary,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "افتح أي باب من حصن المسلم واضغط على رمز القلب لإضافة الذكر إلى المفضلة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = cel.textSecondary,
                            )
                        }
                    }
                }
            } else {
                items(favoriteEntries, key = { (_, d) -> "fav_${d.key}" }) { (chapter, dhikr) ->
                    val prog = progressMap[dhikr.key]
                    val cur = prog?.currentCount ?: 0
                    val customId = dhikr.key.removePrefix("custom_dhikr_").toLongOrNull()
                    DhikrTasbihCard(
                        dhikr = dhikr,
                        chapterLabel = chapter.title,
                        currentCount = cur,
                        isCustomDeletable = customId != null && customAdhkar.any { it.id == customId },
                        onIncrement = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onIncrementDhikr(dhikr.key, dhikr.repeatCount, chapter.title)
                        },
                        onReset = { onResetDhikr(dhikr.key) },
                        onToggleFavorite = { onToggleFavoriteDhikr(dhikr.key) },
                        onDeleteCustom = { customId?.let(onDeleteCustomDhikr) },
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { chapter ->
                val totalItems = chapter.items.size
                val doneItems = chapter.items.count { progressMap[it.key]?.completed == true }
                val favInChapter = chapter.items.count { it.isFavorite }
                val frac = doneItems.toFloat() / totalItems.coerceAtLeast(1)

                CelCard(
                    borderColor = if (doneItems == totalItems && totalItems > 0) cel.onTimeColor else cel.cardBorder,
                    onClick = { openChapter = chapter },
                    testTag = "hisn_chapter_${chapter.id}",
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = chapter.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = cel.textPrimary,
                                )
                                Text(
                                    text = "${chapter.categoryGroup} • $doneItems / $totalItems ذكر" +
                                        if (favInChapter > 0) " • ★ $favInChapter مفضل" else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cel.textSecondary,
                                )
                            }
                            if (doneItems == totalItems && totalItems > 0) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = "مكتمل",
                                    tint = cel.onTimeColor,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        CelProgressBar(
                            progress = frac,
                            fillColor = if (doneItems == totalItems) cel.onTimeColor else cel.primaryAccent,
                            height = 10.dp,
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }

    if (showAddDialog) {
        val defaultCats = (HisnAlMuslimExtraData.allChapters.map { it.categoryGroup } +
            customCategories.filter { it.kind == "ADHKAR" }.map { it.name }).distinct()
        AddCustomDhikrDialog(
            existingCategories = defaultCats,
            onDismiss = { showAddDialog = false },
            onSave = { title, phrase, virtue, count, cat, freq, mask, tod, isNew ->
                onAddCustomDhikr(title, phrase, virtue, count, cat, freq, mask, tod, isNew)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun ChapterReaderView(
    chapter: HisnChapter,
    progressMap: Map<String, DhikrProgressEntity>,
    customAdhkar: List<CustomDhikrEntity>,
    onBack: () -> Unit,
    onIncrement: (String, Int, String) -> Unit,
    onReset: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDeleteCustom: (Long) -> Unit,
) {
    val cel = LocalCelColors.current
    val haptic = LocalHapticFeedback.current
    val total = chapter.items.size
    val done = chapter.items.count { progressMap[it.key]?.completed == true }

    Column(modifier = Modifier.fillMaxSize()) {
        Surface(
            color = cel.cardSurface,
            border = BorderStroke(2.dp, cel.cardBorder),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("dhikr_back_btn")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = cel.primaryAccent)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = chapter.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = cel.textPrimary,
                        )
                        Text(
                            text = "تم إنجاز $done من $total",
                            style = MaterialTheme.typography.labelMedium,
                            color = cel.primaryAccent,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                CelProgressBar(
                    progress = done.toFloat() / total.coerceAtLeast(1),
                    fillColor = if (done == total) cel.onTimeColor else cel.primaryAccent,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(chapter.items, key = { it.key }) { dhikr ->
                val prog = progressMap[dhikr.key]
                val cur = prog?.currentCount ?: 0
                val customId = dhikr.key.removePrefix("custom_dhikr_").toLongOrNull()

                DhikrTasbihCard(
                    dhikr = dhikr,
                    chapterLabel = null,
                    currentCount = cur,
                    isCustomDeletable = customId != null && customAdhkar.any { it.id == customId },
                    onIncrement = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onIncrement(dhikr.key, dhikr.repeatCount, chapter.title)
                    },
                    onReset = { onReset(dhikr.key) },
                    onToggleFavorite = { onToggleFavorite(dhikr.key) },
                    onDeleteCustom = { customId?.let(onDeleteCustom) },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun DhikrTasbihCard(
    dhikr: HisnDhikr,
    chapterLabel: String?,
    currentCount: Int,
    isCustomDeletable: Boolean,
    onIncrement: () -> Unit,
    onReset: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDeleteCustom: () -> Unit,
) {
    val cel = LocalCelColors.current
    val isCompleted = currentCount >= dhikr.repeatCount

    CelCard(
        borderColor = if (isCompleted) cel.onTimeColor else cel.cardBorder,
        onClick = {
            if (!isCompleted) onIncrement()
        },
        testTag = "dhikr_card_${dhikr.key}",
    ) {
        Column {
            if (chapterLabel != null) {
                Text(
                    text = chapterLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = cel.primaryAccent,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = dhikr.text,
                fontFamily = AmiriFontFamily,
                fontSize = 21.sp,
                lineHeight = 34.sp,
                color = cel.textPrimary,
            )
            Spacer(Modifier.height(10.dp))
            if (dhikr.virtue.isNotBlank()) {
                Text(
                    text = "الفضل: ${dhikr.virtue}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cel.secondaryAccent,
                )
                Spacer(Modifier.height(4.dp))
            }
            Text(
                text = "المصدر: ${dhikr.source}",
                style = MaterialTheme.typography.labelSmall,
                color = cel.textSecondary,
            )
            Spacer(Modifier.height(12.dp))
            CelProgressBar(
                progress = currentCount.toFloat() / dhikr.repeatCount.coerceAtLeast(1),
                fillColor = if (isCompleted) cel.onTimeColor else cel.primaryAccent,
                height = 10.dp,
            )
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CutCornerShape(8.dp),
                    color = if (isCompleted) cel.onTimeColor else cel.primaryAccent,
                    border = BorderStroke(1.5.dp, cel.cardBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Filled.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFF040D1A),
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isCompleted) "مكتمل ($currentCount / ${dhikr.repeatCount})" else "تسبيح ($currentCount / ${dhikr.repeatCount})",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF040D1A),
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.testTag("toggle_favorite_${dhikr.key}"),
                    ) {
                        Icon(
                            imageVector = if (dhikr.isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                            contentDescription = if (dhikr.isFavorite) "إزالة من المفضلة" else "إضافة إلى المفضلة",
                            tint = if (dhikr.isFavorite) cel.missedColor else cel.textSecondary,
                        )
                    }
                    IconButton(onClick = onReset) {
                        Icon(Icons.Filled.Refresh, contentDescription = "إعادة العداد", tint = cel.textSecondary)
                    }
                    if (isCustomDeletable) {
                        IconButton(onClick = onDeleteCustom) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "حذف الذكر", tint = cel.missedColor)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCustomDhikrDialog(
    existingCategories: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, String, Int, String, RoutineFrequency, Int, RoutineTimeOfDay, Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var phraseText by remember { mutableStateOf("") }
    var virtue by remember { mutableStateOf("") }
    var targetCountText by remember { mutableStateOf("33") }
    var selectedCategory by remember { mutableStateOf(existingCategories.firstOrNull() ?: "أذكار مخصصة") }
    var createNewCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var frequency by remember { mutableStateOf(RoutineFrequency.DAILY) }
    var daysMask by remember { mutableIntStateOf(0b1111111) }
    var timeOfDay by remember { mutableStateOf(RoutineTimeOfDay.MORNING) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة ذكر يومي مخصص", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("عنوان الذكر (مثال: ورد الاستغفار)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("custom_dhikr_title_input"),
                )
                OutlinedTextField(
                    value = phraseText,
                    onValueChange = { phraseText = it },
                    label = { Text("نص الذكر أو الدعاء") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("custom_dhikr_phrase_input"),
                )
                OutlinedTextField(
                    value = targetCountText,
                    onValueChange = { targetCountText = it.filter { c -> c.isDigit() } },
                    label = { Text("عدد التكرار المطلوب") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = virtue,
                    onValueChange = { virtue = it },
                    label = { Text("الفضل أو الملاحظة (اختياري)") },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                Text("الوقت المفضل", style = MaterialTheme.typography.labelLarge)
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
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && phraseText.isNotBlank() && (!createNewCategory || newCategoryName.isNotBlank()),
                onClick = {
                    val finalCat = if (createNewCategory) newCategoryName else selectedCategory
                    onSave(
                        title,
                        phraseText,
                        virtue,
                        targetCountText.toIntOrNull() ?: 33,
                        finalCat,
                        frequency,
                        daysMask,
                        timeOfDay,
                        createNewCategory,
                    )
                },
                modifier = Modifier.testTag("save_custom_dhikr_btn"),
            ) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
