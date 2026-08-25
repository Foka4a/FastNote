package com.quicknotes.app.overlay

import com.quicknotes.app.domain.voice.FakeVoiceRecognizer
import com.quicknotes.app.domain.voice.VoiceCaptureController
import com.quicknotes.app.domain.voice.VoiceCaptureState
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceCaptureOverlayFlowTest {
    @Test
    fun autoSaveBehaviorTransitionsToTranscribedReadyToPersist() {
        val recognizer = FakeVoiceRecognizer()
        val controller = VoiceCaptureController(recognizer)

        controller.start()
        recognizer.emitResult("pesquisar biblioteca de gráficos")

        assertEquals(VoiceCaptureState.Transcribed("pesquisar biblioteca de gráficos"), controller.state.value)
        // AUTO_SAVE and REVIEW_BEFORE_SAVE both reach Transcribed from here; which one
        // auto-persists vs. waits for a tap is the overlay Composable's job, exercised
        // manually in Step 5 (Compose state branching in a WindowManager overlay isn't
        // practically unit-testable). This test only pins the controller's own contract.
    }
}
