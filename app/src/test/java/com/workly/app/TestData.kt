package com.workly.app

import com.workly.app.data.local.entity.WorkSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

/** Builders used by the unit tests to keep them short and readable. */
object TestData {

    val zone: ZoneId = ZoneId.of("Asia/Tokyo")

    fun instant(date: LocalDate, time: LocalTime): Instant =
        ZonedDateTime.of(date, time, zone).toInstant()

    fun session(
        id: Long = 0L,
        date: LocalDate,
        start: LocalTime,
        end: LocalTime?,
        endOnNextDay: Boolean = false,
        breakMinutes: Int = 0,
        hourlyRateMinor: Long = 1_000L,
        currency: String = "JPY",
        workTypeName: String? = "Main Job",
        note: String = "",
        isActive: Boolean = false,
    ): WorkSessionEntity {
        val startInstant = instant(date, start)
        val endInstant = end?.let {
            instant(if (endOnNextDay) date.plusDays(1) else date, it)
        }
        return WorkSessionEntity(
            id = id,
            externalId = UUID.randomUUID().toString(),
            startTime = startInstant,
            endTime = endInstant,
            breakMinutes = breakMinutes,
            hourlyRateMinor = hourlyRateMinor,
            currency = currency,
            workTypeId = null,
            workTypeName = workTypeName,
            note = note,
            createdAt = startInstant,
            updatedAt = startInstant,
            isActive = isActive,
        )
    }

    /** A finished session of exactly [minutes] starting at 09:00 on [date]. */
    fun sessionOfMinutes(
        date: LocalDate,
        minutes: Long,
        hourlyRateMinor: Long = 1_000L,
        currency: String = "JPY",
        id: Long = 0L,
    ): WorkSessionEntity {
        val start = instant(date, LocalTime.of(9, 0))
        return session(
            id = id,
            date = date,
            start = LocalTime.of(9, 0),
            end = start.plusSeconds(minutes * 60).atZone(zone).toLocalTime(),
            hourlyRateMinor = hourlyRateMinor,
            currency = currency,
        )
    }
}
