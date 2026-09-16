package com.workly.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.workly.app.data.prefs.AppLanguage
import com.workly.app.data.prefs.DurationStyle
import com.workly.app.data.prefs.SUPPORTED_CURRENCIES
import com.workly.app.data.prefs.ThemePalette
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
import com.workly.app.ui.theme.paletteBrand
import com.workly.app.ui.util.decimalHours
import com.workly.app.ui.util.formatDuration
import com.workly.app.ui.util.formatRate
import com.workly.app.ui.util.rememberAppLocale
import com.workly.app.ui.util.message
import com.workly.app.ui.util.messageRes
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.CircleShape
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
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeColorDialog by remember { mutableStateOf(false) }
    var showDurationDialog by remember { mutableStateOf(false) }
    var showWeeklyTargetDialog by remember { mutableStateOf(false) }
    var showRestDaysDialog by remember { mutableStateOf(false) }
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
                title = stringResource(R.string.settings_weekly_target),
                value = if (state.settings.hasWeeklyTarget) {
                    stringResource(
                        R.string.settings_weekly_target_value,
                        formatDuration(state.settings.weeklyTargetMinutes),
                    )
                } else {
                    stringResource(R.string.settings_not_set)
                },
                supporting = stringResource(R.string.settings_weekly_target_hint),
                onClick = { showWeeklyTargetDialog = true },
            )
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_rest_days),
                value = restDaysSummary(state.settings.restDays, locale),
                supporting = stringResource(R.string.settings_rest_days_hint),
                onClick = { showRestDaysDialog = true },
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
                title = stringResource(R.string.settings_language),
                value = languageLabel(state.settings.language),
                onClick = { showLanguageDialog = true },
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
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_theme_color),
                value = paletteLabel(state.settings.themePalette),
                onClick = { showThemeColorDialog = true },
                leadingSwatch = paletteBrand(state.settings.themePalette).lightPrimary,
            )
            SettingsRowDivider()
            SettingRow(
                title = stringResource(R.string.settings_duration_format),
                value = durationStyleLabel(state.settings.durationStyle),
                onClick = { showDurationDialog = true },
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

    if (showThemeColorDialog) {
        ThemeColorDialog(
            selected = state.settings.themePalette,
            onDismiss = { showThemeColorDialog = false },
            onSelect = { palette ->
                showThemeColorDialog = false
                viewModel.setThemePalette(palette)
            },
        )
    }

    if (showDurationDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_duration_format),
            options = DurationStyle.entries,
            selected = state.settings.durationStyle,
            labelOf = { style -> durationStyleLabel(style) },
            onDismiss = { showDurationDialog = false },
            onSelect = { style ->
                showDurationDialog = false
                viewModel.setDurationStyle(style)
            },
        )
    }

    if (showWeeklyTargetDialog) {
        WeeklyTargetDialog(
            initialMinutes = state.settings.weeklyTargetMinutes,
            onDismiss = { showWeeklyTargetDialog = false },
            onConfirm = { text ->
                showWeeklyTargetDialog = false
                viewModel.setWeeklyTargetHours(text)
            },
        )
    }

    if (showRestDaysDialog) {
        RestDaysDialog(
            selected = state.settings.restDays,
            locale = locale,
            onDismiss = { showRestDaysDialog = false },
            onConfirm = { days ->
                showRestDaysDialog = false
                viewModel.setRestDays(days)
            },
        )
    }

    if (showLanguageDialog) {
        ChoiceDialog(
            title = stringResource(R.string.settings_language),
            options = AppLanguage.entries,
            selected = state.settings.language,
            labelOf = { language -> languageLabel(language) },
            onDismiss = { showLanguageDialog = false },
            onSelect = { language ->
                showLanguageDialog = false
                viewModel.setLanguage(language)
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
    leadingSwatch: Color? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingSwatch != null) {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(leadingSwatch),
            )
            Spacer(Modifier.size(12.dp))
        }
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



/**
 * Shows each language in its own language, which is what users expect from a
 * language picker.
 */
@Composable
private fun languageLabel(language: AppLanguage): String = stringResource(
    when (language) {
        AppLanguage.SYSTEM -> R.string.settings_language_system
        AppLanguage.ENGLISH -> R.string.language_name_english
        AppLanguage.JAPANESE -> R.string.language_name_japanese
        AppLanguage.CHINESE -> R.string.language_name_chinese
    },
)

// ---------------------------------------------------------------- helpers

@Composable
private fun paletteLabel(palette: ThemePalette): String = stringResource(
    when (palette) {
        ThemePalette.INDIGO -> R.string.theme_palette_indigo
        ThemePalette.TEAL -> R.string.theme_palette_teal
        ThemePalette.FOREST -> R.string.theme_palette_forest
        ThemePalette.SUNSET -> R.string.theme_palette_sunset
        ThemePalette.ROSE -> R.string.theme_palette_rose
        ThemePalette.MONO -> R.string.theme_palette_mono
    },
)

@Composable
private fun durationStyleLabel(style: DurationStyle): String = stringResource(
    when (style) {
        DurationStyle.HOURS_MINUTES -> R.string.duration_format_hours_minutes
        DurationStyle.DECIMAL_HOURS -> R.string.duration_format_decimal
    },
)

@Composable
private fun restDaysSummary(days: Set<DayOfWeek>, locale: java.util.Locale): String {
    if (days.isEmpty()) return stringResource(R.string.rest_days_none)
    return remember(days, locale) {
        DayOfWeek.entries
            .filter { it in days }
            .joinToString(", ") { it.getDisplayName(TextStyle.SHORT, locale) }
    }
}

/** Colour picker showing an actual swatch of each palette. */
@Composable
private fun ThemeColorDialog(
    selected: ThemePalette,
    onSelect: (ThemePalette) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_theme_color)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                ThemePalette.entries.forEach { palette ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = palette == selected,
                                role = Role.RadioButton,
                                onClick = { onSelect(palette) },
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = palette == selected, onClick = null)
                        Spacer(Modifier.size(12.dp))
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(paletteBrand(palette).lightPrimary),
                        )
                        Spacer(Modifier.size(12.dp))
                        Text(text = paletteLabel(palette), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) }
        },
    )
}

/** Weekday multi-select for the days the user does not work. */
@Composable
private fun RestDaysDialog(
    selected: Set<DayOfWeek>,
    locale: java.util.Locale,
    onConfirm: (Set<DayOfWeek>) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(selected) { mutableStateOf(selected) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_rest_days)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.settings_rest_days_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                DayOfWeek.entries.forEach { day ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = day in draft,
                                role = Role.Checkbox,
                                onValueChange = { checked ->
                                    draft = if (checked) draft + day else draft - day
                                },
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = day in draft, onCheckedChange = null)
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = day.getDisplayName(TextStyle.FULL, locale),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(draft) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** The total hours the user aims to work in a week. */
@Composable
private fun WeeklyTargetDialog(
    initialMinutes: Long,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember {
        mutableStateOf(if (initialMinutes > 0L) decimalHours(initialMinutes) else "")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.weekly_target_dialog_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text(stringResource(R.string.weekly_target_field)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}
