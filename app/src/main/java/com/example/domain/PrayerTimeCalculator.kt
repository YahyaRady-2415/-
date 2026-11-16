package com.example.domain

import com.example.data.model.AsrMadhab
import com.example.data.model.CalculationMethod
import com.example.data.model.PrayerTimesDay
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

object PrayerTimeCalculator {
    private const val KAABA_LAT = 21.422487
    private const val KAABA_LON = 39.826206

    fun calculateForDate(
        year: Int,
        month1To12: Int,
        day: Int,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        madhab: AsrMadhab,
        timeZoneOffsetHours: Double = defaultTimeZoneHours(longitude),
    ): PrayerTimesDay {
        val jd = julianDate(year, month1To12, day) - longitude / (15.0 * 24.0)
        val decl = sunDeclination(jd)
        val eqTime = equationOfTime(jd)

        val dhuhrHours = fixHour(12.0 + timeZoneOffsetHours - longitude / 15.0 - eqTime)
        val sunriseAngle = 0.833
        val sunriseDiff = hourAngle(latitude, decl, sunriseAngle)
        val sunriseHours = dhuhrHours - sunriseDiff
        val sunsetHours = dhuhrHours + sunriseDiff

        val fajrDiff = hourAngle(latitude, decl, method.fajrAngle)
        val fajrHours = dhuhrHours - fajrDiff

        val asrHours = dhuhrHours + asrHourAngle(latitude, decl, madhab.shadowFactor)

        val ishaHours = if (method.ishaMinutesAfterMaghrib != null) {
            sunsetHours + method.ishaMinutesAfterMaghrib / 60.0
        } else {
            dhuhrHours + hourAngle(latitude, decl, method.ishaAngle)
        }

        val fajrMin = toMinutes(fajrHours)
        val dhuhrMin = toMinutes(dhuhrHours)
        val asrMin = toMinutes(asrHours)
        val maghribMin = toMinutes(sunsetHours)
        val ishaMin = toMinutes(ishaHours)

        return PrayerTimesDay(
            fajr = formatHours(fajrHours),
            sunrise = formatHours(sunriseHours),
            dhuhr = formatHours(dhuhrHours),
            asr = formatHours(asrHours),
            maghrib = formatHours(sunsetHours),
            isha = formatHours(ishaHours),
            fajrMinutes = fajrMin,
            dhuhrMinutes = dhuhrMin,
            asrMinutes = asrMin,
            maghribMinutes = maghribMin,
            ishaMinutes = ishaMin,
        )
    }

    /**
     * Computes Qibla bearing in degrees clockwise from True North (0..360).
     */
    fun calculateQiblaBearing(latitude: Double, longitude: Double): Double {
        val phiK = Math.toRadians(KAABA_LAT)
        val lambdaK = Math.toRadians(KAABA_LON)
        val phi = Math.toRadians(latitude)
        val lambda = Math.toRadians(longitude)
        val dLambda = lambdaK - lambda
        val y = sin(dLambda)
        val x = cos(phi) * tan(phiK) - sin(phi) * cos(dLambda)
        val bearing = Math.toDegrees(atan2(y, x))
        return (bearing + 360.0) % 360.0
    }

    /**
     * Haversine distance to the Kaaba in kilometers.
     */
    fun distanceToKaabaKm(latitude: Double, longitude: Double): Int {
        val r = 6371.0
        val dLat = Math.toRadians(KAABA_LAT - latitude)
        val dLon = Math.toRadians(KAABA_LON - longitude)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(latitude)) * cos(Math.toRadians(KAABA_LAT)) *
            sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return (r * c).toInt()
    }

    private fun defaultTimeZoneHours(longitude: Double): Double {
        val cal = Calendar.getInstance()
        val tz = TimeZone.getDefault()
        val systemOffset = tz.getOffset(cal.timeInMillis) / 3_600_000.0
        // If user selected a preset far from device timezone, estimate solar zone offset
        val estimated = kotlin.math.round(longitude / 15.0)
        return if (abs(estimated - systemOffset) <= 1.5) systemOffset else estimated
    }

    private fun julianDate(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    private fun sunDeclination(jd: Double): Double {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        return darcsin(dsin(e) * dsin(l))
    }

    private fun equationOfTime(jd: Double): Double {
        val d = jd - 2451545.0
        val g = fixAngle(357.529 + 0.98560028 * d)
        val q = fixAngle(280.459 + 0.98564736 * d)
        val l = fixAngle(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g))
        val e = 23.439 - 0.00000036 * d
        val ra = darctan2(dcos(e) * dsin(l), dcos(l)) / 15.0
        return (q / 15.0) - fixHour(ra)
    }

    private fun hourAngle(lat: Double, decl: Double, angle: Double): Double {
        val numerator = -dsin(angle) - dsin(lat) * dsin(decl)
        val denominator = dcos(lat) * dcos(decl)
        val ratio = (numerator / denominator).coerceIn(-0.999, 0.999)
        return darccos(ratio) / 15.0
    }

    private fun asrHourAngle(lat: Double, decl: Double, shadowFactor: Int): Double {
        val angle = -darccot(shadowFactor + dtan(abs(lat - decl)))
        return hourAngle(lat, decl, angle)
    }

    private fun toMinutes(hours: Double): Int {
        val fixed = fixHour(hours + 0.5 / 60.0)
        val h = floor(fixed).toInt()
        val m = floor((fixed - h) * 60.0).toInt()
        return h * 60 + m
    }

    private fun formatHours(hours: Double): String {
        val totalMin = toMinutes(hours)
        val h = (totalMin / 60) % 24
        val m = totalMin % 60
        return String.format("%02d:%02d", h, m)
    }

    private fun dsin(d: Double) = sin(Math.toRadians(d))
    private fun dcos(d: Double) = cos(Math.toRadians(d))
    private fun dtan(d: Double) = tan(Math.toRadians(d))
    private fun darcsin(x: Double) = Math.toDegrees(asin(x.coerceIn(-1.0, 1.0)))
    private fun darccos(x: Double) = Math.toDegrees(acos(x.coerceIn(-1.0, 1.0)))
    private fun darctan2(y: Double, x: Double) = Math.toDegrees(atan2(y, x))
    private fun darccot(x: Double) = Math.toDegrees(atan(1.0 / x))
    private fun fixAngle(a: Double): Double = a - 360.0 * floor(a / 360.0)
    private fun fixHour(a: Double): Double = a - 24.0 * floor(a / 24.0)
}
