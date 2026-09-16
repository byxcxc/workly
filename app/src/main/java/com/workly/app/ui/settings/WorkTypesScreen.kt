package com.workly.app.ui.settings

import androidx.compose.runtime.Immutable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.workly.app.AppGraph
import com.workly.app.R
import com.workly.app.data.local.entity.WorkTypeEntity
import com.workly.app.data.repository.WorkTypeRepository
import com.workly.app.domain.Money
import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import com.workly.app.ui.components.ConfirmDialog
import com.workly.app.ui.components.AddIcon
import com.workly.app.ui.components.DeleteIcon
import com.workly.app.ui.components.EmptyState
import com.workly.app.ui.components.LocalSnackbarHostState
import com.workly.app.ui.components.ScreenHeader
import com.workly.app.ui.components.WorklyCard
import com.workly.app.ui.components.showMessage
import com.workly.app.ui.util.formatRate
import com.workly.app.ui.util.messageRes
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

@Immutable
data class WorkTypesUiState(
    val isLoading: Boolean = true,
    val workTypes: List<WorkTypeEntity> = emptyList(),
    val currency: String = "USD",
    val editing: WorkTypeEntity? = null,
    val showEditor: Boolean = false,
)

class WorkTypesViewModel(
    private val workTypeRepository: WorkTypeRepository,
) : ViewModel() {

    private val draft = MutableStateFlow(WorkTypesDraft())

    private val messages = Channel<WorklyError>(Channel.BUFFERED)
    val events = messages.receiveAsFlow()

    val uiState: StateFlow<WorkTypesUiState> = combine(
        workTypeRepository.workTypes,
        draft,
    ) { workTypes, draftState ->
        WorkTypesUiState(
            isLoading = false,
            workTypes = workTypes,
            currency = draftState.currency,
            editing = draftState.editing,
            showEditor = draftState.showEditor,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = WorkTypesUiState(),
    )

    init {
        viewModelScope.launch {
            val currency = AppGraph.settingsRepository.settings.first().currency
            draft.update { it.copy(currency = currency) }
        }
    }

    fun startAdd() = draft.update { it.copy(showEditor = true, editing = null) }

    fun startEdit(type: WorkTypeEntity) = draft.update { it.copy(showEditor = true, editing = type) }

    fun cancelEdit() = draft.update { it.copy(showEditor = false, editing = null) }

    fun save(name: String, rateText: String) {
        viewModelScope.launch {
            val currency = draft.value.currency
            val rateMinor = Money.parseToMinor(rateText, currency) ?: 0L
            val editing = draft.value.editing
            val result = if (editing == null) {
                workTypeRepository.add(name, rateMinor, currency)
            } else {
                workTypeRepository.update(editing, name, rateMinor)
            }
            result.fold(
                onSuccess = { draft.update { it.copy(showEditor = false, editing = null) } },
                onFailure = { throwable ->
                    messages.send((throwable as? WorklyException)?.error ?: WorklyError.UNKNOWN)
                },
            )
        }
    }

    fun delete(type: WorkTypeEntity) {
        viewModelScope.launch {
            workTypeRepository.delete(type).onFailure { throwable ->
                messages.send((throwable as? WorklyException)?.error ?: WorklyError.UNKNOWN)
            }
        }
    }

    private data class WorkTypesDraft(
        val currency: String = "USD",
        val editing: WorkTypeEntity? = null,
        val showEditor: Boolean = false,
    )

    companion object {
        private const val STOP_TIMEOUT_MS = 5_000L

        val Factory = viewModelFactory {
            initializer { WorkTypesViewModel(AppGraph.workTypeRepository) }
        }
    }
}

/** Manage the list of work types and their default rates. */
@Composable
fun WorkTypesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WorkTypesViewModel = viewModel(factory = WorkTypesViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val resources = androidx.compose.ui.platform.LocalResources.current
    var pendingDelete by remember { mutableStateOf<WorkTypeEntity?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { error ->
            snackbar.showMessage(scope, resources.getString(error.messageRes()))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.work_types_title),
            onBack = onBack,
            actions = {
                IconButton(onClick = viewModel::startAdd) {
                    Icon(
                        imageVector = AddIcon,
                        contentDescription = stringResource(R.string.work_types_add),
                    )
                }
            },
        )

        if (state.workTypes.isEmpty() && !state.isLoading) {
            EmptyState(
                title = stringResource(R.string.work_types_empty_title),
                message = stringResource(R.string.work_types_empty_message),
                actionLabel = stringResource(R.string.work_types_add),
                onAction = viewModel::startAdd,
            )
            return@Column
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            state.workTypes.forEach { type ->
                WorklyCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = type.name,
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = if (type.defaultHourlyRateMinor > 0L) {
                                    formatRate(type.defaultHourlyRateMinor, type.currency)
                                } else {
                                    stringResource(R.string.settings_not_set)
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        TextButton(onClick = { viewModel.startEdit(type) }) {
                            Text(stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { pendingDelete = type }) {
                            Icon(
                                imageVector = DeleteIcon,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = viewModel::startAdd,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.work_types_add))
            }
        }
    }

    if (state.showEditor) {
        WorkTypeEditorDialog(
            initial = state.editing,
            currency = state.currency,
            onDismiss = viewModel::cancelEdit,
            onSave = viewModel::save,
        )
    }

    pendingDelete?.let { type ->
        ConfirmDialog(
            title = stringResource(R.string.work_types_delete_title),
            message = stringResource(R.string.work_types_delete_message),
            confirmLabel = stringResource(R.string.action_delete),
            destructive = true,
            onConfirm = {
                pendingDelete = null
                viewModel.delete(type)
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun WorkTypeEditorDialog(
    initial: WorkTypeEntity?,
    currency: String,
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var rate by remember {
        mutableStateOf(
            initial?.defaultHourlyRateMinor
                ?.takeIf { it > 0L }
                ?.let { Money.toEditableString(it, currency) }
                .orEmpty(),
        )
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.work_types_add_title else R.string.work_types_edit_title,
                ),
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.work_types_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = rate,
                    onValueChange = { rate = it },
                    label = { Text(stringResource(R.string.work_types_rate)) },
                    suffix = { Text(currency) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name, rate) },
                enabled = name.isNotBlank(),
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
