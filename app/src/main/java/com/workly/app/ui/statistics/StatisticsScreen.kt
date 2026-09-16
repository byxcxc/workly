package com.workly.app.ui.statistics

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workly.app.R
import com.workly.app.domain.PeriodStats
import com.workly.app.domain.TimeBucket
import com.workly.app.ui.components.EmptyState
import com.workly.app.ui.components.ScreenHeader
import com.workly.app.ui.components.SectionLabel
import com.workly.app.ui.components.StatBlock
import com.workly.app.ui.components.WorklyCard
import com.workly.app.ui.components.BarChartIcon
import com.workly.app.ui.theme.WorklyTheme
import com.workly.app.ui.util.formatDateShort
import com.workly.app.ui.util.formatDuration
import com.workly.app.ui.util.decimalHours
import com.workly.app.ui.util.formatMoney
import com.workly.app.ui.util.formatMoneyCompact
import com.workly.app.ui.util.formatMonthShort
import com.workly.app.ui.util.formatRate
import com.workly.app.ui.util.formatWeekdayShort
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.ceil

/** Statistics for a week, a month, a year or a custom range, plus two trends. */
@Composable
fun StatisticsScreen(
    modifier: Modifier = Modifier,
    viewModel: StatisticsViewModel = viewModel(factory = StatisticsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pickStartDate by remember { mutableStateOf(false) }
    var pickEndDate by remember { mutableStateOf(false) }
    var pendingStart by remember { mutableStateOf<LocalDate?>(null) }

    Column(modifier = modifier.fillMaxSize()) {
        ScreenHeader(title = stringResource(R.string.stats_title))

        if (!state.isLoading && !state.hasAnyRecord) {
            EmptyState(
                title = stringResource(R.string.stats_empty_title),
                message = stringResource(R.string.stats_empty_message),
                icon = BarChartIcon,
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                StatsPeriodOption.entries.forEach { option ->
                    FilterChip(
                        selected = option == state.option,
                        onClick = {
                            if (option == StatsPeriodOption.CUSTOM) {
                                pickStartDate = true
                            } else {
                                viewModel.selectOption(option)
                            }
                        },
                        label = {
                            Text(
                                stringResource(
                                    when (option) {
                                        StatsPeriodOption.WEEK -> R.string.stats_range_week
                                        StatsPeriodOption.MONTH -> R.string.stats_range_month
                                        StatsPeriodOption.YEAR -> R.string.stats_range_year
                                        StatsPeriodOption.CUSTOM -> R.string.stats_range_custom
                                    },
                                ),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(
                    R.string.stats_range_label,
                    formatDateShort(state.range.start),
                    formatDateShort(state.range.endInclusive),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))
            MetricsCard(
                stats = state.stats,
                currency = state.currency,
                plannedWorkDays = state.plannedWorkDays,
                restDayCount = state.restDayCount,
            )

            if (state.hasTarget) {
                Spacer(Modifier.height(16.dp))
                TargetCard(
                    workedMinutes = state.stats.totalMinutes,
                    targetMinutes = state.targetMinutes,
                    remainingMinutes = state.targetRemainingMinutes,
                    progress = state.targetProgress,
                )
            }

            Spacer(Modifier.height(16.dp))
            TrendCard(
                title = stringResource(R.string.stats_income_trend),
                buckets = state.buckets,
                mode = state.bucketMode,
                valueOf = { it.incomeMinor.toFloat() },
                valueLabel = { formatMoneyCompact(it.toLong(), state.currency) },
                totalLabel = formatMoney(state.stats.totalIncomeMinor, state.currency),
                barColor = WorklyTheme.accents.chart,
            )

            Spacer(Modifier.height(16.dp))
            TrendCard(
                title = stringResource(R.string.stats_hours_trend),
                buckets = state.buckets,
                mode = state.bucketMode,
                valueOf = { it.minutes.toFloat() },
                valueLabel = {
                    stringResource(
                        R.string.duration_compact_hours,
                        decimalHours(it.toLong()),
                    )
                },
                totalLabel = formatDuration(state.stats.totalMinutes),
                barColor = WorklyTheme.accents.chartSecondary,
            )

            if (state.stats.sessionCount > 0) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = pluralStringResource(R.plurals.stats_session_count, state.stats.sessionCount, state.stats.sessionCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    if (pickStartDate) {
        DatePickerModal(
            initial = state.customStart ?: state.range.start,
            onDismiss = { pickStartDate = false },
            onConfirm = { date ->
                pickStartDate = false
                pendingStart = date
                pickEndDate = true
            },
        )
    }

    if (pickEndDate) {
        DatePickerModal(
            initial = state.customEnd ?: LocalDate.now(),
            onDismiss = { pickEndDate = false },
            onConfirm = { date ->
                pickEndDate = false
                viewModel.setCustomRange(pendingStart, date)
                pendingStart = null
            },
        )
    }
}

@Composable
private fun MetricsCard(
    stats: PeriodStats,
    currency: String,
    plannedWorkDays: Int,
    restDayCount: Int,
) {
    WorklyCard(modifier = Modifier.fillMaxWidth()) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                label = stringResource(R.string.stats_total_time),
                value = formatDuration(stats.totalMinutes),
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                label = stringResource(R.string.stats_total_income),
                value = formatMoney(stats.totalIncomeMinor, currency),
                valueColor = WorklyTheme.accents.income,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                label = stringResource(R.string.stats_work_days),
                value = stats.workDays.toString(),
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                label = stringResource(R.string.stats_avg_daily_time),
                value = formatDuration(stats.averageDailyMinutes),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                label = stringResource(R.string.stats_planned_days),
                value = stringResource(
                    R.string.stats_planned_days_value,
                    stats.workDays,
                    plannedWorkDays,
                ),
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                label = stringResource(R.string.stats_rest_days),
                value = restDayCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBlock(
                label = stringResource(R.string.stats_avg_daily_income),
                value = formatMoney(stats.averageDailyIncomeMinor, currency),
                valueColor = WorklyTheme.accents.income,
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                label = stringResource(R.string.stats_avg_hourly_rate),
                value = formatRate(stats.averageHourlyRateMinor, currency),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Progress towards the weekly target, scaled to the selected period. */
@Composable
private fun TargetCard(
    workedMinutes: Long,
    targetMinutes: Long,
    remainingMinutes: Long,
    progress: Float,
) {
    WorklyCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionLabel(
                text = stringResource(R.string.stats_target),
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(
                    R.string.stats_target_progress,
                    formatDuration(workedMinutes),
                    formatDuration(targetMinutes),
                ),
                style = MaterialTheme.typography.titleMedium,
            )
        }
        Spacer(Modifier.height(12.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (remainingMinutes == 0L) {
                stringResource(R.string.home_target_reached)
            } else {
                stringResource(R.string.home_target_remaining, formatDuration(remainingMinutes))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrendCard(
    title: String,
    buckets: List<TimeBucket>,
    mode: BucketMode,
    valueOf: (TimeBucket) -> Float,
    valueLabel: @Composable (Float) -> String,
    totalLabel: String,
    barColor: androidx.compose.ui.graphics.Color,
) {
    val values = buckets.map(valueOf)
    val hasData = values.any { it > 0f }
    val labels = chartLabels(buckets, mode)

    WorklyCard(modifier = Modifier.fillMaxWidth()) {
        SectionLabel(title)
        Spacer(Modifier.height(12.dp))
        if (!hasData) {
            ChartPlaceholder()
        } else {
            SimpleBarChart(
                values = values,
                labels = labels,
                barColor = barColor,
                contentDescription = "$title, $totalLabel",
                // `map` is inline, so the composable label lambda can be used here.
                valueLabels = values.map { valueLabel(it) },
            )
        }
    }
}

/**
 * Labels for the chart axis. Only a handful are shown so the axis never becomes
 * unreadable, no matter how many buckets there are.
 */
@Composable
private fun chartLabels(buckets: List<TimeBucket>, mode: BucketMode): List<String> {
    if (buckets.isEmpty()) return emptyList()
    val step = ceil(buckets.size / MAX_LABELS.toDouble()).toInt().coerceAtLeast(1)
    return buckets.mapIndexed { index, bucket ->
        when {
            index % step != 0 && index != buckets.lastIndex -> ""
            mode == BucketMode.MONTH -> formatMonthShort(bucket.labelDate)
            buckets.size <= 7 -> formatWeekdayShort(bucket.labelDate)
            else -> bucket.labelDate.dayOfMonth.toString()
        }
    }
}

private const val MAX_LABELS = 7

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerModal(
    initial: LocalDate,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onConfirm(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
            ) { Text(stringResource(R.string.action_set)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    ) {
        DatePicker(state = pickerState)
    }
}
