package com.quicknotes.app.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.theme.Nocturne

@Composable
fun FoldersScreen(viewModel: FoldersViewModel, onFolderClick: (Long) -> Unit = {}) {
    val folders by viewModel.folders.collectAsState()
    var newFolder by remember { mutableStateOf("") }

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            Row(
                Modifier
                    .weight(1f)
                    .background(Nocturne.Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, Nocturne.BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(horizontal = 13.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.CreateNewFolder, contentDescription = null, tint = Nocturne.AccentText)
                Box {
                    if (newFolder.isEmpty()) Text("nova pasta", color = Nocturne.TextMuted, fontSize = 14.sp)
                    BasicTextField(
                        value = newFolder, onValueChange = { newFolder = it },
                        textStyle = TextStyle(color = Nocturne.TextPrimary, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Nocturne.Accent),
                        modifier = Modifier.testTag("new_folder_field")
                    )
                }
            }
            Text(
                "Criar", color = Nocturne.AccentText, fontSize = 13.sp, fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = {
                        // ponytail: MVP creates only top-level folders; nesting needs a parent picker.
                        if (newFolder.isNotBlank()) { viewModel.create(newFolder.trim(), null); newFolder = "" }
                    })
                    .testTag("add_folder_button")
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            )
        }

        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            items(folders, key = { it.id }) { folder ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onFolderClick(folder.id) }
                        .testTag("folder_row_${folder.id}")
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.spacedBy(11.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Folder, contentDescription = null, tint = Nocturne.AccentText, modifier = Modifier.size(17.dp))
                    Text(folder.name, color = Nocturne.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Nocturne.TextFaint, modifier = Modifier.size(13.dp))
                }
            }
            if (folders.isEmpty()) item { EmptyHint("Nenhuma pasta ainda.") }
        }
    }
}
