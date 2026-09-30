package com.quicknotes.app.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import com.quicknotes.app.ui.components.SectionLabel
import com.quicknotes.app.ui.theme.Nocturne

private fun title(behavior: VoiceCaptureBehavior): String = when (behavior) {
    VoiceCaptureBehavior.AUTO_SAVE -> "Salvar automático"
    VoiceCaptureBehavior.REVIEW_BEFORE_SAVE -> "Revisar antes de salvar"
    VoiceCaptureBehavior.CONTINUE_EDITING -> "Continuar editando no app"
}

private fun description(behavior: VoiceCaptureBehavior): String = when (behavior) {
    VoiceCaptureBehavior.AUTO_SAVE -> "Menos toques possível: a nota cai na Inbox e o overlay fecha."
    VoiceCaptureBehavior.REVIEW_BEFORE_SAVE -> "Mostra a transcrição com Salvar, Editar ou Descartar."
    VoiceCaptureBehavior.CONTINUE_EDITING -> "Abre o editor com o texto transcrito."
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onOpenOnboarding: () -> Unit = {}) {
    val current by viewModel.voiceCaptureBehavior.collectAsState()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        SectionLabel("Depois de transcrever a voz", Modifier.padding(bottom = 10.dp))
        Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
            VoiceCaptureBehavior.entries.forEach { behavior ->
                val selected = current == behavior
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(if (selected) Nocturne.AccentTint else Nocturne.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, if (selected) Nocturne.AccentBorder else Nocturne.BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { viewModel.setVoiceCaptureBehavior(behavior) }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) Nocturne.AccentText else Nocturne.TextFaint,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    Column {
                        Text(title(behavior), color = Nocturne.TextPrimary, fontSize = 14.sp)
                        Text(
                            description(behavior), color = Nocturne.TextSecondary, fontSize = 11.5.sp,
                            lineHeight = 16.sp, modifier = Modifier.padding(top = 3.dp)
                        )
                    }
                }
            }
        }

        SectionLabel("Widget e overlay", Modifier.padding(top = 26.dp, bottom = 10.dp))

        Row(
            Modifier
                .fillMaxWidth()
                .background(Nocturne.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Nocturne.Warning.copy(alpha = .3f), RoundedCornerShape(12.dp))
                .clickable(onClick = onOpenOnboarding)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = Nocturne.Warning)
            Column(Modifier.weight(1f)) {
                Text("Permissão de sobreposição", color = Nocturne.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Toque para revisar o onboarding", color = Nocturne.TextSecondary, fontSize = 11.5.sp)
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(Nocturne.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Nocturne.BorderSubtle, RoundedCornerShape(12.dp))
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Mic, contentDescription = null, tint = Nocturne.TextSecondary)
            Column(Modifier.weight(1f)) {
                Text("Reconhecimento de voz", color = Nocturne.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("Disponível no aparelho · on-device", color = Nocturne.TextSecondary, fontSize = 11.5.sp)
            }
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = Nocturne.Success, modifier = Modifier.size(18.dp))
        }
    }
}
