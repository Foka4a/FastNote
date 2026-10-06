package com.quicknotes.app.domain.voice

class FakeVoiceRecognizer : VoiceRecognizer {
    private var onResult: ((String) -> Unit)? = null
    private var onError: ((String) -> Unit)? = null
    var stopCalled = false
        private set

    override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        this.onResult = onResult
        this.onError = onError
    }

    override fun stopListening() { stopCalled = true }
    override fun destroy() {}

    fun emitResult(text: String) { onResult?.invoke(text) }
    fun emitError(message: String) { onError?.invoke(message) }
}
