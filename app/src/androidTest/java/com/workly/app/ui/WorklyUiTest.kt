package com.workly.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
import com.workly.app.ui.records.NOTE_FIELD_TAG
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end UI tests for the flows described in the product spec: start work,
 * finish work, add a record, edit a record, delete a record and navigation.
 *
 * They drive the real app (real Room database, real DataStore) and run in the
 * instrumented CI job.
 */
@RunWith(AndroidJUnit4::class)
class WorklyUiTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun text(id: Int): String = rule.activity.getString(id)

    /** Gives every test the same starting point: no data, a known rate. */
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

    private fun awaitText(value: String, timeoutMillis: Long = 10_000L) {
        rule.waitUntil(timeoutMillis) {
            rule.onAllNodesWithText(value).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun firstLaunchShowsTheEmptyDashboard() {
        rule.onNodeWithText(text(R.string.empty_title)).assertIsDisplayed()
        rule.onNodeWithText(text(R.string.home_start_work)).assertIsDisplayed()
    }

    @Test
    fun bottomNavigationSwitchesBetweenTabs() {
        rule.onNodeWithText(text(R.string.tab_records)).performClick()
        awaitText(text(R.string.records_title))

        rule.onNodeWithText(text(R.string.tab_statistics)).performClick()
        awaitText(text(R.string.stats_title))

        rule.onNodeWithText(text(R.string.tab_settings)).performClick()
        awaitText(text(R.string.settings_title))

        rule.onNodeWithText(text(R.string.tab_home)).performClick()
        awaitText(text(R.string.empty_title))
    }

    @Test
    fun startWorkThenFinishWorkRecordsASession() {
        rule.onNodeWithText(text(R.string.home_start_work)).performClick()
        awaitText(text(R.string.home_working_now))

        rule.onNodeWithText(text(R.string.home_finish_work)).performClick()
        awaitText(text(R.string.editor_finish_title))

        // The end time defaults to now and the rate comes from settings, so the
        // editor is already valid.
        rule.onNodeWithText(text(R.string.editor_finish_and_save)).performClick()

        awaitText(text(R.string.home_worked_today))
        assertTrue(
            runBlocking { AppGraph.workRepository.sessionCount() } == 1,
        )
    }

    @Test
    fun addRecordManuallyFromTheRecordsTab() {
        rule.onNodeWithText(text(R.string.tab_records)).performClick()
        awaitText(text(R.string.records_title))

        rule.onNodeWithContentDescription(text(R.string.records_add)).performClick()
        awaitText(text(R.string.editor_add_title))

        rule.onNodeWithTag(NOTE_FIELD_TAG).performTextInput("Sprint planning")
        rule.onNodeWithText(text(R.string.editor_save_record)).performClick()

        awaitText(text(R.string.records_title))
        awaitText("Sprint planning")
        assertTrue(runBlocking { AppGraph.workRepository.sessionCount() } == 1)
    }

    @Test
    fun editAnExistingRecord() {
        addRecordManuallyFromTheRecordsTab()

        rule.onNodeWithText("Sprint planning").performClick()
        awaitText(text(R.string.records_detail_title))

        rule.onNodeWithContentDescription(text(R.string.cd_edit_record)).performClick()
        awaitText(text(R.string.editor_edit_title))

        rule.onNodeWithTag(NOTE_FIELD_TAG).performTextClearance()
        rule.onNodeWithTag(NOTE_FIELD_TAG).performTextInput("Retrospective")
        rule.onNodeWithText(text(R.string.editor_save_record)).performClick()

        awaitText("Retrospective")
        assertTrue(
            runBlocking { AppGraph.workRepository.getAllSessions().single().note == "Retrospective" },
        )
    }

    @Test
    fun deleteARecordAfterConfirming() {
        addRecordManuallyFromTheRecordsTab()

        rule.onNodeWithText("Sprint planning").performClick()
        awaitText(text(R.string.records_detail_title))

        rule.onNodeWithContentDescription(text(R.string.action_delete)).performClick()
        awaitText(text(R.string.delete_record_title))
        rule.onNodeWithText(text(R.string.action_delete)).performClick()

        awaitText(text(R.string.records_empty_title))
        assertTrue(runBlocking { AppGraph.workRepository.sessionCount() } == 0)
    }

    @Test
    fun theRunningSessionSurvivesLeavingTheDashboard() {
        rule.onNodeWithText(text(R.string.home_start_work)).performClick()
        awaitText(text(R.string.home_working_now))

        rule.onNodeWithText(text(R.string.tab_records)).performClick()
        awaitText(text(R.string.records_title))
        rule.onNodeWithText(text(R.string.tab_home)).performClick()

        // Reopening the dashboard restores the running session from the database.
        awaitText(text(R.string.home_working_now))
    }
}
