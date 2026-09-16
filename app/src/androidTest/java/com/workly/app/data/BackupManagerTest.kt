package com.workly.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workly.app.data.backup.BackupManager
import com.workly.app.data.local.WorklyDatabase
import com.workly.app.data.prefs.SettingsRepository
import com.workly.app.data.repository.WorkRepository
import com.workly.app.data.repository.WorkTypeRepository
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

@RunWith(AndroidJUnit4::class)
class BackupManagerTest {

    private lateinit var database: WorklyDatabase
    private lateinit var workRepository: WorkRepository
    private lateinit var workTypeRepository: WorkTypeRepository
    private lateinit var backupManager: BackupManager

    private val zone: ZoneId = ZoneOffset.UTC
    private val nine = Instant.parse("2026-09-16T09:00:00Z")
    private val five = Instant.parse("2026-09-16T17:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorklyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        workRepository = WorkRepository(database.workSessionDao(), database.workTypeDao())
        workTypeRepository = WorkTypeRepository(
            workTypeDao = database.workTypeDao(),
            settingsRepository = SettingsRepository(context),
            defaultNames = listOf("Main Job", "Freelance"),
        )
        backupManager = BackupManager(workRepository, workTypeRepository, appVersionName = "test")
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun csvExportContainsAHeaderAndEveryRecord() = runTest {
        workRepository.createManualSession(nine, five, 60, 1_600L, "JPY", null, "first")
        workRepository.createManualSession(nine, five, 0, 1_600L, "JPY", null, "second")

        val output = ByteArrayOutputStream()
        val count = backupManager.exportCsv(output, zone).getOrThrow()

        val text = output.toString(Charsets.UTF_8.name())
        assertEquals(2, count)
        assertTrue(text.startsWith("\uFEFFdate,start_time,end_time"))
        assertEquals(3, text.trim().lines().size)
    }

    @Test
    fun jsonRoundTripRestoresEverything() = runTest {
        workTypeRepository.ensureDefaultWorkTypes()
        val type = workTypeRepository.getAll().first()
        workRepository.createManualSession(nine, five, 30, 1_600L, "JPY", type, "backed up")

        val output = ByteArrayOutputStream()
        backupManager.exportJson(output).getOrThrow()
        val json = output.toString(Charsets.UTF_8.name())

        // Wipe everything, then import the backup back.
        workRepository.deleteEverything().getOrThrow()
        workTypeRepository.deleteAll().getOrThrow()
        assertEquals(0, workRepository.sessionCount())

        val parsed = backupManager.parse(json).getOrThrow()
        val outcome = backupManager.import(parsed).getOrThrow()

        assertEquals(1, outcome.importedSessions)
        assertEquals(1, workRepository.sessionCount())
        val restored = workRepository.getAllSessions().single()
        assertEquals("backed up", restored.note)
        assertEquals(30, restored.breakMinutes)
        assertEquals(1_600L, restored.hourlyRateMinor)
    }

    @Test
    fun importingTwiceSkipsDuplicates() = runTest {
        workRepository.createManualSession(nine, five, 0, 1_600L, "JPY", null, "once")
        val output = ByteArrayOutputStream()
        backupManager.exportJson(output).getOrThrow()
        val parsed = backupManager.parse(output.toString(Charsets.UTF_8.name())).getOrThrow()

        val first = backupManager.import(parsed).getOrThrow()
        val second = backupManager.import(parsed).getOrThrow()

        assertEquals(1, first.importedSessions)
        assertEquals(0, second.importedSessions)
        assertEquals(1, second.skippedSessions)
        assertEquals(1, workRepository.sessionCount())
    }

    @Test
    fun previewCountsNewAndDuplicateRecords() = runTest {
        workRepository.createManualSession(nine, five, 0, 1_600L, "JPY", null, "existing")
        val output = ByteArrayOutputStream()
        backupManager.exportJson(output).getOrThrow()
        val parsed = backupManager.parse(output.toString(Charsets.UTF_8.name())).getOrThrow()

        val before = backupManager.preview(parsed)
        assertEquals(1, before.sessionCount)
        assertEquals(1, before.duplicateSessionCount)
        assertEquals(0, before.newSessionCount)

        backupManager.import(parsed).getOrThrow()
        workRepository.createManualSession(nine, five, 0, 1_600L, "JPY", null, "new one")
        val after = backupManager.preview(parsed)
        assertEquals(0, after.newSessionCount)
    }

    @Test
    fun runningSessionsAreNeverExported() = runTest {
        workRepository.startWork(1_600L, "JPY", null)

        val output = ByteArrayOutputStream()
        val count = backupManager.exportCsv(output, zone).getOrThrow()

        assertEquals(0, count)
        val lines = output.toString(Charsets.UTF_8.name()).trim().lines()
        assertEquals(1, lines.size) // header only
    }

    @Test
    fun invalidJsonIsRejected() = runTest {
        val result = backupManager.parse("this is not a backup")

        assertTrue(result.isFailure)
    }

    @Test
    fun importingSessionsWithAnUnknownWorkTypeStillWorks() = runTest {
        workRepository.createManualSession(nine, five, 0, 1_600L, "JPY", null, "orphan")
        val output = ByteArrayOutputStream()
        backupManager.exportJson(output).getOrThrow()
        val parsed = backupManager.parse(output.toString(Charsets.UTF_8.name())).getOrThrow()

        workRepository.deleteEverything().getOrThrow()
        backupManager.import(parsed).getOrThrow()

        val restored = workRepository.getAllSessions().single()
        assertEquals(null, restored.workTypeId)
    }
}
