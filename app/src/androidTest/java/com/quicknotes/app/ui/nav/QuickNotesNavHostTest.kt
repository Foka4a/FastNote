package com.quicknotes.app.ui.nav

import android.Manifest
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.quicknotes.app.MainActivity
import org.junit.BeforeClass
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickNotesNavHostTest {
    companion object {
        // SYSTEM_ALERT_WINDOW is a special permission GrantPermissionRule can't grant;
        // pre-grant it via appops so MainActivity's Task 14 onboarding gate doesn't
        // intercept this test (which is about NavHost routing, not the permission flow).
        // Must run before the compose rule below launches the activity.
        @JvmStatic
        @BeforeClass
        fun grantOverlayPermission() {
            val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
            val pfd = InstrumentationRegistry.getInstrumentation().uiAutomation
                .executeShellCommand("appops set $packageName SYSTEM_ALERT_WINDOW allow")
            ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
        }
    }

    // Pre-grants RECORD_AUDIO so MainActivity's runtime permission request (Task 14)
    // doesn't pop the system dialog over the activity mid-test.
    @get:Rule
    val permissionRule: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.RECORD_AUDIO)

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesOnInboxRouteByDefault() {
        // Proves the NavHost's startDestination actually resolved to the Inbox screen
        // (InboxScreen.kt tags its LazyColumn "inbox_list" for exactly this check) rather
        // than crashing or landing on a blank composable.
        composeRule.onNodeWithTag("inbox_list").assertExists()
    }

    @Test
    fun fabOpensEditorForANewNote() {
        // The FAB is the only in-app entry point to "create a note", so it has to land on
        // the Editor route with noteId=0 (treated as "new note" by the NavHost).
        composeRule.onNodeWithTag("new_note_fab").performClick()

        composeRule.onNodeWithContentDescription("Salvar").assertExists()
        composeRule.onNodeWithTag("inbox_list").assertDoesNotExist()
    }

    @Test
    fun overflowMenuReachesSettings() {
        // Settings (and the other five secondary screens) had no entry point at all before
        // the top bar menu existed; this proves the menu actually navigates.
        composeRule.onNodeWithTag("nav_menu_button").performClick()
        composeRule.onNodeWithText("Ajustes").performClick()

        composeRule.onNodeWithText("DEPOIS DE TRANSCREVER A VOZ").assertExists()
        composeRule.onNodeWithText("Revisar antes de salvar").assertExists()
    }

    @Test
    fun linkButtonOpensNotePicker() {
        composeRule.onNodeWithTag("new_note_fab").performClick()
        composeRule.onNodeWithContentDescription("Ligar nota").performClick()

        composeRule.onNodeWithTag("link_picker_query").assertExists()
    }
}
