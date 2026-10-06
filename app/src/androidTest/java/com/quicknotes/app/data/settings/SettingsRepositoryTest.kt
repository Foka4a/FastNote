package com.quicknotes.app.data.settings

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

private val android.content.Context.Context_dataStore by preferencesDataStore(name = "settings_test")

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    // ponytail: JUnit4 doesn't run @Test methods in declaration order, and this DataStore
    // persists to disk across runs, so each test must start from a clean slate.
    @Before
    fun clearPersistedPreferences() {
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<android.content.Context>()
            context.Context_dataStore.edit { it.clear() }
        }
    }

    @Test
    fun defaultsToReviewBeforeSave() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SettingsRepository(context.Context_dataStore)
        assertEquals(VoiceCaptureBehavior.REVIEW_BEFORE_SAVE, repository.voiceCaptureBehavior.first())
    }

    @Test
    fun persistsChosenBehavior() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val repository = SettingsRepository(context.Context_dataStore)
        repository.setVoiceCaptureBehavior(VoiceCaptureBehavior.AUTO_SAVE)
        assertEquals(VoiceCaptureBehavior.AUTO_SAVE, repository.voiceCaptureBehavior.first())
    }
}
