package com.workly.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workly.app.BuildConfig
import com.workly.app.R
import com.workly.app.data.prefs.SUPPORTED_CURRENCIES
import com.workly.app.data.prefs.ThemeMode
import com.workly.app.domain.Money
import com.workly.app.domain.WorklyError
import com.workly.app.ui.components.ChevronRightIcon
import com.workly.app.ui.components.ConfirmDialog
import com.workly.app.ui.components.LocalSnackbarHostState
import com.workly.app.ui.components.ScreenHeader
import com.workly.app.ui.components.SectionLabel
import com.workly.app.ui.components.WorklyCard
import com.workly.app.ui.components.showMessage
import com.workly.app.ui.util.rememberAppLocale
import com.workly.app.ui.util.message
import com.workly.app.ui.util.messageRes
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle

/**
 * Settings: rates, currency, work types, appearance, backup and about.
 *
 * Import and export go through the Storage Access Framework, so the user always
 * picks the destination themselves and Workly never needs a storage permission.
 */
@Composable
fun SettingsScreen(
    onOpenWorkTypes: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    val locale = rememberAppLocale()
    val resources = LocalResources.current

    var showRateDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showFirstDayDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val today = remember { LocalDate.now().toString() }
    val exportCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri -> uri?.let(viewModel::exportCsv) }
    val exportJsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportJson) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onImportPicked) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            val text = when (event) {
                is SettingsEvent.Error -> resources.getString(event.error.messageRes())
                is SettingsEvent.Exported -> resources.getQuantityString(
                    if (event.asCsv) R.plurals.export_csv_done else R.plurals.export_json_done,
                    event.count,
                    event.count,
                )

                SettingsEvent.NothingToExport -> resources.getString(R.string.export_nothing)
                is SettingsEvent.Imported -> resources.getQuantityString(
                    R.plurals.import_done,
                    event.sessions,
                    event.sessions,
                    event.workTypes,
                )

                SettingsEvent.NothingNewImported -> resources.getString(R.string.import_nothing_new)
                SettingsEvent.AllDataDeleted -> resources.getString(R.string.delete_all_done)
            }
            snackbar.showMessage(scope, text)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        ScreenHeader(title = stringResource(R.string.settings_title))

        if (state.isBusy) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            }
        }

        SectionLabel(
            text = stringResource(R.string.settings_section_general),
            modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 6.dp),
        )
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.settings_default_rate),
                value = if (state.settings.defaultHourlyRateMinor > 0L) {
                    com.workly.app.ui.util.formatRate(
                        state.settings.defaultHourlyRateMinor,
                        state.settings.currency,
                    )
                } else {
                    stringResource(R.string.settings_not_set)
                },
                supporting = stringResource(R.string.settings_default_rate_hint),
                onClick = { showRateDialog = true },
            )
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_currency),
                value = state.settings.currency,
                onClick = { showCurrencyDialog = true },
            )
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_work_types),
                value = pluralStringResource(R.plurals.settings_work_types_summary, state.workTypeCount, state.workTypeCount),
                onClick = onOpenWorkTypes,
                trailingChevron = true,
            )
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_first_day),
                value = state.settings.firstDayOfWeek.getDisplayName(TextStyle.FULL, locale),
                onClick = { showFirstDayDialog = true },
            )
        }

        SectionLabel(
            text = stringResource(R.string.settings_section_appearance),
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 6.dp),
        )
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.settings_theme),
                value = stringResource(
                    when (state.settings.themeMode) {
                        ThemeMode.SYSTEM -> R.string.settings_theme_system
                        ThemeMode.LIGHT -> R.string.settings_theme_light
                        ThemeMode.DARK -> R.string.settings_theme_dark
                    },
                ),
                onClick = { showThemeDialog = true },
            )
        }

        SectionLabel(
            text = stringResource(R.string.settings_section_data),
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 6.dp),
        )
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.settings_export_csv),
                supporting = stringResource(R.string.settings_export_csv_hint),
                onClick = { exportCsvLauncher.launch("workly-records-$today.csv") },
            )
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_export_json),
                supporting = stringResource(R.string.settings_export_json_hint),
                onClick = { exportJsonLauncher.launch("workly-backup-$today.json") },
            )
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_import_json),
                supporting = stringResource(R.string.settings_import_json_hint),
                onClick = { importLauncher.launch(arrayOf("*/*")) },
            )
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_delete_all),
                supporting = stringResource(R.string.settings_delete_all_hint),
                onClick = { showDeleteDialog = true },
                destructive = true,
            )
        }

        SectionLabel(
            text = stringResource(R.string.settings_section_about),
            modifier = Modifier.padding(start = 20.dp, top = 24.dp, bottom = 6.dp),
        )
        SettingsCard {
            SettingRow(
                title = stringResource(R.string.settings_about),
                value = stringResource(R.string.settings_version, BuildConfig.VERSION_NAME),
                onClick = onOpenAbout,
                trailingChevron = true,
            )
        }
    }

    if (showRateDialog) {
        RateEditDialog(
            currency = state.settings.currency,
            initialMinor = state.settings.defaultHourlyRateMinor,
            onDismiss = { showRateDialog = false },
            onConfirm = { text ->
                showRateDialog = false
                viewModel.setDefaultHourlyRate(text)
            },
        )
    }

    if (showCurrencyDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_currency),
            options = SUPPORTED_CURRENCIES,
            selected = state.settings.currency,
            labelOf = { code -> "$code  ${Money.symbolOf(code, locale)}" },
            onDismiss = { showCurrencyDialog = false },
            onSelect = { code ->
                showCurrencyDialog = false
                viewModel.setCurrency(code)
            },
        )
    }

    if (showThemeDialog) {
        val options = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
        ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = options,
            selected = state.settings.themeMode,
            labelOf = { mode ->
                when (mode) {
                    ThemeMode.SYSTEM -> stringResource(R.string.settings_theme_system)
                    ThemeMode.LIGHT -> stringResource(R.string.settings_theme_light)
                    ThemeMode.DARK -> stringResource(R.string.settings_theme_dark)
                }
            },
            onDismiss = { showThemeDialog = false },
            onSelect = { mode ->
                showThemeDialog = false
                viewModel.setThemeMode(mode)
            },
        )
    }

    if (showFirstDayDialog) {
        val options = listOf(
            DayOfWeek.MONDAY,
            DayOfWeek.SUNDAY,
            DayOfWeek.SATURDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
        )
        ChoiceDialog(
            title = stringResource(R.string.settings_first_day),
            options = options,
            selected = state.settings.firstDayOfWeek,
            labelOf = { day -> day.getDisplayName(TextStyle.FULL, locale) },
            onDismiss = { showFirstDayDialog = false },
            onSelect = { day ->
                showFirstDayDialog = false
                viewModel.setFirstDayOfWeek(day)
            },
        )
    }

    val preview = state.importPreview
    if (preview != null) {
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.import_preview_title)) },
            text = {
                Column {
                    Text(pluralStringResource(R.plurals.import_preview_sessions, preview.sessionCount, preview.sessionCount))
                    Text(pluralStringResource(R.plurals.import_preview_work_types, preview.workTypeCount, preview.workTypeCount))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = pluralStringResource(R.plurals.import_preview_new, preview.newSessionCount, preview.newSessionCount),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (preview.duplicateSessionCount > 0) {
                        Text(
                            text = pluralStringResource(
                                R.plurals.import_preview_duplicates,
                                preview.duplicateSessionCount,
                                preview.duplicateSessionCount,
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmImport) {
                    Text(stringResource(R.string.import_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelImport) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }

    if (showDeleteDialog) {
        ConfirmDialog(
            title = stringResource(R.string.delete_all_title),
            message = stringResource(R.string.delete_all_message),
            confirmLabel = stringResource(R.string.delete_all_confirm),
            destructive = true,
            onConfirm = {
                showDeleteDialog = false
                viewModel.deleteAllData()
            },
            onDismiss = { showDeleteDialog = false },
        )
    }
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    WorklyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
    ) {
        content()
    }
}

@Composable
private fun SettingsRowDivider() {
    androidx.compose.material3.HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    )
}

@Composable
private fun SettingRow(
    title: String,
    onClick: () -> Unit,
    value: String? = null,
    supporting: String? = null,
    trailingChevron: Boolean = false,
    destructive: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (destructive) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.End,
            )
        }
        if (trailingChevron) {
            Spacer(Modifier.size(4.dp))
            Icon(
                imageVector = ChevronRightIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun RateEditDialog(
    currency: String,
    initialMinor: Long,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember {
        mutableStateOf(if (initialMinor > 0L) Money.toEditableString(initialMinor, currency) else "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_default_rate)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.rate_dialog_field)) },
                suffix = { Text(currency) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = text.isNotBlank()) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    labelOf: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(option) },
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Spacer(Modifier.size(12.dp))
                        Text(text = labelOf(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}


