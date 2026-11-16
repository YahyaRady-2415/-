package com.example

import com.example.data.model.AsrMadhab
import com.example.data.model.CalculationMethod
import com.example.data.static.HisnAlMuslimExtraData
import com.example.data.static.QuranRecitersData
import com.example.domain.HijriCalendarHelper
import com.example.domain.PrayerTimeCalculator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun prayerTimeCalculator_makkahAndQiblaBearing_areValid() {
        val times = PrayerTimeCalculator.calculateForDate(
            year = 2026,
            month1To12 = 11,
            day = 16,
            latitude = 21.4225,
            longitude = 39.8262,
            method = CalculationMethod.UMM_AL_QURA,
            madhab = AsrMadhab.SHAFI,
            timeZoneOffsetHours = 3.0,
        )
        assertTrue(times.fajrMinutes < times.dhuhrMinutes)
        assertTrue(times.dhuhrMinutes < times.asrMinutes)
        assertTrue(times.asrMinutes < times.maghribMinutes)
        assertTrue(times.maghribMinutes < times.ishaMinutes)

        // Qibla from Helsinki (~60.17, 24.94) should point SSE (~158 deg)
        val bearing = PrayerTimeCalculator.calculateQiblaBearing(60.1699, 24.9384)
        assertTrue(bearing in 150.0..168.0)
        val distanceKm = PrayerTimeCalculator.distanceToKaabaKm(60.1699, 24.9384)
        assertTrue(distanceKm in 4200..4700)
    }

    @Test
    fun hijriCalendar_convertsAndBuildsMonthGrid() {
        val info = HijriCalendarHelper.toHijri(2026, 11, 16)
        assertTrue(info.year >= 1447)
        assertTrue(info.day in 1..30)
        assertTrue(info.weekdayArabic.isNotEmpty())
        assertTrue(info.fullFormattedArabic.contains(info.monthNameArabic))

        val currentHijri = HijriCalendarHelper.getCurrentHijriDate()
        assertTrue(currentHijri.year >= 1445)
        assertTrue(currentHijri.monthNameArabic.isNotEmpty())
        assertTrue(currentHijri.formattedArabic.endsWith("هـ"))
        assertTrue(currentHijri.gregorianFormattedArabic.endsWith("م"))

        val (_, cells) = HijriCalendarHelper.buildMonthCells(2026, 11)
        assertEquals(30, cells.size)
    }

    @Test
    fun hisnAlMuslim_and_reciters_arePopulated() {
        assertTrue(HisnAlMuslimExtraData.allChapters.size >= 15)
        assertEquals(10, QuranRecitersData.reciters.size)
        assertTrue(QuranRecitersData.reciters.all { it.narration == "حفص عن عاصم" })
        assertTrue(QuranRecitersData.reciters.first().surahAudioUrl(1).endsWith("001.mp3"))
    }
}
