package com.quicknotes.app.ui.tags

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun TagsScreen(viewModel: TagsViewModel) {
    val tags by viewModel.tags.collectAsState()
    Column {
        LazyColumn {
            items(tags, key = { it.id }) { tag ->
                ListItem(headlineContent = { Text("#${tag.name}") })
            }
        }
    }
}
