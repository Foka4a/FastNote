package com.quicknotes.app.ui.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf

/** Hoisted at the app shell (QuickNotesNavHost) so any screen can surface an undo snackbar
 *  without every screen signature threading a SnackbarHostState through. */
val LocalSnackbarHostState = staticCompositionLocalOf<SnackbarHostState> {
    error("No SnackbarHostState provided")
}
