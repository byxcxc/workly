package com.workly.app.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.workly.app.R
import com.workly.app.domain.WorklyError

/**
 * Maps a [WorklyError] to a short, localized sentence. Stack traces are never
 * shown to the user.
 *
 * [messageRes] is a plain function so it can also be used from coroutines, where
 * `stringResource` is not available.
 */
fun WorklyError.messageRes(): Int = when (this) {
    WorklyError.ACTIVE_SESSION_EXISTS -> R.string.error_active_session_exists
    WorklyError.NO_ACTIVE_SESSION -> R.string.error_no_active_session
    WorklyError.END_TIME_EQUALS_START -> R.string.error_end_time_equals_start
    WorklyError.BREAK_LONGER_THAN_WORK -> R.string.error_break_longer_than_work
    WorklyError.END_BEFORE_START -> R.string.error_end_before_start
    WorklyError.NEGATIVE_RATE -> R.string.error_negative_rate
    WorklyError.EMPTY_WORK_TYPE_NAME -> R.string.error_empty_work_type_name
    WorklyError.DUPLICATE_WORK_TYPE_NAME -> R.string.error_duplicate_work_type_name
    WorklyError.INVALID_IMPORT_FILE -> R.string.error_invalid_import_file
    WorklyError.EXPORT_FAILED -> R.string.export_failed
    WorklyError.IMPORT_FAILED -> R.string.import_failed
    WorklyError.UNSUPPORTED_BACKUP_VERSION -> R.string.error_unsupported_backup_version
    WorklyError.DATABASE_ERROR -> R.string.error_database_error
    WorklyError.UNKNOWN -> R.string.error_unknown
}

@Composable
fun WorklyError.message(): String = stringResource(messageRes())
