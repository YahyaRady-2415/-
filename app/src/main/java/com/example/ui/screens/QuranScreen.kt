package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.audio.AudioPlaybackState
import com.example.data.local.QuranDailyLogEntity
import com.example.data.local.SurahHifzEntity
import com.example.data.local.UserSettings
import com.example.data.model.QuranChapterProgress
import com.example.data.model.QuranJuzCatalog
import com.example.data.model.QuranReadingUnit
import com.example.data.model.QuranSurah
import com.example.data.model.WirdMode
import com.example.data.static.QuranRecitersData
import com.example.ui.components.CelActionButton
import com.example.ui.components.CelCard
import com.example.ui.components.CelProgressBar
import com.example.ui.components.CelSectionHeader
import com.example.ui.theme.AmiriFontFamily
import com.example.ui.theme.AmiriQuranFontFamily
import com.example.ui.theme.FinnishBlue
import com.example.ui.theme.LocalCelColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuranScreen(
    surahs: List<QuranSurah>,
    settings: UserSettings,
    todayIso: String,
    quranDailyLogs: List<QuranDailyLogEntity>,
    surahHifzList: List<SurahHifzEntity>,
    chapterProgressToday: List<QuranChapterProgress>,
    audioState: AudioPlaybackState,
    onUpdateBookmark: (Int, Int, Int, Int) -> Unit,
    onLogReadingToday: (Int, Int) -> Unit,
    onCompleteKhatmah: () -> Unit,
    onToggleChapterRead: (QuranReadingUnit, Int, String, Int) -> Unit,
    onUpdateSurahHifz: (Int, Int, Int) -> Unit,
    onUpdateWirdTarget: (WirdMode, Int, Int) -> Unit,
    onUpdateDisplay: (Boolean, Boolean, Int) -> Unit,
    onSelectReciter: (String) -> Unit,
    onPlaySurahAudio: (Int, String) -> Unit,
    onToggleAudioPlayPause: () -> Unit,
    onSeekAudio: (Int) -> Unit,
    onSetAudioSpeed: (Float) -> Unit,
    onStopAudio: () -> Unit,
) {
    val cel = LocalCelColors.current
    var openSurahNumber by remember { mutableStateOf<Int?>(null) }
    var activeSubTab by remember { mutableIntStateOf(0) } // 0 = المصحف والوِرد, 1 = الحفظ, 2 = القرّاء
    var readingUnitFilter by remember { mutableStateOf(QuranReadingUnit.SURAH) }
    var searchQuery by remember { mutableStateOf("") }
    var showTargetDialog by remember { mutableStateOf(false) }

    val readSurahNumbers = remember(chapterProgressToday) {
        chapterProgressToday
            .filter { it.unit == QuranReadingUnit.SURAH && it.isRead }
            .map { it.unitNumber }
            .toSet()
    }
    val readJuzNumbers = remember(chapterProgressToday) {
        chapterProgressToday
            .filter { it.unit == QuranReadingUnit.JUZ && it.isRead }
            .map { it.unitNumber }
            .toSet()
    }

    val openSurah = remember(surahs, openSurahNumber) {
        openSurahNumber?.let { num -> surahs.firstOrNull { it.number == num } }
    }

    if (openSurah != null) {
        BackHandler { openSurahNumber = null }
        val isCurrentSurahRead = openSurah.number in readSurahNumbers
        SurahReaderView(
            surah = openSurah,
            settings = settings,
            audioState = audioState,
            isMarkedAsReadToday = isCurrentSurahRead,
            onToggleMarkedAsRead = {
                val estPages = (openSurah.ayahs.map { it.page }.distinct().size).coerceAtLeast(1)
                onToggleChapterRead(
                    QuranReadingUnit.SURAH,
                    openSurah.number,
                    openSurah.arabicName,
                    estPages,
                )
            },
            onBack = { openSurahNumber = null },
            onBookmarkAyah = { ayah ->
                onUpdateBookmark(openSurah.number, ayah.numberInSurah, ayah.page, ayah.juz)
            },
            onLogPages = { pages -> onLogReadingToday(pages, 10) },
            onUpdateDisplay = onUpdateDisplay,
            onPlaySurah = { onPlaySurahAudio(openSurah.number, openSurah.arabicName) },
            onTogglePlayPause = onToggleAudioPlayPause,
            onSeekAudio = onSeekAudio,
            onSetSpeed = onSetAudioSpeed,
            onStopAudio = onStopAudio,
            onSelectReciter = onSelectReciter,
        )
        return
    }

    val todayLog = remember(quranDailyLogs, todayIso) {
        quranDailyLogs.firstOrNull { it.dateIso == todayIso }
    }
    val hifzMap = remember(surahHifzList) { surahHifzList.associateBy { it.surahNumber } }
    val totalMemorizedAyahs = remember(surahHifzList) { surahHifzList.sumOf { it.memorizedAyahs } }

    val filteredSurahs = remember(surahs, searchQuery) {
        if (searchQuery.isBlank()) surahs
        else surahs.filter {
            it.arabicName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.englishName.contains(searchQuery.trim(), ignoreCase = true) ||
                it.number.toString() == searchQuery.trim()
        }
    }
    val filteredJuz = remember(searchQuery) {
        val list = QuranJuzCatalog.allJuz
        if (searchQuery.isBlank()) list
        else list.filter {
            it.arabicTitle.contains(searchQuery.trim(), ignoreCase = true) ||
                it.startSurahArabic.contains(searchQuery.trim(), ignoreCase = true) ||
                it.juzNumber.toString() == searchQuery.trim()
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Sub-tabs: المصحف والورد | الحفظ | القرّاء
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            listOf("المصحف والوِرد", "تتبع الحفظ", "القرّاء (حفص)").forEachIndexed { idx, label ->
                val active = activeSubTab == idx
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { activeSubTab = idx }
                        .testTag("quran_tab_$idx"),
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

        if (surahs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = cel.primaryAccent)
                    Spacer(Modifier.height(10.dp))
                    Text("جاري تحميل المصحف الشريف كاملاً...", color = cel.textPrimary)
                }
            }
            return
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (activeSubTab) {
                0 -> {
                    // Geometric Banner + Daily Wird Card
                    item {
                        CelCard(
                            contentPadding = PaddingValues(0.dp),
                            borderColor = cel.primaryAccent,
                            testTag = "quran_wird_card",
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(100.dp)
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_quran_geometry_art),
                                        contentDescription = "زخرفة قرآنية هندسية",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(FinnishBlue.copy(alpha = 0.72f))
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Column {
                                            Text(
                                                text = "المصحف برواية حفص عن عاصم",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                            )
                                            Text(
                                                text = "الختمات المكتملة: ${settings.khatmahCount}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFF00E5FF),
                                            )
                                        }
                                        Surface(
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable { showTargetDialog = true },
                                            shape = RoundedCornerShape(8.dp),
                                            color = Color.White,
                                            border = BorderStroke(2.dp, Color(0xFF00E5FF)),
                                        ) {
                                            Text(
                                                text = "تعديل الوِرد",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = FinnishBlue,
                                            )
                                        }
                                    }
                                }

                                Column(modifier = Modifier.padding(14.dp)) {
                                    val pagesToday = todayLog?.pagesRead ?: 0
                                    val minsToday = todayLog?.minutesRead ?: 0
                                    val progress = when (settings.wirdMode) {
                                        WirdMode.MINUTES -> minsToday.toFloat() / settings.dailyTargetMinutes.coerceAtLeast(1)
                                        else -> pagesToday.toFloat() / settings.dailyTargetPages.coerceAtLeast(1)
                                    }.coerceIn(0f, 1f)

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "إنجاز اليوم: $pagesToday / ${settings.dailyTargetPages} صفحة ($minsToday دقيقة)",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = cel.textPrimary,
                                        )
                                        Text(
                                            text = "${(progress * 100).toInt()}%",
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = cel.primaryAccent,
                                        )
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    CelProgressBar(progress = progress, fillColor = cel.primaryAccent)
                                    Spacer(Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        CelActionButton(
                                            text = "+1 صفحة",
                                            onClick = { onLogReadingToday(1, 3) },
                                            modifier = Modifier.weight(1f),
                                            testTag = "log_1_page_btn",
                                        )
                                        CelActionButton(
                                            text = "+4 صفحات",
                                            onClick = { onLogReadingToday(4, 12) },
                                            modifier = Modifier.weight(1f),
                                            containerColor = cel.secondaryAccent,
                                        )
                                        CelActionButton(
                                            text = "ختمة +1",
                                            onClick = onCompleteKhatmah,
                                            modifier = Modifier.weight(1f),
                                            containerColor = cel.goldAccent,
                                            contentColor = Color(0xFF040D1A),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bookmark Resume Card
                    item {
                        val bSurah = surahs.firstOrNull { it.number == settings.lastSurah } ?: surahs.first()
                        CelCard(
                            containerColor = cel.cardSurfaceAlt,
                            borderColor = cel.goldAccent,
                            onClick = { openSurahNumber = bSurah.number },
                            testTag = "quran_bookmark_card",
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Filled.Bookmark,
                                        contentDescription = null,
                                        tint = cel.goldAccent,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "متابعة القراءة من آخر موضع",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = cel.goldAccent,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text(
                                            text = "${bSurah.arabicName} • الآية ${settings.lastAyah} • صفحة ${settings.lastPage}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = cel.textPrimary,
                                        )
                                    }
                                }
                                Text(
                                    text = "افتح",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = cel.primaryAccent,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }

                    // Daily Progress Tracker Toggle: By Surah or By Juz
                    item {
                        CelCard(
                            containerColor = cel.cardSurface,
                            borderColor = cel.cardBorder,
                            accentStripeColor = cel.secondaryAccent,
                            testTag = "quran_chapter_unit_selector",
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "تتبع القراءة اليومي",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = cel.textPrimary,
                                        )
                                        Text(
                                            text = "المقروء اليوم: ${readSurahNumbers.size} سورة • ${readJuzNumbers.size} جزء",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = cel.onTimeColor,
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FilterChip(
                                            selected = readingUnitFilter == QuranReadingUnit.SURAH,
                                            onClick = { readingUnitFilter = QuranReadingUnit.SURAH },
                                            label = { Text("بالسور (114)") },
                                            modifier = Modifier.testTag("unit_filter_surah"),
                                        )
                                        FilterChip(
                                            selected = readingUnitFilter == QuranReadingUnit.JUZ,
                                            onClick = { readingUnitFilter = QuranReadingUnit.JUZ },
                                            label = { Text("بالأجزاء (30)") },
                                            modifier = Modifier.testTag("unit_filter_juz"),
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                val unitProgress = if (readingUnitFilter == QuranReadingUnit.SURAH) {
                                    (readSurahNumbers.size / 114f).coerceIn(0f, 1f)
                                } else {
                                    (readJuzNumbers.size / 30f).coerceIn(0f, 1f)
                                }
                                CelProgressBar(
                                    progress = unitProgress,
                                    fillColor = cel.onTimeColor,
                                    height = 10.dp,
                                )
                            }
                        }
                    }

                    // Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = {
                                Text(
                                    if (readingUnitFilter == QuranReadingUnit.SURAH) {
                                        "ابحث عن سورة (بالاسم أو الرقم)"
                                    } else {
                                        "ابحث عن جزء (بالرقم أو الاسم)"
                                    }
                                )
                            },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("quran_search_input"),
                        )
                    }

                    if (readingUnitFilter == QuranReadingUnit.SURAH) {
                        // All 114 Surahs List with Mark-as-Read Toggle
                        items(filteredSurahs, key = { it.number }) { surah ->
                            val hifzCount = hifzMap[surah.number]?.memorizedAyahs ?: 0
                            val isRead = surah.number in readSurahNumbers
                            val estimatedPages = remember(surah.number) {
                                surah.ayahs.map { it.page }.distinct().size.coerceAtLeast(1)
                            }
                            CelCard(
                                borderColor = if (isRead) cel.onTimeColor else cel.cardBorder,
                                onClick = { openSurahNumber = surah.number },
                                testTag = "surah_card_${surah.number}",
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Surface(
                                            shape = CutCornerShape(8.dp),
                                            color = if (isRead) cel.onTimeColor else cel.primaryAccent,
                                            border = BorderStroke(1.5.dp, cel.cardBorder),
                                        ) {
                                            Text(
                                                text = "${surah.number}",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (cel.isDark || isRead) Color(0xFF040D1A) else Color.White,
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = surah.arabicName,
                                                fontFamily = AmiriFontFamily,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = cel.textPrimary,
                                            )
                                            Text(
                                                text = "${surah.revelationArabic} • ${surah.ayahs.size} آية" +
                                                    (if (isRead) " • مقروءة اليوم" else "") +
                                                    (if (hifzCount > 0) " • محفوظ: $hifzCount" else ""),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isRead) cel.onTimeColor else cel.textSecondary,
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                onToggleChapterRead(
                                                    QuranReadingUnit.SURAH,
                                                    surah.number,
                                                    surah.arabicName,
                                                    estimatedPages,
                                                )
                                            },
                                            modifier = Modifier.testTag("toggle_surah_read_${surah.number}"),
                                        ) {
                                            Icon(
                                                imageVector = if (isRead) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                                contentDescription = if (isRead) "إلغاء تحديد ${surah.arabicName} كمقروءة" else "تحديد ${surah.arabicName} كمقروءة",
                                                tint = if (isRead) cel.onTimeColor else cel.textSecondary,
                                            )
                                        }
                                        IconButton(
                                            onClick = { onPlaySurahAudio(surah.number, surah.arabicName) },
                                        ) {
                                            val isThisPlaying = audioState.surahNumber == surah.number && audioState.isPlaying
                                            Icon(
                                                imageVector = if (isThisPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                                contentDescription = "تلاوة ${surah.arabicName}",
                                                tint = cel.primaryAccent,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // All 30 Ajza' List with Mark-as-Read Toggle
                        items(filteredJuz, key = { "juz_${it.juzNumber}" }) { juz ->
                            val isRead = juz.juzNumber in readJuzNumbers
                            CelCard(
                                borderColor = if (isRead) cel.onTimeColor else cel.cardBorder,
                                containerColor = if (isRead) cel.cardSurfaceAlt else cel.cardSurface,
                                onClick = {
                                    onToggleChapterRead(
                                        QuranReadingUnit.JUZ,
                                        juz.juzNumber,
                                        juz.arabicTitle,
                                        20,
                                    )
                                },
                                testTag = "juz_card_${juz.juzNumber}",
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Surface(
                                            shape = CutCornerShape(8.dp),
                                            color = if (isRead) cel.onTimeColor else cel.secondaryAccent,
                                            border = BorderStroke(1.5.dp, cel.cardBorder),
                                        ) {
                                            Text(
                                                text = "${juz.juzNumber}",
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (cel.isDark || isRead) Color(0xFF040D1A) else Color.White,
                                            )
                                        }
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = juz.arabicTitle,
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = cel.textPrimary,
                                            )
                                            Text(
                                                text = "يبدأ من سورة ${juz.startSurahArabic} (آية ${juz.startAyah})" +
                                                    if (isRead) " • مقروء اليوم" else "",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isRead) cel.onTimeColor else cel.textSecondary,
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            onToggleChapterRead(
                                                QuranReadingUnit.JUZ,
                                                juz.juzNumber,
                                                juz.arabicTitle,
                                                20,
                                            )
                                        },
                                        modifier = Modifier.testTag("toggle_juz_read_${juz.juzNumber}"),
                                    ) {
                                        Icon(
                                            imageVector = if (isRead) Icons.Filled.CheckBox else Icons.Filled.CheckBoxOutlineBlank,
                                            contentDescription = if (isRead) "إلغاء تحديد ${juz.arabicTitle} كمقروء" else "تحديد ${juz.arabicTitle} كمقروء",
                                            tint = if (isRead) cel.onTimeColor else cel.textSecondary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // Memorization (Hifz) Tracker
                    item {
                        CelCard(
                            containerColor = cel.cardSurfaceAlt,
                            borderColor = cel.secondaryAccent,
                            testTag = "hifz_summary_card",
                        ) {
                            Column {
                                CelSectionHeader(
                                    title = "تتبع حفظ القرآن الكريم",
                                    subtitle = "إجمالي الآيات المحفوظة: $totalMemorizedAyahs من 6236 آية",
                                )
                                Spacer(Modifier.height(10.dp))
                                CelProgressBar(
                                    progress = (totalMemorizedAyahs / 6236f).coerceIn(0f, 1f),
                                    fillColor = cel.secondaryAccent,
                                )
                            }
                        }
                    }
                    items(surahs, key = { "hifz_${it.number}" }) { surah ->
                        val total = surah.ayahs.size
                        val mem = (hifzMap[surah.number]?.memorizedAyahs ?: 0).coerceIn(0, total)
                        CelCard(
                            borderColor = if (mem == total) cel.onTimeColor else cel.cardBorder,
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "${surah.number}. ${surah.arabicName}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = cel.textPrimary,
                                        )
                                        Text(
                                            text = "المحفوظ: $mem / $total آية",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = if (mem == total) cel.onTimeColor else cel.textSecondary,
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onUpdateSurahHifz(surah.number, mem - 1, total) }) {
                                            Icon(Icons.Filled.Remove, contentDescription = "إنقاص آية", tint = cel.textPrimary)
                                        }
                                        IconButton(onClick = { onUpdateSurahHifz(surah.number, mem + 1, total) }) {
                                            Icon(Icons.Filled.Add, contentDescription = "زيادة آية", tint = cel.primaryAccent)
                                        }
                                        TextButton(onClick = { onUpdateSurahHifz(surah.number, total, total) }) {
                                            Text("إتمام", color = cel.onTimeColor, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(6.dp))
                                CelProgressBar(
                                    progress = mem.toFloat() / total.coerceAtLeast(1),
                                    fillColor = if (mem == total) cel.onTimeColor else cel.primaryAccent,
                                    height = 10.dp,
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Curated Popular Quran Reciters (Hafs 'an 'Asim)
                    item {
                        CelSectionHeader(
                            title = "نخبة قرّاء القرآن الكريم",
                            subtitle = "جميع التلاوات برواية حفص عن عاصم — متاحة للاستماع الفوري",
                        )
                    }
                    items(QuranRecitersData.reciters, key = { it.id }) { reciter ->
                        val isSelected = settings.selectedReciterId == reciter.id
                        val isPlayingSample = audioState.reciterId == reciter.id && audioState.isPlaying
                        CelCard(
                            borderColor = if (isSelected) cel.primaryAccent else cel.cardBorder,
                            containerColor = if (isSelected) cel.cardSurfaceAlt else cel.cardSurface,
                            onClick = { onSelectReciter(reciter.id) },
                            testTag = "reciter_card_${reciter.id}",
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RecordVoiceOver,
                                        contentDescription = reciter.arabicName,
                                        tint = if (isSelected) cel.primaryAccent else cel.textSecondary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = reciter.arabicName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = cel.textPrimary,
                                        )
                                        Text(
                                            text = "${reciter.country} • ${reciter.style} • ${reciter.narration}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cel.primaryAccent,
                                        )
                                        Text(
                                            text = reciter.baseUrl,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = cel.textSecondary,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        onSelectReciter(reciter.id)
                                        onPlaySurahAudio(1, "سورة الفاتحة")
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isPlayingSample) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = "استماع لنموذج",
                                        tint = cel.goldAccent,
                                    )
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (showTargetDialog) {
        WirdTargetDialog(
            currentMode = settings.wirdMode,
            currentPages = settings.dailyTargetPages,
            currentMinutes = settings.dailyTargetMinutes,
            onDismiss = { showTargetDialog = false },
            onSave = { mode, pages, mins ->
                onUpdateWirdTarget(mode, pages, mins)
                showTargetDialog = false
            },
        )
    }
}

@Composable
private fun SurahReaderView(
    surah: QuranSurah,
    settings: UserSettings,
    audioState: AudioPlaybackState,
    isMarkedAsReadToday: Boolean,
    onToggleMarkedAsRead: () -> Unit,
    onBack: () -> Unit,
    onBookmarkAyah: (com.example.data.model.QuranAyah) -> Unit,
    onLogPages: (Int) -> Unit,
    onUpdateDisplay: (Boolean, Boolean, Int) -> Unit,
    onPlaySurah: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekAudio: (Int) -> Unit,
    onSetSpeed: (Float) -> Unit,
    onStopAudio: () -> Unit,
    onSelectReciter: (String) -> Unit,
) {
    val cel = LocalCelColors.current
    val reciter = remember(settings.selectedReciterId) {
        QuranRecitersData.byId(settings.selectedReciterId)
    }
    var showReciterPicker by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Reader Top Bar
        Surface(
            color = cel.cardSurface,
            border = BorderStroke(2.dp, cel.cardBorder),
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("reader_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = cel.primaryAccent)
                        }
                        Column {
                            Text(
                                text = surah.arabicName,
                                fontFamily = AmiriFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = cel.textPrimary,
                            )
                            Text(
                                text = "${surah.revelationArabic} • ${surah.ayahs.size} آية",
                                style = MaterialTheme.typography.labelSmall,
                                color = cel.textSecondary,
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilterChip(
                            selected = isMarkedAsReadToday,
                            onClick = onToggleMarkedAsRead,
                            label = { Text(if (isMarkedAsReadToday) "مقروءة ✓" else "تحديد كمقروءة") },
                            modifier = Modifier.testTag("reader_mark_surah_read_chip"),
                        )
                        FilterChip(
                            selected = settings.showTafsir,
                            onClick = { onUpdateDisplay(!settings.showTafsir, settings.showTranslation, settings.quranFontSizeSp) },
                            label = { Text("التفسير") },
                        )
                        FilterChip(
                            selected = settings.showTranslation,
                            onClick = { onUpdateDisplay(settings.showTafsir, !settings.showTranslation, settings.quranFontSizeSp) },
                            label = { Text("ترجمة") },
                        )
                    }
                }

                // Font Size & Audio Player Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.TextFields, contentDescription = null, tint = cel.textSecondary, modifier = Modifier.size(18.dp))
                        IconButton(onClick = { onUpdateDisplay(settings.showTafsir, settings.showTranslation, settings.quranFontSizeSp - 2) }) {
                            Icon(Icons.Filled.Remove, contentDescription = "تصغير الخط")
                        }
                        Text("${settings.quranFontSizeSp}", style = MaterialTheme.typography.labelMedium, color = cel.textPrimary)
                        IconButton(onClick = { onUpdateDisplay(settings.showTafsir, settings.showTranslation, settings.quranFontSizeSp + 2) }) {
                            Icon(Icons.Filled.Add, contentDescription = "تكبير الخط")
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { showReciterPicker = true }) {
                            Icon(Icons.Filled.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(reciter.arabicName.split(" ").take(2).joinToString(" "), maxLines = 1)
                        }
                        IconButton(
                            onClick = {
                                if (audioState.surahNumber == surah.number) onTogglePlayPause() else onPlaySurah()
                            }
                        ) {
                            Icon(
                                imageVector = if (audioState.surahNumber == surah.number && audioState.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = "تشغيل التلاوة",
                                tint = cel.primaryAccent,
                            )
                        }
                        if (audioState.surahNumber == surah.number && (audioState.isPlaying || audioState.isBuffering)) {
                            IconButton(onClick = onStopAudio) {
                                Icon(Icons.Filled.Stop, contentDescription = "إيقاف", tint = cel.missedColor)
                            }
                        }
                    }
                }

                if (audioState.surahNumber == surah.number && (audioState.durationMs > 0 || audioState.isBuffering || audioState.errorMessage != null)) {
                    if (audioState.isBuffering) {
                        Text("جاري تحميل التلاوة الصوتية...", style = MaterialTheme.typography.labelSmall, color = cel.primaryAccent)
                    } else if (audioState.errorMessage != null) {
                        Text(audioState.errorMessage, style = MaterialTheme.typography.labelSmall, color = cel.missedColor)
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Slider(
                                value = audioState.positionMs.toFloat(),
                                onValueChange = { onSeekAudio(it.toInt()) },
                                valueRange = 0f..audioState.durationMs.coerceAtLeast(1).toFloat(),
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = {
                                val nextSpeed = when (audioState.speed) {
                                    1.0f -> 1.25f
                                    1.25f -> 0.75f
                                    else -> 1.0f
                                }
                                onSetSpeed(nextSpeed)
                            }) {
                                Text("${audioState.speed}x", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (surah.number != 1 && surah.number != 9) {
                item {
                    Text(
                        text = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ",
                        fontFamily = AmiriQuranFontFamily,
                        fontSize = (settings.quranFontSizeSp + 2).sp,
                        color = cel.primaryAccent,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    )
                }
            }

            items(surah.ayahs, key = { it.numberInSurah }) { ayah ->
                val isBookmarked = settings.lastSurah == surah.number && settings.lastAyah == ayah.numberInSurah
                CelCard(
                    borderColor = if (isBookmarked) cel.goldAccent else cel.cardBorder,
                    onClick = { onBookmarkAyah(ayah) },
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (isBookmarked) cel.goldAccent else cel.cardSurfaceAlt,
                                border = BorderStroke(1.dp, cel.cardBorder),
                            ) {
                                Text(
                                    text = "آية ${ayah.numberInSurah} • ص ${ayah.page} • ج ${ayah.juz}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isBookmarked) Color(0xFF040D1A) else cel.primaryAccent,
                                )
                            }
                            if (isBookmarked) {
                                Icon(
                                    imageVector = Icons.Filled.Bookmark,
                                    contentDescription = "علامة التوقف",
                                    tint = cel.goldAccent,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "${ayah.textUthmani} ﴿${ayah.numberInSurah}﴾",
                            fontFamily = AmiriQuranFontFamily,
                            fontSize = settings.quranFontSizeSp.sp,
                            lineHeight = (settings.quranFontSizeSp * 1.75f).sp,
                            color = cel.textPrimary,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (settings.showTafsir && ayah.tafsirMuyassar.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = cel.cardSurfaceAlt,
                                border = BorderStroke(1.dp, cel.cardBorder.copy(alpha = 0.4f)),
                            ) {
                                Text(
                                    text = "التفسير الميسر: ${ayah.tafsirMuyassar}",
                                    modifier = Modifier.padding(10.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = cel.textSecondary,
                                )
                            }
                        }
                        if (settings.showTranslation && ayah.translationEn.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = ayah.translationEn,
                                style = MaterialTheme.typography.bodyMedium,
                                color = cel.textSecondary,
                            )
                        }
                    }
                }
            }

            item {
                CelActionButton(
                    text = "تسجيل قراءة هذه السورة في الوِرد اليومي",
                    onClick = {
                        val approxPages = ((surah.ayahs.lastOrNull()?.page ?: 1) - (surah.ayahs.firstOrNull()?.page ?: 1) + 1).coerceAtLeast(1)
                        onLogPages(approxPages)
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    if (showReciterPicker) {
        AlertDialog(
            onDismissRequest = { showReciterPicker = false },
            title = { Text("اختر القارئ (حفص عن عاصم)") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(QuranRecitersData.reciters) { r ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onSelectReciter(r.id)
                                    showReciterPicker = false
                                    onPlaySurah()
                                },
                            color = if (r.id == settings.selectedReciterId) cel.primaryAccent.copy(alpha = 0.2f) else cel.cardSurface,
                            border = BorderStroke(1.dp, cel.cardBorder),
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(r.arabicName, fontWeight = FontWeight.Bold, color = cel.textPrimary)
                                Text("${r.country} • ${r.style}", style = MaterialTheme.typography.labelSmall, color = cel.textSecondary)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showReciterPicker = false }) { Text("إغلاق") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WirdTargetDialog(
    currentMode: WirdMode,
    currentPages: Int,
    currentMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (WirdMode, Int, Int) -> Unit,
) {
    var mode by remember { mutableStateOf(currentMode) }
    var pages by remember { mutableStateOf(currentPages.toString()) }
    var minutes by remember { mutableStateOf(currentMinutes.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ضبط الوِرد القرآني اليومي", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("طريقة الحساب", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WirdMode.entries.forEach { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = {
                                mode = m
                                if (m == WirdMode.JUZ) pages = "20"
                            },
                            label = { Text(m.arabicLabel) },
                        )
                    }
                }
                OutlinedTextField(
                    value = pages,
                    onValueChange = { pages = it.filter { ch -> ch.isDigit() } },
                    label = { Text("عدد الصفحات يومياً (الجزء = 20 صفحة)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = minutes,
                    onValueChange = { minutes = it.filter { ch -> ch.isDigit() } },
                    label = { Text("عدد الدقائق يومياً") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(mode, pages.toIntOrNull() ?: 4, minutes.toIntOrNull() ?: 15)
            }) { Text("حفظ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("إلغاء") }
        },
    )
}
