package com.workly.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class WorkTimeTest {

    private val zone: ZoneId = ZoneId.of("Asia/Tokyo")

    private fun instant(date: LocalDate, time: LocalTime): java.time.Instant =
        ZonedDateTime.of(date, time, zone).toInstant()

    @Test
    fun `gross minutes for a normal same day session`() {
        val start = instant(LocalDate.of(2026, 9, 16), LocalTime.of(9, 0))
        val end = instant(LocalDate.of(2026, 9, 16), LocalTime.of(18, 0))

        assertEquals(540L, WorkTime.grossMinutes(start, end))
    }

    @Test
    fun `eight hour day minus one hour break is seven hours`() {
        val net = WorkTime.netMinutes(grossMinutes = 8 * 60, breakMinutes = 60)

        assertEquals(7 * 60L, net)
        assertEquals("7h 00m", WorkTime.formatDuration(net))
    }

    @Test
    fun `crossing midnight resolves the end to the next day`() {
        val date = LocalDate.of(2026, 9, 16)

        val end = WorkTime.resolveEndInstant(date, LocalTime.of(23, 0), LocalTime.of(2, 0), zone)

        assertEquals(instant(date.plusDays(1), LocalTime.of(2, 0)), end)
        assertEquals(
            180L,
            WorkTime.grossMinutes(instant(date, LocalTime.of(23, 0)), end),
        )
        assertEquals("3h 00m", WorkTime.formatDuration(180L))
    }

    @Test
    fun `crossing midnight with a break subtracts the break`() {
        val date = LocalDate.of(2026, 9, 16)
        val start = instant(date, LocalTime.of(22, 30))
        val end = WorkTime.resolveEndInstant(date, LocalTime.of(22, 30), LocalTime.of(1, 30), zone)

        val gross = WorkTime.grossMinutes(start, end)
        assertEquals(180L, gross)
        assertEquals(150L, WorkTime.netMinutes(gross, 30))
    }

    @Test
    fun `identical times do not roll over to the next day`() {
        val date = LocalDate.of(2026, 9, 16)
        val start = instant(date, LocalTime.of(9, 0))

        val end = WorkTime.resolveEndInstant(date, LocalTime.of(9, 0), LocalTime.of(9, 0), zone)

        assertEquals(start, end)
    }

    @Test
    fun `ends next day detection`() {
        assertTrue(WorkTime.endsNextDay(LocalTime.of(23, 0), LocalTime.of(2, 0)))
        assertFalse(WorkTime.endsNextDay(LocalTime.of(2, 0), LocalTime.of(23, 0)))
        assertFalse(WorkTime.endsNextDay(LocalTime.of(9, 0), LocalTime.of(9, 0)))
    }

    @Test
    fun `end time later than start stays on the same date`() {
        val date = LocalDate.of(2026, 12, 31)

        val end = WorkTime.resolveEndInstant(date, LocalTime.of(9, 0), LocalTime.of(17, 30), zone)

        assertEquals(instant(date, LocalTime.of(17, 30)), end)
    }

    @Test
    fun `duration formatting pads minutes and never pads hours`() {
        assertEquals("0h 00m", WorkTime.formatDuration(0))
        assertEquals("0h 05m", WorkTime.formatDuration(5))
        assertEquals("8h 00m", WorkTime.formatDuration(480))
        assertEquals("12h 07m", WorkTime.formatDuration(727))
        assertEquals("0h 00m", WorkTime.formatDuration(-30))
    }

    @Test
    fun `duration across a full day`() {
        val start = instant(LocalDate.of(2026, 1, 1), LocalTime.of(0, 0))
        val end = start.plus(Duration.ofHours(24))

        assertEquals(1440L, WorkTime.grossMinutes(start, end))
        assertEquals(1440L, WorkTime.netMinutes(1440L, 0))
    }
}
