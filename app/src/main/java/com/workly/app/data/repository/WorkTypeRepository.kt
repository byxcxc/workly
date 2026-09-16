package com.workly.app.data.repository

import com.workly.app.data.local.dao.WorkTypeDao
import com.workly.app.data.local.entity.WorkTypeEntity
import com.workly.app.data.prefs.SettingsRepository
import com.workly.app.domain.SessionValidator
import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.Instant

/**
 * Manages the user's work types.
 *
 * @param defaultNames localized names for the four starter work types, resolved
 *   from resources so a Japanese device gets Japanese defaults.
 */
class WorkTypeRepository(
    private val workTypeDao: WorkTypeDao,
    private val settingsRepository: SettingsRepository,
    private val defaultNames: List<String>,
) {

    val workTypes: Flow<List<WorkTypeEntity>> = workTypeDao.observeAll()

    /** Creates the starter work types exactly once, on first launch. */
    suspend fun ensureDefaultWorkTypes() {
        val settings = settingsRepository.settings.first()
        if (settings.defaultsSeeded) return
        if (workTypeDao.count() == 0) {
            val now = Instant.now()
            workTypeDao.insertAll(
                defaultNames.map { name ->
                    WorkTypeEntity(
                        name = name,
                        defaultHourlyRateMinor = settings.defaultHourlyRateMinor,
                        currency = settings.currency,
                        isBuiltIn = true,
                        createdAt = now,
                        updatedAt = now,
                    )
                },
            )
        }
        settingsRepository.setDefaultsSeeded(true)
    }

    suspend fun getAll(): List<WorkTypeEntity> = workTypeDao.getAll()

    suspend fun getById(id: Long): WorkTypeEntity? = workTypeDao.getById(id)

    suspend fun getByName(name: String): WorkTypeEntity? = workTypeDao.getByName(name.trim())

    suspend fun add(
        name: String,
        defaultHourlyRateMinor: Long,
        currency: String,
    ): Result<Long> = dbCall {
        val trimmed = name.trim()
        SessionValidator.validateWorkTypeName(trimmed)?.let { throw WorklyException(it) }
        if (defaultHourlyRateMinor < 0) throw WorklyException(WorklyError.NEGATIVE_RATE)
        if (workTypeDao.getByName(trimmed) != null) {
            throw WorklyException(WorklyError.DUPLICATE_WORK_TYPE_NAME)
        }
        val now = Instant.now()
        workTypeDao.insert(
            WorkTypeEntity(
                name = trimmed,
                defaultHourlyRateMinor = defaultHourlyRateMinor,
                currency = currency,
                isBuiltIn = false,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun update(
        type: WorkTypeEntity,
        name: String,
        defaultHourlyRateMinor: Long,
    ): Result<Unit> = dbCall {
        val trimmed = name.trim()
        SessionValidator.validateWorkTypeName(trimmed)?.let { throw WorklyException(it) }
        if (defaultHourlyRateMinor < 0) throw WorklyException(WorklyError.NEGATIVE_RATE)
        val existing = workTypeDao.getByName(trimmed)
        if (existing != null && existing.id != type.id) {
            throw WorklyException(WorklyError.DUPLICATE_WORK_TYPE_NAME)
        }
        workTypeDao.update(
            type.copy(
                name = trimmed,
                defaultHourlyRateMinor = defaultHourlyRateMinor,
                updatedAt = Instant.now(),
            ),
        )
    }

    /**
     * Deletes a work type. Past sessions keep their rate and the name snapshot
     * they were recorded with, so no historical income changes.
     */
    suspend fun delete(type: WorkTypeEntity): Result<Unit> = dbCall { workTypeDao.delete(type) }

    /** Removes every work type. Used by "Delete all data". */
    suspend fun deleteAll(): Result<Unit> = dbCall { workTypeDao.deleteAll() }

    /** Inserts work types that came from a backup, skipping names that exist. */
    suspend fun insertMissing(types: List<WorkTypeEntity>): Int {        val existing = workTypeDao.getAll().map { it.name.lowercase() }.toSet()
        val toInsert = types.filter { it.name.lowercase() !in existing }
        if (toInsert.isEmpty()) return 0
        return workTypeDao.insertAll(toInsert).count { it != -1L }
    }

    private inline fun <T> dbCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: WorklyException) {
        Result.failure(exception)
    } catch (exception: Exception) {
        Result.failure(WorklyException(WorklyError.DATABASE_ERROR))
    }
}
