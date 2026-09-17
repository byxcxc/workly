package com.workly.app.ui.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workly.app.R

/**
 * A bar chart that always states its numbers.
 *
 * Values are drawn on the bars when they fit, and every data point is listed
 * underneath as well — the list scrolls sideways rather than dropping anything, so
 * a month's worth of days stays readable on a phone. The chart is decorative for
 * screen readers, so it carries one summary description instead of announcing
 * every bar.
 */
@Composable
fun SimpleBarChart(
    values: List<Float>,
    labels: List<String>,
    barColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    /** Short value drawn on each bar, e.g. `6.5h`. */
    valueLabels: List<String> = emptyList(),
    /** `Sep 3 · 6.5h` style pairs listed under the chart, one per data point. */
    detailLabels: List<String> = emptyList(),
) {
    val maxValue = values.maxOrNull() ?: 0f
    val nonEmptyBars = values.count { it > 0f }
    val labelled = valueLabels.size == values.size
    val drawOnBars = labelled && nonEmptyBars in 1..MAX_INLINE_LABELS

    val textMeasurer = rememberTextMeasurer()
    val valueStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { this.contentDescription = contentDescription },
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (drawOnBars) 156.dp else 132.dp),
        ) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val barWidth = (slot * 0.56f).coerceIn(2f, 28f)
            val labelHeight = if (drawOnBars) {
                textMeasurer.measure(AnnotatedString("0"), valueStyle).size.height.toFloat()
            } else {
                0f
            }
            val usableHeight = size.height - BASELINE_PADDING - labelHeight

            drawLine(
                color = baselineColor,
                start = Offset(0f, size.height - 0.5f),
                end = Offset(size.width, size.height - 0.5f),
                strokeWidth = 1f,
            )

            val measured = if (drawOnBars) {
                valueLabels.map { textMeasurer.measure(AnnotatedString(it), valueStyle) }
            } else {
                emptyList()
            }

            values.forEachIndexed { index, value ->
                if (value <= 0f || maxValue <= 0f) return@forEachIndexed
                val fraction = (value / maxValue).coerceIn(0f, 1f)
                val barHeight = (usableHeight * fraction).coerceAtLeast(3f)
                val left = index * slot + (slot - barWidth) / 2f
                val top = size.height - barHeight
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )

                if (drawOnBars) {
                    val text = measured[index]
                    // A label wider than its slot is skipped here; it still appears
                    // in the list below, so no data is lost.
                    if (text.size.width <= slot) {
                        val x = (left + barWidth / 2f - text.size.width / 2f)
                            .coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f))
                        val y = (top - text.size.height - 4f).coerceAtLeast(0f)
                        drawText(textLayoutResult = text, topLeft = Offset(x, y))
                    }
                }
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

        if (detailLabels.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                detailLabels.forEach { detail ->
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    ) {
                        Text(
                            text = detail,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
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

/** Above this many bars, numbers are only listed under the chart. */
private const val MAX_INLINE_LABELS = 12
