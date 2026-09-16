package com.workly.app.ui.records

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
import com.workly.app.domain.WorkRange
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

enum class RecordsViewMode { LIST, CALENDAR }

@Immutable
data class RecordsUiState(
    val isLoading: Boolean = true,
    val viewMode: RecordsViewMode = RecordsViewMode.LIST,
    /** Records grouped by the day they started, newest first. */
    val sessionsByDate: Map<LocalDate, List<WorkSessionEntity>> = emptyMap(),
    val sortedDates: List<LocalDate> = emptyList(),
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate? = null,
    val selectedDateSessions: List<WorkSessionEntity> = emptyList(),
    val currency: String = AppSettings().currency,
    val firstDayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val restDays: Set<DayOfWeek> = com.workly.app.data.prefs.DEFAULT_REST_DAYS,
    val totalCount: Int = 0,
    val monthStats: PeriodStats = PeriodStats(WorkRange.ofMonth(YearMonth.now())),
    val dayStats: PeriodStats = PeriodStats(WorkRange.ofDay(LocalDate.now())),
) {
    val datesWithWork: Set<LocalDate> get() = sessionsByDate.keys
}

class RecordsViewModel(
    private val workRepository: WorkRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val draft = MutableStateFlow(RecordsDraft())

    val uiState: StateFlow<RecordsUiState> = combine(
        workRepository.completedSessions,
        settingsRepository.settings,
        draft,
    ) { sessions, settings, draftState ->
        buildState(sessions, settings, draftState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = RecordsUiState(),
    )

    fun setViewMode(mode: RecordsViewMode) = draft.update { it.copy(viewMode = mode) }

    fun showMonth(month: YearMonth) = draft.update { it.copy(month = month) }

    fun showPreviousMonth() = draft.update { it.copy(month = it.month.minusMonths(1)) }

    fun showNextMonth() = draft.update { it.copy(month = it.month.plusMonths(1)) }

    fun selectDate(date: LocalDate?) = draft.update { it.copy(selectedDate = date) }

    private fun buildState(
        sessions: List<WorkSessionEntity>,
        settings: AppSettings,
        draftState: RecordsDraft,
    ): RecordsUiState {
        val zone = ZoneId.systemDefault()
        val byDate = sessions
            .groupBy { it.startTime.atZone(zone).toLocalDate() }
            .mapValues { (_, value) -> value.sortedByDescending { it.startTime } }
        val selected = draftState.selectedDate

        return RecordsUiState(
            isLoading = false,
            viewMode = draftState.viewMode,
            sessionsByDate = byDate,
            sortedDates = byDate.keys.sortedDescending(),
            month = draftState.month,
            selectedDate = selected,
            selectedDateSessions = selected?.let { byDate[it] }.orEmpty(),
            currency = settings.currency,
            firstDayOfWeek = settings.firstDayOfWeek,
            restDays = settings.restDays,
            totalCount = sessions.size,
            monthStats = StatsCalculator.summarize(
                sessions,
                WorkRange.ofMonth(draftState.month),
                zone,
            ),
            dayStats = selected?.let { StatsCalculator.summarize(sessions, WorkRange.ofDay(it), zone) }
                ?: PeriodStats(WorkRange.ofDay(LocalDate.now())),
        )
    }

    private data class RecordsDraft(
        val viewMode: RecordsViewMode = RecordsViewMode.LIST,
        val month: YearMonth = YearMonth.now(),
        val selectedDate: LocalDate? = LocalDate.now(),
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer {
                RecordsViewModel(
                    workRepository = AppGraph.workRepository,
                    settingsRepository = AppGraph.settingsRepository,
                )
            }
        }
    }
}
