package com.workly.app.data.backup

import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import kotlinx.serialization.json.Json

/** Encodes and decodes the JSON backup format. */
object BackupCodec {

    private val json = Json {
        prettyPrint = true
        encodeDefaults = true
        ignoreUnknownKeys = true
    }

    fun encode(backup: WorklyBackup): String = json.encodeToString(backup)

    fun decode(text: String): Result<WorklyBackup> {
        if (text.isBlank()) return Result.failure(WorklyException(WorklyError.INVALID_IMPORT_FILE))
        val backup = runCatching { json.decodeFromString<WorklyBackup>(text) }
            .getOrElse { return Result.failure(WorklyException(WorklyError.INVALID_IMPORT_FILE)) }
        if (backup.schemaVersion > WorklyBackup.CURRENT_SCHEMA_VERSION) {
            return Result.failure(WorklyException(WorklyError.UNSUPPORTED_BACKUP_VERSION))
        }
        return Result.success(backup)
    }
}
