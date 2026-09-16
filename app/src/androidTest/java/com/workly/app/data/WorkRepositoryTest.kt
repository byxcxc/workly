package com.workly.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workly.app.data.local.WorklyDatabase
import com.workly.app.data.local.entity.WorkTypeEntity
import com.workly.app.data.local.entity.incomeMinor
import com.workly.app.data.local.entity.workedMinutes
import com.workly.app.data.repository.WorkRepository
import com.workly.app.data.repository.WorkTypeRepository
import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(AndroidJUnit4::class)
class WorkRepositoryTest {

    private lateinit var database: WorklyDatabase
    private lateinit var repository: WorkRepository
    private lateinit var workTypes: WorkTypeRepository
    private lateinit var settings: com.workly.app.data.prefs.SettingsRepository

    private val nine = Instant.parse("2026-09-16T09:00:00Z")
    private val five = Instant.parse("2026-09-16T17:00:00Z")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        database = Room.inMemoryDatabaseBuilder(context, WorklyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = WorkRepository(database.workSessionDao(), database.workTypeDao())
        settings = com.workly.app.data.prefs.SettingsRepository(context)
        workTypes = WorkTypeRepository(
            workTypeDao = database.workTypeDao(),
            settingsRepository = settings,
            defaultNames = listOf("Main Job"),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun errorOf(result: Result<*>): WorklyError =
        (result.exceptionOrNull() as WorklyException).error

    @Test
    fun startWorkCreatesARunningSession() = runTest {
        val id = repository.startWork(1_600L, "JPY", null, now = nine).getOrThrow()

        val active = repository.activeSession.first()
        assertEquals(id, active?.id)
        assertTrue(active!!.isActive)
        assertNull(active.endTime)
        assertEquals(1_600L, active.hourlyRateMinor)
    }

    @Test
    fun aSecondStartIsRejectedWhileOneIsRunning() = runTest {
        repository.startWork(1_600L, "JPY", null, now = nine)

        val second = repository.startWork(1_600L, "JPY", null, now = nine.plusSeconds(60))

        assertTrue(second.isFailure)
        assertEquals(WorklyError.ACTIVE_SESSION_EXISTS, errorOf(second))
        assertEquals(1, repository.sessionCount())
    }

    @Test
    fun finishWorkStoresTheConfirmedValues() = runTest {
        val id = repository.startWork(1_600L, "JPY", null, now = nine).getOrThrow()

        repository.finishWork(
            sessionId = id,
            endTime = five,
            breakMinutes = 60,
            hourlyRateMinor = 2_000L,
            currency = "JPY",
            workType = null,
            note = "  shipped the release  ",
        ).getOrThrow()

        val stored = repository.getSession(id)!!
        assertFalse(stored.isActive)
        assertEquals(five, stored.endTime)
        assertEquals(60, stored.breakMinutes)
        assertEquals(2_000L, stored.hourlyRateMinor)
        assertEquals("shipped the release", stored.note)
        assertTrue(repository.activeSession.first() == null)
    }

    @Test
    fun finishWorkRejectsAnInvalidBreak() = runTest {
        val id = repository.startWork(1_600L, "JPY", null, now = nine).getOrThrow()

        val result = repository.finishWork(id, five, breakMinutes = 9_999, hourlyRateMinor = 100L, currency = "JPY", workType = null, note = "")

        assertTrue(result.isFailure)
        assertEquals(WorklyError.BREAK_LONGER_THAN_WORK, errorOf(result))
        // The session is still running so nothing is lost.
        assertTrue(repository.getSession(id)!!.isActive)
    }

    @Test
    fun finishWorkRejectsIdenticalTimes() = runTest {
        val id = repository.startWork(1_600L, "JPY", null, now = nine).getOrThrow()

        val result = repository.finishWork(id, nine, 0, 1_600L, "JPY", null, "")

        assertTrue(result.isFailure)
        assertEquals(WorklyError.END_TIME_EQUALS_START, errorOf(result))
    }

    @Test
    fun manualSessionIsCreatedAndValidated() = runTest {
        val id = repository.createManualSession(nine, five, 60, 1_600L, "JPY", null, "manual").getOrThrow()

        val stored = repository.getSession(id)!!
        assertEquals(60, stored.breakMinutes)
        // Eight hours minus a one hour break.
        assertEquals(420L, stored.workedMinutes)
        assertEquals(11_200L, stored.incomeMinor)
    }

    @Test
    fun manualSessionWithAnEndBeforeTheStartIsRejected() = runTest {
        val result = repository.createManualSession(five, nine, 0, 1_600L, "JPY", null, "")

        assertTrue(result.isFailure)
        assertEquals(WorklyError.END_BEFORE_START, errorOf(result))
    }

    @Test
    fun negativeRatesAreRejected() = runTest {
        val result = repository.createManualSession(nine, five, 0, -1L, "JPY", null, "")

        assertTrue(result.isFailure)
        assertEquals(WorklyError.NEGATIVE_RATE, errorOf(result))
    }

    @Test
    fun updateSessionReplacesTheStoredValues() = runTest {
        val id = repository.createManualSession(nine, five, 0, 1_600L, "JPY", null, "first").getOrThrow()
        val stored = repository.getSession(id)!!

        repository.updateSession(
            session = stored,
            startTime = nine,
            endTime = five,
            breakMinutes = 30,
            hourlyRateMinor = 2_500L,
            currency = "JPY",
            workType = null,
            note = "edited",
        ).getOrThrow()

        val updated = repository.getSession(id)!!
        assertEquals(30, updated.breakMinutes)
        assertEquals(2_500L, updated.hourlyRateMinor)
        assertEquals("edited", updated.note)
    }

    @Test
    fun changingAWorkTypeRateDoesNotTouchHistory() = runTest {
        val typeId = workTypes.add("Main Job", 1_600L, "JPY").getOrThrow()
        val type = workTypes.getById(typeId)!!
        val id = repository.createManualSession(nine, five, 0, 1_600L, "JPY", type, "").getOrThrow()

        workTypes.update(type, "Main Job", 9_999L).getOrThrow()

        val stored = repository.getSession(id)!!
        assertEquals(1_600L, stored.hourlyRateMinor)
        assertEquals("Main Job", stored.workTypeName)
    }

    @Test
    fun discardRemovesTheRunningSession() = runTest {
        repository.startWork(1_600L, "JPY", null, now = nine)

        repository.discardActiveSession().getOrThrow()

        assertNull(repository.activeSession.first())
        assertEquals(0, repository.sessionCount())
    }

    @Test
    fun deleteAndDeleteEverything() = runTest {
        val first = repository.createManualSession(nine, five, 0, 1_600L, "JPY", null, "").getOrThrow()
        repository.createManualSession(nine, five, 0, 1_600L, "JPY", null, "")

        repository.deleteSession(first).getOrThrow()
        assertEquals(1, repository.sessionCount())

        repository.deleteEverything().getOrThrow()
        assertEquals(0, repository.sessionCount())
    }

    @Test
    fun workTypesAreValidatedAndUnique() = runTest {
        workTypes.add("Main Job", 1_600L, "JPY").getOrThrow()

        assertEquals(WorklyError.DUPLICATE_WORK_TYPE_NAME, errorOf(workTypes.add("Main Job", 100L, "JPY")))
        assertEquals(WorklyError.EMPTY_WORK_TYPE_NAME, errorOf(workTypes.add("   ", 100L, "JPY")))
        assertEquals(WorklyError.NEGATIVE_RATE, errorOf(workTypes.add("Other", -5L, "JPY")))
        assertEquals(1, workTypes.getAll().size)
    }

    @Test
    fun defaultWorkTypesAreSeededOnceAndThenRemembered() = runTest {
        // The flag lives in DataStore, so it is set explicitly to keep this test
        // independent of whatever the installed app has already done.
        settings.setDefaultsSeeded(false)

        workTypes.ensureDefaultWorkTypes()
        assertEquals(1, workTypes.getAll().size)

        workTypes.deleteAll().getOrThrow()
        workTypes.ensureDefaultWorkTypes()

        // The flag is remembered, so a user who deletes everything keeps an
        // empty list.
        assertEquals(0, workTypes.getAll().size)
    }
}
