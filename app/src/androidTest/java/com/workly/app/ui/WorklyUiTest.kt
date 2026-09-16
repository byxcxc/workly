package com.workly.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workly.app.AppGraph
import com.workly.app.MainActivity
import com.workly.app.R
import com.workly.app.data.prefs.ThemeMode
import com.workly.app.domain.DayOverride
import kotlinx.coroutines.flow.first
import com.workly.app.ui.components.CONFIRM_BUTTON_TAG
import com.workly.app.ui.navigation.Routes
import com.workly.app.ui.records.NOTE_FIELD_TAG
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI tests for the flows in the product spec: start work, finish work,
 * add a record, edit a record, delete a record and navigation.
 *
 * They drive the real app against a real Room database and DataStore, and run in
 * the instrumented CI job (`.github/workflows/android-instrumented-tests.yml`).
 *
 * Two rules keep them reliable:
 *  - anything inside a scrolling screen is scrolled into view before it is
 *    clicked (the record editor is a long form);
 *  - a step only counts as finished once the *database* agrees, never just
 *    because some text is on screen — the editor shows the very note that the
 *    list will show.
 */
@RunWith(AndroidJUnit4::class)
class WorklyUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int): String = rule.activity.getString(id)

    /** Every test starts from the same place: no data, a known rate. */
    @Before
    fun resetState() {
        runBlocking {
            AppGraph.workRepository.discardActiveSession()
            AppGraph.workRepository.deleteEverything().getOrThrow()
            AppGraph.settingsRepository.setCurrency("JPY")
            AppGraph.settingsRepository.setDefaultHourlyRateMinor(100_000L)
            AppGraph.settingsRepository.setThemeMode(ThemeMode.LIGHT)
            AppGraph.settingsRepository.settings.first().dayOverrides.keys.forEach { date ->
                AppGraph.settingsRepository.setDayOverride(date, DayOverride())
            }
        }
        rule.waitForIdle()
    }

    // ------------------------------------------------------------------ helpers

    private fun awaitText(
        value: String,
        substring: Boolean = false,
        timeoutMillis: Long = 15_000L,
    ) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(value, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitTextGone(value: String, timeoutMillis: Long = 15_000L) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(value).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun sessionCount(): Int = runBlocking { AppGraph.workRepository.sessionCount() }

    /** Sessions that have been finished; a running session is not counted. */
    private fun finishedCount(): Int =
        runBlocking { AppGraph.workRepository.getAllSessions().count { it.endTime != null } }

    private fun awaitFinishedCount(expected: Int, timeoutMillis: Long = 15_000L) {
        rule.waitUntil(timeoutMillis) { finishedCount() == expected }
    }

    private fun awaitSessionCount(expected: Int, timeoutMillis: Long = 15_000L) {
        rule.waitUntil(timeoutMillis) { sessionCount() == expected }
    }

    private fun countTextNodes(value: String): Int =
        rule.onAllNodesWithText(value).fetchSemanticsNodes().size

    /** Clicks a node, scrolling it into view first when it sits in a long form. */
    private fun clickText(value: String, substring: Boolean = false, scroll: Boolean = false) {
        val node = rule.onNodeWithText(value, substring = substring)
        if (scroll) node.performScrollTo()
        node.performClick()
    }

    /**
     * Taps whichever "start" button the dashboard is currently showing: the
     * first-run panel before any record exists, the regular button afterwards.
     */
    private fun startWorkFromDashboard() {
        val firstRunAction = text(R.string.empty_action)
        if (countTextNodes(firstRunAction) > 0) {
            clickText(firstRunAction, scroll = true)
        } else {
            clickText(text(R.string.home_start_work), scroll = true)
        }
    }

    private fun openTab(route: String) {
        rule.onNodeWithTag(bottomTabTag(route)).performClick()
    }

    /** Adds a record through the UI and returns once it is really in the database. */
    private fun addRecordWithNote(note: String) {
        openTab(Routes.RECORDS)
        awaitText(text(R.string.records_empty_title))

        rule.onNodeWithContentDescription(text(R.string.records_add)).performClick()
        awaitText(text(R.string.editor_save_record))

        rule.onNodeWithTag(NOTE_FIELD_TAG).performScrollTo().performTextInput(note)
        clickText(text(R.string.editor_save_record), scroll = true)

        awaitSessionCount(1)
        awaitTextGone(text(R.string.editor_save_record))
    }

    // -------------------------------------------------------------------- tests

    @Test
    fun firstLaunchShowsTheEmptyDashboard() {
        rule.onNodeWithText(text(R.string.empty_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.empty_action)).assertIsDisplayed()
    }

    @Test
    fun bottomNavigationSwitchesBetweenTabs() {
        openTab(Routes.RECORDS)
        awaitText(text(R.string.records_empty_title))

        openTab(Routes.STATISTICS)
        awaitText(text(R.string.stats_empty_title))

        openTab(Routes.SETTINGS)
        awaitText(text(R.string.settings_default_rate))

        openTab(Routes.HOME)
        awaitText(text(R.string.empty_title))
    }

    @Test
    fun longPressingACalendarDayOpensTheDayEditor() {
        openTab(Routes.RECORDS)
        awaitText(text(R.string.records_empty_title))
        clickText(text(R.string.records_view_calendar), scroll = true)

        // Today's cell is found by its accessibility label, which is the date.
        val today = LocalDate.now()
        val locale = rule.activity.resources.configuration.locales[0]
        val label = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(locale)
            .format(today)

        rule.onNode(hasContentDescription(label, substring = true))
            .performTouchInput { longClick() }

        awaitText(text(R.string.day_override_rest))
        rule.onNodeWithText(text(R.string.day_override_rest)).performClick()
        clickText(text(R.string.action_save))

        val stored = runBlocking { AppGraph.settingsRepository.settings.first() }.dayOverrides[today]
        // The weekly pattern for today is untouched, so the override follows it
        // unless the day was already a rest day.
        assertNotNull(stored)
    }

    @Test
    fun theLanguagePickerOffersEverySupportedLanguage() {
        openTab(Routes.SETTINGS)
        awaitText(text(R.string.settings_language))

        clickText(text(R.string.settings_language), scroll = true)

        // The names are never translated, so they read the same in every locale.
        awaitText(text(R.string.language_name_english))
        awaitText(text(R.string.language_name_japanese))
        awaitText(text(R.string.language_name_chinese))
        awaitText(text(R.string.settings_language_system))
    }

    @Test
    fun theDashboardCanOpenTheManualRecorder() {
        clickText(text(R.string.home_add_record), scroll = true)

        awaitText(text(R.string.editor_add_title))
    }

    @Test
    fun startWorkThenFinishWorkRecordsASession() {
        startWorkFromDashboard()
        awaitText(text(R.string.home_working_now))

        clickText(text(R.string.home_finish_work), scroll = true)
        awaitText(text(R.string.editor_finish_and_save))

        // The end time defaults to now and the rate comes from settings, so the
        // editor is already valid.
        clickText(text(R.string.editor_finish_and_save), scroll = true)

        awaitFinishedCount(1)
        awaitText(text(R.string.home_worked_today))
    }

    @Test
    fun addRecordManuallyFromTheRecordsTab() {
        addRecordWithNote("Sprint planning")

        assertEquals(1, sessionCount())
        awaitText("Sprint planning", substring = true)
    }

    @Test
    fun editAnExistingRecord() {
        addRecordWithNote("Sprint planning")

        clickText("Sprint planning", substring = true)
        awaitText(text(R.string.records_detail_title))

        rule.onNodeWithContentDescription(text(R.string.cd_edit_record)).performClick()
        awaitText(text(R.string.editor_edit_title))

        rule.onNodeWithTag(NOTE_FIELD_TAG).performScrollTo().performTextClearance()
        rule.onNodeWithTag(NOTE_FIELD_TAG).performTextInput("Retrospective")
        clickText(text(R.string.editor_save_record), scroll = true)

        rule.waitUntil(15_000L) {
            runBlocking { AppGraph.workRepository.getAllSessions().single().note } == "Retrospective"
        }
        awaitText("Retrospective", substring = true)
    }

    @Test
    fun deleteARecordAfterConfirming() {
        addRecordWithNote("Sprint planning")

        clickText("Sprint planning", substring = true)
        awaitText(text(R.string.records_detail_title))

        clickText(text(R.string.action_delete), scroll = true)
        awaitText(text(R.string.delete_record_title))
        rule.onNodeWithTag(CONFIRM_BUTTON_TAG).performClick()

        awaitSessionCount(0)
        awaitText(text(R.string.records_empty_title))
    }

    @Test
    fun theRunningSessionSurvivesLeavingTheDashboard() {
        startWorkFromDashboard()
        awaitText(text(R.string.home_working_now))

        openTab(Routes.RECORDS)
        awaitText(text(R.string.records_empty_title))
        openTab(Routes.HOME)

        // The dashboard restores the running session from the database.
        awaitText(text(R.string.home_working_now))
        assertEquals(1, sessionCount())
    }
}
