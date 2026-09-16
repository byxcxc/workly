package com.workly.app.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workly.app.R
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.incomeMinor
import com.workly.app.data.local.entity.workedMinutes
import com.workly.app.domain.CalendarGrid
import com.workly.app.domain.PeriodStats
import com.workly.app.ui.components.AddIcon
import com.workly.app.ui.components.BackIcon
import com.workly.app.ui.components.CalendarIcon
import com.workly.app.ui.components.ChevronRightIcon
import com.workly.app.ui.components.EmptyState
import com.workly.app.ui.components.ListIcon
import com.workly.app.ui.components.ScreenHeader
import com.workly.app.ui.components.SessionRow
import com.workly.app.ui.components.WorklyCard
import com.workly.app.ui.theme.WorklyTheme
import com.workly.app.ui.util.formatDateMedium
import com.workly.app.ui.util.formatDuration
import com.workly.app.ui.util.formatMoney
import com.workly.app.ui.util.formatMonthYear
import com.workly.app.ui.util.formatWeekdayNarrow
import com.workly.app.ui.util.rememberAppLocale
import java.time.LocalDate
import java.time.YearMonth

/**
 * Records has two views over the same data: a chronological list and a calendar.
 * Both live on this screen so the bottom bar keeps only four destinations.
 */
@Composable
fun RecordsScreen(
    onOpenRecord: (Long) -> Unit,
    onAddRecord: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: RecordsViewModel = viewModel(factory = RecordsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(
            title = stringResource(R.string.records_title),
            subtitle = if (state.totalCount > 0) {
                pluralStringResource(R.plurals.records_session_count, state.totalCount, state.totalCount)
            } else {
                null
            },
            actions = {
                IconButton(onClick = onAddRecord) {
                    Icon(
                        imageVector = AddIcon,
                        contentDescription = stringResource(R.string.records_add),
                    )
                }
            },
        )

        if (state.isLoading) {
            Spacer(Modifier.height(40.dp))
        } else if (state.totalCount == 0) {
            EmptyState(
                title = stringResource(R.string.records_empty_title),
                message = stringResource(R.string.records_empty_message),
                icon = CalendarIcon,
                actionLabel = stringResource(R.string.records_add),
                onAction = onAddRecord,
            )
        } else {
            ViewModeSelector(
                mode = state.viewMode,
                onSelect = viewModel::setViewMode,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(8.dp))
            when (state.viewMode) {
                RecordsViewMode.LIST -> RecordsList(state = state, onOpenRecord = onOpenRecord)
                RecordsViewMode.CALENDAR -> CalendarPanel(
                    state = state,
                    onPreviousMonth = viewModel::showPreviousMonth,
                    onNextMonth = viewModel::showNextMonth,
                    onSelectDate = viewModel::selectDate,
                    onOpenRecord = onOpenRecord,
                )
            }
        }
    }
}

@Composable
private fun ViewModeSelector(
    mode: RecordsViewMode,
    onSelect: (RecordsViewMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        RecordsViewMode.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = entry == mode,
                onClick = { onSelect(entry) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = RecordsViewMode.entries.size),
                icon = {
                    Icon(
                        imageVector = if (entry == RecordsViewMode.LIST) ListIcon else CalendarIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                },
                label = {
                    Text(
                        stringResource(
                            if (entry == RecordsViewMode.LIST) {
                                R.string.records_view_list
                            } else {
                                R.string.records_view_calendar
                            },
                        ),
                    )
                },
            )
        }
    }
}

@Composable
private fun RecordsList(state: RecordsUiState, onOpenRecord: (Long) -> Unit) {
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        state.sortedDates.forEach { date ->
            val daySessions = state.sessionsByDate[date].orEmpty()
            item(key = "header-$date") {
                DateHeader(date = date, sessions = daySessions, currency = state.currency)
            }
            items(
                count = daySessions.size,
                key = { index -> daySessions[index].id },
            ) { index ->
                val session = daySessions[index]
                SessionRow(session = session, onClick = { onOpenRecord(session.id) })
            }
        }
    }
}

@Composable
private fun DateHeader(
    date: LocalDate,
    sessions: List<WorkSessionEntity>,
    currency: String,
) {
    val minutes = sessions.sumOf { it.workedMinutes }
    val income = sessions.sumOf { it.incomeMinor }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = formatDateMedium(date),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatDuration(minutes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = " · ",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatMoney(income, currency),
            style = MaterialTheme.typography.titleSmall,
            color = WorklyTheme.accents.income,
        )
    }
}

@Composable
private fun CalendarPanel(
    state: RecordsUiState,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onOpenRecord: (Long) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 96.dp)) {
        item {
            MonthHeader(
                month = state.month,
                onPrevious = onPreviousMonth,
                onNext = onNextMonth,
            )
        }
        item {
            MonthSummary(stats = state.monthStats, currency = state.currency)
        }
        item {
            MonthGrid(
                month = state.month,
                firstDayOfWeek = state.firstDayOfWeek,
                datesWithWork = state.datesWithWork,
                restDays = state.restDays,
                selectedDate = state.selectedDate,
                onSelectDate = onSelectDate,
            )
        }
        val selected = state.selectedDate
        if (selected != null) {
            item {
                DaySummary(
                    date = selected,
                    stats = state.dayStats,
                    currency = state.currency,
                )
            }
            if (state.selectedDateSessions.isEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.records_no_work_on_day),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }
            } else {
                items(
                    count = state.selectedDateSessions.size,
                    key = { index -> state.selectedDateSessions[index].id },
                ) { index ->
                    val session = state.selectedDateSessions[index]
                    SessionRow(session = session, onClick = { onOpenRecord(session.id) })
                }
            }
        }
    }
}

@Composable
private fun MonthHeader(
    month: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = BackIcon,
                contentDescription = stringResource(R.string.cd_previous_month),
            )
        }
        Text(
            text = formatMonthYear(month),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = ChevronRightIcon,
                contentDescription = stringResource(R.string.cd_next_month),
            )
        }
    }
}

@Composable
private fun MonthSummary(stats: PeriodStats, currency: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = formatDuration(stats.totalMinutes),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = formatMoney(stats.totalIncomeMinor, currency),
            style = MaterialTheme.typography.titleMedium,
            color = WorklyTheme.accents.income,
        )
        Text(
            text = pluralStringResource(R.plurals.records_work_days, stats.workDays, stats.workDays),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    firstDayOfWeek: java.time.DayOfWeek,
    datesWithWork: Set<LocalDate>,
    restDays: Set<java.time.DayOfWeek>,
    selectedDate: LocalDate?,
    onSelectDate: (LocalDate) -> Unit,
) {
    val locale = rememberAppLocale()
    val weekdayNames = formatWeekdayNarrow(locale)
    val order = CalendarGrid.weekdayOrder(firstDayOfWeek)
    val weeks = CalendarGrid.weeks(month, firstDayOfWeek)

    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            order.forEachIndexed { index, day ->
                Text(
                    text = weekdayNames[day.value - 1],
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 6.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        }
        weeks.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        hasWork = date != null && date in datesWithWork,
                        isRestDay = date != null && date.dayOfWeek in restDays,
                        isSelected = date != null && date == selectedDate,
                        isToday = date == LocalDate.now(),
                        onSelect = { date?.let(onSelectDate) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate?,
    hasWork: Boolean,
    isRestDay: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (date == null) {
        Box(modifier = modifier.height(52.dp))
        return
    }

    val label = formatDateMedium(date)
    val description = when {
        hasWork -> stringResource(R.string.cd_calendar_day_with_work, label)
        isRestDay -> stringResource(R.string.cd_calendar_rest_day, label)
        else -> stringResource(R.string.cd_calendar_day, label)
    }
    val background = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.surfaceContainerHighest
        else -> MaterialTheme.colorScheme.surface
    }
    val contentColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        // A day off is drawn quietly, but the day number is still readable.
        isRestDay -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .height(52.dp)
            .padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(background)
                .clickable(onClick = onSelect)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (hasWork) FontWeight.SemiBold else FontWeight.Normal,
                    color = contentColor,
                )
                if (hasWork) {
                    Spacer(Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    WorklyTheme.accents.income
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DaySummary(date: LocalDate, stats: PeriodStats, currency: String) {
    WorklyCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Text(
            text = formatDateMedium(date),
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(12.dp))
        SummaryLine(
            label = stringResource(R.string.editor_worked_time),
            value = formatDuration(stats.totalMinutes),
        )
        Spacer(Modifier.height(6.dp))
        SummaryLine(
            label = stringResource(R.string.editor_income),
            value = formatMoney(stats.totalIncomeMinor, currency),
            valueColor = WorklyTheme.accents.income,
        )
    }
}

@Composable
private fun SummaryLine(
    label: String,
    value: String,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium, color = valueColor)
    }
}
