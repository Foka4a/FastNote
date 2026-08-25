package com.quicknotes.app.ui.archive

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun ArchiveScreen(viewModel: ArchiveViewModel) {
    val notes by viewModel.notes.collectAsState()
    LazyColumn {
        items(notes, key = { it.id }) { note ->
            ListItem(
                headlineContent = { Text(note.title.ifBlank { note.content.take(40) }) },
                trailingContent = {
                    IconButton(onClick = { viewModel.unarchive(note.id) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Desarquivar")
                    }
                }
            )
        }
    }
}
