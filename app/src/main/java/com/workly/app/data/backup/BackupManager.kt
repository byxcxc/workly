package com.workly.app.data.backup

import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.WorkTypeEntity
import com.workly.app.data.repository.WorkRepository
import com.workly.app.data.repository.WorkTypeRepository
import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import java.io.OutputStream
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

/**
 * Import / export of user data. Everything happens locally through a file the
 * user picks with the Storage Access Framework; Workly never talks to a server.
 */
class BackupManager(
    private val workRepository: WorkRepository,
    private val workTypeRepository: WorkTypeRepository,
    private val appVersionName: String,
    private val now: () -> Instant = { Instant.now() },
) {

    /** @return the number of exported sessions. */
    suspend fun exportCsv(output: OutputStream, zone: ZoneId): Result<Int> = ioCall(WorklyError.EXPORT_FAILED) {
        val sessions = workRepository.getAllSessions().filter { it.endTime != null }
        output.use { it.write(CsvExporter.toCsv(sessions, zone).toByteArray(Charsets.UTF_8)) }
        sessions.size
    }

    /** @return the number of exported sessions. */
    suspend fun exportJson(output: OutputStream): Result<Int> = ioCall(WorklyError.EXPORT_FAILED) {
        val sessions = workRepository.getAllSessions().filter { it.endTime != null }
        val workTypes = workTypeRepository.getAll()
        val backup = WorklyBackup(
            appVersion = appVersionName,
            exportedAt = now().toEpochMilli(),
            workTypes = workTypes.map { it.toBackup() },
            sessions = sessions.map { it.toBackup() },
        )
        output.use { it.write(BackupCodec.encode(backup).toByteArray(Charsets.UTF_8)) }
        sessions.size
    }

    suspend fun parse(text: String): Result<WorklyBackup> = BackupCodec.decode(text)

    /** Counts what an import would do, without touching the database. */
    suspend fun preview(backup: WorklyBackup): ImportPreview {
        val existingIds = workRepository.getAllSessions().map { it.externalId }.toSet()
        val existingNames = workTypeRepository.getAll().map { it.name.lowercase() }.toSet()
        val importable = backup.sessions.filter { it.endTime != null }
        val duplicates = importable.count { it.externalId in existingIds }
        return ImportPreview(
            sessionCount = importable.size,
            workTypeCount = backup.workTypes.size,
            newSessionCount = importable.size - duplicates,
            duplicateSessionCount = duplicates,
            newWorkTypeCount = backup.workTypes.count { it.name.lowercase() !in existingNames },
        )
    }

    /**
     * Imports a backup. Records are de-duplicated by their stable `externalId`, so
     * importing the same file twice never creates doubles, and work types are
     * matched by name.
     */
    suspend fun import(backup: WorklyBackup): Result<ImportOutcome> = ioCall(WorklyError.IMPORT_FAILED) {
        val importedTypes = workTypeRepository.insertMissing(backup.workTypes.map { it.toEntity() })

        val localTypesByName = workTypeRepository.getAll().associateBy { it.name.lowercase() }
        val existingIds = workRepository.getAllSessions().map { it.externalId }.toMutableSet()
        val timestamp = now()

        val entities = ArrayList<WorkSessionEntity>()
        var skipped = 0
        backup.sessions.forEach { session ->
            val end = session.endTime
            val valid = end != null &&
                end > session.startTime &&
                session.breakMinutes >= 0 &&
                session.hourlyRateMinor >= 0
            if (!valid) {
                skipped++
                return@forEach
            }
            val externalId = session.externalId.ifBlank { UUID.randomUUID().toString() }
            if (!existingIds.add(externalId)) {
                skipped++
                return@forEach
            }
            val workType = session.workTypeName?.lowercase()?.let { localTypesByName[it] }
            entities += WorkSessionEntity(
                externalId = externalId,
                startTime = Instant.ofEpochMilli(session.startTime),
                endTime = Instant.ofEpochMilli(end),
                breakMinutes = session.breakMinutes,
                hourlyRateMinor = session.hourlyRateMinor,
                currency = session.currency,
                workTypeId = workType?.id,
                workTypeName = session.workTypeName ?: workType?.name,
                note = session.note,
                createdAt = Instant.ofEpochMilli(session.createdAt),
                updatedAt = timestamp,
                isActive = false,
            )
        }

        if (entities.isNotEmpty()) {
            workRepository.insertImported(entities)
        }
        ImportOutcome(
            importedSessions = entities.size,
            skippedSessions = skipped,
            importedWorkTypes = importedTypes,
        )
    }

    private inline fun <T> ioCall(fallback: WorklyError, block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: WorklyException) {
        Result.failure(exception)
    } catch (exception: Exception) {
        Result.failure(WorklyException(fallback))
    }

    private fun WorkSessionEntity.toBackup() = BackupSession(
        id = id,
        externalId = externalId,
        startTime = startTime.toEpochMilli(),
        endTime = endTime?.toEpochMilli(),
        breakMinutes = breakMinutes,
        hourlyRateMinor = hourlyRateMinor,
        currency = currency,
        workTypeId = workTypeId,
        workTypeName = workTypeName,
        note = note,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

    private fun WorkTypeEntity.toBackup() = BackupWorkType(
        id = id,
        name = name,
        defaultHourlyRateMinor = defaultHourlyRateMinor,
        currency = currency,
        createdAt = createdAt.toEpochMilli(),
        updatedAt = updatedAt.toEpochMilli(),
    )

    private fun BackupWorkType.toEntity() = WorkTypeEntity(
        name = name,
        defaultHourlyRateMinor = defaultHourlyRateMinor,
        currency = currency,
        isBuiltIn = false,
        createdAt = Instant.ofEpochMilli(createdAt),
        updatedAt = Instant.ofEpochMilli(updatedAt),
    )
}
