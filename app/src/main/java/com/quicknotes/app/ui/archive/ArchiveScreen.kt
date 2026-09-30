package com.quicknotes.app.ui.archive

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.components.NoteListRow
import com.quicknotes.app.ui.components.noteMeta

@Composable
fun ArchiveScreen(viewModel: ArchiveViewModel, onNoteClick: (Long) -> Unit = {}) {
    val notes by viewModel.notes.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
        items(notes, key = { it.id }) { note ->
            NoteListRow(
                title = note.title, content = note.content, meta = noteMeta(note.captureSource, note.updatedAt),
                actionIcon = Icons.AutoMirrored.Filled.ArrowBack, actionDescription = "Desarquivar",
                onClick = { onNoteClick(note.id) }, onAction = { viewModel.unarchive(note.id) }
            )
        }
        if (notes.isEmpty()) {
            item { EmptyHint("Nada arquivado. Arquivar tira da visão principal sem excluir.") }
        }
    }
}
