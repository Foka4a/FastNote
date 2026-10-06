package com.quicknotes.app.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.data.settings.SettingsRepository
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {
    val voiceCaptureBehavior: StateFlow<VoiceCaptureBehavior> = settingsRepository.voiceCaptureBehavior
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VoiceCaptureBehavior.REVIEW_BEFORE_SAVE)

    fun setVoiceCaptureBehavior(behavior: VoiceCaptureBehavior) {
        viewModelScope.launch { settingsRepository.setVoiceCaptureBehavior(behavior) }
    }
}
