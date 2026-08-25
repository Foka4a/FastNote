package com.quicknotes.app.overlay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import com.quicknotes.app.domain.voice.VoiceCaptureController
import com.quicknotes.app.domain.voice.VoiceCaptureState

@Composable
fun VoiceCaptureOverlay(
    controller: VoiceCaptureController,
    behavior: VoiceCaptureBehavior,
    onSave: (String) -> Unit,
    onContinueEditing: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val state by controller.state.collectAsState()

    LaunchedEffect(Unit) { controller.start() }

    MaterialTheme {
        Surface(Modifier.padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                when (val current = state) {
                    is VoiceCaptureState.Idle -> Text("Preparando...")
                    is VoiceCaptureState.Listening -> Text("Ouvindo...")
                    is VoiceCaptureState.Transcribed -> {
                        when (behavior) {
                            // onSave alone, not onDismiss too: the service's saveNote() already
                            // closes the overlay itself once the async Room write completes.
                            // Calling onDismiss here would race stopSelf() ahead of that write
                            // (the exact bug Task 15's review caught and fixed for text capture).
                            VoiceCaptureBehavior.AUTO_SAVE -> LaunchedEffect(current.text) {
                                onSave(current.text)
                            }
                            VoiceCaptureBehavior.CONTINUE_EDITING -> LaunchedEffect(current.text) {
                                onContinueEditing(current.text); onDismiss()
                            }
                            VoiceCaptureBehavior.REVIEW_BEFORE_SAVE -> {
                                Text(current.text)
                                Button(onClick = { onSave(current.text) }) { Text("Salvar") }
                                Button(onClick = onDismiss) { Text("Descartar") }
                            }
                        }
                    }
                    is VoiceCaptureState.Error -> {
                        Text(current.message)
                        Button(onClick = onDismiss) { Text("Fechar") }
                    }
                }
            }
        }
    }
}
