package com.quicknotes.app.ui.favorites

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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unfavoriteClearsFlag() = runTest {
        val repository = FakeNoteRepository()
        val id = repository.saveNote(
            Note(title = "A", content = "", createdAt = 1, updatedAt = 1, folderId = null,
                favorite = true, archived = false, inbox = false, captureSource = CaptureSource.APP)
        )
        val viewModel = FavoritesViewModel(repository)
        // Subscribe to the StateFlow to trigger collection
        async { viewModel.notes.first() }.await()

        assertEquals(1, viewModel.notes.value.size)

        viewModel.unfavorite(id)

        assertTrue(!repository.getNote(id)!!.favorite)
    }
}
