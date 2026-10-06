package com.quicknotes.app.ui.editor

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LinksSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun hiddenWhenThereAreNoLinks() {
        composeRule.setContent { LinksSection(emptyList(), emptyList(), onOpenNote = {}, onCreateGhost = {}) }
        composeRule.onNodeWithTag("links_section").assertDoesNotExist()
    }

    @Test
    fun ghostRowCallsCreateWithItsTitle() {
        val opened = mutableListOf<Long>()
        val created = mutableListOf<String>()
        composeRule.setContent {
            LinksSection(
                backlinks = listOf(NoteRef(3, "Origem")),
                outgoing = listOf(NoteLink(1, 0, 2, "Real"), NoteLink(1, 1, null, "Futura"), NoteLink(1, 2, 2, "real")),
                onOpenNote = { opened += it },
                onCreateGhost = { created += it }
            )
        }

        composeRule.onNodeWithText("fantasma").assertExists()
        composeRule.onNodeWithText("Futura").performClick()
        composeRule.onNodeWithText("Real").performClick()
        composeRule.onNodeWithText("Origem").performClick()

        assertEquals(listOf("Futura"), created)
        assertEquals(listOf(2L, 3L), opened)
        composeRule.onNodeWithText("real").assertDoesNotExist() // same target listed once, first occurrence wins
    }
}
