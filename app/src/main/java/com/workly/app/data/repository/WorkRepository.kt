package com.workly.app.data.repository

import com.workly.app.data.local.dao.WorkSessionDao
import com.workly.app.data.local.dao.WorkTypeDao
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.WorkTypeEntity
import com.workly.app.domain.SessionValidator
import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.util.UUID

/**
 * Single entry point for reading and writing work sessions.
 *
 * Every mutating function returns a [Result] instead of throwing, so callers can
 * react to a failure (and show it) without any try/catch of their own.
 */
class WorkRepository(
    private val sessionDao: WorkSessionDao,
    private val workTypeDao: WorkTypeDao,
) {

    val allSessions: Flow<List<WorkSessionEntity>> = sessionDao.observeAll()

    /** Finished sessions only; used by statistics and the records list. */
    val completedSessions: Flow<List<WorkSessionEntity>> = sessionDao.observeCompleted()

    val activeSession: Flow<WorkSessionEntity?> = sessionDao.observeActive()

    fun observeSession(id: Long): Flow<WorkSessionEntity?> = sessionDao.observeById(id)

    suspend fun getSession(id: Long): WorkSessionEntity? = sessionDao.getById(id)

    suspend fun getSessionOnce(id: Long): WorkSessionEntity? = sessionDao.getById(id)

    suspend fun hasActiveSession(): Boolean = sessionDao.getActive() != null

    /**
     * Starts a new session "now". The row is written immediately with
     * `isActive = true`, so the running session is never lost even if the process
     * dies a second later.
     */
    suspend fun startWork(
        hourlyRateMinor: Long,
        currency: String,
        workType: WorkTypeEntity?,
        note: String = "",
        now: Instant = Instant.now(),
    ): Result<Long> = dbCall {
        if (sessionDao.getActive() != null) throw WorklyException(WorklyError.ACTIVE_SESSION_EXISTS)
        if (hourlyRateMinor < 0) throw WorklyException(WorklyError.NEGATIVE_RATE)
        val session = WorkSessionEntity(
            externalId = UUID.randomUUID().toString(),
            startTime = now,
            endTime = null,
            breakMinutes = 0,
            hourlyRateMinor = hourlyRateMinor,
            currency = currency,
            workTypeId = workType?.id,
            workTypeName = workType?.name,
            note = note,
            createdAt = now,
            updatedAt = now,
            isActive = true,
        )
        sessionDao.upsert(session)
    }

    /** Ends the running session with the values the user confirmed. */
    suspend fun finishWork(
        sessionId: Long,
        endTime: Instant,
        breakMinutes: Int,
        hourlyRateMinor: Long,
        currency: String,
        workType: WorkTypeEntity?,
        note: String,
        now: Instant = Instant.now(),
    ): Result<Unit> = dbCall {
        val session = sessionDao.getById(sessionId)
            ?: throw WorklyException(WorklyError.NO_ACTIVE_SESSION)
        if (!session.isActive) throw WorklyException(WorklyError.NO_ACTIVE_SESSION)
        requireValid(session.startTime, endTime, breakMinutes, hourlyRateMinor)
        sessionDao.update(
            session.copy(
                endTime = endTime,
                breakMinutes = breakMinutes,
                hourlyRateMinor = hourlyRateMinor,
                currency = currency,
                workTypeId = workType?.id,
                workTypeName = workType?.name,
                note = note.trim(),
                isActive = false,
                updatedAt = now,
            ),
        )
    }

    /** Adds a record that was typed in by hand. */
    suspend fun createManualSession(
        startTime: Instant,
        endTime: Instant,
        breakMinutes: Int,
        hourlyRateMinor: Long,
        currency: String,
        workType: WorkTypeEntity?,
        note: String,
        now: Instant = Instant.now(),
    ): Result<Long> = dbCall {
        requireValid(startTime, endTime, breakMinutes, hourlyRateMinor)
        sessionDao.upsert(
            WorkSessionEntity(
                externalId = UUID.randomUUID().toString(),
                startTime = startTime,
                endTime = endTime,
                breakMinutes = breakMinutes,
                hourlyRateMinor = hourlyRateMinor,
                currency = currency,
                workTypeId = workType?.id,
                workTypeName = workType?.name,
                note = note.trim(),
                createdAt = now,
                updatedAt = now,
                isActive = false,
            ),
        )
    }

    /** Saves an edited record. */
    suspend fun updateSession(
        session: WorkSessionEntity,
        startTime: Instant,
        endTime: Instant,
        breakMinutes: Int,
        hourlyRateMinor: Long,
        currency: String,
        workType: WorkTypeEntity?,
        note: String,
        now: Instant = Instant.now(),
    ): Result<Unit> = dbCall {
        requireValid(startTime, endTime, breakMinutes, hourlyRateMinor)
        val stored = sessionDao.getById(session.id)
            ?: throw WorklyException(WorklyError.DATABASE_ERROR)
        sessionDao.update(
            stored.copy(
                startTime = startTime,
                endTime = endTime,
                breakMinutes = breakMinutes,
                hourlyRateMinor = hourlyRateMinor,
                currency = currency,
                workTypeId = workType?.id,
                workTypeName = workType?.name,
                note = note.trim(),
                isActive = false,
                updatedAt = now,
            ),
        )
    }

    /**
     * Inserts sessions that come from a backup. Rows whose `externalId` already
     * exists are skipped, so importing the same file twice is harmless.
     *
     * @return the number of rows really inserted.
     */
    suspend fun insertImported(sessions: List<WorkSessionEntity>): Int =
        sessionDao.insertIgnoringDuplicates(sessions).count { it != -1L }

    suspend fun deleteSession(id: Long): Result<Unit> = dbCall { sessionDao.deleteById(id) }

    /** Throws away the running session without recording any work. */
    suspend fun discardActiveSession(): Result<Unit> = dbCall { sessionDao.deleteActive() }

    suspend fun getAllSessions(): List<WorkSessionEntity> = sessionDao.getAll()

    suspend fun sessionCount(): Int = sessionDao.count()

    suspend fun deleteEverything(): Result<Unit> = dbCall { sessionDao.deleteAll() }

    private fun requireValid(
        start: Instant,
        end: Instant,
        breakMinutes: Int,
        hourlyRateMinor: Long,
    ) {
        when (val result = SessionValidator.validate(start, end, breakMinutes, hourlyRateMinor)) {
            is SessionValidator.Result.Invalid -> throw WorklyException(result.error)
            is SessionValidator.Result.Valid -> Unit
        }
    }

    private inline fun <T> dbCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (exception: WorklyException) {
        Result.failure(exception)
    } catch (exception: Exception) {
        Result.failure(WorklyException(WorklyError.DATABASE_ERROR))
    }
}
