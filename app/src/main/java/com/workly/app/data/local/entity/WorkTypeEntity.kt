package com.workly.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A kind of work the user performs, optionally with its own default hourly rate.
 *
 * A work type is only a *template*: when a session is created the current default
 * rate is copied into the session, so later edits here never rewrite history.
 */
@Entity(
    tableName = "work_types",
    indices = [Index(value = ["name"], unique = true)],
)
data class WorkTypeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val name: String,
    /** Default hourly rate in minor units (cents) per hour. */
    val defaultHourlyRateMinor: Long = 0L,
    val currency: String,
    /** True for the four starter work types shipped with the app. */
    val isBuiltIn: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
)
