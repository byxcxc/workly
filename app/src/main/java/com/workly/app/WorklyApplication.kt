package com.workly.app

import android.app.Application

/**
 * Application entry point.
 *
 * Workly is a small single-module app, so dependencies are wired by hand in
 * [AppGraph] instead of using a dependency injection framework. This keeps the
 * project easy to follow and to maintain.
 */
class WorklyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
