package com.quicknotes.app.ui.editor

import com.quicknotes.app.domain.FakeFolderRepository
import com.quicknotes.app.domain.FakeNoteRepository
import com.quicknotes.app.domain.FakeTagRepository
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

private fun editorViewModel(
    repository: FakeNoteRepository,
    noteId: Long?,
    prefillContent: String? = null
) = EditorViewModel(repository, FakeTagRepository(), FakeFolderRepository(), noteId, prefillContent)

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
        val viewModel = editorViewModel(repository, noteId = null)

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
        val viewModel = editorViewModel(repository, noteId = id)
        viewModel.uiState.value // trigger load in real impl via init block

        viewModel.updateTitle("Editado")
        viewModel.save {}

        assertEquals("Editado", repository.getNote(id)!!.title)
        assertEquals(1, repository.notes.value.size)
    }

    @Test
    fun editingExistingNotePreservesCreatedAt() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.saveNote(
            Note(title = "Original", content = "", createdAt = 1000, updatedAt = 1000, folderId = null,
                favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP)
        )
        val viewModel = editorViewModel(repository, noteId = id)
        viewModel.uiState.value // trigger load in real impl via init block

        viewModel.updateTitle("Editado")
        viewModel.save {}

        val saved = repository.getNote(id)!!
        assertEquals(1000, saved.createdAt)
        assertTrue(saved.updatedAt > 1000)
    }

    @Test
    fun newNoteStartsWithPrefilledContentFromVoiceCapture() = runTest {
        val viewModel = editorViewModel(FakeNoteRepository(), noteId = null, prefillContent = "ditado por voz")

        assertEquals("ditado por voz", viewModel.uiState.value.content)
    }

    @Test
    fun prefillIsIgnoredWhenOpeningAnExistingNote() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.saveNote(
            Note(title = "Original", content = "conteúdo salvo", createdAt = 1, updatedAt = 1, folderId = null,
                favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP)
        )

        val viewModel = editorViewModel(repository, noteId = id, prefillContent = "não deve aparecer")

        assertEquals("conteúdo salvo", viewModel.uiState.value.content)
    }

    @Test
    fun toggleFavoriteFlipsState() = runTest {
        val viewModel = editorViewModel(FakeNoteRepository(), noteId = null)
        assertTrue(!viewModel.uiState.value.favorite)
        viewModel.toggleFavorite()
        assertTrue(viewModel.uiState.value.favorite)
    }
}
