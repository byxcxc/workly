package com.workly.app.ui.statistics

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workly.app.R

/**
 * A deliberately simple bar chart with the numbers written on it.
 *
 * Workly is a personal tracker, not a BI dashboard: one series, rounded bars, a
 * baseline and a few axis labels — plus each bar's value printed above it, since
 * "which day was that?" is the question the chart exists to answer. Values are
 * only drawn while they stay readable; past that the axis labels take over.
 */
@Composable
fun SimpleBarChart(
    values: List<Float>,
    labels: List<String>,
    barColor: Color,
    contentDescription: String,
    modifier: Modifier = Modifier,
    valueLabels: List<String> = emptyList(),
) {
    val maxValue = values.maxOrNull() ?: 0f
    val nonEmptyBars = values.count { it > 0f }
    // Reserve room for the numbers, but decide inside the canvas whether they fit.
    val mayShowValues = valueLabels.size == values.size && nonEmptyBars in 1..MAX_VALUE_LABELS

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
                .height(if (mayShowValues) 156.dp else 132.dp),
        ) {
            if (values.isEmpty()) return@Canvas
            val slot = size.width / values.size
            val barWidth = (slot * 0.56f).coerceIn(2f, 28f)
            val labelHeight = if (mayShowValues) {
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

            val measured = if (mayShowValues) {
                valueLabels.map { textMeasurer.measure(AnnotatedString(it), valueStyle) }
            } else {
                emptyList()
            }
            // If any number would overlap its neighbours, none of them are drawn:
            // a half-labelled chart reads worse than an unlabelled one.
            val showValues = measured.isNotEmpty() && measured.all { it.size.width <= slot * 0.95f }

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

                if (showValues) {
                    val text = measured[index]
                    // Keep the label inside the chart even on the first and last bar.
                    val x = (left + barWidth / 2f - text.size.width / 2f)
                        .coerceIn(0f, (size.width - text.size.width).coerceAtLeast(0f))
                    val y = (top - text.size.height - 4f).coerceAtLeast(0f)
                    drawText(textLayoutResult = text, topLeft = Offset(x, y))
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

/** Above this many bars the numbers would collide, so they are left out. */
private const val MAX_VALUE_LABELS = 12
