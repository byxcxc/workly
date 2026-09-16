package com.workly.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workly.app.R
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.incomeMinor
import com.workly.app.data.local.entity.workedMinutes
import com.workly.app.ui.theme.WorklyTheme
import com.workly.app.ui.util.formatDuration
import com.workly.app.ui.util.formatMoney
import com.workly.app.ui.util.formatTimeRange

/**
 * One record in a list: time range, work type, duration and income.
 *
 * The whole row is a single clickable target (well above the 48dp minimum) and
 * exposes one merged description to TalkBack.
 */
@Composable
fun SessionRow(
    session: WorkSessionEntity,
    modifier: Modifier = Modifier,
    showDate: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val duration = formatDuration(session.workedMinutes)
    val income = formatMoney(session.incomeMinor, session.currency)
    val timeRange = formatTimeRange(session.startTime, session.endTime)
    val workType = session.workTypeName

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = timeRange,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val secondary = listOfNotNull(
                workType?.takeIf { it.isNotBlank() },
                session.note.takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            if (secondary.isNotEmpty()) {
                Text(
                    text = secondary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = duration, style = MaterialTheme.typography.titleMedium)
            Text(
                text = income,
                style = MaterialTheme.typography.bodyMedium,
                color = WorklyTheme.accents.income,
            )
        }
    }
}

/** Small pulsing dot used to mark the running session (never colour alone). */
@Composable
fun LiveDot(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .size(8.dp)
            .clearAndSetSemantics { },
        shape = CircleShape,
        color = WorklyTheme.accents.working,
        content = {},
    )
}

/** Label + value row used in detail screens. */
@Composable
fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

/** Shared empty label used when a section has no content. */
@Composable
fun InlineEmpty(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
