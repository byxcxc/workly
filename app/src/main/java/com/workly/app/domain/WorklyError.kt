package com.workly.app.domain

/**
 * Everything that can go wrong in a way the user should be told about.
 *
 * Repositories and validators return these instead of throwing, and the UI maps
 * each entry to a localized message. Stack traces are never shown to the user.
 */
enum class WorklyError {
    /** A session is already running; only one can be active at a time. */
    ACTIVE_SESSION_EXISTS,

    /** Tried to finish a session that is not running (e.g. deleted meanwhile). */
    NO_ACTIVE_SESSION,

    /** Start and end time are identical. */
    END_TIME_EQUALS_START,

    /** The break is longer than the elapsed time. */
    BREAK_LONGER_THAN_WORK,

    /** End time is before start time and the session does not cross midnight. */
    END_BEFORE_START,

    /** The hourly rate is negative. */
    NEGATIVE_RATE,

    /** A work type name was blank. */
    EMPTY_WORK_TYPE_NAME,

    /** A work type with that name already exists. */
    DUPLICATE_WORK_TYPE_NAME,

    /** The chosen file could not be read or is not valid JSON. */
    INVALID_IMPORT_FILE,

    /** The chosen destination could not be written to. */
    EXPORT_FAILED,

    /** Reading the picked file failed. */
    IMPORT_FAILED,

    /** The backup was produced by a newer, unsupported version of the app. */
    UNSUPPORTED_BACKUP_VERSION,

    /** Room / SQLite failure. */
    DATABASE_ERROR,

    /** Anything else. */
    UNKNOWN,
}
