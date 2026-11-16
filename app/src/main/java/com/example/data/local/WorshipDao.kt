package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface WorshipDao {
    // Prayers
    @Query("SELECT * FROM prayer_logs ORDER BY dateIso DESC")
    fun observeAllPrayerLogs(): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_logs WHERE dateIso = :dateIso")
    fun observePrayerLogsForDate(dateIso: String): Flow<List<PrayerLogEntity>>

    @Query("SELECT * FROM prayer_logs WHERE dateIso = :dateIso AND prayerName = :prayerName LIMIT 1")
    suspend fun getPrayerLog(dateIso: String, prayerName: String): PrayerLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPrayerLog(entity: PrayerLogEntity)

    @Update
    suspend fun updatePrayerLog(entity: PrayerLogEntity)

    @Query("DELETE FROM prayer_logs WHERE dateIso = :dateIso AND prayerName = :prayerName")
    suspend fun deletePrayerLog(dateIso: String, prayerName: String)

    // Nawafil
    @Query("SELECT * FROM nafl_logs ORDER BY dateIso DESC")
    fun observeAllNaflLogs(): Flow<List<NaflLogEntity>>

    @Query("SELECT * FROM nafl_logs WHERE dateIso = :dateIso")
    fun observeNaflLogsForDate(dateIso: String): Flow<List<NaflLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNaflLog(entity: NaflLogEntity)

    // Quran Daily Logs & Hifz
    @Query("SELECT * FROM quran_daily_logs ORDER BY dateIso DESC")
    fun observeAllQuranDailyLogs(): Flow<List<QuranDailyLogEntity>>

    @Query("SELECT * FROM quran_daily_logs WHERE dateIso = :dateIso LIMIT 1")
    fun observeQuranLogForDate(dateIso: String): Flow<QuranDailyLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuranDailyLog(entity: QuranDailyLogEntity)

    @Query("SELECT * FROM surah_hifz ORDER BY surahNumber ASC")
    fun observeAllSurahHifz(): Flow<List<SurahHifzEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSurahHifz(entity: SurahHifzEntity)

    // Quran Reading Progress by Surah or Juz
    @Query("SELECT * FROM quran_chapter_progress WHERE dateIso = :dateIso ORDER BY unitType ASC, unitNumber ASC")
    fun observeQuranChapterProgressForDate(dateIso: String): Flow<List<QuranChapterProgressEntity>>

    @Query("SELECT * FROM quran_chapter_progress ORDER BY dateIso DESC, unitNumber ASC")
    fun observeAllQuranChapterProgress(): Flow<List<QuranChapterProgressEntity>>

    @Query("SELECT * FROM quran_chapter_progress WHERE dateIso = :dateIso AND unitType = :unitType AND unitNumber = :unitNumber LIMIT 1")
    suspend fun getQuranChapterProgress(
        dateIso: String,
        unitType: String,
        unitNumber: Int,
    ): QuranChapterProgressEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertQuranChapterProgress(entity: QuranChapterProgressEntity)

    @Query("DELETE FROM quran_chapter_progress WHERE dateIso = :dateIso AND unitType = :unitType AND unitNumber = :unitNumber")
    suspend fun deleteQuranChapterProgress(
        dateIso: String,
        unitType: String,
        unitNumber: Int,
    )

    @Query("SELECT * FROM quran_chapter_progress")
    suspend fun getAllQuranChapterProgressOnce(): List<QuranChapterProgressEntity>

    // Adhkar progress
    @Query("SELECT * FROM dhikr_progress WHERE dateIso = :dateIso")
    fun observeDhikrProgressForDate(dateIso: String): Flow<List<DhikrProgressEntity>>

    @Query("SELECT * FROM dhikr_progress ORDER BY dateIso DESC")
    fun observeAllDhikrProgress(): Flow<List<DhikrProgressEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDhikrProgress(entity: DhikrProgressEntity)

    @Query("DELETE FROM dhikr_progress WHERE dateIso = :dateIso AND dhikrKey = :dhikrKey")
    suspend fun resetDhikrProgress(dateIso: String, dhikrKey: String)

    // Hisn al-Muslim Adhkar & Favorites
    @Query("SELECT * FROM hisn_adhkar ORDER BY sortOrder ASC")
    fun observeAllHisnAdhkar(): Flow<List<HisnDhikrEntity>>

    @Query("SELECT * FROM hisn_adhkar WHERE isFavorite = 1 ORDER BY sortOrder ASC")
    fun observeFavoriteHisnAdhkar(): Flow<List<HisnDhikrEntity>>

    @Query("SELECT * FROM hisn_adhkar WHERE categoryGroup = :categoryGroup ORDER BY sortOrder ASC")
    fun observeHisnAdhkarByCategory(categoryGroup: String): Flow<List<HisnDhikrEntity>>

    @Query("SELECT * FROM hisn_adhkar WHERE dhikrKey = :dhikrKey LIMIT 1")
    suspend fun getHisnDhikr(dhikrKey: String): HisnDhikrEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertHisnAdhkarIfMissing(items: List<HisnDhikrEntity>)

    @Query("UPDATE hisn_adhkar SET isFavorite = :isFavorite, updatedAt = :updatedAt WHERE dhikrKey = :dhikrKey")
    suspend fun updateHisnDhikrFavorite(
        dhikrKey: String,
        isFavorite: Boolean,
        updatedAt: Long = System.currentTimeMillis(),
    )

    // Custom Adhkar
    @Query("SELECT * FROM custom_adhkar ORDER BY createdAt DESC")
    fun observeCustomAdhkar(): Flow<List<CustomDhikrEntity>>

    @Query("SELECT * FROM custom_adhkar WHERE id = :id LIMIT 1")
    suspend fun getCustomDhikrById(id: Long): CustomDhikrEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomDhikr(entity: CustomDhikrEntity)

    @Query("UPDATE custom_adhkar SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateCustomDhikrFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM custom_adhkar WHERE id = :id")
    suspend fun deleteCustomDhikr(id: Long)

    // Custom Nafl Routines
    @Query("SELECT * FROM custom_nafl_routines ORDER BY createdAt DESC")
    fun observeCustomNaflRoutines(): Flow<List<CustomNaflRoutineEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomNaflRoutine(entity: CustomNaflRoutineEntity)

    @Query("DELETE FROM custom_nafl_routines WHERE id = :id")
    suspend fun deleteCustomNaflRoutine(id: Long)

    // Custom Categories
    @Query("SELECT * FROM custom_categories ORDER BY createdAt ASC")
    fun observeCustomCategories(): Flow<List<CustomCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomCategory(entity: CustomCategoryEntity)

    // Snapshot export helpers
    @Query("SELECT * FROM prayer_logs")
    suspend fun getAllPrayerLogsOnce(): List<PrayerLogEntity>

    @Query("SELECT * FROM nafl_logs")
    suspend fun getAllNaflLogsOnce(): List<NaflLogEntity>

    @Query("SELECT * FROM quran_daily_logs")
    suspend fun getAllQuranDailyLogsOnce(): List<QuranDailyLogEntity>

    @Query("SELECT * FROM custom_adhkar")
    suspend fun getAllCustomAdhkarOnce(): List<CustomDhikrEntity>

    @Query("SELECT * FROM custom_nafl_routines")
    suspend fun getAllCustomNaflRoutinesOnce(): List<CustomNaflRoutineEntity>
}
