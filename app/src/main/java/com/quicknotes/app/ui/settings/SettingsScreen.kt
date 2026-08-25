package com.quicknotes.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quicknotes.app.domain.model.VoiceCaptureBehavior

private fun label(behavior: VoiceCaptureBehavior): String = when (behavior) {
    VoiceCaptureBehavior.AUTO_SAVE -> "Salvar automaticamente"
    VoiceCaptureBehavior.REVIEW_BEFORE_SAVE -> "Revisar antes de salvar"
    VoiceCaptureBehavior.CONTINUE_EDITING -> "Continuar editando no app"
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val current by viewModel.voiceCaptureBehavior.collectAsState()
    Column(Modifier.padding(16.dp)) {
        Text("Após transcrever a voz:")
        VoiceCaptureBehavior.entries.forEach { behavior ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.selectable(
                    selected = current == behavior,
                    onClick = { viewModel.setVoiceCaptureBehavior(behavior) }
                )
            ) {
                RadioButton(
                    selected = current == behavior,
                    onClick = { viewModel.setVoiceCaptureBehavior(behavior) }
                )
                Text(label(behavior))
            }
        }
    }
}
