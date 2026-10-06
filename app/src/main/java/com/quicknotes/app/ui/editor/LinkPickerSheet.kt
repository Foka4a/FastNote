package com.quicknotes.app.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.ui.theme.Nocturne

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkPickerSheet(
    candidates: List<NoteRef>,
    onQueryChange: (String) -> Unit,
    onPick: (NoteRef) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { onQueryChange("") } // show every note before the first keystroke
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Nocturne.Surface) {
        Column(Modifier.padding(horizontal = 16.dp).padding(bottom = 18.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; onQueryChange(it) },
                placeholder = { Text("Buscar nota para ligar…", color = Nocturne.TextMuted) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("link_picker_query")
            )
            if (candidates.isEmpty()) {
                Text("Nenhuma nota encontrada", color = Nocturne.TextMuted, fontSize = 12.5.sp, modifier = Modifier.padding(vertical = 14.dp))
            }
            LazyColumn(Modifier.heightIn(max = 360.dp)) {
                items(candidates, key = { it.id }) { ref ->
                    Box(
                        Modifier.fillMaxWidth().heightIn(min = 48.dp)
                            .clickable(onClickLabel = "Ligar a ${ref.title}", role = Role.Button) { onPick(ref) }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(ref.title, color = Nocturne.TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
