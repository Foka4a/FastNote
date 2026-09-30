package com.quicknotes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.ui.theme.Nocturne

/** A round icon button sized to match the mockup's 42dp header controls. */
@Composable
fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    IconButton(onClick = onClick, modifier = modifier.size(42.dp)) {
        Icon(icon, contentDescription = contentDescription, tint = Nocturne.TextSecondary)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = Nocturne.TextMuted,
        fontSize = 11.sp,
        letterSpacing = 0.6.sp,
        modifier = modifier
    )
}

/** A pill-shaped source icon, colored to show whether a note came from voice, text or the app. */
@Composable
fun SourceAvatar(source: CaptureSource, modifier: Modifier = Modifier) {
    val icon = when (source) {
        CaptureSource.WIDGET_VOICE -> Icons.Filled.Mic
        CaptureSource.WIDGET_TEXT -> Icons.Filled.Edit
        CaptureSource.APP -> Icons.AutoMirrored.Filled.Notes
    }
    Box(
        modifier = modifier
            .size(30.dp)
            .background(Nocturne.TextPrimary.copy(alpha = .06f), CircleShape)
            .border(1.dp, Nocturne.BorderSubtle, CircleShape),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Nocturne.TextSecondary, modifier = Modifier.size(14.dp))
    }
}

/** Solid pill chip for a value attached to a note (a tag already applied, a picked folder). */
@Composable
fun FilledChip(
    label: String,
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    onClick: (() -> Unit)? = null,
    onRemove: (() -> Unit)? = null
) {
    val bg = if (selected) Nocturne.AccentTint else Color.Transparent
    val border = if (selected) Nocturne.AccentBorder else Nocturne.BorderSubtle
    val fg = if (selected) Nocturne.AccentText else Nocturne.TextSecondary
    Row(
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .background(bg, RoundedCornerShape(999.dp))
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    ) {
        Text(label, color = fg, fontSize = 12.sp)
        if (onRemove != null) {
            Icon(
                Icons.Filled.Close, contentDescription = "Remover",
                tint = fg, modifier = Modifier.size(11.dp).clickable(onClick = onRemove)
            )
        }
    }
}

/** Dashed outline chip offering to add a tag/value that isn't attached yet. */
@Composable
fun AddableChip(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        "+ $label",
        color = Nocturne.TextSecondary,
        fontSize = 12.sp,
        modifier = modifier
            .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(999.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp)
    )
}

@Composable
fun EmptyHint(text: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.padding(40.dp)) {
        Text(text, color = Nocturne.TextMuted, fontSize = 12.5.sp, lineHeight = 19.sp)
    }
}
