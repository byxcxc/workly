package com.workly.app.ui.preview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.workly.app.R
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.domain.PeriodStats
import com.workly.app.domain.TimeBucket
import com.workly.app.domain.WorkRange
import com.workly.app.ui.components.EmptyState
import com.workly.app.ui.components.PlayIcon
import com.workly.app.ui.components.SessionRow
import com.workly.app.ui.components.StatBlock
import com.workly.app.ui.components.WorklyCard
import com.workly.app.ui.home.TodayCard
import com.workly.app.ui.home.WeekCard
import com.workly.app.ui.home.WorkingNowCard
import com.workly.app.ui.statistics.SimpleBarChart
import com.workly.app.ui.theme.WorklyTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Design gallery.
 *
 * These previews are the source of the screenshots in the README: open this file
 * in Android Studio, render a preview and export it. They are also a quick way to
 * check the visual language without running the app.
 */

private val previewZone = ZoneOffset.UTC

private fun previewSession(
    id: Long = 1L,
    startIso: String = "2026-09-16T09:00:00Z",
    endIso: String? = "2026-09-16T17:00:00Z",
    breakMinutes: Int = 60,
    rateMinor: Long = 1_600L,
    currency: String = "JPY",
    workTypeName: String? = "Main Job",
    note: String = "",
) = WorkSessionEntity(
    id = id,
    externalId = "preview-$id",
    startTime = Instant.parse(startIso),
    endTime = endIso?.let(Instant::parse),
    breakMinutes = breakMinutes,
    hourlyRateMinor = rateMinor,
    currency = currency,
    workTypeId = null,
    workTypeName = workTypeName,
    note = note,
    createdAt = Instant.parse(startIso),
    updatedAt = Instant.parse(startIso),
    isActive = false,
)

private val previewStats = PeriodStats(
    range = WorkRange.ofDay(LocalDate.of(2026, 9, 16)),
    totalMinutes = 452,
    totalIncomeMinor = 1_205_333,
    workDays = 1,
    sessionCount = 2,
)

private val previewWeek = PeriodStats(
    range = WorkRange.ofDay(LocalDate.of(2026, 9, 16)),
    totalMinutes = 1_938,
    totalIncomeMinor = 5_168_000,
    workDays = 4,
    sessionCount = 6,
)

@Preview(name = "Home – today", showBackground = true, widthDp = 380)
@Composable
private fun HomeTodayPreview() {
    WorklyTheme {
        Surface {
            Column(modifier = Modifier.padding(20.dp)) {
                TodayCard(today = previewStats, currency = "JPY")
                Spacer(Modifier.height(16.dp))
                WeekCard(week = previewWeek, currency = "JPY")
            }
        }
    }
}

@Preview(name = "Home – working now", showBackground = true, widthDp = 380)
@Composable
private fun HomeWorkingPreview() {
    WorklyTheme {
        Surface {
            WorkingNowCard(
                session = previewSession(
                    endIso = null,
                    breakMinutes = 0,
                    startIso = "2026-09-16T07:26:00Z",
                ),
            )
        }
    }
}

@Preview(name = "Record row", showBackground = true, widthDp = 380)
@Composable
private fun SessionRowPreview() {
    WorklyTheme {
        Surface {
            Column {
                SessionRow(session = previewSession())
                SessionRow(
                    session = previewSession(
                        id = 2L,
                        startIso = "2026-09-15T23:00:00Z",
                        endIso = "2026-09-16T02:00:00Z",
                        breakMinutes = 0,
                        workTypeName = "Freelance",
                        note = "Night release",
                    ),
                )
            }
        }
    }
}

@Preview(name = "Statistics card", showBackground = true, widthDp = 380)
@Composable
private fun StatisticsCardPreview() {
    WorklyTheme {
        Surface {
            WorklyCard(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                StatBlock(
                    label = "TOTAL WORK TIME",
                    value = "32h 18m",
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(20.dp))
                StatBlock(
                    label = "TOTAL INCOME",
                    value = "¥56,800",
                    valueColor = com.workly.app.ui.theme.WorklyTheme.accents.income,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Preview(name = "Trend chart", showBackground = true, widthDp = 380)
@Composable
private fun ChartPreview() {
    val buckets = listOf(7, 8, 6, 0, 8, 4, 0).mapIndexed { index, hours ->
        TimeBucket(
            labelDate = LocalDate.of(2026, 9, 14).plusDays(index.toLong()),
            minutes = hours * 60L,
            incomeMinor = hours * 1_600L * 100L,
        )
    }
    WorklyTheme {
        Surface {
            WorklyCard(modifier = Modifier.padding(20.dp).fillMaxWidth()) {
                SimpleBarChart(
                    values = buckets.map { it.incomeMinor.toFloat() },
                    labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
                    barColor = com.workly.app.ui.theme.WorklyTheme.accents.chart,
                    contentDescription = "Income trend",
                )
            }
        }
    }
}

@Preview(name = "Empty state", showBackground = true, widthDp = 380)
@Composable
private fun EmptyStatePreview() {
    WorklyTheme {
        Surface {
            EmptyState(
                title = "Your work starts here.",
                message = "Track your hours and earnings effortlessly.",
                icon = PlayIcon,
                actionLabel = "Start your first session",
                onAction = {},
                modifier = Modifier.width(380.dp),
            )
        }
    }
}
