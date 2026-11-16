package com.example.data.repository

import com.example.data.local.PrayerLogEntity
import com.example.data.local.WorshipDao
import com.example.data.model.PrayerMode
import com.example.data.model.PrayerName
import com.example.data.model.PrayerStatus
import kotlinx.coroutines.flow.Flow

/**
 * Repository abstracting Room access for tracking the five daily prayers.
 */
class PrayerRepository(
    private val dao: WorshipDao,
) {
    val allPrayerLogs: Flow<List<PrayerLogEntity>> = dao.observeAllPrayerLogs()

    fun observePrayersForDate(dateIso: String): Flow<List<PrayerLogEntity>> {
        return dao.observePrayerLogsForDate(dateIso)
    }

    suspend fun getAllPrayerLogsOnce(): List<PrayerLogEntity> {
        return dao.getAllPrayerLogsOnce()
    }

    suspend fun setPrayerStatus(
        dateIso: String,
        prayer: PrayerName,
        status: PrayerStatus,
        mode: PrayerMode,
    ): PrayerLogEntity {
        val entity = PrayerLogEntity(
            dateIso = dateIso,
            prayerName = prayer.name,
            status = status.name,
            mode = mode.name,
            isCompleted = status.isCompleted,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsertPrayerLog(entity)
        return entity
    }

    suspend fun togglePrayerCompletion(
        dateIso: String,
        prayer: PrayerName,
        defaultMode: PrayerMode = PrayerMode.SOLO,
    ): PrayerLogEntity {
        val existing = dao.getPrayerLog(dateIso, prayer.name)
        val currentlyCompleted = existing?.isCompleted == true ||
            (existing != null && runCatching { PrayerStatus.valueOf(existing.status).isCompleted }.getOrDefault(false))

        val nextStatus = if (currentlyCompleted) PrayerStatus.PENDING else PrayerStatus.ON_TIME
        val preservedMode = existing?.mode ?: defaultMode.name
        val updated = PrayerLogEntity(
            dateIso = dateIso,
            prayerName = prayer.name,
            status = nextStatus.name,
            mode = preservedMode,
            isCompleted = nextStatus.isCompleted,
            updatedAt = System.currentTimeMillis(),
        )
        dao.upsertPrayerLog(updated)
        return updated
    }

    suspend fun deletePrayerLog(dateIso: String, prayer: PrayerName) {
        dao.deletePrayerLog(dateIso, prayer.name)
    }
}
