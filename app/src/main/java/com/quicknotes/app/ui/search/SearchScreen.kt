package com.quicknotes.app.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.components.SourceAvatar
import com.quicknotes.app.ui.theme.Nocturne

@Composable
fun SearchScreen(viewModel: SearchViewModel) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Nocturne.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = Nocturne.AccentText)
            Column(Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text("Título, conteúdo, tag ou pasta", color = Nocturne.TextMuted, fontSize = 14.sp)
                }
                BasicTextField(
                    value = query,
                    onValueChange = viewModel::onQueryChange,
                    textStyle = TextStyle(color = Nocturne.TextPrimary, fontSize = 14.sp),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Nocturne.Accent)
                )
            }
        }

        Text(
            "${results.size} ${if (results.size == 1) "resultado" else "resultados"}",
            color = Nocturne.TextMuted, fontSize = 11.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp, start = 4.dp)
        )

        LazyColumn(Modifier.fillMaxWidth()) {
            items(results, key = { it.id }) { note ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .background(Nocturne.Surface, RoundedCornerShape(12.dp))
                        .border(1.dp, Nocturne.BorderSubtle, RoundedCornerShape(12.dp))
                        .clickable { }
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SourceAvatar(note.captureSource)
                    Column {
                        Text(note.title.ifBlank { note.content.take(40) }, color = Nocturne.TextPrimary, fontSize = 14.sp)
                        if (note.content.isNotBlank()) {
                            Text(
                                note.content, color = Nocturne.TextSecondary, fontSize = 12.sp,
                                maxLines = 1, modifier = Modifier.padding(top = 3.dp)
                            )
                        }
                    }
                }
            }
            if (results.isEmpty() && query.isNotBlank()) {
                item { EmptyHint("Nenhum resultado para \"$query\".") }
            }
        }
    }
}
