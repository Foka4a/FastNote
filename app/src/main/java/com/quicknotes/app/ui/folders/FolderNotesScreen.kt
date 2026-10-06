package com.quicknotes.app.ui.folders

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.components.NoteListRow
import com.quicknotes.app.ui.components.noteMeta

@Composable
fun FolderNotesScreen(viewModel: FolderNotesViewModel, onNoteClick: (Long) -> Unit = {}) {
    val notes by viewModel.notes.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp).testTag("folder_notes_list")) {
        items(notes, key = { it.id }) { note ->
            // ponytail: NoteListRow requires an action; the chevron just opens the note too.
            NoteListRow(
                title = note.title, content = note.content, meta = noteMeta(note.captureSource, note.updatedAt),
                actionIcon = Icons.Filled.ChevronRight, actionDescription = "Abrir",
                onClick = { onNoteClick(note.id) }, onAction = { onNoteClick(note.id) }
            )
        }
        if (notes.isEmpty()) {
            item { EmptyHint("Pasta vazia. Mova notas para cá pelo editor.") }
        }
    }
}
