package com.quicknotes.app.ui.tags

import com.quicknotes.app.domain.FakeTagRepository
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
class TagsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createThenDeleteTag() = runTest {
        val repository = FakeTagRepository()
        val viewModel = TagsViewModel(repository)
        // Subscribe to the StateFlow to trigger collection
        async { viewModel.tags.first() }.await()

        viewModel.create("trabalho")
        assertEquals(1, viewModel.tags.value.size)

        viewModel.delete(viewModel.tags.value[0].id)
        assertEquals(0, viewModel.tags.value.size)
    }
}
