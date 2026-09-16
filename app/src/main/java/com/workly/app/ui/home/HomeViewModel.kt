package com.workly.app.ui.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workly.app.AppGraph
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.WorkTypeEntity
import com.workly.app.data.prefs.AppSettings
import com.workly.app.data.prefs.SettingsRepository
import com.workly.app.data.repository.WorkRepository
import com.workly.app.data.repository.WorkTypeRepository
import com.workly.app.domain.Money
import com.workly.app.domain.PeriodStats
import com.workly.app.domain.StatsCalculator
import com.workly.app.domain.WorkRange
import com.workly.app.domain.WorkSchedule
import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

/** Everything the dashboard needs, in one immutable snapshot. */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val today: PeriodStats = PeriodStats(WorkRange.ofDay(LocalDate.now())),
    val week: PeriodStats = PeriodStats(WorkRange.ofDay(LocalDate.now())),
    val activeSession: WorkSessionEntity? = null,
    val recentSessions: List<WorkSessionEntity> = emptyList(),
    val workTypes: List<WorkTypeEntity> = emptyList(),
    val selectedWorkType: WorkTypeEntity? = null,
    val currency: String = AppSettings().currency,
    val effectiveHourlyRateMinor: Long = 0L,
    val hasAnySession: Boolean = false,
    val showRatePrompt: Boolean = false,
    /** Minutes the user aims to work this week; 0 when no target is set. */
    val weekTargetMinutes: Long = 0L,
    val weekPlannedDays: Int = 0,
    val weekTotalDays: Int = 0,
) {
    val weekProgress: Float get() = WorkSchedule.progress(week.totalMinutes, weekTargetMinutes)

    val weekRemainingMinutes: Long
        get() = (weekTargetMinutes - week.totalMinutes).coerceAtLeast(0L)

    val hasWeekTarget: Boolean get() = weekTargetMinutes > 0L
}

class HomeViewModel(
    private val workRepository: WorkRepository,
    private val workTypeRepository: WorkTypeRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val draft = MutableStateFlow(HomeDraft())

    private val messages = Channel<WorklyError>(Channel.BUFFERED)

    /** One-shot problems to show in a snackbar. */
    val events = messages.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(
        workRepository.allSessions,
        workTypeRepository.workTypes,
        settingsRepository.settings,
        draft,
    ) { sessions, workTypes, settings, draftState ->
        buildState(sessions, workTypes, settings, draftState)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(),
    )

    fun selectWorkType(id: Long?) {
        draft.update { it.copy(selectedWorkTypeId = id) }
    }

    /** Starts the timer, asking for a rate first if none has been configured yet. */
    fun startWork() {
        viewModelScope.launch {
            val state = uiState.value
            val rate = state.effectiveHourlyRateMinor
            if (rate <= 0L) {
                draft.update { it.copy(showRatePrompt = true) }
                return@launch
            }
            val result = workRepository.startWork(
                hourlyRateMinor = rate,
                currency = state.currency,
                workType = state.selectedWorkType,
            )
            result.exceptionOrNull()?.let { messages.send(it.toWorklyError()) }
        }
    }

    /** Confirms the rate dialog: stores the rate as the new default and starts. */
    fun startWorkWithRate(rateText: String) {
        viewModelScope.launch {
            val state = uiState.value
            val minor = Money.parseToMinor(rateText, state.currency)
            if (minor == null || minor < 0L) {
                messages.send(WorklyError.NEGATIVE_RATE)
                return@launch
            }
            draft.update { it.copy(showRatePrompt = false) }
            if (state.selectedWorkType?.defaultHourlyRateMinor?.let { it > 0L } != true) {
                settingsRepository.setDefaultHourlyRateMinor(minor)
            }
            val result = workRepository.startWork(
                hourlyRateMinor = minor,
                currency = state.currency,
                workType = state.selectedWorkType,
            )
            result.exceptionOrNull()?.let { messages.send(it.toWorklyError()) }
        }
    }

    fun dismissRatePrompt() {
        draft.update { it.copy(showRatePrompt = false) }
    }

    fun discardActiveSession() {
        viewModelScope.launch {
            workRepository.discardActiveSession()
                .exceptionOrNull()?.let { messages.send(it.toWorklyError()) }
        }
    }

    private fun buildState(
        sessions: List<WorkSessionEntity>,
        workTypes: List<WorkTypeEntity>,
        settings: AppSettings,
        draftState: HomeDraft,
    ): HomeUiState {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val completed = sessions.filter { it.endTime != null }
        val selectedType = workTypes.firstOrNull { it.id == draftState.selectedWorkTypeId }
            ?: workTypes.firstOrNull { it.id == settings.defaultWorkTypeId }
            ?: workTypes.firstOrNull()
        val effectiveRate = selectedType?.defaultHourlyRateMinor?.takeIf { it > 0L }
            ?: settings.defaultHourlyRateMinor

        val weekRange = WorkRange.ofWeek(today, settings.firstDayOfWeek)
        return HomeUiState(
            isLoading = false,
            today = StatsCalculator.summarize(completed, WorkRange.ofDay(today), zone),
            week = StatsCalculator.summarize(completed, weekRange, zone),
            activeSession = sessions.firstOrNull { it.isActive },
            recentSessions = completed.sortedByDescending { it.startTime }.take(RECENT_LIMIT),
            workTypes = workTypes,
            selectedWorkType = selectedType,
            currency = settings.currency,
            effectiveHourlyRateMinor = effectiveRate,
            hasAnySession = completed.isNotEmpty(),
            showRatePrompt = draftState.showRatePrompt,
            weekTargetMinutes = WorkSchedule.targetMinutes(
                range = weekRange,
                weeklyTargetMinutes = settings.weeklyTargetMinutes,
                restDays = settings.restDays,
            ),
            weekPlannedDays = WorkSchedule.plannedWorkDays(weekRange, settings.restDays),
            weekTotalDays = weekRange.dayCount.toInt(),
        )
    }

    private fun Throwable.toWorklyError(): WorklyError =
        (this as? WorklyException)?.error ?: WorklyError.UNKNOWN

    private data class HomeDraft(
        val selectedWorkTypeId: Long? = null,
        val showRatePrompt: Boolean = false,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val RECENT_LIMIT = 4

        val Factory = viewModelFactory {
            initializer {
                HomeViewModel(
                    workRepository = AppGraph.workRepository,
                    workTypeRepository = AppGraph.workTypeRepository,
                    settingsRepository = AppGraph.settingsRepository,
                )
            }
        }
    }
}
