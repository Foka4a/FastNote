package com.quicknotes.app.ui.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.quicknotes.app.data.settings.SettingsRepository
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

// ponytail: in-memory DataStore<Preferences> fake so the ViewModel test stays a plain JVM
// unit test instead of needing a real Android Context (that's what the instrumented
// SettingsRepositoryTest already covers).
private class FakeDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())
    override val data = state

    override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun defaultsToReviewBeforeSave() = runTest {
        val viewModel = SettingsViewModel(SettingsRepository(FakeDataStore()))
        // Subscribe to the StateFlow to trigger collection
        async { viewModel.voiceCaptureBehavior.first() }.await()

        assertEquals(VoiceCaptureBehavior.REVIEW_BEFORE_SAVE, viewModel.voiceCaptureBehavior.value)
    }

    @Test
    fun setVoiceCaptureBehaviorUpdatesState() = runTest {
        val viewModel = SettingsViewModel(SettingsRepository(FakeDataStore()))
        async { viewModel.voiceCaptureBehavior.first() }.await()

        viewModel.setVoiceCaptureBehavior(VoiceCaptureBehavior.AUTO_SAVE)

        assertEquals(VoiceCaptureBehavior.AUTO_SAVE, viewModel.voiceCaptureBehavior.value)
    }
}
