package com.quicknotes.app.domain.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCaptureControllerTest {
    @Test
    fun startMovesToListeningThenResultMovesToTranscribed() {
        val recognizer = FakeVoiceRecognizer()
        val controller = VoiceCaptureController(recognizer)

        controller.start()
        assertEquals(VoiceCaptureState.Listening, controller.state.value)

        recognizer.emitResult("comprar filtro de óleo")
        assertEquals(VoiceCaptureState.Transcribed("comprar filtro de óleo"), controller.state.value)
    }

    @Test
    fun errorMovesToErrorState() {
        val recognizer = FakeVoiceRecognizer()
        val controller = VoiceCaptureController(recognizer)

        controller.start()
        recognizer.emitError("no speech detected")

        assertEquals(VoiceCaptureState.Error("no speech detected"), controller.state.value)
    }

    @Test
    fun cancelStopsRecognizerAndResetsToIdle() {
        val recognizer = FakeVoiceRecognizer()
        val controller = VoiceCaptureController(recognizer)

        controller.start()
        controller.cancel()

        assertTrue(recognizer.stopCalled)
        assertEquals(VoiceCaptureState.Idle, controller.state.value)
    }
}
