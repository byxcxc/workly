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

    const val ADD_RECORD = "record/new"
    const val SESSION_DETAIL = "record/detail/{sessionId}"
    const val SESSION_EDIT = "record/edit/{sessionId}"
    const val SESSION_FINISH = "session/finish/{sessionId}"

    const val ARG_SESSION_ID = "sessionId"

    val bottomBarRoutes = setOf(HOME, RECORDS, STATISTICS, SETTINGS)

    fun sessionDetail(sessionId: Long) = "record/detail/$sessionId"

    fun sessionEdit(sessionId: Long) = "record/edit/$sessionId"

    fun sessionFinish(sessionId: Long) = "session/finish/$sessionId"
}
