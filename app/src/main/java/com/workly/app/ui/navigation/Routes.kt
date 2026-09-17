package com.workly.app.ui.navigation

/**
 * Every destination in the app.
 *
 * The four top level destinations are the ones shown in the bottom bar; the rest
 * are pushed on top of them. Detail and edit routes use their own prefixes so a
 * record id can never be confused with the literal `new` route.
 */
object Routes {
    const val HOME = "home"
    const val RECORDS = "records"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"

    const val WORK_TYPES = "settings/work-types"
    const val ABOUT = "settings/about"

    /**
     * Adding a record. The date is optional: the calendar passes one when the
     * user long-presses a day, everything else just opens "today".
     */
    const val ADD_RECORD = "record/new?date={date}"
    const val SESSION_DETAIL = "record/detail/{sessionId}"
    const val SESSION_EDIT = "record/edit/{sessionId}"
    const val SESSION_FINISH = "session/finish/{sessionId}"

    const val ARG_SESSION_ID = "sessionId"
    const val ARG_DATE = "date"

    val bottomBarRoutes = setOf(HOME, RECORDS, STATISTICS, SETTINGS)

    /** Opens the new-record screen on [date]; `null` means today. */
    fun addRecordOn(date: java.time.LocalDate?): String =
        if (date == null) "record/new" else "record/new?date=${date.toEpochDay()}"

    fun sessionDetail(sessionId: Long) = "record/detail/$sessionId"

    fun sessionEdit(sessionId: Long) = "record/edit/$sessionId"

    fun sessionFinish(sessionId: Long) = "session/finish/$sessionId"
}
