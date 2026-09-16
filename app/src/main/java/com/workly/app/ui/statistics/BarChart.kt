package com.workly.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workly.app.R

/**
 * A deliberately simple bar chart.
 *
 * Workly is a personal tracker, not a BI dashboard: one series, rounded bars, a
 * baseline and a few labels. The chart is decorative for screen readers, so it
 * carries a single summary description instead of announcing every bar.
 */
@Composable
fun SimpleBarChart(
    values: List<Float>,
    labels: List<String>,
    barColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val maxValue = values.maxOrNull() ?: 0f
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(132.dp),
        ) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val barWidth = (slot * 0.56f).coerceIn(2f, 28f)
            val usableHeight = size.height - BASELINE_PADDING

            // Baseline
            drawLine(
                color = baselineColor,
                start = Offset(0f, size.height - 0.5f),
                end = Offset(size.width, size.height - 0.5f),
                strokeWidth = 1f,
            )

            values.forEachIndexed { index, value ->
                if (value <= 0f || maxValue <= 0f) return@forEachIndexed
                val fraction = (value / maxValue).coerceIn(0f, 1f)
                val barHeight = (usableHeight * fraction).coerceAtLeast(3f)
                val left = index * slot + (slot - barWidth) / 2f
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            labels.forEach { label ->
                Text(
                    text = label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }
        }
    }
}

/** Message shown in place of a chart when the period has no data. */
@Composable
fun ChartPlaceholder(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.chart_no_data),
        modifier = modifier.padding(vertical = 24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private val baselineColor = Color.Black.copy(alpha = 0.08f)
private const val BASELINE_PADDING = 8f
