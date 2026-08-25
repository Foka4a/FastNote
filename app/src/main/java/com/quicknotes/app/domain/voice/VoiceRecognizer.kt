package com.quicknotes.app.domain.voice

interface VoiceRecognizer {
    fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit)
    fun stopListening()
    fun destroy()
}
