package com.workly.app.ui.records

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workly.app.AppGraph
import com.workly.app.R
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.incomeMinor
import com.workly.app.data.local.entity.workedMinutes
import com.workly.app.data.repository.WorkRepository
import com.workly.app.ui.components.ConfirmDialog
import com.workly.app.ui.components.DetailRow
import com.workly.app.ui.components.EditIcon
import com.workly.app.ui.components.ScreenHeader
import com.workly.app.ui.components.SectionLabel
import com.workly.app.ui.components.WorklyCard
import com.workly.app.ui.theme.WorklyTheme
import com.workly.app.ui.util.formatDateMedium
import com.workly.app.ui.util.formatDuration
import com.workly.app.ui.util.formatMoney
import com.workly.app.ui.util.formatRate
import com.workly.app.ui.util.formatTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.ZoneId

class SessionDetailViewModel(
    private val workRepository: WorkRepository,
    private val sessionId: Long,
) : ViewModel() {

    val session: StateFlow<WorkSessionEntity?> = workRepository.observeSession(sessionId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

    private val deletedState = MutableStateFlow(false)
    val deleted: StateFlow<Boolean> = deletedState

    fun delete() {
        viewModelScope.launch {
            workRepository.deleteSession(sessionId).onSuccess { deletedState.value = true }
        }
    }

    companion object {
        fun factory(sessionId: Long) = viewModelFactory {
            initializer { SessionDetailViewModel(AppGraph.workRepository, sessionId) }
        }
    }
}

/** Read-only view of one record, with the two actions that act on it. */
@Composable
fun SessionDetailScreen(
    sessionId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SessionDetailViewModel = viewModel(
        key = "detail-$sessionId",
        factory = SessionDetailViewModel.factory(sessionId),
    ),
) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) { if (deleted) onBack() }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.records_detail_title),
            onBack = onBack,
            actions = {
                IconButton(onClick = { onEdit(sessionId) }) {
                    Icon(
                        imageVector = EditIcon,
                        contentDescription = stringResource(R.string.cd_edit_record),
                    )
                }
            },
        )

        val record = session
        if (record == null) {
            Spacer(Modifier.height(40.dp))
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            WorklyCard(modifier = Modifier.fillMaxWidth()) {
                SectionLabel(stringResource(R.string.editor_worked_time))
                Text(
                    text = formatDuration(record.workedMinutes),
                    style = MaterialTheme.typography.displayMedium,
                )
                Spacer(Modifier.height(16.dp))
                SectionLabel(stringResource(R.string.editor_income))
                Text(
                    text = formatMoney(record.incomeMinor, record.currency),
                    style = MaterialTheme.typography.displaySmall,
                    color = WorklyTheme.accents.income,
                )
            }

            Spacer(Modifier.height(20.dp))
            WorklyCard(modifier = Modifier.fillMaxWidth(), contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp)) {
                val zone = ZoneId.systemDefault()
                DetailRow(
                    label = stringResource(R.string.editor_date),
                    value = formatDateMedium(record.startTime.atZone(zone).toLocalDate()),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                DetailRow(
                    label = stringResource(R.string.editor_start_time),
                    value = formatTime(record.startTime),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                record.endTime?.let { end ->
                    DetailRow(
                        label = stringResource(R.string.editor_end_time),
                        value = formatTime(end),
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
                DetailRow(
                    label = stringResource(R.string.editor_break_minutes),
                    value = "${record.breakMinutes}",
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                DetailRow(
                    label = stringResource(R.string.editor_hourly_rate),
                    value = formatRate(record.hourlyRateMinor, record.currency),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                DetailRow(
                    label = stringResource(R.string.editor_work_type),
                    value = record.workTypeName
                        ?: stringResource(R.string.editor_no_work_type),
                    modifier = Modifier.padding(horizontal = 12.dp),
                )
                if (record.note.isNotBlank()) {
                    DetailRow(
                        label = stringResource(R.string.editor_note),
                        value = record.note,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
                TextButton(onClick = { showDeleteDialog = true }) {
                    Text(
                        text = stringResource(R.string.action_delete),
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
}
