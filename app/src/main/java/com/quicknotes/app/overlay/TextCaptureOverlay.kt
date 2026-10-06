package com.quicknotes.app.overlay

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.theme.Nocturne
import com.quicknotes.app.ui.theme.QuickNotesTheme

@Composable
fun TextCaptureOverlay(onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    QuickNotesTheme {
        Column(
            Modifier
                .padding(14.dp)
                .fillMaxWidth()
                .background(Nocturne.Surface, RoundedCornerShape(20.dp))
                .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(20.dp))
                .padding(18.dp)
        ) {
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                androidx.compose.material3.Icon(Icons.Filled.Bolt, contentDescription = null, tint = Nocturne.AccentText, modifier = Modifier.size(14.dp))
                Text("Captura rápida · vai para a Inbox", color = Nocturne.TextSecondary, fontSize = 11.sp, modifier = Modifier.weight(1f))
                Text("Cancelar", color = Nocturne.TextMuted, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onDismiss))
            }
            Column {
                if (text.isEmpty()) {
                    Text("Escreva…", color = Nocturne.TextMuted, fontSize = 16.sp, modifier = Modifier.padding(top = 14.dp))
                }
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    textStyle = TextStyle(color = Nocturne.TextPrimary, fontSize = 16.sp, lineHeight = 24.sp),
                    cursorBrush = SolidColor(Nocturne.Accent),
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp).focusRequester(focusRequester)
                )
            }
            Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.End) {
                Text(
                    "Salvar",
                    color = Nocturne.OnAccent, fontSize = 13.5.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    modifier = Modifier
                        .background(Nocturne.Accent, RoundedCornerShape(20.dp))
                        .clickable { onSave(text) }
                        .padding(horizontal = 20.dp, vertical = 11.dp)
                )
            }
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}
