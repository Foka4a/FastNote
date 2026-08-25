package com.quicknotes.app.ui.nav

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickNotesNavHostTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launchesOnInboxRouteByDefault() {
        // Proves the NavHost's startDestination actually resolved to the Inbox screen
        // (InboxScreen.kt tags its LazyColumn "inbox_list" for exactly this check) rather
        // than crashing or landing on a blank composable.
        composeRule.onNodeWithTag("inbox_list").assertExists()
    }
}
