package com.workly.app.ui.settings

import androidx.compose.runtime.Immutable
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workly.app.AppGraph
import com.workly.app.LocaleController
import com.workly.app.data.backup.BackupManager
import com.workly.app.data.backup.ImportPreview
import com.workly.app.data.backup.WorklyBackup
import com.workly.app.data.prefs.AppLanguage
import com.workly.app.data.prefs.AppSettings
import com.workly.app.data.prefs.BackgroundMode
import com.workly.app.data.prefs.DurationStyle
import com.workly.app.data.prefs.ThemePalette
import com.workly.app.data.prefs.SettingsRepository
import com.workly.app.data.prefs.ThemeMode
import com.workly.app.data.repository.WorkRepository
import com.workly.app.data.repository.WorkTypeRepository
import com.workly.app.domain.Money
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
import java.time.DayOfWeek
import java.time.ZoneId

/** One-shot feedback that the settings screen turns into a snackbar or dialog. */
sealed interface SettingsEvent {
    data class Error(val error: WorklyError) : SettingsEvent

    data class Exported(val count: Int, val asCsv: Boolean) : SettingsEvent

    data object NothingToExport : SettingsEvent

    data class Imported(val sessions: Int, val workTypes: Int) : SettingsEvent

    data object NothingNewImported : SettingsEvent

    data object AllDataDeleted : SettingsEvent
}

@Immutable
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val workTypeCount: Int = 0,
    val isBusy: Boolean = false,
    val importPreview: ImportPreview? = null,
) {
    val hasImportPending: Boolean get() = importPreview != null
}

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val workTypeRepository: WorkTypeRepository,
    private val workRepository: WorkRepository,
    private val backupManager: BackupManager,
    private val contentResolver: ContentResolver,
) : ViewModel() {

    private val draft = MutableStateFlow(SettingsDraft())

    private val eventsChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.settings,
        workTypeRepository.workTypes,
        draft,
    ) { settings, workTypes, draftState ->
        SettingsUiState(
            settings = settings,
            workTypeCount = workTypes.size,
            isBusy = draftState.isBusy,
            importPreview = draftState.preview,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SettingsUiState(),
    )

    fun setDefaultHourlyRate(text: String) {
        viewModelScope.launch {
            val currency = uiState.value.settings.currency
            val minor = Money.parseToMinor(text, currency)
            if (minor == null || minor < 0L) {
                eventsChannel.send(SettingsEvent.Error(WorklyError.NEGATIVE_RATE))
                return@launch
            }
            settingsRepository.setDefaultHourlyRateMinor(minor)
        }
    }

    fun setCurrency(code: String) {
        viewModelScope.launch { settingsRepository.setCurrency(code) }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    // ------------------------------------------------------------ background

    fun setBackgroundMode(mode: BackgroundMode) {
        viewModelScope.launch { settingsRepository.setBackgroundMode(mode) }
    }

    fun setBackgroundColor(argb: Long) {
        viewModelScope.launch { settingsRepository.setBackgroundColor(argb) }
    }

    /**
     * Remembers the picked picture.
     *
     * The read permission is persisted with the URI, otherwise the background
     * would disappear after the next reboot.
     */
    fun setBackgroundImage(uri: Uri) {
        viewModelScope.launch {
            runCatching {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            settingsRepository.setBackgroundImageUri(uri.toString())
            settingsRepository.setBackgroundMode(BackgroundMode.IMAGE)
        }
    }

    fun setBackgroundBlurPercent(percent: Int) {
        viewModelScope.launch { settingsRepository.setBackgroundBlurPercent(percent) }
    }

    fun setAdaptToImage(adapt: Boolean) {
        viewModelScope.launch { settingsRepository.setAdaptToImage(adapt) }
    }

    fun setThemePalette(palette: ThemePalette) {
        viewModelScope.launch { settingsRepository.setThemePalette(palette) }
    }

    fun setDurationStyle(style: DurationStyle) {
        viewModelScope.launch { settingsRepository.setDurationStyle(style) }
    }

    /** Stores the weekly target in minutes; 0 clears it. */
    fun setWeeklyTargetHours(text: String) {
        viewModelScope.launch {
            val hours = text.trim().replace(',', '.').toBigDecimalOrNull()
            if (hours == null || hours.signum() < 0) {
                eventsChannel.send(SettingsEvent.Error(WorklyError.NEGATIVE_RATE))
                return@launch
            }
            val minutes = hours.multiply(java.math.BigDecimal(60))
                .setScale(0, java.math.RoundingMode.HALF_UP)
                .toLong()
            settingsRepository.setWeeklyTargetMinutes(minutes)
        }
    }

    fun setRestDays(days: Set<DayOfWeek>) {
        viewModelScope.launch { settingsRepository.setRestDays(days) }
    }

    fun setFirstDayOfWeek(day: DayOfWeek) {
        viewModelScope.launch { settingsRepository.setFirstDayOfWeek(day) }
    }

    /**
     * Stores the choice and hands it to the platform, which recreates the
     * activity so every screen — and the launcher label — follows immediately.
     */
    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            settingsRepository.setLanguage(language)
            LocaleController.apply(language)
        }
    }

    // ---------------------------------------------------------------- export

    fun exportCsv(uri: Uri) =
        export(uri, asCsv = true) { backupManager.exportCsv(it, ZoneId.systemDefault()) }

    fun exportJson(uri: Uri) =
        export(uri, asCsv = false) { backupManager.exportJson(it) }

    private fun export(
        uri: Uri,
        asCsv: Boolean,
        writer: suspend (java.io.OutputStream) -> Result<Int>,
    ) {
        viewModelScope.launch {
            draft.update { it.copy(isBusy = true) }
            val stream = runCatching { contentResolver.openOutputStream(uri) }.getOrNull()
            if (stream == null) {
                draft.update { it.copy(isBusy = false) }
                eventsChannel.send(SettingsEvent.Error(WorklyError.UNKNOWN))
                return@launch
            }
            writer(stream).fold(
                onSuccess = { count ->
                    eventsChannel.send(
                        if (count == 0) SettingsEvent.NothingToExport else SettingsEvent.Exported(count, asCsv),
                    )
                },
                onFailure = { throwable ->
                    eventsChannel.send(
                        SettingsEvent.Error((throwable as? WorklyException)?.error ?: WorklyError.EXPORT_FAILED),
                    )
                },
            )
            draft.update { it.copy(isBusy = false) }
        }
    }

    // ---------------------------------------------------------------- import

    /** Reads the picked file, validates it and shows the preview dialog. */
    fun onImportPicked(uri: Uri) {
        viewModelScope.launch {
            draft.update { it.copy(isBusy = true) }
            val text = readText(uri)
            if (text == null) {
                draft.update { it.copy(isBusy = false) }
                eventsChannel.send(SettingsEvent.Error(WorklyError.INVALID_IMPORT_FILE))
                return@launch
            }
            backupManager.parse(text).fold(
                onSuccess = { backup ->
                    val preview = backupManager.preview(backup)
                    draft.update { it.copy(isBusy = false, backup = backup, preview = preview) }
                },
                onFailure = { throwable ->
                    draft.update { it.copy(isBusy = false) }
                    eventsChannel.send(
                        SettingsEvent.Error((throwable as? WorklyException)?.error ?: WorklyError.INVALID_IMPORT_FILE),
                    )
                },
            )
        }
    }

    fun confirmImport() {
        val backup = draft.value.backup ?: return
        viewModelScope.launch {
            draft.update { it.copy(isBusy = true) }
            backupManager.import(backup).fold(
                onSuccess = { outcome ->
                    draft.update { it.copy(isBusy = false, backup = null, preview = null) }
                    eventsChannel.send(
                        if (outcome.importedSessions == 0 && outcome.importedWorkTypes == 0) {
                            SettingsEvent.NothingNewImported
                        } else {
                            SettingsEvent.Imported(outcome.importedSessions, outcome.importedWorkTypes)
                        },
                    )
                },
                onFailure = { throwable ->
                    draft.update { it.copy(isBusy = false) }
                    eventsChannel.send(
                        SettingsEvent.Error((throwable as? WorklyException)?.error ?: WorklyError.INVALID_IMPORT_FILE),
                    )
                },
            )
        }
    }

    fun cancelImport() = draft.update { it.copy(backup = null, preview = null) }

    // ----------------------------------------------------------------- reset

    /** Removes every record and work type, then lets the starter types be recreated. */
    fun deleteAllData() {
        viewModelScope.launch {
            draft.update { it.copy(isBusy = true) }
            val result = workRepository.deleteEverything()
            result.fold(
                onSuccess = {
                    workTypeRepository.deleteAll()
                    settingsRepository.setDefaultsSeeded(false)
                    workTypeRepository.ensureDefaultWorkTypes()
                    eventsChannel.send(SettingsEvent.AllDataDeleted)
                },
                onFailure = { throwable ->
                    eventsChannel.send(
                        SettingsEvent.Error((throwable as? WorklyException)?.error ?: WorklyError.DATABASE_ERROR),
                    )
                },
            )
            draft.update { it.copy(isBusy = false) }
        }
    }

    /** Reads a text file with a hard cap so a huge file cannot exhaust memory. */
    private fun readText(uri: Uri): String? = runCatching {
        contentResolver.openInputStream(uri)?.use { stream ->
            val reader = stream.bufferedReader()
            val buffer = CharArray(16 * 1024)
            val builder = StringBuilder()
            var total = 0
            while (true) {
                val read = reader.read(buffer)
                if (read <= 0) break
                total += read
                if (total > MAX_IMPORT_CHARS) break
                builder.append(buffer, 0, read)
            }
            builder.toString()
        }
    }.getOrNull()

    private data class SettingsDraft(
        val isBusy: Boolean = false,
        val backup: WorklyBackup? = null,
        val preview: ImportPreview? = null,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L
        private const val MAX_IMPORT_CHARS = 8 * 1024 * 1024

        val Factory = viewModelFactory {
            initializer {
                SettingsViewModel(
                    settingsRepository = AppGraph.settingsRepository,
                    workTypeRepository = AppGraph.workTypeRepository,
                    workRepository = AppGraph.workRepository,
                    backupManager = AppGraph.backupManager,
                    contentResolver = AppGraph.appContext.contentResolver,
                )
            }
        }
    }
}
