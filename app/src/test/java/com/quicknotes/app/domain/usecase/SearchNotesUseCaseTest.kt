package com.quicknotes.app.domain.usecase

import com.quicknotes.app.domain.FakeNoteRepository
import com.quicknotes.app.domain.model.CaptureSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchNotesUseCaseTest {
    @Test
    fun blankQueryReturnsNoResults() = runTest {
        val useCase = SearchNotesUseCase(FakeNoteRepository())
        assertTrue(useCase("").isEmpty())
    }

    @Test
    fun matchesTitleOrContent() = runTest {
        val repository = FakeNoteRepository()
        repository.saveNote(
            com.quicknotes.app.domain.model.Note(
                title = "Pesquisar biblioteca", content = "gráficos", createdAt = 1, updatedAt = 1,
                folderId = null, favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP
            )
        )
        val useCase = SearchNotesUseCase(repository)

        assertEquals(1, useCase("biblioteca").size)
        assertEquals(1, useCase("gráficos").size)
        assertEquals(0, useCase("inexistente").size)
    }
}
