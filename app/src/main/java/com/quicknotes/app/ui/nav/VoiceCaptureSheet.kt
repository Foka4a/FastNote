package com.quicknotes.app.ui.nav

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
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import com.quicknotes.app.domain.voice.VoiceCaptureController
import com.quicknotes.app.domain.voice.VoiceCaptureState
import com.quicknotes.app.ui.theme.Nocturne
import com.quicknotes.app.voice.AndroidSpeechRecognizerAdapter

/** In-app voice capture, triggered by holding the FAB — same behavior/settings as the
 *  widget's overlay capture (domain.voice.VoiceCaptureController), styled as a bottom sheet. */
@Composable
fun VoiceCaptureSheet(
    behavior: VoiceCaptureBehavior,
    onSave: (String) -> Unit,
    onContinueEditing: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val recognizer = remember { AndroidSpeechRecognizerAdapter(context) }
    val controller = remember { VoiceCaptureController(recognizer) }
    val state by controller.state.collectAsState()

    LaunchedEffect(Unit) { controller.start() }
    DisposableEffect(Unit) { onDispose { recognizer.destroy() } }

    Row(
        Modifier.fillMaxSize().clickable(onClick = onDismiss),
        verticalAlignment = Alignment.Bottom
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Nocturne.Surface, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(enabled = false) {}
                .padding(22.dp)
        ) {
            when (val current = state) {
                is VoiceCaptureState.Idle -> VoiceStatus("Preparando…")
                is VoiceCaptureState.Listening -> {
                    VoiceStatus("Gravando · on-device", dotColor = Nocturne.Danger)
                    PulsingMic()
                }
                is VoiceCaptureState.Transcribed -> {
                    when (behavior) {
                        VoiceCaptureBehavior.AUTO_SAVE -> LaunchedEffect(current.text) { onSave(current.text) }
                        VoiceCaptureBehavior.CONTINUE_EDITING -> LaunchedEffect(current.text) { onContinueEditing(current.text) }
                        VoiceCaptureBehavior.REVIEW_BEFORE_SAVE -> {
                            VoiceStatus("Transcrito · revise antes de salvar")
                            Text(
                                current.text.ifBlank { "Não entendi. Tente de novo." },
                                color = Nocturne.TextPrimary, fontSize = 15.sp, lineHeight = 23.sp,
                                modifier = Modifier.padding(vertical = 16.dp)
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                SheetButton("Descartar", Nocturne.TextSecondary, Nocturne.BorderSubtle, Modifier.weight(1f), onClick = onDismiss)
                                SheetButton("Editar", Nocturne.AccentText, Nocturne.AccentBorder, Modifier.weight(1f)) { onContinueEditing(current.text) }
                                SheetButton("Salvar", Nocturne.OnAccent, Nocturne.Accent, Modifier.weight(1f), filled = true) { onSave(current.text) }
                            }
                        }
                    }
                }
                is VoiceCaptureState.Error -> {
                    Text(current.message, color = Nocturne.TextPrimary, fontSize = 14.sp)
                    Row(Modifier.padding(top = 16.dp)) {
                        SheetButton("Fechar", Nocturne.TextSecondary, Nocturne.BorderSubtle, Modifier.fillMaxWidth(), onClick = onDismiss)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceStatus(text: String, dotColor: androidx.compose.ui.graphics.Color = Nocturne.TextMuted) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.size(7.dp).background(dotColor, CircleShape)) {}
        Text(text, color = Nocturne.TextSecondary, fontSize = 11.5.sp)
    }
}

@Composable
private fun PulsingMic() {
    val transition = rememberInfiniteTransition(label = "mic-pulse")
    val scale by transition.animateFloat(
        initialValue = .9f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "mic-scale"
    )
    Row(
        Modifier
            .padding(vertical = 20.dp)
            .size(96.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(Nocturne.AccentTint, CircleShape)
            .border(1.dp, Nocturne.AccentBorder, CircleShape),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Mic, contentDescription = null, tint = Nocturne.AccentText, modifier = Modifier.size(34.dp))
    }
}

@Composable
private fun SheetButton(
    label: String,
    fg: androidx.compose.ui.graphics.Color,
    border: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    filled: Boolean = false,
    onClick: () -> Unit
) {
    Text(
        label, color = fg, fontSize = 13.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .background(if (filled) Nocturne.Accent else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(11.dp))
            .border(1.dp, border, RoundedCornerShape(11.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp)
    )
}
