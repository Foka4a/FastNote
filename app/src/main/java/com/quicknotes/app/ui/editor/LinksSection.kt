package com.quicknotes.app.ui.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.domain.link.LinkParser
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.ui.components.SectionLabel
import com.quicknotes.app.ui.theme.Nocturne

/** Backlinks + outgoing links of the saved note; tapping a ghost creates it with the title prefilled. */
@Composable
fun LinksSection(
    backlinks: List<NoteRef>,
    outgoing: List<NoteLink>,
    onOpenNote: (Long) -> Unit,
    onCreateGhost: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (backlinks.isEmpty() && outgoing.isEmpty()) return
    Column(modifier.testTag("links_section")) {
        SectionLabel("Ligações", Modifier.padding(top = 22.dp, bottom = 9.dp))
        if (backlinks.isNotEmpty()) {
            GroupLabel("← Backlinks")
            backlinks.forEach { ref -> LinkRow(ref.title.ifBlank { "(sem título)" }, ghost = false) { onOpenNote(ref.id) } }
        }
        if (outgoing.isNotEmpty()) {
            GroupLabel("→ Links de saída")
            // Already ordered by index; the same title typed twice is listed once.
            outgoing.distinctBy { LinkParser.normalize(it.targetTitle) }.forEach { link ->
                LinkRow(link.targetTitle, ghost = link.isGhost) {
                    link.targetId?.let(onOpenNote) ?: onCreateGhost(link.targetTitle)
                }
            }
        }
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, color = Nocturne.TextMuted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 6.dp, bottom = 2.dp))
}

@Composable
private fun LinkRow(label: String, ghost: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(horizontal = 4.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            if (ghost) Icons.Filled.Add else Icons.Filled.Link,
            contentDescription = null,
            tint = if (ghost) Nocturne.TextMuted else Nocturne.AccentText,
            modifier = Modifier.size(17.dp)
        )
        Text(
            label, color = if (ghost) Nocturne.TextSecondary else Nocturne.TextPrimary, fontSize = 13.5.sp,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
        )
        if (ghost) Text("fantasma", color = Nocturne.TextFaint, fontSize = 11.sp)
    }
}
