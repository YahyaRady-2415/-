package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PrayerLogEntity::class,
        NaflLogEntity::class,
        QuranDailyLogEntity::class,
        QuranChapterProgressEntity::class,
        SurahHifzEntity::class,
        DhikrProgressEntity::class,
        HisnDhikrEntity::class,
        CustomDhikrEntity::class,
        CustomNaflRoutineEntity::class,
        CustomCategoryEntity::class,
    ],
    version = 4,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun worshipDao(): WorshipDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE prayer_logs ADD COLUMN isCompleted INTEGER NOT NULL DEFAULT 0"
                )
                db.execSQL(
                    "UPDATE prayer_logs SET isCompleted = 1 WHERE status IN ('ON_TIME', 'LATE', 'QADA')"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quran_chapter_progress` (
                        `dateIso` TEXT NOT NULL,
                        `unitType` TEXT NOT NULL,
                        `unitNumber` INTEGER NOT NULL,
                        `chapterTitle` TEXT NOT NULL,
                        `isRead` INTEGER NOT NULL,
                        `pagesEstimate` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`dateIso`, `unitType`, `unitNumber`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `hisn_adhkar` (
                        `dhikrKey` TEXT NOT NULL,
                        `chapterId` TEXT NOT NULL,
                        `chapterTitle` TEXT NOT NULL,
                        `categoryGroup` TEXT NOT NULL,
                        `text` TEXT NOT NULL,
                        `virtue` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `repeatCount` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isFavorite` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`dhikrKey`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "ALTER TABLE custom_adhkar ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tariq_al_jannah.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
