package com.quicknotes.app.ui.inbox

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

@Composable
fun InboxScreen(viewModel: InboxViewModel, onNoteClick: (Long) -> Unit) {
    val notes by viewModel.notes.collectAsState()
    LazyColumn(modifier = Modifier.testTag("inbox_list")) {
        items(notes, key = { it.id }) { note ->
            ListItem(
                headlineContent = { Text(note.title.ifBlank { note.content.take(40) }) },
                trailingContent = {
                    IconButton(onClick = { viewModel.delete(note.id) }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Excluir")
                    }
                },
                modifier = Modifier.clickable { onNoteClick(note.id) }
            )
        }
    }
}
