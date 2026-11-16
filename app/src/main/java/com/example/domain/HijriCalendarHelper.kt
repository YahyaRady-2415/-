package com.example.domain

import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.floor

data class HijriDateInfo(
    val day: Int,
    val month1To12: Int,
    val year: Int,
    val monthNameArabic: String,
    val formattedArabic: String,
    val weekdayArabic: String = "",
    val fullFormattedArabic: String = formattedArabic,
    val gregorianFormattedArabic: String = "",
)

data class CalendarDayCell(
    val dateIso: String,
    val gregorianDay: Int,
    val hijriDay: Int,
    val hijriMonthName: String,
    val isToday: Boolean,
)

object HijriCalendarHelper {
    private val hijriMonths = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر",
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    private val gregorianMonths = listOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    private val arabicWeekDays = listOf(
        "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت"
    )

    fun todayIso(): String {
        val cal = Calendar.getInstance()
        return formatIso(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun formatIso(year: Int, month1To12: Int, day: Int): String {
        return String.format(Locale.US, "%04d-%02d-%02d", year, month1To12, day)
    }

    /**
     * Converts the current system date (or provided [calendar]) into a full [HijriDateInfo].
     */
    fun getCurrentHijriDate(calendar: Calendar = Calendar.getInstance()): HijriDateInfo {
        val year = calendar.get(Calendar.YEAR)
        val month1To12 = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val weekday = arabicWeekDays.getOrElse(calendar.get(Calendar.DAY_OF_WEEK) - 1) { "" }
        val base = toHijri(year, month1To12, day)
        val gMonth = gregorianMonths.getOrElse(month1To12 - 1) { "" }
        val gregorianLabel = "$day $gMonth $year م"
        val fullArabic = if (weekday.isNotEmpty()) "$weekday، ${base.formattedArabic}" else base.formattedArabic
        return base.copy(
            weekdayArabic = weekday,
            fullFormattedArabic = fullArabic,
            gregorianFormattedArabic = gregorianLabel,
        )
    }

    fun convertToHijri(calendar: Calendar): HijriDateInfo = getCurrentHijriDate(calendar)

    fun convertToHijri(date: Date): HijriDateInfo {
        val cal = Calendar.getInstance().apply { time = date }
        return getCurrentHijriDate(cal)
    }

    /**
     * Converts Gregorian date to Hijri using the Kuwaiti / Astronomical Islamic calendar algorithm.
     */
    fun toHijri(year: Int, month1To12: Int, day: Int): HijriDateInfo {
        var m = month1To12
        var y = year
        if (m < 3) {
            y -= 1
            m += 12
        }
        var a = floor(y / 100.0)
        var b = 2 - a + floor(a / 4.0)
        if (y < 1583) b = 0.0
        if (y == 1582) {
            if (m > 10) b = -10.0
            if (m == 10) {
                b = 0.0
                if (day > 4) b = -10.0
            }
        }
        val jd = floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524
        b = 0.0
        if (jd > 2299160) {
            a = floor((jd - 1867216.25) / 36524.25)
            b = 1 + a - floor(a / 4.0)
        }
        val bb = jd + b + 1524
        var cc = floor((bb - 122.1) / 365.25)
        val dd = floor(365.25 * cc)
        val ee = floor((bb - dd) / 30.6001)
        val gDay = (bb - dd) - floor(30.6001 * ee)
        var gMonth = ee - 1
        if (ee > 13) {
            cc += 1
            gMonth = ee - 13
        }
        val gYear = cc - 4716
        val wd = (((jd + 1).toLong() % 7) + 7) % 7
        val _unused = wd + gDay + gMonth + gYear

        val iyear = 10631.0 / 30.0
        val epochAstro = 1948084.0
        val shift1 = 8.01 / 60.0
        var z = jd - epochAstro
        val cyc = floor(z / 10631.0)
        z -= 10631.0 * cyc
        val j = floor((z - shift1) / iyear)
        val iy = (30 * cyc + j).toInt()
        z -= floor(j * iyear + shift1)
        var im = floor((z + 28.5001) / 29.5).toInt()
        if (im == 13) im = 12
        val id = (z - floor(29.5001 * im - 29)).toInt().coerceIn(1, 30)

        val cal = Calendar.getInstance().apply {
            set(year, month1To12 - 1, day, 12, 0, 0)
        }
        val weekday = arabicWeekDays.getOrElse(cal.get(Calendar.DAY_OF_WEEK) - 1) { "" }
        val gMonthName = gregorianMonths.getOrElse(month1To12 - 1) { "" }
        val mName = hijriMonths.getOrElse(im - 1) { "محرم" }
        val formatted = "$id $mName $iy هـ"
        val fullArabic = if (weekday.isNotEmpty()) "$weekday، $formatted" else formatted

        return HijriDateInfo(
            day = id,
            month1To12 = im,
            year = iy,
            monthNameArabic = mName,
            formattedArabic = formatted,
            weekdayArabic = weekday,
            fullFormattedArabic = fullArabic,
            gregorianFormattedArabic = "$day $gMonthName $year م",
        )
    }

    fun todayFormattedHeader(): Pair<String, String> {
        val hijri = getCurrentHijriDate()
        val gregorianWithDay = if (hijri.weekdayArabic.isNotEmpty()) {
            "${hijri.weekdayArabic} ${hijri.gregorianFormattedArabic}"
        } else {
            hijri.gregorianFormattedArabic
        }
        return hijri.formattedArabic to gregorianWithDay
    }

    fun gregorianMonthTitle(year: Int, month1To12: Int): String {
        val gMonth = gregorianMonths.getOrElse(month1To12 - 1) { "" }
        return "$gMonth $year"
    }

    fun buildMonthCells(year: Int, month1To12: Int): Pair<Int, List<CalendarDayCell>> {
        val cal = Calendar.getInstance().apply {
            set(year, month1To12 - 1, 1, 12, 0, 0)
        }
        val firstDayOffset = when (cal.get(Calendar.DAY_OF_WEEK)) {
            Calendar.SATURDAY -> 0
            Calendar.SUNDAY -> 1
            Calendar.MONDAY -> 2
            Calendar.TUESDAY -> 3
            Calendar.WEDNESDAY -> 4
            Calendar.THURSDAY -> 5
            else -> 6
        }
        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = todayIso()
        val cells = (1..maxDay).map { day ->
            val iso = formatIso(year, month1To12, day)
            val h = toHijri(year, month1To12, day)
            CalendarDayCell(
                dateIso = iso,
                gregorianDay = day,
                hijriDay = h.day,
                hijriMonthName = h.monthNameArabic,
                isToday = iso == today,
            )
        }
        return firstDayOffset to cells
    }

    fun lastNDatesIso(days: Int): List<String> {
        val cal = Calendar.getInstance()
        val result = mutableListOf<String>()
        for (i in (days - 1) downTo 0) {
            val c = cal.clone() as Calendar
            c.add(Calendar.DAY_OF_YEAR, -i)
            result.add(
                formatIso(
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH) + 1,
                    c.get(Calendar.DAY_OF_MONTH)
                )
            )
        }
        return result
    }
}
