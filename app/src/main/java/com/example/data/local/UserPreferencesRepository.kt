package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.AsrMadhab
import com.example.data.model.CalculationMethod
import com.example.data.model.WirdMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tariq_prefs")

data class UserSettings(
    val isDarkMode: Boolean = true,
    val snowfallEnabled: Boolean = true,
    val cityName: String = "مكة المكرمة",
    val latitude: Double = 21.4225,
    val longitude: Double = 39.8262,
    val calculationMethod: CalculationMethod = CalculationMethod.UMM_AL_QURA,
    val asrMadhab: AsrMadhab = AsrMadhab.SHAFI,
    // Quran preferences
    val lastSurah: Int = 1,
    val lastAyah: Int = 1,
    val lastPage: Int = 1,
    val lastJuz: Int = 1,
    val wirdMode: WirdMode = WirdMode.PAGES,
    val dailyTargetPages: Int = 4,
    val dailyTargetMinutes: Int = 15,
    val khatmahCount: Int = 0,
    val selectedReciterId: String = "alafasy",
    val showTafsir: Boolean = true,
    val showTranslation: Boolean = false,
    val quranFontSizeSp: Int = 24,
    // Notifications & Reminders
    val prayerNotificationsEnabled: Boolean = true,
    val adhanSoundEnabled: Boolean = true,
    val morningAdhkarReminderEnabled: Boolean = true,
    val morningAdhkarHour: Int = 5,
    val morningAdhkarMinute: Int = 30,
    val eveningAdhkarReminderEnabled: Boolean = true,
    val eveningAdhkarHour: Int = 16,
    val eveningAdhkarMinute: Int = 30,
    val quranWirdReminderEnabled: Boolean = true,
    val quranWirdHour: Int = 20,
    val quranWirdMinute: Int = 0,
    val qiyamReminderEnabled: Boolean = false,
    val qiyamHour: Int = 3,
    val qiyamMinute: Int = 30,
    val duhaReminderEnabled: Boolean = false,
    val duhaHour: Int = 9,
    val duhaMinute: Int = 0,
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val SNOWFALL_ENABLED = booleanPreferencesKey("snowfall_enabled")
        val CITY_NAME = stringPreferencesKey("city_name")
        val LATITUDE = doublePreferencesKey("latitude")
        val LONGITUDE = doublePreferencesKey("longitude")
        val CALC_METHOD = stringPreferencesKey("calc_method")
        val ASR_MADHAB = stringPreferencesKey("asr_madhab")

        val LAST_SURAH = intPreferencesKey("last_surah")
        val LAST_AYAH = intPreferencesKey("last_ayah")
        val LAST_PAGE = intPreferencesKey("last_page")
        val LAST_JUZ = intPreferencesKey("last_juz")
        val WIRD_MODE = stringPreferencesKey("wird_mode")
        val DAILY_TARGET_PAGES = intPreferencesKey("daily_target_pages")
        val DAILY_TARGET_MINUTES = intPreferencesKey("daily_target_minutes")
        val KHATMAH_COUNT = intPreferencesKey("khatmah_count")
        val SELECTED_RECITER = stringPreferencesKey("selected_reciter")
        val SHOW_TAFSIR = booleanPreferencesKey("show_tafsir")
        val SHOW_TRANSLATION = booleanPreferencesKey("show_translation")
        val QURAN_FONT_SIZE = intPreferencesKey("quran_font_size")

        val PRAYER_NOTIF = booleanPreferencesKey("prayer_notif")
        val ADHAN_SOUND = booleanPreferencesKey("adhan_sound")
        val MORNING_REMINDER = booleanPreferencesKey("morning_reminder")
        val MORNING_HOUR = intPreferencesKey("morning_hour")
        val MORNING_MINUTE = intPreferencesKey("morning_minute")
        val EVENING_REMINDER = booleanPreferencesKey("evening_reminder")
        val EVENING_HOUR = intPreferencesKey("evening_hour")
        val EVENING_MINUTE = intPreferencesKey("evening_minute")
        val QURAN_REMINDER = booleanPreferencesKey("quran_reminder")
        val QURAN_HOUR = intPreferencesKey("quran_hour")
        val QURAN_MINUTE = intPreferencesKey("quran_minute")
        val QIYAM_REMINDER = booleanPreferencesKey("qiyam_reminder")
        val QIYAM_HOUR = intPreferencesKey("qiyam_hour")
        val QIYAM_MINUTE = intPreferencesKey("qiyam_minute")
        val DUHA_REMINDER = booleanPreferencesKey("duha_reminder")
        val DUHA_HOUR = intPreferencesKey("duha_hour")
        val DUHA_MINUTE = intPreferencesKey("duha_minute")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { p ->
        UserSettings(
            isDarkMode = p[Keys.IS_DARK_MODE] ?: true,
            snowfallEnabled = p[Keys.SNOWFALL_ENABLED] ?: true,
            cityName = p[Keys.CITY_NAME] ?: "مكة المكرمة",
            latitude = p[Keys.LATITUDE] ?: 21.4225,
            longitude = p[Keys.LONGITUDE] ?: 39.8262,
            calculationMethod = runCatching {
                CalculationMethod.valueOf(p[Keys.CALC_METHOD] ?: CalculationMethod.UMM_AL_QURA.name)
            }.getOrDefault(CalculationMethod.UMM_AL_QURA),
            asrMadhab = runCatching {
                AsrMadhab.valueOf(p[Keys.ASR_MADHAB] ?: AsrMadhab.SHAFI.name)
            }.getOrDefault(AsrMadhab.SHAFI),
            lastSurah = p[Keys.LAST_SURAH] ?: 1,
            lastAyah = p[Keys.LAST_AYAH] ?: 1,
            lastPage = p[Keys.LAST_PAGE] ?: 1,
            lastJuz = p[Keys.LAST_JUZ] ?: 1,
            wirdMode = runCatching {
                WirdMode.valueOf(p[Keys.WIRD_MODE] ?: WirdMode.PAGES.name)
            }.getOrDefault(WirdMode.PAGES),
            dailyTargetPages = p[Keys.DAILY_TARGET_PAGES] ?: 4,
            dailyTargetMinutes = p[Keys.DAILY_TARGET_MINUTES] ?: 15,
            khatmahCount = p[Keys.KHATMAH_COUNT] ?: 0,
            selectedReciterId = p[Keys.SELECTED_RECITER] ?: "alafasy",
            showTafsir = p[Keys.SHOW_TAFSIR] ?: true,
            showTranslation = p[Keys.SHOW_TRANSLATION] ?: false,
            quranFontSizeSp = p[Keys.QURAN_FONT_SIZE] ?: 24,
            prayerNotificationsEnabled = p[Keys.PRAYER_NOTIF] ?: true,
            adhanSoundEnabled = p[Keys.ADHAN_SOUND] ?: true,
            morningAdhkarReminderEnabled = p[Keys.MORNING_REMINDER] ?: true,
            morningAdhkarHour = p[Keys.MORNING_HOUR] ?: 5,
            morningAdhkarMinute = p[Keys.MORNING_MINUTE] ?: 30,
            eveningAdhkarReminderEnabled = p[Keys.EVENING_REMINDER] ?: true,
            eveningAdhkarHour = p[Keys.EVENING_HOUR] ?: 16,
            eveningAdhkarMinute = p[Keys.EVENING_MINUTE] ?: 30,
            quranWirdReminderEnabled = p[Keys.QURAN_REMINDER] ?: true,
            quranWirdHour = p[Keys.QURAN_HOUR] ?: 20,
            quranWirdMinute = p[Keys.QURAN_MINUTE] ?: 0,
            qiyamReminderEnabled = p[Keys.QIYAM_REMINDER] ?: false,
            qiyamHour = p[Keys.QIYAM_HOUR] ?: 3,
            qiyamMinute = p[Keys.QIYAM_MINUTE] ?: 30,
            duhaReminderEnabled = p[Keys.DUHA_REMINDER] ?: false,
            duhaHour = p[Keys.DUHA_HOUR] ?: 9,
            duhaMinute = p[Keys.DUHA_MINUTE] ?: 0,
        )
    }

    suspend fun updateTheme(isDark: Boolean) {
        context.dataStore.edit { it[Keys.IS_DARK_MODE] = isDark }
    }

    suspend fun updateSnowfall(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SNOWFALL_ENABLED] = enabled }
    }

    suspend fun updateLocation(cityName: String, lat: Double, lon: Double) {
        context.dataStore.edit {
            it[Keys.CITY_NAME] = cityName
            it[Keys.LATITUDE] = lat
            it[Keys.LONGITUDE] = lon
        }
    }

    suspend fun updateCalculationMethod(method: CalculationMethod) {
        context.dataStore.edit { it[Keys.CALC_METHOD] = method.name }
    }

    suspend fun updateAsrMadhab(madhab: AsrMadhab) {
        context.dataStore.edit { it[Keys.ASR_MADHAB] = madhab.name }
    }

    suspend fun updateBookmark(surah: Int, ayah: Int, page: Int, juz: Int) {
        context.dataStore.edit {
            it[Keys.LAST_SURAH] = surah
            it[Keys.LAST_AYAH] = ayah
            it[Keys.LAST_PAGE] = page
            it[Keys.LAST_JUZ] = juz
        }
    }

    suspend fun updateWirdTarget(mode: WirdMode, targetPages: Int, targetMinutes: Int) {
        context.dataStore.edit {
            it[Keys.WIRD_MODE] = mode.name
            it[Keys.DAILY_TARGET_PAGES] = targetPages.coerceIn(1, 100)
            it[Keys.DAILY_TARGET_MINUTES] = targetMinutes.coerceIn(5, 180)
        }
    }

    suspend fun incrementKhatmah() {
        context.dataStore.edit {
            val cur = it[Keys.KHATMAH_COUNT] ?: 0
            it[Keys.KHATMAH_COUNT] = cur + 1
        }
    }

    suspend fun updateReciter(reciterId: String) {
        context.dataStore.edit { it[Keys.SELECTED_RECITER] = reciterId }
    }

    suspend fun updateQuranDisplay(showTafsir: Boolean, showTranslation: Boolean, fontSizeSp: Int) {
        context.dataStore.edit {
            it[Keys.SHOW_TAFSIR] = showTafsir
            it[Keys.SHOW_TRANSLATION] = showTranslation
            it[Keys.QURAN_FONT_SIZE] = fontSizeSp.coerceIn(18, 36)
        }
    }

    suspend fun updatePrayerNotifications(enabled: Boolean, adhanSound: Boolean) {
        context.dataStore.edit {
            it[Keys.PRAYER_NOTIF] = enabled
            it[Keys.ADHAN_SOUND] = adhanSound
        }
    }

    suspend fun updateReminder(
        kind: String,
        enabled: Boolean,
        hour: Int,
        minute: Int,
    ) {
        context.dataStore.edit {
            when (kind) {
                "MORNING" -> {
                    it[Keys.MORNING_REMINDER] = enabled
                    it[Keys.MORNING_HOUR] = hour
                    it[Keys.MORNING_MINUTE] = minute
                }
                "EVENING" -> {
                    it[Keys.EVENING_REMINDER] = enabled
                    it[Keys.EVENING_HOUR] = hour
                    it[Keys.EVENING_MINUTE] = minute
                }
                "QURAN" -> {
                    it[Keys.QURAN_REMINDER] = enabled
                    it[Keys.QURAN_HOUR] = hour
                    it[Keys.QURAN_MINUTE] = minute
                }
                "QIYAM" -> {
                    it[Keys.QIYAM_REMINDER] = enabled
                    it[Keys.QIYAM_HOUR] = hour
                    it[Keys.QIYAM_MINUTE] = minute
                }
                "DUHA" -> {
                    it[Keys.DUHA_REMINDER] = enabled
                    it[Keys.DUHA_HOUR] = hour
                    it[Keys.DUHA_MINUTE] = minute
                }
            }
        }
    }
}
