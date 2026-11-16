package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import androidx.work.workDataOf
import com.example.data.local.AppDatabase
import com.example.data.local.UserSettings
import com.example.data.model.PrayerName
import com.example.data.model.PrayerStatus
import com.example.data.model.QuranReadingUnit
import com.example.data.repository.HisnAlMuslimRepository
import com.example.data.repository.PrayerRepository
import com.example.data.repository.QuranReadingProgressRepository
import com.example.data.static.QuranRepository
import com.example.notifications.PrayerNotificationWorker
import com.example.notifications.PrayerWorkScheduler
import com.example.ui.viewmodel.PrayerTrackerViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun readAppNameAndOfflineQuran() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("طريق الجنة", appName)

        val surahs = QuranRepository(context).getAllSurahs()
        assertEquals(114, surahs.size)
        assertEquals(6236, surahs.sumOf { it.ayahs.size })
    }

    @Test
    fun roomPrayerDatabaseAndViewModel_toggleFiveDailyPrayers() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = PrayerRepository(db.worshipDao())
            val dateIso = "2026-11-16"
            val vm = PrayerTrackerViewModel(repository = repo, initialDateIso = dateIso)

            // Toggle all 5 daily prayers on
            PrayerName.entries.forEach { prayer ->
                val toggled = repo.togglePrayerCompletion(dateIso, prayer)
                assertTrue(toggled.isCompleted)
                assertEquals(PrayerStatus.ON_TIME.name, toggled.status)
            }

            val savedLogs = repo.observePrayersForDate(dateIso).first()
            assertEquals(5, savedLogs.size)
            assertTrue(savedLogs.all { it.isCompleted })

            // Toggle Fajr off and verify
            val fajrUntoggled = repo.togglePrayerCompletion(dateIso, PrayerName.FAJR)
            assertFalse(fajrUntoggled.isCompleted)
            assertEquals(PrayerStatus.PENDING.name, fajrUntoggled.status)

            val afterUntoggle = repo.observePrayersForDate(dateIso).first()
            assertEquals(4, afterUntoggle.count { it.isCompleted })
            assertEquals(5, vm.uiState.value.totalCount)
        } finally {
            db.close()
        }
    }

    @Test
    fun roomQuranChapterProgress_tracksBySurahAndJuz() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = QuranReadingProgressRepository(db.worshipDao())
            val dateIso = "2026-11-16"

            val surahMarked = repo.toggleChapterRead(
                dateIso = dateIso,
                unit = QuranReadingUnit.SURAH,
                unitNumber = 18,
                chapterTitle = "الكهف",
                pagesEstimate = 12,
            )
            assertTrue(surahMarked.isRead)

            val juzMarked = repo.toggleChapterRead(
                dateIso = dateIso,
                unit = QuranReadingUnit.JUZ,
                unitNumber = 30,
                chapterTitle = "الجزء الثلاثون (عمّ يتساءلون)",
                pagesEstimate = 20,
            )
            assertTrue(juzMarked.isRead)

            val progressToday = repo.observeProgressForDate(dateIso).first()
            assertEquals(2, progressToday.size)
            assertEquals(1, progressToday.count { it.unit == QuranReadingUnit.SURAH && it.isRead })
            assertEquals(1, progressToday.count { it.unit == QuranReadingUnit.JUZ && it.isRead })

            // Toggle Surah back to unread
            val surahUnmarked = repo.toggleChapterRead(
                dateIso = dateIso,
                unit = QuranReadingUnit.SURAH,
                unitNumber = 18,
                chapterTitle = "الكهف",
                pagesEstimate = 12,
            )
            assertFalse(surahUnmarked.isRead)
        } finally {
            db.close()
        }
    }

    @Test
    fun roomHisnAlMuslim_categoriesAndFavoritesPersist() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val repo = HisnAlMuslimRepository(db.worshipDao())
            repo.seedIfNeeded()

            val chapters = repo.groupedChapters.first()
            assertTrue(chapters.any { it.categoryGroup == "أذكار الصباح" })
            assertTrue(chapters.any { it.categoryGroup == "أذكار المساء" })
            assertTrue(chapters.any { it.categoryGroup == "أذكار الصلاة" })

            // Toggle favorite on a Morning dhikr
            val added = repo.toggleFavorite("morn_kursi")
            assertTrue(added)

            val favorites = repo.favoriteHisnAdhkar.first()
            assertEquals(1, favorites.size)
            assertEquals("morn_kursi", favorites.first().dhikrKey)

            // Toggle off
            val removed = repo.toggleFavorite("morn_kursi")
            assertFalse(removed)
            assertTrue(repo.favoriteHisnAdhkar.first().isEmpty())
        } finally {
            db.close()
        }
    }

    @Test
    fun workManager_schedulesAndExecutesLocationBasedPrayerNotifications() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        WorkManagerTestInitHelper.initializeTestWorkManager(context)
        val wm = WorkManager.getInstance(context)

        val settings = UserSettings(
            cityName = "مكة المكرمة",
            latitude = 21.4225,
            longitude = 39.8262,
            prayerNotificationsEnabled = true,
        )
        PrayerWorkScheduler.scheduleAllPrayers(context, settings)

        val scheduledInfos = wm.getWorkInfosByTag(PrayerWorkScheduler.TAG_PRAYER_WORK).get()
        // 5 unique OneTimeWorkRequests + 1 DailyPrayerWorkSyncWorker PeriodicWorkRequest
        assertEquals(6, scheduledInfos.size)
        PrayerName.entries.forEach { prayer ->
            val infos = wm.getWorkInfosForUniqueWork(PrayerWorkScheduler.uniqueWorkNameFor(prayer)).get()
            assertEquals(1, infos.size)
            assertEquals(WorkInfo.State.ENQUEUED, infos.first().state)
        }

        // Execute PrayerNotificationWorker directly
        val worker = TestListenableWorkerBuilder<PrayerNotificationWorker>(context)
            .setInputData(workDataOf(PrayerNotificationWorker.KEY_PRAYER_NAME to PrayerName.FAJR.name))
            .build()
        val result = worker.doWork()
        assertEquals(ListenableWorker.Result.success(), result)

        // Cancel all prayer notifications
        PrayerWorkScheduler.cancelAllPrayers(context)
        val afterCancel = wm.getWorkInfosByTag(PrayerWorkScheduler.TAG_PRAYER_WORK).get()
        assertTrue(afterCancel.all { it.state == WorkInfo.State.CANCELLED })
    }
}
