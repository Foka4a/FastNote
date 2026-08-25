package com.quicknotes.app.ui.folders

import com.quicknotes.app.domain.FakeFolderRepository
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
class FoldersViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createNestedFolder() = runTest {
        val repository = FakeFolderRepository()
        val viewModel = FoldersViewModel(repository)
        // Subscribe to the StateFlow to trigger collection
        async { viewModel.folders.first() }.await()

        viewModel.create("Projetos", null)
        val parentId = viewModel.folders.value[0].id
        viewModel.create("FastNote", parentId)

        assertEquals(2, viewModel.folders.value.size)
        assertEquals(parentId, viewModel.folders.value[1].parentId)
    }
}
