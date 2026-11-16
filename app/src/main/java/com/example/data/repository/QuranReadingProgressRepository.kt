package com.example.data.repository

import com.example.data.local.QuranChapterProgressEntity
import com.example.data.local.WorshipDao
import com.example.data.model.QuranChapterProgress
import com.example.data.model.QuranReadingUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Repository abstracting Room access for tracking daily Quran reading progress by Surah or Juz.
 */
class QuranReadingProgressRepository(
    private val dao: WorshipDao,
) {
    val allChapterProgress: Flow<List<QuranChapterProgress>> =
        dao.observeAllQuranChapterProgress().map { list -> list.map { it.toDomain() } }

    fun observeProgressForDate(dateIso: String): Flow<List<QuranChapterProgress>> =
        dao.observeQuranChapterProgressForDate(dateIso).map { list -> list.map { it.toDomain() } }

    suspend fun toggleChapterRead(
        dateIso: String,
        unit: QuranReadingUnit,
        unitNumber: Int,
        chapterTitle: String,
        pagesEstimate: Int = 0,
    ): QuranChapterProgress {
        val existing = dao.getQuranChapterProgress(dateIso, unit.name, unitNumber)
        val nextRead = !(existing?.isRead ?: false)
        val updated = QuranChapterProgressEntity(
            dateIso = dateIso,
            unitType = unit.name,
            unitNumber = unitNumber,
            chapterTitle = chapterTitle,
            isRead = nextRead,
            pagesEstimate = pagesEstimate,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsertQuranChapterProgress(updated)
        return updated.toDomain()
    }

    suspend fun setChapterRead(
        dateIso: String,
        unit: QuranReadingUnit,
        unitNumber: Int,
        chapterTitle: String,
        isRead: Boolean,
        pagesEstimate: Int = 0,
    ): QuranChapterProgress {
        val updated = QuranChapterProgressEntity(
            dateIso = dateIso,
            unitType = unit.name,
            unitNumber = unitNumber,
            chapterTitle = chapterTitle,
            isRead = isRead,
            pagesEstimate = pagesEstimate,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsertQuranChapterProgress(updated)
        return updated.toDomain()
    }

    private fun QuranChapterProgressEntity.toDomain(): QuranChapterProgress {
        val unit = runCatching { QuranReadingUnit.valueOf(unitType) }
            .getOrDefault(QuranReadingUnit.SURAH)
        return QuranChapterProgress(
            dateIso = dateIso,
            unit = unit,
            unitNumber = unitNumber,
            title = chapterTitle,
            isRead = isRead,
            pagesEstimate = pagesEstimate,
            updatedAt = updatedAt,
        )
    }
}
