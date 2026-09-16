package com.workly.app.data.backup

import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.incomeMinor
import com.workly.app.data.local.entity.workedMinutes
import com.workly.app.domain.Money
import java.math.BigDecimal
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Builds the CSV export.
 *
 * Money is written in major units with a `.` decimal separator (never localized)
 * so the file opens correctly in any spreadsheet, and the UTF-8 BOM added by
 * [CsvExporter] makes Excel detect the encoding.
 */
object CsvExporter {

    const val BOM = "\uFEFF"

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT)

    private val header = listOf(
        "date",
        "start_time",
        "end_time",
        "end_date",
        "break_minutes",
        "worked_minutes",
        "hourly_rate",
        "currency",
        "income",
        "work_type",
        "note",
    )

    fun toCsv(sessions: List<WorkSessionEntity>, zone: ZoneId): String {
        val builder = StringBuilder()
        builder.append(BOM)
        builder.append(header.joinToString(SEPARATOR))
        builder.append(LINE_BREAK)
        sessions
            .sortedBy { it.startTime }
            .forEach { session -> builder.append(row(session, zone)).append(LINE_BREAK) }
        return builder.toString()
    }

    private fun row(session: WorkSessionEntity, zone: ZoneId): String {
        val start = session.startTime.atZone(zone)
        val end = session.endTime?.atZone(zone)
        val digits = Money.fractionDigits(session.currency)

        val columns = listOf(
            dateFormatter.format(start),
            timeFormatter.format(start),
            end?.let { timeFormatter.format(it) }.orEmpty(),
            end?.let { dateFormatter.format(it) }.orEmpty(),
            session.breakMinutes.toString(),
            session.workedMinutes.toString(),
            majorUnits(session.hourlyRateMinor, digits),
            session.currency,
            majorUnits(session.incomeMinor, digits),
            session.workTypeName.orEmpty(),
            session.note,
        )
        return columns.joinToString(SEPARATOR) { escape(it) }
    }

    private fun majorUnits(minorUnits: Long, digits: Int): String =
        BigDecimal.valueOf(minorUnits, digits).toPlainString()

    /** RFC 4180 escaping: quote when the value contains a separator, quote or newline. */
    private fun escape(value: String): String {
        val needsQuotes = value.any { it == SEPARATOR_CHAR || it == '"' || it == '\n' || it == '\r' }
        if (!needsQuotes) return value
        return "\"" + value.replace("\"", "\"\"") + "\""
    }

    private const val SEPARATOR = ","
    private const val SEPARATOR_CHAR = ','
    private const val LINE_BREAK = "\r\n"
}
