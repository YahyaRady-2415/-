package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.PrayerLogEntity
import com.example.data.model.PrayerMode
import com.example.data.model.PrayerName
import com.example.data.model.PrayerStatus
import com.example.data.repository.PrayerRepository
import com.example.domain.HijriCalendarHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyPrayerItemUi(
    val prayer: PrayerName,
    val status: PrayerStatus,
    val mode: PrayerMode,
    val isCompleted: Boolean,
    val updatedAt: Long? = null,
)

data class DailyPrayersUiState(
    val dateIso: String,
    val prayers: List<DailyPrayerItemUi>,
    val completedCount: Int,
    val totalCount: Int = PrayerName.entries.size,
) {
    val completionRatio: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()
}

/**
 * Focused ViewModel backed by [PrayerRepository] and Room for tracking the five daily prayers
 * and toggling their completion status.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PrayerTrackerViewModel(
    private val repository: PrayerRepository,
    initialDateIso: String = HijriCalendarHelper.todayIso(),
) : ViewModel() {

    private val _selectedDateIso = MutableStateFlow(initialDateIso)
    val selectedDateIso: StateFlow<String> = _selectedDateIso.asStateFlow()

    val uiState: StateFlow<DailyPrayersUiState> = _selectedDateIso
        .flatMapLatest { dateIso ->
            repository.observePrayersForDate(dateIso).map { logs ->
                buildDailyState(dateIso, logs)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = buildDailyState(initialDateIso, emptyList()),
        )

    fun selectDate(dateIso: String) {
        _selectedDateIso.value = dateIso
    }

    fun togglePrayerCompletion(prayer: PrayerName) {
        val dateIso = _selectedDateIso.value
        viewModelScope.launch {
            repository.togglePrayerCompletion(dateIso = dateIso, prayer = prayer)
        }
    }

    fun setPrayerStatus(
        prayer: PrayerName,
        status: PrayerStatus,
        mode: PrayerMode = PrayerMode.SOLO,
    ) {
        val dateIso = _selectedDateIso.value
        viewModelScope.launch {
            repository.setPrayerStatus(
                dateIso = dateIso,
                prayer = prayer,
                status = status,
                mode = mode,
            )
        }
    }

    private fun buildDailyState(
        dateIso: String,
        logs: List<PrayerLogEntity>,
    ): DailyPrayersUiState {
        val logByName = logs.associateBy { it.prayerName }
        val items = PrayerName.entries.map { prayer ->
            val log = logByName[prayer.name]
            val status = runCatching {
                PrayerStatus.valueOf(log?.status ?: PrayerStatus.PENDING.name)
            }.getOrDefault(PrayerStatus.PENDING)
            val mode = runCatching {
                PrayerMode.valueOf(log?.mode ?: PrayerMode.SOLO.name)
            }.getOrDefault(PrayerMode.SOLO)
            val completed = log?.isCompleted ?: status.isCompleted

            DailyPrayerItemUi(
                prayer = prayer,
                status = status,
                mode = mode,
                isCompleted = completed,
                updatedAt = log?.updatedAt,
            )
        }
        return DailyPrayersUiState(
            dateIso = dateIso,
            prayers = items,
            completedCount = items.count { it.isCompleted },
        )
    }
}

class PrayerTrackerViewModelFactory(
    private val context: Context,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val db = AppDatabase.getInstance(context.applicationContext)
        return PrayerTrackerViewModel(
            repository = PrayerRepository(db.worshipDao()),
        ) as T
    }
}
