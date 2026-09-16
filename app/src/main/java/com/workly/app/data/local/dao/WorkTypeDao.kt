package com.workly.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.workly.app.data.local.entity.WorkTypeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkTypeDao {

    @Query("SELECT * FROM work_types ORDER BY id ASC")
    fun observeAll(): Flow<List<WorkTypeEntity>>

    @Query("SELECT * FROM work_types ORDER BY id ASC")
    suspend fun getAll(): List<WorkTypeEntity>

    @Query("SELECT * FROM work_types WHERE id = :id")
    suspend fun getById(id: Long): WorkTypeEntity?

    @Query("SELECT * FROM work_types WHERE name = :name LIMIT 1")
    suspend fun getByName(name: String): WorkTypeEntity?

    @Query("SELECT COUNT(*) FROM work_types")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(type: WorkTypeEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(types: List<WorkTypeEntity>): List<Long>

    @Update
    suspend fun update(type: WorkTypeEntity)

    @Delete
    suspend fun delete(type: WorkTypeEntity)

    @Query("DELETE FROM work_types")
    suspend fun deleteAll()
}
