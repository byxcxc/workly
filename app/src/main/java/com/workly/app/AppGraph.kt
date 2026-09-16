package com.workly.app

import android.app.Application
import androidx.room.Room
import com.workly.app.data.backup.BackupManager
import com.workly.app.data.local.WorklyDatabase
import com.workly.app.data.prefs.SettingsRepository
import com.workly.app.data.repository.WorkRepository
import com.workly.app.data.repository.WorkTypeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Very small hand written dependency container.
 *
 * Workly is a single module app, so a DI framework would add more ceremony than it
 * removes. Everything the UI needs is exposed here as a lazy singleton and is
 * created the first time it is used.
 */
object AppGraph {

    private lateinit var application: Application

    /** Application context, used for content resolvers and resources. */
    val appContext: Application get() = application

    private val applicationScope: CoroutineScope by lazy {
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    val database: WorklyDatabase by lazy {
        Room.databaseBuilder(application, WorklyDatabase::class.java, WorklyDatabase.NAME)
            .build()
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(application) }

    val workRepository: WorkRepository by lazy {
        WorkRepository(
            sessionDao = database.workSessionDao(),
            workTypeDao = database.workTypeDao(),
        )
    }

    val workTypeRepository: WorkTypeRepository by lazy {
        WorkTypeRepository(
            workTypeDao = database.workTypeDao(),
            settingsRepository = settingsRepository,
            defaultNames = application.resources.getStringArray(R.array.default_work_types).toList(),
        )
    }

    val backupManager: BackupManager by lazy {
        BackupManager(
            workRepository = workRepository,
            workTypeRepository = workTypeRepository,
            appVersionName = BuildConfig.VERSION_NAME,
        )
    }

    fun init(application: Application) {
        this.application = application
        // Create the four starter work types on first launch.
        applicationScope.launch {
            runCatching { workTypeRepository.ensureDefaultWorkTypes() }
        }
    }
}
