package com.example.data.model

enum class PrayerName(val arabicName: String, val orderIndex: Int) {
    FAJR("الفجر", 0),
    DHUHR("الظهر", 1),
    ASR("العصر", 2),
    MAGHRIB("المغرب", 3),
    ISHA("العشاء", 4);
}

enum class PrayerStatus(val arabicLabel: String, val isCompleted: Boolean) {
    PENDING("لم تُسجّل", false),
    ON_TIME("في وقتها", true),
    LATE("تأخرت", true),
    MISSED("فاتت", false),
    QADA("قضاء", true);
}

enum class PrayerMode(val arabicLabel: String) {
    SOLO("منفرداً"),
    CONGREGATION("جماعة");
}

enum class CalculationMethod(
    val arabicName: String,
    val fajrAngle: Double,
    val ishaAngle: Double,
    val ishaMinutesAfterMaghrib: Int? = null,
) {
    UMM_AL_QURA("أم القرى (مكة المكرمة)", 18.5, 0.0, 90),
    EGYPTIAN("الهيئة المصرية العامة للمساحة", 19.5, 17.5, null),
    MWL("رابطة العالم الإسلامي", 18.0, 17.0, null),
    ISNA("الجمعية الإسلامية لأمريكا الشمالية", 15.0, 15.0, null),
    KARACHI("جامعة العلوم الإسلامية بكراتشي", 18.0, 18.0, null);
}

enum class AsrMadhab(val arabicName: String, val shadowFactor: Int) {
    SHAFI("الجمهور (الشافعي والمالكي والحنبلي)", 1),
    HANAFI("المذهب الحنفي", 2);
}

enum class NaflType(
    val key: String,
    val arabicTitle: String,
    val defaultRakaat: Int,
    val categoryLabel: String,
) {
    SUNNAH_FAJR("sunnah_fajr", "سنة الفجر (قبلية)", 2, "السنن الرواتب"),
    SUNNAH_DHUHR_BEFORE("sunnah_dhuhr_before", "سنة الظهر القبلية", 4, "السنن الرواتب"),
    SUNNAH_DHUHR_AFTER("sunnah_dhuhr_after", "سنة الظهر البعدية", 2, "السنن الرواتب"),
    SUNNAH_MAGHRIB_AFTER("sunnah_maghrib_after", "سنة المغرب البعدية", 2, "السنن الرواتب"),
    SUNNAH_ISHA_AFTER("sunnah_isha_after", "سنة العشاء البعدية", 2, "السنن الرواتب"),
    DUHA("duha", "صلاة الضحى", 4, "نوافل اليوم والليلة"),
    QIYAM("qiyam", "قيام الليل / التهجد", 4, "نوافل اليوم والليلة"),
    WITR("witr", "صلاة الوتر", 3, "نوافل اليوم والليلة"),
    TAHIYYAT_MASJID("tahiyyat_masjid", "تحية المسجد", 2, "نوافل اختيارية"),
    SUNNAH_WUDU("sunnah_wudu", "سنة الوضوء", 2, "نوافل اختيارية"),
    ISTIKHARA("istikhara", "صلاة الاستخارة", 2, "نوافل اختيارية");
}

data class CityPreset(
    val arabicName: String,
    val latitude: Double,
    val longitude: Double,
    val method: CalculationMethod,
)

object CityPresets {
    val list = listOf(
        CityPreset("مكة المكرمة", 21.4225, 39.8262, CalculationMethod.UMM_AL_QURA),
        CityPreset("المدينة المنورة", 24.4672, 39.6111, CalculationMethod.UMM_AL_QURA),
        CityPreset("الرياض", 24.7136, 46.6753, CalculationMethod.UMM_AL_QURA),
        CityPreset("القاهرة", 30.0444, 31.2357, CalculationMethod.EGYPTIAN),
        CityPreset("دبي", 25.2048, 55.2708, CalculationMethod.UMM_AL_QURA),
        CityPreset("إسطنبول", 41.0082, 28.9784, CalculationMethod.MWL),
        CityPreset("هلسنكي", 60.1699, 24.9384, CalculationMethod.MWL),
        CityPreset("لندن", 51.5072, -0.1276, CalculationMethod.MWL),
    )
}

data class PrayerTimesDay(
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val fajrMinutes: Int,
    val dhuhrMinutes: Int,
    val asrMinutes: Int,
    val maghribMinutes: Int,
    val ishaMinutes: Int,
) {
    fun timeFor(prayer: PrayerName): String = when (prayer) {
        PrayerName.FAJR -> fajr
        PrayerName.DHUHR -> dhuhr
        PrayerName.ASR -> asr
        PrayerName.MAGHRIB -> maghrib
        PrayerName.ISHA -> isha
    }

    fun minutesFor(prayer: PrayerName): Int = when (prayer) {
        PrayerName.FAJR -> fajrMinutes
        PrayerName.DHUHR -> dhuhrMinutes
        PrayerName.ASR -> asrMinutes
        PrayerName.MAGHRIB -> maghribMinutes
        PrayerName.ISHA -> ishaMinutes
    }
}
