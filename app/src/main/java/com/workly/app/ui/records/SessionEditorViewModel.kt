package com.workly.app.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workly.app.AppGraph
import com.workly.app.data.local.entity.WorkTypeEntity
import com.workly.app.data.prefs.AppSettings
import com.workly.app.data.prefs.SettingsRepository
import com.workly.app.data.repository.WorkRepository
import com.workly.app.data.repository.WorkTypeRepository
import com.workly.app.domain.Money
import com.workly.app.domain.SessionValidator
import com.workly.app.domain.WorkTime
import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The three ways the shared editor is used. */
enum class SessionEditorMode { CREATE, EDIT, FINISH }

data class SessionEditorUiState(
    val mode: SessionEditorMode = SessionEditorMode.CREATE,
    val isLoading: Boolean = true,
    val sessionId: Long? = null,
    val startDate: LocalDate = LocalDate.now(),
    val startTime: LocalTime = LocalTime.now(),
    val endTime: LocalTime = LocalTime.now(),
    val endsNextDay: Boolean = false,
    val breakText: String = "0",
    val rateText: String = "",
    val currency: String = AppSettings().currency,
    val workTypes: List<WorkTypeEntity> = emptyList(),
    val selectedWorkTypeId: Long? = null,
    val note: String = "",
    val workedMinutes: Long = 0L,
    val incomeMinor: Long = 0L,
    val validationError: WorklyError? = null,
    val isSaving: Boolean = false,
    /** Set once the record has been written; the UI navigates away. */
    val saved: Boolean = false,
    val deleted: Boolean = false,
) {
    val isValid: Boolean get() = validationError == null
    val canSave: Boolean get() = isValid && !isSaving
    val selectedWorkType: WorkTypeEntity? get() = workTypes.firstOrNull { it.id == selectedWorkTypeId }
}

/**
 * Backs the shared record editor: adding a record, editing an existing one and
 * confirming a running session before it is saved.
 *
 * All three modes use the same fields, so the user only ever learns one screen.
 */
class SessionEditorViewModel(
    private val workRepository: WorkRepository,
    private val workTypeRepository: WorkTypeRepository,
    private val settingsRepository: SettingsRepository,
    private val mode: SessionEditorMode,
    private val sessionId: Long?,
    private val now: () -> Instant = { Instant.now() },
) : ViewModel() {

    private val draft = MutableStateFlow(EditorDraft())

    private val messages = Channel<WorklyError>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    val uiState: StateFlow<SessionEditorUiState> = combine(
        draft,
        workTypeRepository.workTypes,
        settingsRepository.settings,
    ) { draftState, workTypes, settings ->
        buildState(draftState, workTypes, settings)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SessionEditorUiState(mode = mode),
    )

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val settings = settingsRepository.settings.first()
        val workTypes = workTypeRepository.workTypes.first()
        val zone = ZoneId.systemDefault()

        if (mode == SessionEditorMode.CREATE || sessionId == null) {
            val now = LocalTime.now().withSecond(0).withNano(0)
            val defaultType = workTypes.firstOrNull { it.id == settings.defaultWorkTypeId }
                ?: workTypes.firstOrNull()
            val rate = defaultType?.defaultHourlyRateMinor?.takeIf { it > 0L }
                ?: settings.defaultHourlyRateMinor
            draft.value = EditorDraft(
                startDate = LocalDate.now(zone),
                startTime = now.minusHours(1),
                endTime = now,
                breakText = "0",
                rateText = Money.toEditableString(rate, settings.currency),
                selectedWorkTypeId = defaultType?.id,
                note = "",
                loaded = true,
            )
            return
        }

        val session = workRepository.getSession(sessionId)
        if (session == null) {
            messages.send(WorklyError.DATABASE_ERROR)
            draft.update { it.copy(loaded = true) }
            return
        }
        val start = session.startTime.atZone(zone)
        val end = session.endTime?.atZone(zone)?.toLocalTime()
        val now = LocalTime.now().withSecond(0).withNano(0)
        draft.value = EditorDraft(
            startDate = start.toLocalDate(),
            startTime = start.toLocalTime().withSecond(0).withNano(0),
            endTime = end?.withSecond(0)?.withNano(0) ?: now,
            breakText = session.breakMinutes.toString(),
            rateText = Money.toEditableString(session.hourlyRateMinor, session.currency),
            selectedWorkTypeId = session.workTypeId,
            note = session.note,
            loaded = true,
        )
    }

    fun onStartDateChange(date: LocalDate) = draft.update { it.copy(startDate = date) }

    fun onStartTimeChange(time: LocalTime) = draft.update { it.copy(startTime = time) }

    fun onEndTimeChange(time: LocalTime) =
        draft.update { it.copy(endTime = time, endTimeEdited = true) }

    fun onBreakChange(text: String) =
        draft.update { it.copy(breakText = text.filter(Char::isDigit).take(4)) }

    fun onRateChange(text: String) = draft.update { it.copy(rateText = text.take(18)) }

    fun onNoteChange(text: String) = draft.update { it.copy(note = text.take(NOTE_MAX_LENGTH)) }

    /** Selecting a work type fills in its default rate, which can still be edited. */
    fun onWorkTypeChange(type: WorkTypeEntity?) = draft.update { current ->
        val appliedRate = type?.defaultHourlyRateMinor
            ?.takeIf { it > 0L }
            ?.let { Money.toEditableString(it, type.currency) }
        current.copy(
            selectedWorkTypeId = type?.id,
            rateText = appliedRate ?: current.rateText,
        )
    }

    /**
     * Puts the end time back to "right now" on the finish screen.
     *
     * This counts as *not* edited: "now" keeps second precision so a session that
     * is finished inside the same minute it started can still be saved.
     */
    fun setEndTimeToNow() = draft.update {
        it.copy(endTime = LocalTime.now().withSecond(0).withNano(0), endTimeEdited = false)
    }

    fun save() {
        val state = uiState.value
        if (!state.isValid) {
            state.validationError?.let { error -> viewModelScope.launch { messages.send(error) } }
            return
        }
        viewModelScope.launch {
            draft.update { it.copy(isSaving = true) }
            val zone = ZoneId.systemDefault()
            val draftState = draft.value
            val startInstant = state.startDate.atTime(state.startTime).atZone(zone).toInstant()
            // Resolved exactly the way the validator did, so whatever the screen
            // shows as valid is what actually gets written.
            val endInstant = resolveEndInstant(draftState, startInstant, zone)
            val rateMinor = Money.parseToMinor(state.rateText, state.currency) ?: 0L
            val breakMinutes = state.breakText.toIntOrNull() ?: 0
            val workType = state.selectedWorkType

            val result: Result<Long?> = when (mode) {
                SessionEditorMode.CREATE -> workRepository.createManualSession(
                    startTime = startInstant,
                    endTime = endInstant,
                    breakMinutes = breakMinutes,
                    hourlyRateMinor = rateMinor,
                    currency = state.currency,
                    workType = workType,
                    note = state.note,
                ).map { it }

                SessionEditorMode.EDIT -> workRepository.updateSession(
                    session = sessionId?.let { workRepository.getSession(it) }
                        ?: return@launch report(WorklyError.DATABASE_ERROR),
                    startTime = startInstant,
                    endTime = endInstant,
                    breakMinutes = breakMinutes,
                    hourlyRateMinor = rateMinor,
                    currency = state.currency,
                    workType = workType,
                    note = state.note,
                ).map { null }

                SessionEditorMode.FINISH -> workRepository.finishWork(
                    sessionId = sessionId ?: -1L,
                    endTime = endInstant,
                    breakMinutes = breakMinutes,
                    hourlyRateMinor = rateMinor,
                    currency = state.currency,
                    workType = workType,
                    note = state.note,
                ).map { null }
            }

            draft.update { it.copy(isSaving = false) }
            result.fold(
                onSuccess = { draft.update { current -> current.copy(saved = true) } },
                onFailure = { throwable ->
                    messages.send((throwable as? WorklyException)?.error ?: WorklyError.UNKNOWN)
                },
            )
        }
    }

    fun delete() {
        val id = sessionId ?: return
        viewModelScope.launch {
            workRepository.deleteSession(id).fold(
                onSuccess = { draft.update { it.copy(deleted = true) } },
                onFailure = { throwable ->
                    messages.send((throwable as? WorklyException)?.error ?: WorklyError.UNKNOWN)
                },
            )
        }
    }

    /** Throws the running session away without recording any work. */
    fun discardActiveSession() {
        viewModelScope.launch {
            workRepository.discardActiveSession().fold(
                onSuccess = { draft.update { it.copy(deleted = true) } },
                onFailure = { throwable ->
                    messages.send((throwable as? WorklyException)?.error ?: WorklyError.UNKNOWN)
                },
            )
        }
    }

    private suspend fun report(error: WorklyError) {
        draft.update { it.copy(isSaving = false) }
        messages.send(error)
    }

    private fun buildState(
        draftState: EditorDraft,
        workTypes: List<WorkTypeEntity>,
        settings: AppSettings,
    ): SessionEditorUiState {
        val currency = settings.currency
        val rateMinor = Money.parseToMinor(draftState.rateText, currency)
        val breakMinutes = draftState.breakText.toIntOrNull() ?: 0
        val zone = ZoneId.systemDefault()
        val startInstant = draftState.startDate.atTime(draftState.startTime).atZone(zone).toInstant()
        val endInstant = resolveEndInstant(draftState, startInstant, zone)
        val validation = SessionValidator.validate(
            start = startInstant,
            end = endInstant,
            breakMinutes = breakMinutes,
            hourlyRateMinor = rateMinor ?: -1L,
        )
        val workedMinutes = (validation as? SessionValidator.Result.Valid)?.workedMinutes ?: 0L

        return SessionEditorUiState(
            mode = mode,
            isLoading = !draftState.loaded,
            sessionId = sessionId,
            startDate = draftState.startDate,
            startTime = draftState.startTime,
            endTime = draftState.endTime,
            endsNextDay = WorkTime.endsNextDay(draftState.startTime, draftState.endTime),
            breakText = draftState.breakText,
            rateText = draftState.rateText,
            currency = currency,
            workTypes = workTypes,
            selectedWorkTypeId = draftState.selectedWorkTypeId,
            note = draftState.note,
            workedMinutes = workedMinutes,
            incomeMinor = Money.incomeMinor(rateMinor ?: 0L, workedMinutes),
            validationError = (validation as? SessionValidator.Result.Invalid)?.error,
            isSaving = draftState.isSaving,
            saved = draftState.saved,
            deleted = draftState.deleted,
        )
    }

    /**
     * Resolves the end instant for the current draft.
     *
     * Normally the picked clock times decide. The exception is a running session
     * that is finished before the next full minute: the start and end clock times
     * are then identical, which the validator would reject, yet the user clearly
     * meant "now". In that one case the real instant is used, so the record is
     * saved with the (short) time that actually elapsed.
     */
    private fun resolveEndInstant(
        draftState: EditorDraft,
        startInstant: Instant,
        zone: ZoneId,
    ): Instant {
        val picked = WorkTime.resolveEndInstant(
            startDate = draftState.startDate,
            startTime = draftState.startTime,
            endTime = draftState.endTime,
            zone = zone,
        )
        val defaultFinishTime = mode == SessionEditorMode.FINISH && !draftState.endTimeEdited
        if (!defaultFinishTime || picked.isAfter(startInstant)) return picked
        val now = now()
        return if (now.isAfter(startInstant)) now else startInstant.plusSeconds(1)
    }

    private data class EditorDraft(
        val startDate: LocalDate = LocalDate.now(),
        val startTime: LocalTime = LocalTime.now(),
        val endTime: LocalTime = LocalTime.now(),
        val breakText: String = "0",
        val rateText: String = "",
        val selectedWorkTypeId: Long? = null,
        val note: String = "",
        val endTimeEdited: Boolean = false,
        val loaded: Boolean = false,
        val isSaving: Boolean = false,
        val saved: Boolean = false,
        val deleted: Boolean = false,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        const val NOTE_MAX_LENGTH = 500

        fun factory(mode: SessionEditorMode, sessionId: Long?): ViewModelProvider.Factory =
            viewModelFactory {
                initializer {
                    SessionEditorViewModel(
                        workRepository = AppGraph.workRepository,
                        workTypeRepository = AppGraph.workTypeRepository,
                        settingsRepository = AppGraph.settingsRepository,
                        mode = mode,
                        sessionId = sessionId,
                    )
                }
            }
    }
}
