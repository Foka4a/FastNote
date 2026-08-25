package com.quicknotes.app.ui.search

import com.quicknotes.app.domain.FakeNoteRepository
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.usecase.SearchNotesUseCase
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
class SearchViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun blankQueryYieldsNoResults() = runTest {
        val viewModel = SearchViewModel(SearchNotesUseCase(FakeNoteRepository()))
        viewModel.onQueryChange("")
        assertTrue(viewModel.results.value.isEmpty())
    }

    @Test
    fun queryUpdatesResultsFromUseCase() = runTest {
        val repository = FakeNoteRepository()
        repository.saveNote(
            Note(title = "Ideia Maker", content = "", createdAt = 1, updatedAt = 1, folderId = null,
                favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP)
        )
        val viewModel = SearchViewModel(SearchNotesUseCase(repository))

        viewModel.onQueryChange("Maker")

        assertEquals("Maker", viewModel.query.value)
        assertEquals(1, viewModel.results.value.size)
    }
}
