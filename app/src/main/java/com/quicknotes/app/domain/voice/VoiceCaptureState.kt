package com.quicknotes.app.domain.voice

sealed class VoiceCaptureState {
    data object Idle : VoiceCaptureState()
    data object Listening : VoiceCaptureState()
    data class Transcribed(val text: String) : VoiceCaptureState()
    data class Error(val message: String) : VoiceCaptureState()
}
