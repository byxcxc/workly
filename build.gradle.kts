// Top-level build file. Plugins are declared here (without applying them) so that
// compatibility is resolved once and shared by every module.
//
// AGP 9 has *built-in Kotlin* support, so the `org.jetbrains.kotlin.android`
// plugin is no longer applied. AGP brings its own Kotlin Gradle plugin version;
// the buildscript block below pins the newer Kotlin release this project uses,
// which also has to match the Compose compiler and kotlinx.serialization plugins.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        classpath("com.google.devtools.ksp:symbol-processing-gradle-plugin:${libs.versions.ksp.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

