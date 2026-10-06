package com.quicknotes.app.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SettingsRepository(private val dataStore: DataStore<Preferences>) {
    private val voiceCaptureBehaviorKey = stringPreferencesKey("voice_capture_behavior")

    val voiceCaptureBehavior: Flow<VoiceCaptureBehavior> = dataStore.data.map { prefs ->
        prefs[voiceCaptureBehaviorKey]?.let { VoiceCaptureBehavior.valueOf(it) }
            ?: VoiceCaptureBehavior.REVIEW_BEFORE_SAVE
    }

    suspend fun setVoiceCaptureBehavior(behavior: VoiceCaptureBehavior) {
        dataStore.edit { it[voiceCaptureBehaviorKey] = behavior.name }
    }
}
