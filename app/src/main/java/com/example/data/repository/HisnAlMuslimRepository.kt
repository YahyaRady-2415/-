package com.example.data.repository

import com.example.data.local.HisnDhikrEntity
import com.example.data.local.WorshipDao
import com.example.data.model.HisnChapter
import com.example.data.model.HisnDhikr
import com.example.data.static.HisnAlMuslimExtraData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository abstracting Room persistence for categorized Hisn al-Muslim adhkar and user favorites.
 */
class HisnAlMuslimRepository(
    private val dao: WorshipDao,
) {
    val allHisnAdhkar: Flow<List<HisnDhikrEntity>> = dao.observeAllHisnAdhkar()

    val favoriteHisnAdhkar: Flow<List<HisnDhikrEntity>> = dao.observeFavoriteHisnAdhkar()

    val groupedChapters: Flow<List<HisnChapter>> = dao.observeAllHisnAdhkar().map { entities ->
        if (entities.isEmpty()) {
            HisnAlMuslimExtraData.allChapters
        } else {
            entities
                .groupBy { it.chapterId }
                .values
                .map { chapterEntities ->
                    val first = chapterEntities.first()
                    HisnChapter(
                        id = first.chapterId,
                        title = first.chapterTitle,
                        categoryGroup = first.categoryGroup,
                        items = chapterEntities.map { e ->
                            HisnDhikr(
                                key = e.dhikrKey,
                                text = e.text,
                                virtue = e.virtue,
                                source = e.source,
                                repeatCount = e.repeatCount,
                                isFavorite = e.isFavorite,
                            )
                        },
                    )
                }
        }
    }

    fun observeByCategory(categoryGroup: String): Flow<List<HisnDhikrEntity>> =
        dao.observeHisnAdhkarByCategory(categoryGroup)

    suspend fun seedIfNeeded() = withContext(Dispatchers.IO) {
        var order = 0
        val entities = HisnAlMuslimExtraData.allChapters.flatMap { chapter ->
            chapter.items.map { item ->
                HisnDhikrEntity(
                    dhikrKey = item.key,
                    chapterId = chapter.id,
                    chapterTitle = chapter.title,
                    categoryGroup = chapter.categoryGroup,
                    text = item.text,
                    virtue = item.virtue,
                    source = item.source,
                    repeatCount = item.repeatCount,
                    sortOrder = order++,
                    isFavorite = item.isFavorite,
                )
            }
        }
        dao.insertHisnAdhkarIfMissing(entities)
    }

    suspend fun toggleFavorite(dhikrKey: String): Boolean {
        val customId = dhikrKey.removePrefix("custom_dhikr_").toLongOrNull()
        if (customId != null && dhikrKey.startsWith("custom_dhikr_")) {
            val custom = dao.getCustomDhikrById(customId) ?: return false
            val next = !custom.isFavorite
            dao.updateCustomDhikrFavorite(customId, next)
            return next
        }

        val existing = dao.getHisnDhikr(dhikrKey)
        if (existing != null) {
            val next = !existing.isFavorite
            dao.updateHisnDhikrFavorite(dhikrKey, next)
            return next
        }

        // Ensure seeded if called before init finished
        seedIfNeeded()
        val seeded = dao.getHisnDhikr(dhikrKey) ?: return false
        val next = !seeded.isFavorite
        dao.updateHisnDhikrFavorite(dhikrKey, next)
        return next
    }
}
