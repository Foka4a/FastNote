package com.quicknotes.app.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quicknotes.app.domain.model.VoiceCaptureBehavior

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val current by viewModel.voiceCaptureBehavior.collectAsState()
    Column {
        Text("Após transcrever a voz:")
        VoiceCaptureBehavior.entries.forEach { behavior ->
            RadioButton(
                selected = current == behavior,
                onClick = { viewModel.setVoiceCaptureBehavior(behavior) }
            )
        }
    }
}
