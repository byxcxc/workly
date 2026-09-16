package com.workly.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.workly.app.data.local.dao.WorkSessionDao
import com.workly.app.data.local.dao.WorkTypeDao
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.WorkTypeEntity

/**
 * The single Room database of the app.
 *
 * Version 1 is the first public schema. When the schema changes, bump [version]
 * and add a migration; the previous schemas are exported to `app/schemas` so the
 * migration can be reviewed in a pull request.
 */
@Database(
    entities = [WorkSessionEntity::class, WorkTypeEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class WorklyDatabase : RoomDatabase() {

    abstract fun workSessionDao(): WorkSessionDao

    abstract fun workTypeDao(): WorkTypeDao

    companion object {
        const val NAME = "workly.db"
    }
}
