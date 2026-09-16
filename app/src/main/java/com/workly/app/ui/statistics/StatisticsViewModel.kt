package com.workly.app.ui.statistics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workly.app.AppGraph
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.prefs.AppSettings
import com.workly.app.data.prefs.SettingsRepository
import com.workly.app.data.repository.WorkRepository
import com.workly.app.domain.PeriodStats
import com.workly.app.domain.StatsCalculator
import com.workly.app.domain.TimeBucket
import com.workly.app.domain.WorkRange
import com.workly.app.domain.WorkSchedule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class StatsPeriodOption { WEEK, MONTH, YEAR, CUSTOM }

/** Granularity of the trend charts. */
enum class BucketMode { DAY, MONTH }

@Immutable
data class StatisticsUiState(
    val isLoading: Boolean = true,
    val option: StatsPeriodOption = StatsPeriodOption.WEEK,
    val range: WorkRange = WorkRange.ofWeek(LocalDate.now(), java.time.DayOfWeek.MONDAY),
    val stats: PeriodStats = PeriodStats(WorkRange.ofDay(LocalDate.now())),
    val buckets: List<TimeBucket> = emptyList(),
    val bucketMode: BucketMode = BucketMode.DAY,
    val currency: String = AppSettings().currency,
    val hasAnyRecord: Boolean = false,
    val customStart: LocalDate? = null,
    val customEnd: LocalDate? = null,
    val isPickingCustomRange: Boolean = false,
    val restDays: Set<java.time.DayOfWeek> = com.workly.app.data.prefs.DEFAULT_REST_DAYS,
    val plannedWorkDays: Int = 0,
    val restDayCount: Int = 0,
    /** Minutes the user aims to work in this period; 0 when no target is set. */
    val targetMinutes: Long = 0L,
) {
    val hasTarget: Boolean get() = targetMinutes > 0L

    val targetProgress: Float get() = WorkSchedule.progress(stats.totalMinutes, targetMinutes)

    val targetRemainingMinutes: Long
        get() = (targetMinutes - stats.totalMinutes).coerceAtLeast(0L)
}

class StatisticsViewModel(
    private val workRepository: WorkRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val draft = MutableStateFlow(StatsDraft())

    val uiState: StateFlow<StatisticsUiState> = combine(
        workRepository.completedSessions,
        settingsRepository.settings,
        draft,
    ) { sessions, settings, draftState ->
        buildState(sessions, settings, draftState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = StatisticsUiState(),
    )

    fun selectOption(option: StatsPeriodOption) = draft.update {
        it.copy(option = option, isPickingCustomRange = option == StatsPeriodOption.CUSTOM)
    }

    fun startCustomRange() = draft.update { it.copy(isPickingCustomRange = true) }

    fun setCustomRange(start: LocalDate?, end: LocalDate?) = draft.update {
        it.copy(
            customStart = start ?: it.customStart,
            customEnd = end ?: it.customEnd,
            option = StatsPeriodOption.CUSTOM,
            isPickingCustomRange = false,
        )
    }

    fun dismissCustomRange() = draft.update { it.copy(isPickingCustomRange = false) }

    private fun buildState(
        sessions: List<WorkSessionEntity>,
        settings: AppSettings,
        draftState: StatsDraft,
    ): StatisticsUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val range = when (draftState.option) {
            StatsPeriodOption.WEEK -> WorkRange.ofWeek(today, settings.firstDayOfWeek)
            StatsPeriodOption.MONTH -> WorkRange.ofMonth(YearMonth.from(today))
            StatsPeriodOption.YEAR -> WorkRange.ofYear(today.year)
            StatsPeriodOption.CUSTOM -> {
                val start = draftState.customStart ?: today.minusDays(29)
                val end = draftState.customEnd ?: today
                WorkRange.custom(start, end)
            }
        }
        val useMonths = range.dayCount > MAX_DAILY_BUCKETS
        val buckets = if (useMonths) {
            StatsCalculator.monthlyBuckets(sessions, range, zone)
        } else {
            StatsCalculator.dailyBuckets(sessions, range, zone)
        }

        val restDays = settings.restDays
        return StatisticsUiState(
            isLoading = false,
            restDays = restDays,
            plannedWorkDays = WorkSchedule.plannedWorkDays(range, restDays),
            restDayCount = WorkSchedule.plannedRestDays(range, restDays),
            targetMinutes = WorkSchedule.targetMinutes(
                range = range,
                weeklyTargetMinutes = settings.weeklyTargetMinutes,
                restDays = restDays,
            ),
            option = draftState.option,
            range = range,
            stats = StatsCalculator.summarize(sessions, range, zone),
            buckets = buckets,
            bucketMode = if (useMonths) BucketMode.MONTH else BucketMode.DAY,
            currency = settings.currency,
            hasAnyRecord = sessions.isNotEmpty(),
            customStart = draftState.customStart,
            customEnd = draftState.customEnd,
            isPickingCustomRange = draftState.isPickingCustomRange,
        )
    }

    private data class StatsDraft(
        val option: StatsPeriodOption = StatsPeriodOption.WEEK,
        val customStart: LocalDate? = null,
        val customEnd: LocalDate? = null,
        val isPickingCustomRange: Boolean = false,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        /** Above this many days the charts switch to monthly buckets. */
        private const val MAX_DAILY_BUCKETS = 62L

        val Factory = viewModelFactory {
            initializer {
                StatisticsViewModel(
                    workRepository = AppGraph.workRepository,
                    settingsRepository = AppGraph.settingsRepository,
                )
            }
        }
    }
}
