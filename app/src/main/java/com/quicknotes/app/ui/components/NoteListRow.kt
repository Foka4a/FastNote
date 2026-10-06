package com.quicknotes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.theme.Nocturne

/** Shared card for a note in a flat list (Favorites, Archive): title/content, a meta line, and
 *  one trailing action (unstar, restore) — the "isList" style from the QuickNotes design. */
@Composable
fun NoteListRow(
    title: String,
    content: String,
    meta: String,
    actionIcon: ImageVector,
    actionDescription: String,
    onClick: () -> Unit,
    onAction: () -> Unit,
    actionTint: androidx.compose.ui.graphics.Color = Nocturne.TextSecondary
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .background(Nocturne.Surface, RoundedCornerShape(12.dp))
            .border(1.dp, Nocturne.BorderSubtle, RoundedCornerShape(12.dp))
            .padding(13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).clickable(onClick = onClick)) {
            Text(title.ifBlank { content.take(40) }, color = Nocturne.TextPrimary, fontSize = 14.5.sp, fontWeight = FontWeight.Medium)
            if (content.isNotBlank()) {
                Text(
                    content, color = Nocturne.TextSecondary, fontSize = 12.5.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp)
                )
            }
            Text(meta, color = Nocturne.TextFaint, fontSize = 10.5.sp, modifier = Modifier.padding(top = 7.dp))
        }
        Row(
            Modifier
                .size(36.dp)
                .background(androidx.compose.ui.graphics.Color.Transparent, CircleShape)
                .clickable(onClick = onAction),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(actionIcon, contentDescription = actionDescription, tint = actionTint)
        }
    }
}
