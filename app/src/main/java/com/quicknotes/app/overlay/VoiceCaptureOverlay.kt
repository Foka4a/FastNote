package com.quicknotes.app.overlay

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import com.quicknotes.app.domain.voice.VoiceCaptureController
import com.quicknotes.app.domain.voice.VoiceCaptureState
import com.quicknotes.app.ui.theme.Nocturne
import com.quicknotes.app.ui.theme.QuickNotesTheme

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

    QuickNotesTheme {
        Column(
            Modifier
                .padding(14.dp)
                .fillMaxWidth()
                .background(Nocturne.Surface, RoundedCornerShape(20.dp))
                .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(20.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val current = state) {
                is VoiceCaptureState.Idle -> Text("Preparando…", color = Nocturne.TextSecondary, fontSize = 12.sp)
                is VoiceCaptureState.Listening -> {
                    Text("Gravando · on-device", color = Nocturne.TextSecondary, fontSize = 11.5.sp)
                    PulsingMic()
                    Text("Ouvindo…", color = Nocturne.TextPrimary, fontSize = 15.sp)
                }
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
                            Text("Transcrito · revise antes de salvar", color = Nocturne.TextSecondary, fontSize = 11.5.sp)
                            Text(
                                current.text.ifBlank { "Não entendi. Tente de novo." },
                                color = Nocturne.TextPrimary, fontSize = 15.sp, lineHeight = 23.sp,
                                modifier = Modifier.padding(vertical = 14.dp)
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CaptureButton("Descartar", Nocturne.TextSecondary, Nocturne.BorderSubtle, Modifier.weight(1f), onClick = onDismiss)
                                CaptureButton("Salvar", Nocturne.OnAccent, Nocturne.Accent, Modifier.weight(1f), filled = true, onClick = { onSave(current.text) })
                            }
                        }
                    }
                }
                is VoiceCaptureState.Error -> {
                    Text(current.message, color = Nocturne.TextPrimary, fontSize = 14.sp, textAlign = TextAlign.Center)
                    CaptureButton("Fechar", Nocturne.TextSecondary, Nocturne.BorderSubtle, Modifier.fillMaxWidth().padding(top = 14.dp), onClick = onDismiss)
                }
            }
        }
    }
}

@Composable
private fun PulsingMic() {
    val transition = rememberInfiniteTransition(label = "mic-pulse")
    val scale by transition.animateFloat(
        initialValue = .9f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "mic-scale"
    )
    Row(
        Modifier
            .padding(vertical = 16.dp)
            .size(80.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(Nocturne.AccentTint, CircleShape)
            .border(1.dp, Nocturne.AccentBorder, CircleShape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Mic, contentDescription = null, tint = Nocturne.AccentText, modifier = Modifier.size(30.dp))
    }
}

@Composable
private fun CaptureButton(
    label: String,
    fg: Color,
    border: Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        label, color = fg, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
        modifier = modifier
            .background(if (filled) Nocturne.Accent else Color.Transparent, RoundedCornerShape(11.dp))
            .border(1.dp, border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}
