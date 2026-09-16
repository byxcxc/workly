package com.workly.app.data.backup

import kotlinx.serialization.Serializable

/**
 * The JSON backup format. It is versioned so that a future release can migrate
 * old files instead of guessing.
 *
 * Instants are stored as epoch milliseconds (UTC). Only finished sessions are
 * exported: a session that is still running is live state, not a record.
 */
@Serializable
data class WorklyBackup(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersion: String = "",
    val exportedAt: Long = 0L,
    val workTypes: List<BackupWorkType> = emptyList(),
    val sessions: List<BackupSession> = emptyList(),
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION = 1
    }
}

@Serializable
data class BackupWorkType(
    val id: Long,
    val name: String,
    val defaultHourlyRateMinor: Long,
    val currency: String,
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class BackupSession(
    val id: Long,
    val externalId: String,
    val startTime: Long,
    val endTime: Long?,
    val breakMinutes: Int,
    val hourlyRateMinor: Long,
    val currency: String,
    val workTypeId: Long? = null,
    val workTypeName: String? = null,
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

/** What the user is shown before confirming an import. */
data class ImportPreview(
    val sessionCount: Int,
    val workTypeCount: Int,
    val newSessionCount: Int,
    val duplicateSessionCount: Int,
    val newWorkTypeCount: Int,
)

/** What actually happened after an import. */
data class ImportOutcome(
    val importedSessions: Int,
    val skippedSessions: Int,
    val importedWorkTypes: Int,
)
