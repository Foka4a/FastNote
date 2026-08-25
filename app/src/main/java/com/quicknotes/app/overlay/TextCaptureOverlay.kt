package com.quicknotes.app.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect

@Composable
fun TextCaptureOverlay(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    MaterialTheme {
        Surface(Modifier.padding(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Nova nota") },
                    modifier = Modifier.focusRequester(focusRequester)
                )
                Button(onClick = { onSave(text); onDismiss() }) { Text("Salvar") }
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
