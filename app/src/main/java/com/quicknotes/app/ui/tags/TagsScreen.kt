package com.quicknotes.app.ui.tags

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tag
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.theme.Nocturne

@Composable
fun TagsScreen(viewModel: TagsViewModel) {
    val tags by viewModel.tags.collectAsState()
    var newTag by remember { mutableStateOf("") }

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
                Icon(Icons.Filled.Tag, contentDescription = null, tint = Nocturne.AccentText)
                Box {
                    if (newTag.isEmpty()) Text("nova tag", color = Nocturne.TextMuted, fontSize = 14.sp)
                    BasicTextField(
                        value = newTag, onValueChange = { newTag = it },
                        textStyle = TextStyle(color = Nocturne.TextPrimary, fontSize = 14.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Nocturne.Accent),
                        modifier = Modifier.testTag("new_tag_field")
                    )
                }
            }
            Text(
                "Criar", color = Nocturne.AccentText, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                modifier = Modifier
                    .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = {
                        if (newTag.isNotBlank()) { viewModel.create(newTag.trim()); newTag = "" }
                    })
                    .testTag("add_tag_button")
                    .padding(horizontal = 16.dp, vertical = 11.dp)
            )
        }

        LazyColumn(Modifier.fillMaxWidth().padding(top = 8.dp)) {
            items(tags, key = { it.id }) { tag ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    Text("#${tag.name}", color = Nocturne.AccentText, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
                    Icon(
                        Icons.Filled.Close, contentDescription = "Excluir tag",
                        tint = Nocturne.TextMuted,
                        modifier = Modifier.clickable { viewModel.delete(tag.id) }
                    )
                }
            }
            if (tags.isEmpty()) item { EmptyHint("Nenhuma tag ainda.") }
        }
    }
}
