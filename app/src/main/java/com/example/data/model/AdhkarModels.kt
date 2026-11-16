package com.example.data.model

enum class RoutineFrequency(val arabicLabel: String) {
    DAILY("يومياً"),
    WEEKLY("أسبوعياً"),
    SPECIFIC_DAYS("أيام محددة");
}

enum class RoutineTimeOfDay(val arabicLabel: String) {
    MORNING("الصباح"),
    DUHA("وقت الضحى"),
    AFTER_PRAYER("بعد الصلاة"),
    EVENING("المساء"),
    BEFORE_SLEEP("قبل النوم"),
    QIYAM("قيام الليل"),
    ANYTIME("أي وقت");
}

data class HisnDhikr(
    val key: String,
    val text: String,
    val virtue: String,
    val source: String,
    val repeatCount: Int,
    val isFavorite: Boolean = false,
)

data class HisnChapter(
    val id: String,
    val title: String,
    val categoryGroup: String,
    val items: List<HisnDhikr>,
)

data class DayOption(
    val bitMask: Int,
    val shortArabic: String,
    val fullArabic: String,
)

object WeekDays {
    val all = listOf(
        DayOption(1 shl 0, "سبت", "السبت"),
        DayOption(1 shl 1, "أحد", "الأحد"),
        DayOption(1 shl 2, "إثن", "الإثنين"),
        DayOption(1 shl 3, "ثلا", "الثلاثاء"),
        DayOption(1 shl 4, "أرب", "الأربعاء"),
        DayOption(1 shl 5, "خمي", "الخميس"),
        DayOption(1 shl 6, "جمع", "الجمعة"),
    )
}
