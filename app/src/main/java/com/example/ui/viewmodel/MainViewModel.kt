package com.example.ui.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackState
import com.example.audio.QuranAudioPlayer
import com.example.data.local.AppDatabase
import com.example.data.local.CustomCategoryEntity
import com.example.data.local.CustomDhikrEntity
import com.example.data.local.CustomNaflRoutineEntity
import com.example.data.local.DhikrProgressEntity
import com.example.data.local.NaflLogEntity
import com.example.data.local.PrayerLogEntity
import com.example.data.local.QuranDailyLogEntity
import com.example.data.local.SurahHifzEntity
import com.example.data.local.UserPreferencesRepository
import com.example.data.local.UserSettings
import com.example.data.local.WorshipDao
import com.example.data.model.AchievementBadge
import com.example.data.model.AsrMadhab
import com.example.data.model.CalculationMethod
import com.example.data.model.DailyWisdom
import com.example.data.model.HisnChapter
import com.example.data.model.NaflType
import com.example.data.model.PrayerMode
import com.example.data.model.PrayerName
import com.example.data.model.PrayerStatus
import com.example.data.model.PrayerTimesDay
import com.example.data.model.QuranChapterProgress
import com.example.data.model.QuranReadingUnit
import com.example.data.model.QuranSurah
import com.example.data.model.RoutineFrequency
import com.example.data.model.RoutineTimeOfDay
import com.example.data.model.WirdMode
import com.example.data.repository.HisnAlMuslimRepository
import com.example.data.repository.PrayerRepository
import com.example.data.repository.QuranReadingProgressRepository
import com.example.data.static.DailyWisdomData
import com.example.data.static.HisnAlMuslimExtraData
import com.example.data.static.QuranRepository
import com.example.domain.HijriCalendarHelper
import com.example.domain.HijriDateInfo
import com.example.domain.PrayerTimeCalculator
import com.example.notifications.ReminderScheduler
import com.example.sensors.CompassReading
import com.example.sensors.CompassSensorManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

data class PrayerSummaryStats(
    val streakAllDays: Int = 0,
    val streakOnTimeDays: Int = 0,
    val weeklyPercent: Int = 0,
    val monthlyPercent: Int = 0,
    val yearlyPercent: Int = 0,
    val congregationPercent: Int = 0,
)

data class NawafilSummaryStats(
    val todayCompletedCount: Int = 0,
    val todayTotalRakaat: Int = 0,
    val weeklySessions: Int = 0,
    val monthlySessions: Int = 0,
    val totalRakaatAllTime: Int = 0,
)

data class CelebrationState(
    val triggerKey: Int = 0,
    val label: String? = null,
)

class MainViewModel(
    private val appContext: Context,
    private val dao: WorshipDao,
    private val prayerRepo: PrayerRepository,
    private val quranProgressRepo: QuranReadingProgressRepository,
    private val hisnRepo: HisnAlMuslimRepository,
    private val prefs: UserPreferencesRepository,
    private val quranRepo: QuranRepository,
    private val audioPlayer: QuranAudioPlayer,
    private val compassManager: CompassSensorManager,
) : ViewModel() {

    val todayIso: String = HijriCalendarHelper.todayIso()
    val currentHijriDate: HijriDateInfo = HijriCalendarHelper.getCurrentHijriDate()
    val todayHeader: Pair<String, String> = HijriCalendarHelper.todayFormattedHeader()
    val dailyWisdom: DailyWisdom = DailyWisdomData.wisdomForToday()

    private val _selectedDateIso = MutableStateFlow(todayIso)
    val selectedDateIso: StateFlow<String> = _selectedDateIso.asStateFlow()

    val settings: StateFlow<UserSettings> = prefs.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = UserSettings(),
    )

    val allPrayerLogs: StateFlow<List<PrayerLogEntity>> = prayerRepo.allPrayerLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val allNaflLogs: StateFlow<List<NaflLogEntity>> = dao.observeAllNaflLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val allQuranDailyLogs: StateFlow<List<QuranDailyLogEntity>> = dao.observeAllQuranDailyLogs().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val surahHifzList: StateFlow<List<SurahHifzEntity>> = dao.observeAllSurahHifz().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val quranChapterProgressToday: StateFlow<List<QuranChapterProgress>> =
        quranProgressRepo.observeProgressForDate(todayIso).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val dhikrProgressToday: StateFlow<List<DhikrProgressEntity>> =
        dao.observeDhikrProgressForDate(todayIso).stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val allDhikrProgress: StateFlow<List<DhikrProgressEntity>> =
        dao.observeAllDhikrProgress().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val customAdhkar: StateFlow<List<CustomDhikrEntity>> = dao.observeCustomAdhkar().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    val hisnChapters: StateFlow<List<HisnChapter>> = hisnRepo.groupedChapters.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HisnAlMuslimExtraData.allChapters,
    )

    val customNaflRoutines: StateFlow<List<CustomNaflRoutineEntity>> =
        dao.observeCustomNaflRoutines().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val customCategories: StateFlow<List<CustomCategoryEntity>> =
        dao.observeCustomCategories().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList(),
        )

    val compassReading: StateFlow<CompassReading> = compassManager.observeAzimuth().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CompassReading(0f, false),
    )

    val audioState: StateFlow<AudioPlaybackState> = audioPlayer.state

    private val _surahs = MutableStateFlow<List<QuranSurah>>(emptyList())
    val surahs: StateFlow<List<QuranSurah>> = _surahs.asStateFlow()

    private val _celebration = MutableStateFlow(CelebrationState())
    val celebration: StateFlow<CelebrationState> = _celebration.asStateFlow()

    private val _locationStatusMessage = MutableStateFlow<String?>(null)
    val locationStatusMessage: StateFlow<String?> = _locationStatusMessage.asStateFlow()

    val todayPrayerTimes: StateFlow<PrayerTimesDay> = settings.map { s ->
        val cal = Calendar.getInstance()
        PrayerTimeCalculator.calculateForDate(
            year = cal.get(Calendar.YEAR),
            month1To12 = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            latitude = s.latitude,
            longitude = s.longitude,
            method = s.calculationMethod,
            madhab = s.asrMadhab,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrayerTimeCalculator.calculateForDate(
            2026, 11, 16, 21.4225, 39.8262, CalculationMethod.UMM_AL_QURA, AsrMadhab.SHAFI
        ),
    )

    val prayerStats: StateFlow<PrayerSummaryStats> = allPrayerLogs.map { logs ->
        computePrayerStats(logs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PrayerSummaryStats(),
    )

    val nawafilStats: StateFlow<NawafilSummaryStats> = allNaflLogs.map { logs ->
        computeNawafilStats(logs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = NawafilSummaryStats(),
    )

    val achievements: StateFlow<List<AchievementBadge>> = combine(
        prayerStats,
        allPrayerLogs,
        allQuranDailyLogs,
        settings,
        allDhikrProgress,
        allNaflLogs,
        surahHifzList,
    ) { arr ->
        val pStats = arr[0] as PrayerSummaryStats
        @Suppress("UNCHECKED_CAST")
        val pLogs = arr[1] as List<PrayerLogEntity>
        @Suppress("UNCHECKED_CAST")
        val qLogs = arr[2] as List<QuranDailyLogEntity>
        val uSettings = arr[3] as UserSettings
        @Suppress("UNCHECKED_CAST")
        val dLogs = arr[4] as List<DhikrProgressEntity>
        @Suppress("UNCHECKED_CAST")
        val nLogs = arr[5] as List<NaflLogEntity>
        @Suppress("UNCHECKED_CAST")
        val hList = arr[6] as List<SurahHifzEntity>

        buildAchievements(pStats, pLogs, qLogs, uSettings, dLogs, nLogs, hList)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList(),
    )

    init {
        viewModelScope.launch {
            hisnRepo.seedIfNeeded()
        }
        viewModelScope.launch {
            _surahs.value = quranRepo.getAllSurahs()
        }
        viewModelScope.launch {
            val s = prefs.settingsFlow.first()
            ReminderScheduler.rescheduleAll(appContext, s)
        }
    }

    fun selectCalendarDate(dateIso: String) {
        _selectedDateIso.value = dateIso
    }

    fun clearCelebration() {
        _celebration.value = _celebration.value.copy(label = null)
    }

    private fun fireCelebration(message: String) {
        _celebration.value = CelebrationState(
            triggerKey = _celebration.value.triggerKey + 1,
            label = message,
        )
    }

    fun updatePrayerStatus(
        dateIso: String,
        prayer: PrayerName,
        status: PrayerStatus,
        mode: PrayerMode,
    ) {
        viewModelScope.launch {
            prayerRepo.setPrayerStatus(
                dateIso = dateIso,
                prayer = prayer,
                status = status,
                mode = mode,
            )
            if (status == PrayerStatus.ON_TIME) {
                fireCelebration("تقبّل الله صلاة ${prayer.arabicName} في وقتها!")
            }
        }
    }

    fun togglePrayerCompletion(
        dateIso: String,
        prayer: PrayerName,
    ) {
        viewModelScope.launch {
            val updated = prayerRepo.togglePrayerCompletion(dateIso = dateIso, prayer = prayer)
            if (updated.isCompleted) {
                fireCelebration("تقبّل الله صلاة ${prayer.arabicName}!")
            }
        }
    }

    fun toggleNaflCompleted(
        dateIso: String,
        naflKey: String,
        title: String,
        rakaat: Int,
        categoryName: String,
        isCustom: Boolean,
        completed: Boolean,
    ) {
        viewModelScope.launch {
            dao.upsertNaflLog(
                NaflLogEntity(
                    dateIso = dateIso,
                    naflKey = naflKey,
                    title = title,
                    rakaat = rakaat,
                    completed = completed,
                    categoryName = categoryName,
                    isCustom = isCustom,
                )
            )
            if (completed) {
                fireCelebration("أُنجزت نافلة: $title")
            }
        }
    }

    fun addCustomNaflRoutine(
        title: String,
        rakaatCount: Int,
        categoryName: String,
        frequency: RoutineFrequency,
        daysMask: Int,
        timeOfDay: RoutineTimeOfDay,
        notes: String,
        isNewCategory: Boolean,
    ) {
        viewModelScope.launch {
            val trimmedCat = categoryName.trim().ifEmpty { "نوافل مخصصة" }
            if (isNewCategory) {
                dao.insertCustomCategory(CustomCategoryEntity(name = trimmedCat, kind = "NAWAFIL"))
            }
            dao.insertCustomNaflRoutine(
                CustomNaflRoutineEntity(
                    title = title.trim(),
                    rakaatCount = rakaatCount.coerceIn(1, 30),
                    categoryName = trimmedCat,
                    frequency = frequency.name,
                    daysMask = daysMask,
                    timeOfDay = timeOfDay.name,
                    notes = notes.trim(),
                )
            )
            fireCelebration("تمت إضافة روتين: ${title.trim()}")
        }
    }

    fun deleteCustomNaflRoutine(id: Long) {
        viewModelScope.launch { dao.deleteCustomNaflRoutine(id) }
    }

    fun incrementDhikr(key: String, targetCount: Int, labelForCelebration: String) {
        viewModelScope.launch {
            val current = dhikrProgressToday.value.firstOrNull { it.dhikrKey == key }
            val currentCount = current?.currentCount ?: 0
            if (currentCount >= targetCount) return@launch
            val next = currentCount + 1
            val done = next >= targetCount
            dao.upsertDhikrProgress(
                DhikrProgressEntity(
                    dateIso = todayIso,
                    dhikrKey = key,
                    currentCount = next,
                    targetCount = targetCount,
                    completed = done,
                )
            )
            if (done) {
                fireCelebration("تم إتمام: $labelForCelebration")
            }
        }
    }

    fun resetDhikr(key: String) {
        viewModelScope.launch { dao.resetDhikrProgress(todayIso, key) }
    }

    fun addCustomDhikr(
        title: String,
        phraseText: String,
        virtue: String,
        targetCount: Int,
        categoryName: String,
        frequency: RoutineFrequency,
        daysMask: Int,
        timeOfDay: RoutineTimeOfDay,
        isNewCategory: Boolean,
    ) {
        viewModelScope.launch {
            val trimmedCat = categoryName.trim().ifEmpty { "أذكار مخصصة" }
            if (isNewCategory) {
                dao.insertCustomCategory(CustomCategoryEntity(name = trimmedCat, kind = "ADHKAR"))
            }
            dao.insertCustomDhikr(
                CustomDhikrEntity(
                    title = title.trim(),
                    phraseText = phraseText.trim(),
                    virtue = virtue.trim(),
                    targetCount = targetCount.coerceIn(1, 1000),
                    categoryName = trimmedCat,
                    frequency = frequency.name,
                    daysMask = daysMask,
                    timeOfDay = timeOfDay.name,
                )
            )
            fireCelebration("تمت إضافة الذكر: ${title.trim()}")
        }
    }

    fun deleteCustomDhikr(id: Long) {
        viewModelScope.launch { dao.deleteCustomDhikr(id) }
    }

    fun toggleFavoriteDhikr(dhikrKey: String) {
        viewModelScope.launch {
            val isFav = hisnRepo.toggleFavorite(dhikrKey)
            if (isFav) {
                fireCelebration("تمت إضافة الذكر إلى المفضلة ★")
            }
        }
    }

    // Quran actions
    fun updateBookmark(surah: Int, ayah: Int, page: Int, juz: Int) {
        viewModelScope.launch { prefs.updateBookmark(surah, ayah, page, juz) }
    }

    fun logQuranReadingToday(addPages: Int, addMinutes: Int) {
        viewModelScope.launch {
            val current = allQuranDailyLogs.value.firstOrNull { it.dateIso == todayIso }
            val newPages = ((current?.pagesRead ?: 0) + addPages).coerceAtLeast(0)
            val newMinutes = ((current?.minutesRead ?: 0) + addMinutes).coerceAtLeast(0)
            dao.upsertQuranDailyLog(
                QuranDailyLogEntity(
                    dateIso = todayIso,
                    pagesRead = newPages,
                    minutesRead = newMinutes,
                    hifzAyahsAdded = current?.hifzAyahsAdded ?: 0,
                )
            )
            val target = settings.value.dailyTargetPages
            if (newPages >= target && (current?.pagesRead ?: 0) < target) {
                fireCelebration("ما شاء الله! أتممت ورد القرآن اليومي")
            }
        }
    }

    fun completeKhatmah() {
        viewModelScope.launch {
            prefs.incrementKhatmah()
            fireCelebration("مبارك! ختمة قرآن كاملة جديدة")
        }
    }

    fun toggleChapterRead(
        unit: QuranReadingUnit,
        unitNumber: Int,
        chapterTitle: String,
        pagesEstimate: Int = 0,
    ) {
        viewModelScope.launch {
            val updated = quranProgressRepo.toggleChapterRead(
                dateIso = todayIso,
                unit = unit,
                unitNumber = unitNumber,
                chapterTitle = chapterTitle,
                pagesEstimate = pagesEstimate,
            )
            if (updated.isRead) {
                fireCelebration("تقبّل الله! تم تحديد $chapterTitle كمقروء")
            }
        }
    }

    fun updateSurahHifz(surahNumber: Int, memorizedAyahs: Int, totalAyahs: Int) {
        viewModelScope.launch {
            val prev = surahHifzList.value.firstOrNull { it.surahNumber == surahNumber }?.memorizedAyahs ?: 0
            val clamped = memorizedAyahs.coerceIn(0, totalAyahs)
            dao.upsertSurahHifz(
                SurahHifzEntity(
                    surahNumber = surahNumber,
                    memorizedAyahs = clamped,
                    totalAyahs = totalAyahs,
                )
            )
            val delta = (clamped - prev).coerceAtLeast(0)
            if (delta > 0) {
                val curDaily = allQuranDailyLogs.value.firstOrNull { it.dateIso == todayIso }
                dao.upsertQuranDailyLog(
                    QuranDailyLogEntity(
                        dateIso = todayIso,
                        pagesRead = curDaily?.pagesRead ?: 0,
                        minutesRead = curDaily?.minutesRead ?: 0,
                        hifzAyahsAdded = (curDaily?.hifzAyahsAdded ?: 0) + delta,
                    )
                )
            }
            if (clamped == totalAyahs && prev < totalAyahs) {
                fireCelebration("مبارك! أتممت حفظ السورة")
            }
        }
    }

    fun updateWirdTarget(mode: WirdMode, pages: Int, minutes: Int) {
        viewModelScope.launch { prefs.updateWirdTarget(mode, pages, minutes) }
    }

    fun updateQuranDisplay(showTafsir: Boolean, showTranslation: Boolean, fontSizeSp: Int) {
        viewModelScope.launch { prefs.updateQuranDisplay(showTafsir, showTranslation, fontSizeSp) }
    }

    fun selectReciter(reciterId: String) {
        viewModelScope.launch { prefs.updateReciter(reciterId) }
    }

    fun playSurahAudio(surahNumber: Int, surahTitle: String) {
        audioPlayer.playSurah(surahNumber, surahTitle, settings.value.selectedReciterId)
    }

    fun toggleAudioPlayPause() = audioPlayer.togglePlayPause()
    fun seekAudio(ms: Int) = audioPlayer.seekTo(ms)
    fun setAudioSpeed(speed: Float) = audioPlayer.setSpeed(speed)
    fun stopAudio() = audioPlayer.stop()

    // Settings actions
    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch { prefs.updateTheme(isDark) }
    }

    fun toggleSnowfall(enabled: Boolean) {
        viewModelScope.launch { prefs.updateSnowfall(enabled) }
    }

    fun selectCityPreset(cityName: String, lat: Double, lon: Double, method: CalculationMethod) {
        viewModelScope.launch {
            prefs.updateLocation(cityName, lat, lon)
            prefs.updateCalculationMethod(method)
            rescheduleAfterUpdate()
        }
    }

    fun updateCalculationMethod(method: CalculationMethod) {
        viewModelScope.launch {
            prefs.updateCalculationMethod(method)
            rescheduleAfterUpdate()
        }
    }

    fun updateAsrMadhab(madhab: AsrMadhab) {
        viewModelScope.launch {
            prefs.updateAsrMadhab(madhab)
            rescheduleAfterUpdate()
        }
    }

    fun updatePrayerNotifications(enabled: Boolean, adhanSound: Boolean) {
        viewModelScope.launch {
            prefs.updatePrayerNotifications(enabled, adhanSound)
            rescheduleAfterUpdate()
        }
    }

    fun updateReminder(kind: String, enabled: Boolean, hour: Int, minute: Int) {
        viewModelScope.launch {
            prefs.updateReminder(kind, enabled, hour, minute)
            rescheduleAfterUpdate()
        }
    }

    fun sendTestNotification() {
        ReminderScheduler.showImmediateNotification(
            context = appContext,
            title = "تنبيه تجريبي — طريق الجنة",
            body = "نظام الإشعارات المحلية يعمل بنجاح لتنبيهات الصلاة والأذكار والوِرد.",
            isPrayer = true,
            useAdhan = settings.value.adhanSoundEnabled,
            notifId = 888,
        )
    }

    private suspend fun rescheduleAfterUpdate() {
        val latest = prefs.settingsFlow.first()
        ReminderScheduler.rescheduleAll(appContext, latest)
    }

    @SuppressLint("MissingPermission")
    fun refreshLocationFromGps() {
        _locationStatusMessage.value = "جاري تحديد الموقع..."
        val client = LocationServices.getFusedLocationProviderClient(appContext)
        client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    viewModelScope.launch {
                        val city = resolveCityName(loc.latitude, loc.longitude)
                        prefs.updateLocation(city, loc.latitude, loc.longitude)
                        rescheduleAfterUpdate()
                        _locationStatusMessage.value = "تم تحديث الموقع: $city"
                    }
                } else {
                    _locationStatusMessage.value = "تعذر الحصول على الموقع حالياً؛ يمكنك اختيار مدينة من القائمة."
                }
            }
            .addOnFailureListener {
                _locationStatusMessage.value = "تعذر تحديد الموقع تلقائياً؛ يمكنك اختيار مدينة يدوياً."
            }
    }

    private suspend fun resolveCityName(lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        runCatching {
            val geocoder = Geocoder(appContext, Locale("ar"))
            @Suppress("DEPRECATION")
            val list = geocoder.getFromLocation(lat, lon, 1)
            val addr = list?.firstOrNull()
            addr?.locality ?: addr?.adminArea ?: addr?.countryName
        }.getOrNull() ?: String.format(Locale.US, "%.2f, %.2f", lat, lon)
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val root = JSONObject()
        val pArr = JSONArray()
        prayerRepo.getAllPrayerLogsOnce().forEach {
            pArr.put(JSONObject().apply {
                put("date", it.dateIso)
                put("prayer", it.prayerName)
                put("status", it.status)
                put("mode", it.mode)
                put("isCompleted", it.isCompleted)
            })
        }
        val nArr = JSONArray()
        dao.getAllNaflLogsOnce().forEach {
            nArr.put(JSONObject().apply {
                put("date", it.dateIso)
                put("key", it.naflKey)
                put("title", it.title)
                put("rakaat", it.rakaat)
                put("completed", it.completed)
            })
        }
        val qArr = JSONArray()
        dao.getAllQuranDailyLogsOnce().forEach {
            qArr.put(JSONObject().apply {
                put("date", it.dateIso)
                put("pages", it.pagesRead)
                put("minutes", it.minutesRead)
            })
        }
        root.put("app", "TariqAlJannah")
        root.put("exportedAt", System.currentTimeMillis())
        root.put("khatmahCount", settings.value.khatmahCount)
        root.put("prayers", pArr)
        root.put("nawafil", nArr)
        root.put("quranLogs", qArr)
        root.toString(2)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.stop()
    }

    private fun computePrayerStats(logs: List<PrayerLogEntity>): PrayerSummaryStats {
        val byDate = logs.groupBy { it.dateIso }
        fun calcRate(days: Int): Int {
            val dates = HijriCalendarHelper.lastNDatesIso(days)
            val completed = dates.sumOf { d ->
                byDate[d]?.count {
                    it.status == PrayerStatus.ON_TIME.name ||
                        it.status == PrayerStatus.LATE.name ||
                        it.status == PrayerStatus.QADA.name
                } ?: 0
            }
            return ((completed * 100.0) / (days * 5.0)).toInt().coerceIn(0, 100)
        }

        // Streak counting backwards from today (if today isn't finished yet, allow starting from yesterday)
        val dates365 = HijriCalendarHelper.lastNDatesIso(365).reversed()
        var streakAll = 0
        var streakOnTime = 0
        for ((idx, d) in dates365.withIndex()) {
            val dayLogs = byDate[d].orEmpty()
            val doneCount = dayLogs.count {
                it.status == PrayerStatus.ON_TIME.name ||
                    it.status == PrayerStatus.LATE.name ||
                    it.status == PrayerStatus.QADA.name
            }
            if (doneCount == 5) {
                streakAll++
            } else if (idx > 0) {
                break
            }
        }
        for ((idx, d) in dates365.withIndex()) {
            val dayLogs = byDate[d].orEmpty()
            val onTimeCount = dayLogs.count { it.status == PrayerStatus.ON_TIME.name }
            if (onTimeCount == 5) {
                streakOnTime++
            } else if (idx > 0) {
                break
            }
        }

        val performed = logs.filter {
            it.status == PrayerStatus.ON_TIME.name || it.status == PrayerStatus.LATE.name
        }
        val congPercent = if (performed.isEmpty()) 0 else {
            (performed.count { it.mode == PrayerMode.CONGREGATION.name } * 100) / performed.size
        }

        return PrayerSummaryStats(
            streakAllDays = streakAll,
            streakOnTimeDays = streakOnTime,
            weeklyPercent = calcRate(7),
            monthlyPercent = calcRate(30),
            yearlyPercent = calcRate(365),
            congregationPercent = congPercent,
        )
    }

    private fun computeNawafilStats(logs: List<NaflLogEntity>): NawafilSummaryStats {
        val completed = logs.filter { it.completed }
        val todayLogs = completed.filter { it.dateIso == todayIso }
        val weekDates = HijriCalendarHelper.lastNDatesIso(7).toSet()
        val monthDates = HijriCalendarHelper.lastNDatesIso(30).toSet()

        return NawafilSummaryStats(
            todayCompletedCount = todayLogs.size,
            todayTotalRakaat = todayLogs.sumOf { it.rakaat },
            weeklySessions = completed.count { it.dateIso in weekDates },
            monthlySessions = completed.count { it.dateIso in monthDates },
            totalRakaatAllTime = completed.sumOf { it.rakaat },
        )
    }

    private fun buildAchievements(
        pStats: PrayerSummaryStats,
        pLogs: List<PrayerLogEntity>,
        qLogs: List<QuranDailyLogEntity>,
        uSettings: UserSettings,
        dLogs: List<DhikrProgressEntity>,
        nLogs: List<NaflLogEntity>,
        hList: List<SurahHifzEntity>,
    ): List<AchievementBadge> {
        val fajrCongDays = pLogs
            .filter { it.prayerName == PrayerName.FAJR.name && it.mode == PrayerMode.CONGREGATION.name && it.status == PrayerStatus.ON_TIME.name }
            .distinctBy { it.dateIso }.size
        val quranDays = qLogs.count { it.pagesRead > 0 }
        val adhkarDays = dLogs.filter { it.completed }.distinctBy { it.dateIso }.size
        val qiyamDays = nLogs.count { it.completed && (it.naflKey == NaflType.QIYAM.key || it.title.contains("قيام")) }
        val memorizedSurahs = hList.count { it.memorizedAyahs >= it.totalAyahs && it.totalAyahs > 0 }

        return listOf(
            AchievementBadge(
                id = "week_on_time",
                title = "أسبوع كامل في وقت الصلاة",
                description = "إتمام الصلوات الخمس في وقتها 7 أيام متتالية",
                requirementLabel = "${pStats.streakOnTimeDays} / 7 أيام",
                isUnlocked = pStats.streakOnTimeDays >= 7,
                progress = (pStats.streakOnTimeDays / 7f).coerceIn(0f, 1f),
            ),
            AchievementBadge(
                id = "fajr_cong",
                title = "فجر الجماعة",
                description = "أداء صلاة الفجر في جماعة 7 مرات في وقتها",
                requirementLabel = "$fajrCongDays / 7 مرات",
                isUnlocked = fajrCongDays >= 7,
                progress = (fajrCongDays / 7f).coerceIn(0f, 1f),
            ),
            AchievementBadge(
                id = "khatmah",
                title = "ختمة مكتملة",
                description = "إتمام ختمة كاملة للقرآن الكريم",
                requirementLabel = "${uSettings.khatmahCount} ختمة",
                isUnlocked = uSettings.khatmahCount >= 1,
                progress = if (uSettings.khatmahCount >= 1) 1f else (uSettings.lastPage / 604f).coerceIn(0f, 0.99f),
            ),
            AchievementBadge(
                id = "wird_steady",
                title = "ثابت على الوِرد",
                description = "المداومة على تلاوة القرآن 14 يوماً",
                requirementLabel = "$quranDays / 14 يوماً",
                isUnlocked = quranDays >= 14,
                progress = (quranDays / 14f).coerceIn(0f, 1f),
            ),
            AchievementBadge(
                id = "adhkar_guardian",
                title = "حصن المسلم الدائم",
                description = "إتمام أذكار يومية في 7 أيام مختلفة",
                requirementLabel = "$adhkarDays / 7 أيام",
                isUnlocked = adhkarDays >= 7,
                progress = (adhkarDays / 7f).coerceIn(0f, 1f),
            ),
            AchievementBadge(
                id = "qiyam_nights",
                title = "مُحيي الليل",
                description = "أداء صلاة قيام الليل 5 ليالٍ",
                requirementLabel = "$qiyamDays / 5 ليالٍ",
                isUnlocked = qiyamDays >= 5,
                progress = (qiyamDays / 5f).coerceIn(0f, 1f),
            ),
            AchievementBadge(
                id = "hifz_surahs",
                title = "حافظ للسور",
                description = "إتمام حفظ 3 سور من القرآن الكريم",
                requirementLabel = "$memorizedSurahs / 3 سور",
                isUnlocked = memorizedSurahs >= 3,
                progress = (memorizedSurahs / 3f).coerceIn(0f, 1f),
            ),
        )
    }

    val totalHisnDhikrCount: Int = HisnAlMuslimExtraData.allChapters.sumOf { it.items.size }
}

class MainViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val appCtx = context.applicationContext
        val db = AppDatabase.getInstance(appCtx)
        val worshipDao = db.worshipDao()
        return MainViewModel(
            appContext = appCtx,
            dao = worshipDao,
            prayerRepo = PrayerRepository(worshipDao),
            quranProgressRepo = QuranReadingProgressRepository(worshipDao),
            hisnRepo = HisnAlMuslimRepository(worshipDao),
            prefs = UserPreferencesRepository(appCtx),
            quranRepo = QuranRepository(appCtx),
            audioPlayer = QuranAudioPlayer(),
            compassManager = CompassSensorManager(appCtx),
        ) as T
    }
}
