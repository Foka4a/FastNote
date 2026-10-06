package com.quicknotes.app.ui.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.components.NoteListRow
import com.quicknotes.app.ui.components.noteMeta
import com.quicknotes.app.ui.theme.Nocturne

@Composable
fun FavoritesScreen(viewModel: FavoritesViewModel, onNoteClick: (Long) -> Unit = {}) {
    val notes by viewModel.notes.collectAsState()
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 4.dp)) {
        items(notes, key = { it.id }) { note ->
            NoteListRow(
                title = note.title, content = note.content, meta = noteMeta(note.captureSource, note.updatedAt),
                actionIcon = Icons.Filled.Star, actionDescription = "Remover dos favoritos",
                actionTint = Nocturne.Warning,
                onClick = { onNoteClick(note.id) }, onAction = { viewModel.unfavorite(note.id) }
            )
        }
        if (notes.isEmpty()) {
            item { EmptyHint("Nenhuma nota favoritada ainda. Arraste uma nota da Inbox para a direita.") }
        }
    }
}
