package com.quicknotes.app.ui.inbox

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.components.FilledChip
import com.quicknotes.app.ui.components.LocalSnackbarHostState
import com.quicknotes.app.ui.components.SourceAvatar
import com.quicknotes.app.ui.theme.Nocturne
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun InboxScreen(viewModel: InboxViewModel, onNoteClick: (Long) -> Unit) {
    val notes by viewModel.notes.collectAsState()
    val selected by viewModel.selected.collectAsState()
    val snackbarHostState = LocalSnackbarHostState.current

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message,
                actionLabel = if (event.undo != null) "DESFAZER" else null
            )
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) event.undo?.invoke()
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.testTag("inbox_list").fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text("${notes.size} para revisar", color = Nocturne.AccentText, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        if (selected.isEmpty()) "Selecionar" else "Cancelar",
                        color = Nocturne.TextSecondary,
                        fontSize = 11.5.sp,
                        modifier = Modifier
                            .border(1.dp, Nocturne.BorderSubtle, RoundedCornerShape(999.dp))
                            .clickable { viewModel.selectAll() }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
            items(notes, key = { it.note.id }) { item ->
                SwipeableInboxCard(
                    item = item,
                    isSelected = item.note.id in selected,
                    onOpen = { if (selected.isNotEmpty()) viewModel.toggleSelect(item.note.id) else onNoteClick(item.note.id) },
                    onToggleSelect = { viewModel.toggleSelect(item.note.id) },
                    onFavorite = { viewModel.setFavorite(item.note.id, !item.note.favorite) },
                    onArchive = { viewModel.setArchived(item.note.id, true) }
                )
            }
            item {
                EmptyHint(
                    "Arraste uma nota para o lado para favoritar ou arquivar. Nada sai da Inbox sozinho — capturar primeiro, organizar depois.",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (selected.isNotEmpty()) {
            SelectionBar(
                count = selected.size,
                onFavorite = viewModel::batchFavorite,
                onArchive = viewModel::batchArchive,
                onDelete = viewModel::batchDelete,
                modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp)
            )
        }
    }
}

@Composable
private fun SelectionBar(
    count: Int,
    onFavorite: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Nocturne.SurfaceElevated, RoundedCornerShape(14.dp))
            .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(14.dp))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("$count", color = Nocturne.AccentText, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 6.dp))
        BatchAction("Favoritar", Icons.Filled.Star, onFavorite, Modifier.weight(1f))
        BatchAction("Arquivar", Icons.Filled.Archive, onArchive, Modifier.weight(1f))
        BatchAction("Excluir", Icons.Filled.Delete, onDelete, Modifier.weight(1f))
    }
}

@Composable
private fun BatchAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = Nocturne.TextPrimary.copy(alpha = .8f), modifier = Modifier.size(17.dp))
        Text(label, color = Nocturne.TextPrimary.copy(alpha = .8f), fontSize = 9.5.sp)
    }
}

@Composable
private fun SwipeableInboxCard(
    item: InboxNoteUi,
    isSelected: Boolean,
    onOpen: () -> Unit,
    onToggleSelect: () -> Unit,
    onFavorite: () -> Unit,
    onArchive: () -> Unit
) {
    val note = item.note
    val density = LocalDensity.current
    val thresholdPx = with(density) { 80.dp.toPx() }
    val maxPx = with(density) { 140.dp.toPx() }
    val offsetX = remember(note.id) { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        Row(
            Modifier.fillMaxWidth().background(Nocturne.SurfaceDim).padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(Icons.Filled.Star, contentDescription = null, tint = Nocturne.Warning, modifier = Modifier.size(15.dp))
                Text("Favoritar", color = Nocturne.Warning, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("Arquivar", color = Nocturne.Info, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Icon(Icons.Filled.Archive, contentDescription = null, tint = Nocturne.Info, modifier = Modifier.size(15.dp))
            }
        }
        Row(
            modifier = Modifier
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .fillMaxWidth()
                .background(Nocturne.Surface, RoundedCornerShape(12.dp))
                .border(1.dp, if (isSelected) Nocturne.AccentBorder else Nocturne.BorderSubtle, RoundedCornerShape(12.dp))
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch { offsetX.snapTo((offsetX.value + delta).coerceIn(-maxPx, maxPx)) }
                    },
                    onDragStopped = {
                        val v = offsetX.value
                        if (v > thresholdPx) onFavorite() else if (v < -thresholdPx) onArchive()
                        offsetX.animateTo(0f, tween(220))
                    }
                )
                .padding(13.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val avatarBg = if (isSelected) Nocturne.Accent else Nocturne.TextPrimary.copy(alpha = .06f)
            Box(
                Modifier
                    .size(30.dp)
                    .background(avatarBg, CircleShape)
                    .clickable(onClick = onToggleSelect)
            ) {
                if (isSelected) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BasicText("✓", style = androidx.compose.ui.text.TextStyle(color = Nocturne.OnAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    }
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        SourceAvatarInline(note.captureSource)
                    }
                }
            }
            Column(Modifier.weight(1f).clickable(onClick = onOpen)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        note.title.ifBlank { note.content.take(40) },
                        color = Nocturne.TextPrimary,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (note.content.isNotBlank()) {
                    Text(
                        note.content,
                        color = Nocturne.TextSecondary,
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 3.dp)
                    )
                }
                if (item.tagNames.isNotEmpty()) {
                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item.tagNames.forEach { name -> FilledChip("#$name", selected = true) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceAvatarInline(source: com.quicknotes.app.domain.model.CaptureSource) {
    val icon = when (source) {
        com.quicknotes.app.domain.model.CaptureSource.WIDGET_VOICE -> Icons.Filled.Mic
        com.quicknotes.app.domain.model.CaptureSource.WIDGET_TEXT -> Icons.Filled.Edit
        com.quicknotes.app.domain.model.CaptureSource.APP -> Icons.AutoMirrored.Filled.Notes
    }
    Icon(icon, contentDescription = null, tint = Nocturne.TextSecondary, modifier = Modifier.size(14.dp))
}
