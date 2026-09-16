package com.workly.app.ui.records

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workly.app.R
import com.workly.app.domain.WorklyError
import com.workly.app.ui.components.ConfirmDialog
import com.workly.app.ui.components.DeleteIcon
import com.workly.app.ui.components.LocalSnackbarHostState
import com.workly.app.ui.components.ScreenHeader
import com.workly.app.ui.components.SectionLabel
import com.workly.app.ui.components.WorklyCard
import com.workly.app.ui.components.showMessage
import com.workly.app.ui.theme.WorklyTheme
import com.workly.app.ui.util.formatDateMedium
import com.workly.app.ui.util.formatDuration
import com.workly.app.ui.util.formatMoney
import com.workly.app.ui.util.message
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * The single editor used to add a record, edit a record and confirm a running
 * session before it is saved. All three modes share the same fields, so there is
 * only one screen to learn.
 */
@Composable
fun SessionEditorScreen(
    mode: SessionEditorMode,
    sessionId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionEditorViewModel = viewModel(
        key = "editor-$mode-$sessionId",
        factory = SessionEditorViewModel.factory(mode, sessionId),
    ),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var pendingError by remember { mutableStateOf<WorklyError?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) { viewModel.events.collect { pendingError = it } }

    val errorMessage = pendingError?.message()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbar.showMessage(scope, errorMessage)
            pendingError = null
        }
    }

    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onSaved()
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(
                when (mode) {
                    SessionEditorMode.CREATE -> R.string.editor_add_title
                    SessionEditorMode.EDIT -> R.string.editor_edit_title
                    SessionEditorMode.FINISH -> R.string.editor_finish_title
                },
            ),
            onBack = onBack,
            actions = {
                if (mode == SessionEditorMode.EDIT) {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = DeleteIcon,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            SummaryCard(state = state)
            Spacer(Modifier.height(20.dp))

            DateField(
                label = stringResource(R.string.editor_date),
                date = state.startDate,
                onDateChange = viewModel::onStartDateChange,
            )

            Spacer(Modifier.height(12.dp))
            TimeField(
                label = stringResource(R.string.editor_start_time),
                time = state.startTime,
                onTimeChange = viewModel::onStartTimeChange,
            )

            Spacer(Modifier.height(12.dp))
            TimeField(
                label = stringResource(R.string.editor_end_time),
                time = state.endTime,
                onTimeChange = viewModel::onEndTimeChange,
                supporting = if (state.endsNextDay) stringResource(R.string.editor_ends_next_day) else null,
                trailing = if (mode == SessionEditorMode.FINISH) {
                    {
                        TextButton(onClick = viewModel::setEndTimeToNow) {
                            Text(stringResource(R.string.editor_set_now))
                        }
                    }
                } else {
                    null
                },
            )

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.breakText,
                onValueChange = viewModel::onBreakChange,
                label = { Text(stringResource(R.string.editor_break_minutes)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = state.rateText,
                onValueChange = viewModel::onRateChange,
                label = { Text(stringResource(R.string.editor_hourly_rate)) },
                suffix = { Text(state.currency) },
                singleLine = true,
                isError = state.validationError == WorklyError.NEGATIVE_RATE,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            if (state.workTypes.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                SectionLabel(stringResource(R.string.editor_work_type))
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.workTypes.forEach { type ->
                        FilterChip(
                            selected = type.id == state.selectedWorkTypeId,
                            onClick = {
                                viewModel.onWorkTypeChange(
                                    if (type.id == state.selectedWorkTypeId) null else type,
                                )
                            },
                            label = { Text(type.name) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            OutlinedTextField(
                value = state.note,
                onValueChange = viewModel::onNoteChange,
                label = { Text(stringResource(R.string.editor_note)) },
                placeholder = { Text(stringResource(R.string.editor_note_hint)) },
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 96.dp)
                    .testTag(NOTE_FIELD_TAG),
            )

            val validationError = state.validationError
            if (validationError != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = validationError.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(Modifier.height(28.dp))
            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = MaterialTheme.shapes.large,
            ) {
                Text(
                    text = stringResource(
                        if (mode == SessionEditorMode.FINISH) {
                            R.string.editor_finish_and_save
                        } else {
                            R.string.editor_save_record
                        },
                    ),
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            if (mode == SessionEditorMode.FINISH) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = { showDiscardDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = stringResource(R.string.editor_discard),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.delete_record_title),
            message = stringResource(R.string.delete_record_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.delete()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }

    if (showDiscardDialog) {
        ConfirmDialog(
            title = stringResource(R.string.editor_discard_title),
            message = stringResource(R.string.editor_discard_message),
            confirmLabel = stringResource(R.string.editor_discard),
            destructive = true,
            onConfirm = {
                showDiscardDialog = false
                viewModel.discardActiveSession()
            },
            onDismiss = { showDiscardDialog = false },
        )
    }
}

@Composable
private fun SummaryCard(state: SessionEditorUiState) {
    WorklyCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.editor_worked_time))
        Text(
            text = formatDuration(state.workedMinutes),
            style = MaterialTheme.typography.displayMedium,
        )
        Spacer(Modifier.height(16.dp))
        SectionLabel(stringResource(R.string.editor_income))
        Text(
            text = formatMoney(state.incomeMinor, state.currency),
            style = MaterialTheme.typography.displaySmall,
            color = WorklyTheme.accents.income,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }

    FieldRow(
        label = label,
        value = formatDateMedium(date),
        onClick = { showPicker = true },
    )

    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            // The picker reports UTC midnight of the chosen day.
                            onDateChange(
                                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                            )
                        }
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.action_set)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeField(
    label: String,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    var showPicker by remember { mutableStateOf(false) }

    Column {
        FieldRow(
            label = label,
            value = time.toLocalizedTime(),
            onClick = { showPicker = true },
        )
        if (supporting != null) {
            Text(
                text = supporting,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            )
        }
        if (trailing != null) {
            Row(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.fillMaxWidth(),
            ) { trailing() }
        }
    }

    if (showPicker) {
        val is24Hour = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = is24Hour,
        )
        Dialog(onDismissRequest = { showPicker = false }) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    TimePicker(state = pickerState)
                    Spacer(Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = { showPicker = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                onTimeChange(LocalTime.of(pickerState.hour, pickerState.minute))
                                showPicker = false
                            },
                        ) { Text(stringResource(R.string.action_set)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FieldRow(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Formats a wall-clock time the same way the rest of the app does. */
@Composable
private fun LocalTime.toLocalizedTime(): String {
    val instant = LocalDate.now().atTime(this).atZone(ZoneId.systemDefault()).toInstant()
    return com.workly.app.ui.util.formatTime(instant)
}

/** Test tag for the note field, so UI tests can type into it reliably. */
const val NOTE_FIELD_TAG = "editor_note_field"
