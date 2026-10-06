package com.quicknotes.app.domain.link

import com.quicknotes.app.domain.link.LinkParser.LinkInfo
import com.quicknotes.app.domain.model.NoteRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LinkParserTest {
    @Test
    fun extractsLinksInOrderWithTrimmedTitles() {
        assertEquals(
            listOf(LinkInfo("Nota A", 0), LinkInfo("Outra", 1)),
            LinkParser.extractLinks("ver [[ Nota A ]] e depois [[Outra]]")
        )
    }

    @Test
    fun ignoresSingleBracketsAndBlankLinks() {
        // Blank links do not consume an index: indexes stay dense for the (sourceId, index) PK.
        assertEquals(listOf(LinkInfo("Real", 0)), LinkParser.extractLinks("[texto normal] [[  ]] [[Real]]"))
    }

    @Test
    fun noLinksGivesEmptyList() {
        assertTrue(LinkParser.extractLinks("sem ligações").isEmpty())
    }

    @Test
    fun keepsInnerSpacesAndDuplicatesInOrder() {
        assertEquals(
            listOf(LinkInfo("Plano de aula", 0), LinkInfo("plano de aula", 1)),
            LinkParser.extractLinks("[[Plano de aula]] e [[plano de aula]]")
        )
    }

    @Test
    fun linksDoNotSpanLines() {
        assertTrue(LinkParser.extractLinks("[[abre\nfecha]]").isEmpty())
    }

    @Test
    fun titleIndexMatchesCaseAndAccentsInsensitivelyAndPrefersOldestNote() {
        val index = LinkParser.titleIndex(
            listOf(NoteRef(5, "Reunião"), NoteRef(2, "reunião"), NoteRef(3, "   "))
        )

        assertEquals(2L, index[LinkParser.normalize("REUNIÃO")])
        assertEquals(1, index.size) // blank title never becomes a target
    }

    @Test
    fun renameRewritesOnlyMatchingLinks() {
        assertEquals(
            "ver [[Nova]] e [[Nova]] mas não [[Outra]] nem Antiga",
            LinkParser.renameLinks("ver [[Antiga]] e [[ antiga ]] mas não [[Outra]] nem Antiga", "Antiga", "Nova")
        )
    }

    @Test
    fun renameHandlesSpecialCharacters() {
        assertEquals(
            "x [[Nova (v2) \$1]] y",
            LinkParser.renameLinks("x [[C++ & 100% (rascunho)]] y", "C++ & 100% (rascunho)", "Nova (v2) \$1")
        )
    }

    @Test
    fun renameWithBlankOldTitleKeepsBlankLinks() {
        assertEquals("a [[  ]] b", LinkParser.renameLinks("a [[  ]] b", "  ", "X"))
    }
}
