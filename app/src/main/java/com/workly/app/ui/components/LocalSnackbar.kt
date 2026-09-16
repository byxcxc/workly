package com.workly.app.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The app's single snackbar host, provided once at the top of the tree so any
 * screen can report a problem without threading callbacks through the navigation
 * graph.
 */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState was provided")
}

/** Shows [message], replacing whatever is currently on screen. */
fun SnackbarHostState.showMessage(scope: CoroutineScope, message: String) {
    scope.launch {
        currentSnackbarData?.dismiss()
        showSnackbar(message)
    }
}
