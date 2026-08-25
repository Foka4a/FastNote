package com.quicknotes.app.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun EditorScreen(viewModel: EditorViewModel, onSaved: () -> Unit) {
    val state by viewModel.uiState.collectAsState()
    Column(Modifier.padding(16.dp)) {
        OutlinedTextField(value = state.title, onValueChange = viewModel::updateTitle, label = { Text("Título") })
        OutlinedTextField(value = state.content, onValueChange = viewModel::updateContent, label = { Text("Conteúdo") })
        Text("Favorito")
        Switch(checked = state.favorite, onCheckedChange = { viewModel.toggleFavorite() })
        Text("Arquivado")
        Switch(checked = state.archived, onCheckedChange = { viewModel.toggleArchived() })
        Button(onClick = { viewModel.save(onSaved) }) { Text("Salvar") }
    }
}
