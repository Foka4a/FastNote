package com.quicknotes.app.ui.tags

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
fun TagsScreen(viewModel: TagsViewModel) {
    val tags by viewModel.tags.collectAsState()
    var newTag by remember { mutableStateOf("") }
    Column {
        Row(Modifier.padding(16.dp)) {
            OutlinedTextField(
                value = newTag,
                onValueChange = { newTag = it },
                label = { Text("Nova tag") },
                modifier = Modifier.testTag("new_tag_field")
            )
            Button(
                onClick = {
                    if (newTag.isNotBlank()) {
                        viewModel.create(newTag.trim())
                        newTag = ""
                    }
                },
                modifier = Modifier.testTag("add_tag_button")
            ) { Text("Adicionar") }
        }
        LazyColumn {
            items(tags, key = { it.id }) { tag ->
                ListItem(
                    headlineContent = { Text("#${tag.name}") },
                    trailingContent = {
                        IconButton(onClick = { viewModel.delete(tag.id) }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Excluir tag")
                        }
                    }
                )
            }
        }
    }
}
