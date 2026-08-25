package com.quicknotes.app.ui.folders

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun FoldersScreen(viewModel: FoldersViewModel) {
    val folders by viewModel.folders.collectAsState()
    Column {
        LazyColumn {
            items(folders, key = { it.id }) { folder ->
                ListItem(headlineContent = { Text(folder.name) })
            }
        }
    }
}
