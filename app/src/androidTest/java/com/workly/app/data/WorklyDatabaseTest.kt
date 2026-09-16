package com.workly.app.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workly.app.data.local.WorklyDatabase
import com.workly.app.data.local.entity.WorkSessionEntity
import com.workly.app.data.local.entity.WorkTypeEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

/**
 * Room tests.
 *
 * These are instrumented tests rather than JVM tests because Room needs the
 * Android SQLite implementation. They run in the CI emulator job (see
 * `.github/workflows/android-instrumented-tests.yml`).
 */
@RunWith(AndroidJUnit4::class)
class WorklyDatabaseTest {

    private lateinit var database: WorklyDatabase
    private val now: Instant = Instant.parse("2026-09-16T09:00:00Z")

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            WorklyDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun session(
        start: Instant = now,
        end: Instant? = now.plusSeconds(3600),
        isActive: Boolean = false,
        externalId: String = UUID.randomUUID().toString(),
        workTypeId: Long? = null,
    ) = WorkSessionEntity(
        externalId = externalId,
        startTime = start,
        endTime = end,
        breakMinutes = 0,
        hourlyRateMinor = 1_600L,
        currency = "JPY",
        workTypeId = workTypeId,
        workTypeName = "Main Job",
        note = "",
        createdAt = start,
        updatedAt = start,
        isActive = isActive,
    )

    @Test
    fun instantsSurviveARoundTrip() = runTest {
        val dao = database.workSessionDao()
        dao.upsert(session())

        val stored = dao.getAll().single()

        assertEquals(now, stored.startTime)
        assertEquals(now.plusSeconds(3600), stored.endTime)
        assertEquals("2026-09-16T09:00:00Z", stored.startTime.toString())
    }

    @Test
    fun activeSessionIsObservable() = runTest {
        val dao = database.workSessionDao()
        dao.upsert(session(end = null, isActive = true))

        val active = dao.observeActive().first()

        assertNotNull(active)
        assertTrue(active!!.isActive)
        assertNull(active.endTime)
    }

    @Test
    fun completedSessionsExcludeTheRunningOne() = runTest {
        val dao = database.workSessionDao()
        dao.upsert(session(end = null, isActive = true))
        dao.upsert(session(start = now.minusSeconds(7200), end = now.minusSeconds(3600)))

        assertEquals(1, dao.observeCompleted().first().size)
    }

    @Test
    fun sessionsAreReturnedNewestFirst() = runTest {
        val dao = database.workSessionDao()
        dao.upsert(session(start = now.minusSeconds(86_400), end = now.minusSeconds(82_800)))
        dao.upsert(session(start = now, end = now.plusSeconds(3600)))

        assertEquals(now, dao.observeAll().first().first().startTime)
    }

    @Test
    fun rangeQueryOnlyReturnsTheWindow() = runTest {
        val dao = database.workSessionDao()
        dao.upsert(session(start = now.minusSeconds(86_400), end = now.minusSeconds(82_800)))
        dao.upsert(session(start = now, end = now.plusSeconds(3600)))

        assertEquals(1, dao.getInRange(now.minusSeconds(60), now.plusSeconds(60)).size)
    }

    @Test
    fun importingTheSameBackupTwiceDoesNotDuplicate() = runTest {
        val dao = database.workSessionDao()
        val externalId = "stable-id"

        val first = dao.insertIgnoringDuplicates(listOf(session(externalId = externalId)))
        val second = dao.insertIgnoringDuplicates(listOf(session(externalId = externalId)))

        assertEquals(1, first.count { it != -1L })
        assertEquals(0, second.count { it != -1L })
        assertEquals(1, dao.count())
    }

    @Test
    fun deletingAWorkTypeKeepsItsRecords() = runTest {
        val typeDao = database.workTypeDao()
        val sessionDao = database.workSessionDao()
        val typeId = typeDao.insert(
            WorkTypeEntity(
                name = "Freelance",
                defaultHourlyRateMinor = 3_000L,
                currency = "JPY",
                createdAt = now,
                updatedAt = now,
            ),
        )
        sessionDao.upsert(session(workTypeId = typeId))

        typeDao.delete(typeDao.getById(typeId)!!)

        val stored = sessionDao.getAll().single()
        assertNull(stored.workTypeId)
        // The rate and name snapshots survive, so past earnings never change.
        assertEquals(1_600L, stored.hourlyRateMinor)
        assertEquals("Main Job", stored.workTypeName)
    }

    @Test
    fun workTypeNamesAreUnique() = runTest {
        val dao = database.workTypeDao()
        val type = WorkTypeEntity(
            name = "Main Job",
            defaultHourlyRateMinor = 0L,
            currency = "JPY",
            createdAt = now,
            updatedAt = now,
        )
        dao.insert(type)

        val duplicate = runCatching { dao.insert(type.copy(id = 0L)) }

        assertTrue(duplicate.isFailure)
        assertEquals(1, dao.count())
    }

    @Test
    fun countsAndBulkDeletesBehave() = runTest {
        val dao = database.workSessionDao()
        repeat(3) { dao.upsert(session(start = now.minusSeconds(it * 3600L))) }
        assertEquals(3, dao.count())

        dao.deleteAll()

        assertEquals(0, dao.count())
    }
}
