package com.quicknotes.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import com.quicknotes.app.domain.voice.VoiceRecognizer
import java.util.Locale

class AndroidSpeechRecognizerAdapter(context: Context) : VoiceRecognizer {
    private val recognizer: SpeechRecognizer? =
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null

    private val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
    }

    override fun startListening(onResult: (String) -> Unit, onError: (String) -> Unit) {
        val engine = recognizer
        if (engine == null) {
            onError("Reconhecimento de voz indisponível neste aparelho")
            return
        }
        engine.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle) {
                val text = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
                onResult(text)
            }
            override fun onError(error: Int) { onError("Erro no reconhecimento (código $error)") }
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
        engine.startListening(intent)
    }

    override fun stopListening() { recognizer?.stopListening() }
    override fun destroy() { recognizer?.destroy() }
}
