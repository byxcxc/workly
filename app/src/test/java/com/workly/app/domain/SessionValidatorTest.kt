package com.workly.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

class SessionValidatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Tokyo")

    private fun instant(date: LocalDate, time: LocalTime): Instant =
        ZonedDateTime.of(date, time, zone).toInstant()

    private val nineAm = instant(LocalDate.of(2026, 9, 16), LocalTime.of(9, 0))

    @Test
    fun `eight hours with a one hour break is valid and yields seven hours`() {
        val end = nineAm.plusSeconds(8 * 3600)

        val result = SessionValidator.validate(nineAm, end, breakMinutes = 60, hourlyRateMinor = 1000)

        assertEquals(SessionValidator.Result.Valid(420L), result)
    }

    @Test
    fun `identical start and end is rejected`() {
        val result = SessionValidator.validate(nineAm, nineAm, 0, 1000)

        assertEquals(SessionValidator.Result.Invalid(WorklyError.END_TIME_EQUALS_START), result)
    }

    @Test
    fun `end before start is rejected`() {
        val end = nineAm.minusSeconds(3600)

        val result = SessionValidator.validate(nineAm, end, 0, 1000)

        assertEquals(SessionValidator.Result.Invalid(WorklyError.END_BEFORE_START), result)
    }

    @Test
    fun `break longer than the worked time is rejected`() {
        val end = nineAm.plusSeconds(3600)

        val result = SessionValidator.validate(nineAm, end, breakMinutes = 61, hourlyRateMinor = 1000)

        assertEquals(SessionValidator.Result.Invalid(WorklyError.BREAK_LONGER_THAN_WORK), result)
    }

    @Test
    fun `a break exactly as long as the work is allowed but earns nothing`() {
        val end = nineAm.plusSeconds(3600)

        val result = SessionValidator.validate(nineAm, end, breakMinutes = 60, hourlyRateMinor = 1000)

        assertEquals(SessionValidator.Result.Valid(0L), result)
    }

    @Test
    fun `negative hourly rate is rejected`() {
        val end = nineAm.plusSeconds(3600)

        val result = SessionValidator.validate(nineAm, end, 0, hourlyRateMinor = -1)

        assertEquals(SessionValidator.Result.Invalid(WorklyError.NEGATIVE_RATE), result)
    }

    @Test
    fun `a zero rate is allowed`() {
        val end = nineAm.plusSeconds(3600)

        val result = SessionValidator.validate(nineAm, end, 0, hourlyRateMinor = 0)

        assertEquals(SessionValidator.Result.Valid(60L), result)
    }

    @Test
    fun `cross midnight session is valid once the end is on the next day`() {
        val start = instant(LocalDate.of(2026, 9, 16), LocalTime.of(23, 0))
        val end = WorkTime.resolveEndInstant(
            LocalDate.of(2026, 9, 16),
            LocalTime.of(23, 0),
            LocalTime.of(2, 0),
            zone,
        )

        val result = SessionValidator.validate(start, end, 0, 1000)

        assertEquals(SessionValidator.Result.Valid(180L), result)
    }

    @Test
    fun `work type names must not be blank`() {
        assertEquals(WorklyError.EMPTY_WORK_TYPE_NAME, SessionValidator.validateWorkTypeName("   "))
        assertEquals(null, SessionValidator.validateWorkTypeName("Freelance"))
    }
}
