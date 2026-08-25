package com.quicknotes.app.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()
    Column {
        OutlinedTextField(value = query, onValueChange = viewModel::onQueryChange, label = { Text("Buscar") })
        LazyColumn {
            items(results, key = { it.id }) { note ->
                ListItem(headlineContent = { Text(note.title.ifBlank { note.content.take(40) }) })
            }
        }
    }
}
