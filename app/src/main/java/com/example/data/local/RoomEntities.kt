package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "prayer_logs", primaryKeys = ["dateIso", "prayerName"])
data class PrayerLogEntity(
    val dateIso: String,
    val prayerName: String,
    val status: String,
    val mode: String,
    val isCompleted: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "nafl_logs", primaryKeys = ["dateIso", "naflKey"])
data class NaflLogEntity(
    val dateIso: String,
    val naflKey: String,
    val title: String,
    val rakaat: Int,
    val completed: Boolean,
    val categoryName: String,
    val isCustom: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "quran_daily_logs")
data class QuranDailyLogEntity(
    @PrimaryKey val dateIso: String,
    val pagesRead: Int,
    val minutesRead: Int,
    val hifzAyahsAdded: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "quran_chapter_progress",
    primaryKeys = ["dateIso", "unitType", "unitNumber"],
)
data class QuranChapterProgressEntity(
    val dateIso: String,
    val unitType: String, // "SURAH" or "JUZ"
    val unitNumber: Int,  // 1..114 for Surah, 1..30 for Juz
    val chapterTitle: String,
    val isRead: Boolean,
    val pagesEstimate: Int = 0,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "surah_hifz")
data class SurahHifzEntity(
    @PrimaryKey val surahNumber: Int,
    val memorizedAyahs: Int,
    val totalAyahs: Int,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "dhikr_progress", primaryKeys = ["dateIso", "dhikrKey"])
data class DhikrProgressEntity(
    val dateIso: String,
    val dhikrKey: String,
    val currentCount: Int,
    val targetCount: Int,
    val completed: Boolean,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "hisn_adhkar")
data class HisnDhikrEntity(
    @PrimaryKey val dhikrKey: String,
    val chapterId: String,
    val chapterTitle: String,
    val categoryGroup: String,
    val text: String,
    val virtue: String,
    val source: String,
    val repeatCount: Int,
    val sortOrder: Int = 0,
    val isFavorite: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "custom_adhkar")
data class CustomDhikrEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val phraseText: String,
    val virtue: String,
    val targetCount: Int,
    val categoryName: String,
    val frequency: String,
    val daysMask: Int,
    val timeOfDay: String,
    val isFavorite: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "custom_nafl_routines")
data class CustomNaflRoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val rakaatCount: Int,
    val categoryName: String,
    val frequency: String,
    val daysMask: Int,
    val timeOfDay: String,
    val notes: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(tableName = "custom_categories")
data class CustomCategoryEntity(
    @PrimaryKey val name: String,
    val kind: String, // "ADHKAR" or "NAWAFIL"
    val createdAt: Long = System.currentTimeMillis(),
)
