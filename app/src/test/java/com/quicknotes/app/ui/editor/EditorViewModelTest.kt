package com.quicknotes.app.ui.editor

import com.quicknotes.app.domain.FakeNoteRepository
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditorViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun savingNewNoteCreatesItInRepository() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = EditorViewModel(repository, noteId = null)

        viewModel.updateTitle("Nova nota")
        viewModel.updateContent("conteúdo")
        var saved = false
        viewModel.save { saved = true }

        assertTrue(saved)
        assertEquals(1, repository.notes.value.size)
        assertEquals("Nova nota", repository.notes.value[0].title)
    }

    @Test
    fun editingExistingNoteUpdatesItInPlace() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.saveNote(
            Note(title = "Original", content = "", createdAt = 1, updatedAt = 1, folderId = null,
                favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP)
        )
        val viewModel = EditorViewModel(repository, noteId = id)
        viewModel.uiState.value // trigger load in real impl via init block

        viewModel.updateTitle("Editado")
        viewModel.save {}

        assertEquals("Editado", repository.getNote(id)!!.title)
        assertEquals(1, repository.notes.value.size)
    }

    @Test
    fun toggleFavoriteFlipsState() = runTest {
        val viewModel = EditorViewModel(FakeNoteRepository(), noteId = null)
        assertTrue(!viewModel.uiState.value.favorite)
        viewModel.toggleFavorite()
        assertTrue(viewModel.uiState.value.favorite)
    }
}
