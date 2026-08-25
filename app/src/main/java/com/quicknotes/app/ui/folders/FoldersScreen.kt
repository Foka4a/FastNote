package com.quicknotes.app.ui.folders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun FoldersScreen(viewModel: FoldersViewModel) {
    val folders by viewModel.folders.collectAsState()
    var newFolder by remember { mutableStateOf("") }
    Column {
        Row(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = newFolder,
                onValueChange = { newFolder = it },
                label = { Text("Nova pasta") },
                modifier = Modifier.testTag("new_folder_field")
            )
            Button(
                onClick = {
                    if (newFolder.isNotBlank()) {
                        // ponytail: MVP creates only top-level folders; nesting needs a parent picker.
                        viewModel.create(newFolder.trim(), null)
                        newFolder = ""
                    }
                },
                modifier = Modifier.testTag("add_folder_button")
            ) { Text("Adicionar") }
        }
        LazyColumn {
            items(folders, key = { it.id }) { folder ->
                ListItem(
                    headlineContent = { Text(folder.name) },
                    trailingContent = {
                        IconButton(onClick = { viewModel.delete(folder.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir pasta")
                        }
                    }
                )
            }
        }
    }
}
