package com.workly.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.workly.app.domain.Money
import com.workly.app.domain.WorkTime
import java.time.Instant

/**
 * One tracked piece of work.
 *
 * An *active* session is simply a row with [isActive] set to `true` and a `null`
 * [endTime]. Because it is written to the database the moment the user taps
 * "Start work", the session survives the app being closed, the device being
 * locked, the process being killed by the system and a phone reboot.
 *
 * [hourlyRateMinor] is a snapshot of the rate that was valid when the work was
 * performed. Editing the default rate of a work type later never changes the
 * income of existing records.
 */
@Entity(
    tableName = "work_sessions",
    indices = [
        Index(value = ["externalId"], unique = true),
        Index(value = ["startTime"]),
        Index(value = ["isActive"]),
        Index(value = ["workTypeId"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = WorkTypeEntity::class,
            parentColumns = ["id"],
            childColumns = ["workTypeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
)
data class WorkSessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /**
     * Stable identifier used to de-duplicate records when a backup is imported.
     * It is independent of the local auto-increment [id].
     */
    val externalId: String,
    val startTime: Instant,
    val endTime: Instant? = null,
    val breakMinutes: Int = 0,
    /** Hourly rate in minor units (cents) per hour, e.g. 160050 == 1,600.50. */
    val hourlyRateMinor: Long,
    /** ISO-4217 code, e.g. "JPY", "USD", "EUR". */
    val currency: String,
    val workTypeId: Long? = null,
    /**
     * Name of the work type at the time of the record. Kept so that deleting a
     * work type never leaves historical records unlabelled.
     */
    val workTypeName: String? = null,
    val note: String = "",
    val createdAt: Instant,
    val updatedAt: Instant,
    val isActive: Boolean = false,
)

/**
 * Minutes actually worked, i.e. total elapsed time minus the break. Sessions that
 * are still running (or somehow inconsistent) report 0.
 */
val WorkSessionEntity.workedMinutes: Long
    get() = endTime?.let { end ->
        WorkTime.netMinutes(WorkTime.grossMinutes(startTime, end), breakMinutes).coerceAtLeast(0L)
    } ?: 0L

/** Income earned by this record, in minor units. */
val WorkSessionEntity.incomeMinor: Long
    get() = Money.incomeMinor(hourlyRateMinor, workedMinutes)
