package com.workly.app.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `every supported language maps from its own tag`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromTag("ja"))
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag("zh-Hans"))
    }

    @Test
    fun `regional and script variants map onto the shipped language`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-US"))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-GB"))
        assertEquals(AppLanguage.JAPANESE, AppLanguage.fromTag("ja-JP"))
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag("zh-Hans-CN"))
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag("zh-CN"))
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag("zh-TW"))
        assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag("ZH-hant"))
    }

    @Test
    fun `an unset or unknown language falls back to following the system`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("de-DE"))
    }

    @Test
    fun `the tags handed to the platform are the ones the resources use`() {
        assertEquals(null, AppLanguage.SYSTEM.tag)
        assertEquals("en", AppLanguage.ENGLISH.tag)
        assertEquals("ja", AppLanguage.JAPANESE.tag)
        // values-zh is selected by the language part of zh-Hans.
        assertEquals("zh-Hans", AppLanguage.CHINESE.tag)
    }
}
