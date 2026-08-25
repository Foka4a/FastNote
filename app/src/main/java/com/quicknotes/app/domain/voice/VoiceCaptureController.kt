package com.quicknotes.app.domain.voice

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VoiceCaptureController(private val recognizer: VoiceRecognizer) {
    private val _state = MutableStateFlow<VoiceCaptureState>(VoiceCaptureState.Idle)
    val state: StateFlow<VoiceCaptureState> = _state.asStateFlow()

    fun start() {
        _state.value = VoiceCaptureState.Listening
        recognizer.startListening(
            onResult = { text -> _state.value = VoiceCaptureState.Transcribed(text) },
            onError = { message -> _state.value = VoiceCaptureState.Error(message) }
        )
    }

    fun cancel() {
        recognizer.stopListening()
        _state.value = VoiceCaptureState.Idle
    }

    fun reset() { _state.value = VoiceCaptureState.Idle }
}
