package com.quicknotes.app.ui.folders

import com.quicknotes.app.domain.FakeNoteRepository
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderNotesViewModelTest {
    @Before
    fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    private fun note(title: String, folderId: Long?) = Note(
        title = title, content = "", createdAt = 1, updatedAt = 1, folderId = folderId,
        favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP
    )

    @Test
    fun listsOnlyNotesOfItsFolderAndPicksUpMovedNotes() = runTest {
        val repository = FakeNoteRepository()
        repository.saveNote(note("dentro", folderId = 7))
        val outsideId = repository.saveNote(note("fora", folderId = null))
        val viewModel = FolderNotesViewModel(repository, folderId = 7)
        async { viewModel.notes.first() }.await()

        assertEquals(listOf("dentro"), viewModel.notes.value.map { it.title })

        // Moving a note into the folder (editor save with a new folderId) shows it here.
        repository.saveNote(repository.getNote(outsideId)!!.copy(folderId = 7))

        assertEquals(setOf("dentro", "fora"), viewModel.notes.value.map { it.title }.toSet())
    }
}
