package com.workly.app.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.workly.app.R
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.domain.Money
import com.workly.app.domain.PeriodStats
import com.workly.app.domain.WorklyError
import com.workly.app.ui.components.CheckIcon
import com.workly.app.ui.components.LiveDot
import com.workly.app.ui.components.LocalSnackbarHostState
import com.workly.app.ui.components.PlayIcon
import com.workly.app.ui.components.SectionLabel
import com.workly.app.ui.components.SessionRow
import com.workly.app.ui.components.StatBlock
import com.workly.app.ui.components.WorklyCard
import com.workly.app.ui.components.showMessage
import com.workly.app.ui.theme.WorklyTheme
import com.workly.app.ui.util.formatDateFull
import com.workly.app.ui.util.formatDuration
import com.workly.app.ui.util.formatDurationWithSeconds
import com.workly.app.ui.util.formatMoney
import com.workly.app.ui.util.formatTime
import com.workly.app.ui.util.message
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * The dashboard.
 *
 * Within a few seconds of opening the app the user should know how long they
 * worked today, how much they earned, whether a session is running and how the
 * week is going.
 */
@Composable
fun HomeScreen(
    onOpenRecord: (Long) -> Unit,
    onFinishWork: (Long) -> Unit,
    onSeeAllRecords: () -> Unit,
    onAddRecord: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = LocalSnackbarHostState.current
    val scope = rememberCoroutineScope()
    var pendingError by remember { mutableStateOf<WorklyError?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { pendingError = it }
    }

    val errorMessage = pendingError?.message()
    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbar.showMessage(scope, errorMessage)
            pendingError = null
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 32.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(greetingRes()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatDateFull(LocalDate.now()),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        Spacer(Modifier.height(20.dp))

        if (state.isLoading) {
            Spacer(Modifier.height(80.dp))
        } else if (!state.hasAnySession && state.activeSession == null) {
            FirstRunPanel(
                onStart = viewModel::startWork,
                onAddRecord = onAddRecord,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        } else {
            HeroCard(
                state = state,
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .animateContentSize(),
            )
            Spacer(Modifier.height(16.dp))
            if (state.activeSession == null && state.workTypes.size > 1) {
                WorkTypeChips(
                    state = state,
                    onSelect = viewModel::selectWorkType,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                Spacer(Modifier.height(16.dp))
            }
            PrimaryAction(
                isWorking = state.activeSession != null,
                onStart = viewModel::startWork,
                onFinish = { state.activeSession?.let { onFinishWork(it.id) } },
            )
            if (state.activeSession == null) {
                SecondaryAction(
                    label = stringResource(R.string.home_add_record),
                    onClick = onAddRecord,
                )
            }
            Spacer(Modifier.height(24.dp))
            WeekCard(
                state = state,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(28.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionLabel(
                    text = stringResource(R.string.home_recent_records),
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onSeeAllRecords) {
                    Text(stringResource(R.string.home_see_all))
                }
            }
            if (state.recentSessions.isEmpty()) {
                Text(
                    text = stringResource(R.string.home_none_yet_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                )
            } else {
                state.recentSessions.forEach { session ->
                    SessionRow(
                        session = session,
                        onClick = { onOpenRecord(session.id) },
                    )
                }
            }
        }
    }

    if (state.showRatePrompt) {
        RatePromptDialog(
            currency = state.currency,
            onConfirm = viewModel::startWorkWithRate,
            onDismiss = viewModel::dismissRatePrompt,
        )
    }
}

@Composable
private fun HeroCard(state: HomeUiState, modifier: Modifier = Modifier) {
    AnimatedContent(
        targetState = state.activeSession,
        transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
        label = "hero",
        modifier = modifier,
    ) { session ->
        if (session != null) {
            WorkingNowCard(session = session)
        } else {
            TodayCard(today = state.today, currency = state.currency)
        }
    }
}

@Composable
internal fun TodayCard(today: PeriodStats, currency: String) {
    WorklyCard(modifier = Modifier.fillMaxWidth()) {
        StatBlock(
            label = stringResource(R.string.home_worked_today),
            value = formatDuration(today.totalMinutes),
        )
        Spacer(Modifier.height(20.dp))
        StatBlock(
            label = stringResource(R.string.home_earned_today),
            value = formatMoney(today.totalIncomeMinor, currency),
            valueColor = WorklyTheme.accents.income,
        )
    }
}

@Composable
internal fun WorkingNowCard(session: WorkSessionEntity) {
    WorklyCard(
        modifier = Modifier.fillMaxWidth(),
        containerColor = WorklyTheme.accents.workingContainer,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LiveDot()
            Spacer(Modifier.width(8.dp))
            SectionLabel(
                text = stringResource(R.string.home_working_now),
                color = WorklyTheme.accents.onWorkingContainer,
            )
        }
        // Only this block re-reads the ticking clock, so the rest of the card is
        // never recomposed while the timer runs.
        LiveValues(
            session = session,
            startedAt = stringResource(R.string.home_started_at, formatTime(session.startTime)),
        )
    }
}

/**
 * The once-a-second part of the running card.
 *
 * The elapsed time is derived from the start instant, so the display stays
 * correct even if the app was in the background for an hour.
 */
@Composable
private fun LiveValues(session: WorkSessionEntity, startedAt: String) {
    val elapsedSeconds = rememberElapsedSeconds(session.startTime)
    val elapsedText = formatDurationWithSeconds(elapsedSeconds)
    val income = formatMoney(
        Money.incomeMinor(session.hourlyRateMinor, elapsedSeconds / 60),
        session.currency,
    )
    val description = stringResource(R.string.cd_working_timer, "$elapsedText, $income")

    Column(
        modifier = Modifier.semantics(mergeDescendants = true) { contentDescription = description },
    ) {
        Spacer(Modifier.height(10.dp))
        Text(
            text = elapsedText,
            style = MaterialTheme.typography.displayLarge,
            color = WorklyTheme.accents.onWorkingContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = startedAt,
            style = MaterialTheme.typography.bodyMedium,
            color = WorklyTheme.accents.onWorkingContainer,
        )
        Spacer(Modifier.height(18.dp))
        SectionLabel(
            text = stringResource(R.string.home_live_earnings),
            color = WorklyTheme.accents.onWorkingContainer,
        )
        Text(
            text = income,
            style = MaterialTheme.typography.displaySmall,
            color = WorklyTheme.accents.onWorkingContainer,
        )
    }
}

@Composable
internal fun WeekCard(state: HomeUiState, modifier: Modifier = Modifier) {
    val week = state.week
    val currency = state.currency
    WorklyCard(modifier = modifier.fillMaxWidth()) {
        SectionLabel(stringResource(R.string.home_this_week))
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatBlock(
                label = stringResource(R.string.editor_worked_time),
                value = formatDuration(week.totalMinutes),
                modifier = Modifier.weight(1f),
            )
            StatBlock(
                label = stringResource(R.string.editor_income),
                value = formatMoney(week.totalIncomeMinor, currency),
                valueColor = WorklyTheme.accents.income,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.hasWeekTarget) {
            Spacer(Modifier.height(14.dp))
            LinearProgressIndicator(
                progress = { state.weekProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = if (state.weekRemainingMinutes == 0L) {
                    stringResource(R.string.home_target_reached)
                } else {
                    stringResource(
                        R.string.home_target_progress,
                        formatDuration(week.totalMinutes),
                        formatDuration(state.weekTargetMinutes),
                    ) + " · " + stringResource(
                        R.string.home_target_remaining,
                        formatDuration(state.weekRemainingMinutes),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (week.sessionCount > 0) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = buildString {
                    append(
                        pluralStringResource(
                            R.plurals.home_week_detail,
                            week.sessionCount,
                            formatDuration(week.averageDailyMinutes),
                            week.sessionCount,
                        ),
                    )
                    if (state.weekPlannedDays > 0) {
                        append(" · ")
                        val daysWorked = week.sessionCount.coerceAtMost(state.weekPlannedDays)
                        append(
                            pluralStringResource(
                                R.plurals.home_week_planned,
                                daysWorked,
                                daysWorked,
                                state.weekPlannedDays,
                            ),
                        )
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Quieter, secondary action shown under the main button. */
@Composable
private fun SecondaryAction(label: String, onClick: () -> Unit) {
    Spacer(Modifier.height(4.dp))
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
    ) {
        Text(text = label, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun WorkTypeChips(
    state: HomeUiState,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SectionLabel(stringResource(R.string.home_choose_work_type))
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.workTypes.forEach { type ->
                FilterChip(
                    selected = type.id == state.selectedWorkType?.id,
                    onClick = { onSelect(type.id) },
                    label = { Text(type.name) },
                )
            }
        }
    }
}

@Composable
private fun PrimaryAction(
    isWorking: Boolean,
    onStart: () -> Unit,
    onFinish: () -> Unit,
) {
    Button(
        onClick = if (isWorking) onFinish else onStart,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(60.dp),
        shape = MaterialTheme.shapes.large,
        colors = if (isWorking) {
            ButtonDefaults.buttonColors(
                containerColor = WorklyTheme.accents.working,
                contentColor = MaterialTheme.colorScheme.surface,
            )
        } else {
            ButtonDefaults.buttonColors()
        },
    ) {
        Icon(
            imageVector = if (isWorking) CheckIcon else PlayIcon,
            contentDescription = null,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(
                if (isWorking) R.string.home_finish_work else R.string.home_start_work,
            ),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun FirstRunPanel(
    onStart: () -> Unit,
    onAddRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WorklyCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.empty_title),
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.empty_message),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text(stringResource(R.string.empty_action))
        }
        TextButton(onClick = onAddRecord, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.home_add_record))
        }
    }
}

@Composable
private fun RatePromptDialog(
    currency: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.rate_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.rate_dialog_message),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.rate_dialog_field)) },
                    suffix = { Text(currency) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text) },
                enabled = text.isNotBlank(),
            ) { Text(stringResource(R.string.rate_dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

/** Seconds since [startTime], refreshed once per second. */
@Composable
private fun rememberElapsedSeconds(startTime: Instant): Long {
    var seconds by remember(startTime) {
        mutableLongStateOf(Duration.between(startTime, Instant.now()).seconds.coerceAtLeast(0L))
    }
    LaunchedEffect(startTime) {
        while (true) {
            seconds = Duration.between(startTime, Instant.now()).seconds.coerceAtLeast(0L)
            delay(1_000L)
        }
    }
    return seconds
}

private fun greetingRes(): Int = when (LocalTime.now().hour) {
    in 5..11 -> R.string.greeting_morning
    in 12..17 -> R.string.greeting_afternoon
    in 18..22 -> R.string.greeting_evening
    else -> R.string.greeting_night
}
