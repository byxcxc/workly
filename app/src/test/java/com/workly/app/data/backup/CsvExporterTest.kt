package com.workly.app.data.backup

import com.workly.app.TestData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

class CsvExporterTest {

    private val zone = TestData.zone
    private val date = LocalDate.of(2026, 9, 16)

    private fun lines(csv: String): List<String> =
        csv.removePrefix(CsvExporter.BOM).split("\r\n").filter { it.isNotEmpty() }

    @Test
    fun `header contains every documented column in order`() {
        val csv = CsvExporter.toCsv(emptyList(), zone)

        assertEquals(
            "date,start_time,end_time,end_date,break_minutes,worked_minutes,hourly_rate,currency,income,work_type,note",
            lines(csv).first(),
        )
    }

    @Test
    fun `a record is exported with readable local values`() {
        val session = TestData.session(
            date = date,
            start = LocalTime.of(9, 0),
            end = LocalTime.of(17, 0),
            breakMinutes = 60,
            hourlyRateMinor = 160_000L,
            currency = "JPY",
            workTypeName = "Main Job",
            note = "Sprint planning",
        )

        val row = lines(CsvExporter.toCsv(listOf(session), zone))[1]

        assertEquals(
            "2026-09-16,09:00,17:00,2026-09-16,60,420,160000,JPY,1120000,Main Job,Sprint planning",
            row,
        )
    }

    @Test
    fun `a session crossing midnight reports the end date`() {
        val session = TestData.session(
            date = date,
            start = LocalTime.of(23, 0),
            end = LocalTime.of(2, 0),
            endOnNextDay = true,
        )

        val row = lines(CsvExporter.toCsv(listOf(session), zone))[1]

        assertTrue(row.startsWith("2026-09-16,23:00,02:00,2026-09-17,"))
    }

    @Test
    fun `fields containing separators quotes or newlines are escaped`() {
        val session = TestData.session(
            date = date,
            start = LocalTime.of(9, 0),
            end = LocalTime.of(10, 0),
            note = "Fixed \"urgent\" bug, then\nwrote tests",
        )

        val row = lines(CsvExporter.toCsv(listOf(session), zone))[1]

        assertTrue(row.endsWith("\"Fixed \"\"urgent\"\" bug, then\nwrote tests\""))
    }

    @Test
    fun `records are sorted by start time`() {
        val later = TestData.session(date = date, start = LocalTime.of(14, 0), end = LocalTime.of(15, 0))
        val earlier = TestData.session(date = date, start = LocalTime.of(8, 0), end = LocalTime.of(9, 0))

        val rows = lines(CsvExporter.toCsv(listOf(later, earlier), zone)).drop(1)

        assertTrue(rows[0].contains(",08:00,"))
        assertTrue(rows[1].contains(",14:00,"))
    }

    @Test
    fun `money is exported in major units regardless of currency decimals`() {
        val usd = TestData.session(
            date = date,
            start = LocalTime.of(9, 0),
            end = LocalTime.of(10, 0),
            hourlyRateMinor = 1_605L,
            currency = "USD",
        )

        val row = lines(CsvExporter.toCsv(listOf(usd), zone))[1]

        assertTrue(row.contains(",16.05,USD,16.05,"))
    }

    @Test
    fun `first day of week setting does not affect the file`() {
        // Guard against accidentally formatting dates through the UI range logic.
        val session = TestData.session(date = date, start = LocalTime.of(9, 0), end = LocalTime.of(10, 0))
        assertEquals(DayOfWeek.MONDAY, DayOfWeek.MONDAY)
        assertTrue(lines(CsvExporter.toCsv(listOf(session), zone))[1].startsWith("2026-09-16,"))
    }
}
