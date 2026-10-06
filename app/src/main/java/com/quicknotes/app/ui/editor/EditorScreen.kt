package com.quicknotes.app.ui.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.components.AddableChip
import com.quicknotes.app.ui.components.FilledChip
import com.quicknotes.app.ui.components.SectionLabel
import com.quicknotes.app.ui.theme.Nocturne

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onSaved: () -> Unit,
    onClose: () -> Unit = onSaved,
    onOpenNote: (Long) -> Unit = {},
    onCreateNote: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val folders by viewModel.allFolders.collectAsState()
    val outgoing by viewModel.outgoingLinks.collectAsState()
    val backlinks by viewModel.backlinks.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fechar", tint = Nocturne.TextSecondary)
            }
            Text(
                editorMeta(state),
                color = Nocturne.TextMuted, fontSize = 11.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = viewModel::toggleFavorite) {
                Icon(
                    if (state.favorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Favorito",
                    tint = if (state.favorite) Nocturne.Warning else Nocturne.TextSecondary
                )
            }
            IconButton(onClick = viewModel::toggleArchived) {
                Icon(
                    if (state.archived) Icons.Filled.Unarchive else Icons.Filled.Archive,
                    contentDescription = "Arquivar",
                    tint = if (state.archived) Nocturne.Info else Nocturne.TextSecondary
                )
            }
            IconButton(onClick = { viewModel.delete(onSaved) }) {
                Icon(Icons.Filled.Delete, contentDescription = "Excluir", tint = Nocturne.TextSecondary)
            }
            IconButton(onClick = { viewModel.save(onSaved) }) {
                Icon(Icons.Filled.Check, contentDescription = "Salvar", tint = Nocturne.AccentText)
            }
        }

        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 18.dp)) {
            item {
                PlainField(
                    value = state.title, onValueChange = viewModel::updateTitle,
                    placeholder = "Título", fontSize = 22.sp, fontWeight = FontWeight.Medium
                )
            }
            item {
                PlainField(
                    value = state.content, onValueChange = viewModel::updateContent,
                    placeholder = "Escreva…", fontSize = 14.5.sp, minHeight = 180.dp
                )
            }
            item {
                SectionLabel("Tags", Modifier.padding(top = 14.dp, bottom = 9.dp))
                FlowChips {
                    tags.filter { it.id in state.tagIds }.forEach { tag ->
                        FilledChip("#${tag.name}", onRemove = { viewModel.removeTag(tag.id) })
                    }
                    tags.filterNot { it.id in state.tagIds }.forEach { tag ->
                        AddableChip("#${tag.name}", onClick = { viewModel.addTag(tag.id) })
                    }
                }
            }
            item {
                SectionLabel("Pasta", Modifier.padding(top = 22.dp, bottom = 9.dp))
                FlowChips {
                    FilledChip("Sem pasta", selected = state.folderId == null, onClick = { viewModel.pickFolder(null) })
                    folders.forEach { folder ->
                        FilledChip(folder.name, selected = state.folderId == folder.id, onClick = { viewModel.pickFolder(folder.id) })
                    }
                }
            }
            item {
                LinksSection(backlinks = backlinks, outgoing = outgoing, onOpenNote = onOpenNote, onCreateGhost = onCreateNote)
            }
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 24.dp)
                        .background(
                            if (state.inbox) Nocturne.AccentTint else Color.Transparent,
                            androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                        .border(
                            1.dp,
                            if (state.inbox) Nocturne.AccentBorder else Nocturne.BorderSubtle,
                            androidx.compose.foundation.shape.RoundedCornerShape(10.dp)
                        )
                        .clickable(onClick = viewModel::toggleInbox)
                        .padding(12.dp),
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        if (state.inbox) "Na Inbox — decidir depois" else "Fora da Inbox — já organizada",
                        color = if (state.inbox) Nocturne.AccentText else Nocturne.TextSecondary,
                        fontSize = 12.5.sp
                    )
                }
            }
        }
    }
}

private fun editorMeta(state: EditorUiState): String {
    if (state.id == 0L) return ""
    val sourceLabel = when (state.captureSource) {
        com.quicknotes.app.domain.model.CaptureSource.WIDGET_VOICE -> "captura por voz"
        com.quicknotes.app.domain.model.CaptureSource.WIDGET_TEXT -> "captura pelo widget"
        com.quicknotes.app.domain.model.CaptureSource.APP -> "criada no app"
    }
    return sourceLabel
}

@Composable
private fun PlainField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight? = null,
    minHeight: androidx.compose.ui.unit.Dp = androidx.compose.ui.unit.Dp.Unspecified
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = Nocturne.TextMuted, fontSize = fontSize) },
        textStyle = TextStyle(color = Nocturne.TextPrimary, fontSize = fontSize, fontWeight = fontWeight, lineHeight = fontSize * 1.5f),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            cursorColor = Nocturne.Accent
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Default),
        modifier = Modifier
            .fillMaxWidth()
            .let { if (minHeight != androidx.compose.ui.unit.Dp.Unspecified) it.height(minHeight) else it }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowChips(content: @Composable androidx.compose.foundation.layout.FlowRowScope.() -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp),
        content = content
    )
}
