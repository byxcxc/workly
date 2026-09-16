package com.workly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.workly.app.data.local.entity.WorkSessionEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface WorkSessionDao {

    @Query("SELECT * FROM work_sessions ORDER BY startTime DESC")
    fun observeAll(): Flow<List<WorkSessionEntity>>

    @Query("SELECT * FROM work_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    fun observeActive(): Flow<WorkSessionEntity?>

    @Query("SELECT * FROM work_sessions WHERE isActive = 1 ORDER BY startTime DESC LIMIT 1")
    suspend fun getActive(): WorkSessionEntity?

    @Query("SELECT * FROM work_sessions WHERE id = :id")
    fun observeById(id: Long): Flow<WorkSessionEntity?>

    @Query("SELECT * FROM work_sessions WHERE id = :id")
    suspend fun getById(id: Long): WorkSessionEntity?

    @Query(
        "SELECT * FROM work_sessions WHERE startTime >= :from AND startTime < :to " +
            "ORDER BY startTime DESC",
    )
    fun observeInRange(from: Instant, to: Instant): Flow<List<WorkSessionEntity>>

    @Query(
        "SELECT * FROM work_sessions WHERE startTime >= :from AND startTime < :to " +
            "ORDER BY startTime DESC",
    )
    suspend fun getInRange(from: Instant, to: Instant): List<WorkSessionEntity>

    @Query("SELECT * FROM work_sessions WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun observeCompleted(): Flow<List<WorkSessionEntity>>

    @Query("SELECT * FROM work_sessions ORDER BY startTime ASC")
    suspend fun getAll(): List<WorkSessionEntity>

    @Query("SELECT externalId FROM work_sessions")
    suspend fun getAllExternalIds(): List<String>

    @Query("SELECT COUNT(*) FROM work_sessions")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: WorkSessionEntity): Long

    /** Duplicate [WorkSessionEntity.externalId] values are ignored, not replaced. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnoringDuplicates(sessions: List<WorkSessionEntity>): List<Long>

    @Update
    suspend fun update(session: WorkSessionEntity)

    @Delete
    suspend fun delete(session: WorkSessionEntity)

    @Query("DELETE FROM work_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM work_sessions WHERE isActive = 1")
    suspend fun deleteActive()

    @Query("DELETE FROM work_sessions")
    suspend fun deleteAll()
}
