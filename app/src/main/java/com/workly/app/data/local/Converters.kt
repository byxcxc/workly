package com.workly.app.data.local

import androidx.room.TypeConverter
import java.time.Instant

/**
 * Room type converters.
 *
 * Instants are stored as epoch milliseconds (UTC). No local-time string is ever
 * persisted, so changing the device time zone never shifts historical records.
 */
class Converters {

    @TypeConverter
    fun fromInstant(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun toInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
}
