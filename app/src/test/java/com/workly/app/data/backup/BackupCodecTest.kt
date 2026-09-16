package com.workly.app.data.backup

import com.workly.app.domain.WorklyError
import com.workly.app.domain.WorklyException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    private val backup = WorklyBackup(
        schemaVersion = WorklyBackup.CURRENT_SCHEMA_VERSION,
        appVersion = "1.0.0",
        exportedAt = 1_760_000_000_000L,
        workTypes = listOf(
            BackupWorkType(
                id = 1L,
                name = "Main Job",
                defaultHourlyRateMinor = 160_000L,
                currency = "JPY",
                createdAt = 1L,
                updatedAt = 2L,
            ),
        ),
        sessions = listOf(
            BackupSession(
                id = 5L,
                externalId = "abc-123",
                startTime = 1_760_000_000_000L,
                endTime = 1_760_028_800_000L,
                breakMinutes = 60,
                hourlyRateMinor = 160_000L,
                currency = "JPY",
                workTypeId = 1L,
                workTypeName = "Main Job",
                note = "Line one\nLine \"two\"",
                createdAt = 1L,
                updatedAt = 2L,
            ),
        ),
    )

    @Test
    fun `round trip keeps every field`() {
        val decoded = BackupCodec.decode(BackupCodec.encode(backup)).getOrThrow()

        assertEquals(backup, decoded)
    }

    @Test
    fun `unknown fields are ignored so older versions stay readable`() {
        val json = BackupCodec.encode(backup).replaceFirst("{", "{\"futureField\": 42,")

        val decoded = BackupCodec.decode(json).getOrThrow()

        assertEquals(1, decoded.sessions.size)
    }

    @Test
    fun `blank input is rejected`() {
        val result = BackupCodec.decode("   ")

        assertTrue(result.isFailure)
        assertEquals(
            WorklyError.INVALID_IMPORT_FILE,
            (result.exceptionOrNull() as WorklyException).error,
        )
    }

    @Test
    fun `malformed json is rejected without throwing`() {
        val result = BackupCodec.decode("{ not json at all ")

        assertTrue(result.isFailure)
        assertEquals(
            WorklyError.INVALID_IMPORT_FILE,
            (result.exceptionOrNull() as WorklyException).error,
        )
    }

    @Test
    fun `a newer schema version is refused`() {
        val json = BackupCodec.encode(backup)
            .replace("\"schemaVersion\": ${WorklyBackup.CURRENT_SCHEMA_VERSION}", "\"schemaVersion\": 99")

        val result = BackupCodec.decode(json)

        assertTrue(result.isFailure)
        assertEquals(
            WorklyError.UNSUPPORTED_BACKUP_VERSION,
            (result.exceptionOrNull() as WorklyException).error,
        )
    }

    @Test
    fun `an empty backup decodes to empty lists`() {
        val decoded = BackupCodec.decode("{}").getOrThrow()

        assertTrue(decoded.sessions.isEmpty())
        assertTrue(decoded.workTypes.isEmpty())
    }
}
