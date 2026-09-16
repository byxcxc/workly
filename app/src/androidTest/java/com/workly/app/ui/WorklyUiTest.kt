package com.workly.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.workly.app.AppGraph
import com.workly.app.MainActivity
import com.workly.app.R
import com.workly.app.data.prefs.ThemeMode
import com.workly.app.ui.navigation.Routes
import com.workly.app.ui.records.NOTE_FIELD_TAG
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
        }
        rule.waitForIdle()
    }

    private fun awaitText(value: String, substring: Boolean = false, timeoutMillis: Long = 10_000L) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(value, substring = substring).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun countTextNodes(value: String): Int =
        rule.onAllNodesWithText(value).fetchSemanticsNodes().size

    /**
     * Taps whichever "start" button the dashboard is currently showing: the
     * first-run panel before any record exists, the regular button afterwards.
     */
    private fun startWorkFromDashboard() {
        val firstRunAction = text(R.string.empty_action)
        if (countTextNodes(firstRunAction) > 0) {
            rule.onNodeWithText(firstRunAction).performClick()
        } else {
            rule.onNodeWithText(text(R.string.home_start_work)).performClick()
        }
    }

    private fun openTab(route: String) {
        rule.onNodeWithTag(bottomTabTag(route)).performClick()
    }

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
    fun startWorkThenFinishWorkRecordsASession() {
        startWorkFromDashboard()
        awaitText(text(R.string.home_working_now))

        rule.onNodeWithText(text(R.string.home_finish_work)).performClick()
        awaitText(text(R.string.editor_finish_and_save))

        // The end time defaults to now and the rate comes from settings, so the
        // editor is already valid.
        rule.onNodeWithText(text(R.string.editor_finish_and_save)).performClick()

        awaitText(text(R.string.home_worked_today))
        assertEquals(1, runBlocking { AppGraph.workRepository.sessionCount() })
    }

    @Test
    fun addRecordManuallyFromTheRecordsTab() {
        addRecordWithNote("Sprint planning")

        awaitText("Sprint planning", substring = true)
        assertEquals(1, runBlocking { AppGraph.workRepository.sessionCount() })
    }

    @Test
    fun editAnExistingRecord() {
        addRecordWithNote("Sprint planning")

        rule.onNodeWithText("Sprint planning", substring = true).performClick()
        awaitText(text(R.string.records_detail_title))

        rule.onNodeWithContentDescription(text(R.string.cd_edit_record)).performClick()
        awaitText(text(R.string.editor_edit_title))

        rule.onNodeWithTag(NOTE_FIELD_TAG).performTextClearance()
        rule.onNodeWithTag(NOTE_FIELD_TAG).performTextInput("Retrospective")
        rule.onNodeWithText(text(R.string.editor_save_record)).performClick()

        awaitText("Retrospective", substring = true)
        assertEquals(
            "Retrospective",
            runBlocking { AppGraph.workRepository.getAllSessions().single().note },
        )
    }

    @Test
    fun deleteARecordAfterConfirming() {
        addRecordWithNote("Sprint planning")

        rule.onNodeWithText("Sprint planning", substring = true).performClick()
        awaitText(text(R.string.records_detail_title))

        rule.onNodeWithContentDescription(text(R.string.action_delete)).performClick()
        awaitText(text(R.string.delete_record_title))
        rule.onNodeWithText(text(R.string.action_delete)).performClick()

        awaitText(text(R.string.records_empty_title))
        assertEquals(0, runBlocking { AppGraph.workRepository.sessionCount() })
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
        assertEquals(1, runBlocking { AppGraph.workRepository.sessionCount() })
    }

    /** Adds a record through the UI and waits until it is listed. */
    private fun addRecordWithNote(note: String) {
        openTab(Routes.RECORDS)
        awaitText(text(R.string.records_empty_title))

        rule.onNodeWithContentDescription(text(R.string.records_add)).performClick()
        awaitText(text(R.string.editor_save_record))

        rule.onNodeWithTag(NOTE_FIELD_TAG).performTextInput(note)
        rule.onNodeWithText(text(R.string.editor_save_record)).performClick()

        awaitText(note, substring = true)
    }
}
