package com.quicknotes.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// "Nocturne" — the dark palette from the QuickNotes design (claude.ai/design project
// 448015ec-1b52-49cd-83b0-c3505a626794). Values are lifted directly from the mockup's hex/rgba,
// not rounded to a grid, so components can reference them exactly where a Material role doesn't fit.
object Nocturne {
    val Canvas = Color(0xFF0F1018)
    val Background = Color(0xFF161826)
    val Surface = Color(0xFF232532)
    val SurfaceDim = Color(0xFF1B1D2B)
    val SurfaceElevated = Color(0xFF2B2E3D)
    val BottomBar = Color(0xFF1A1C29)

    val Accent = Color(0xFF9184D9)
    val AccentText = Color(0xFFB9AFE8)
    val OnAccent = Color(0xFF161826)

    val TextPrimary = Color(0xFFE9E9ED)
    val TextSecondary = TextPrimary.copy(alpha = .55f)
    val TextMuted = TextPrimary.copy(alpha = .40f)
    val TextFaint = TextPrimary.copy(alpha = .30f)

    val BorderSubtle = TextPrimary.copy(alpha = .09f)
    val BorderFaint = TextPrimary.copy(alpha = .06f)
    val AccentBorder = Accent.copy(alpha = .45f)
    val AccentTint = Accent.copy(alpha = .14f)

    val Warning = Color(0xFFE6C98A)
    val Info = Color(0xFF8FB8D9)
    val Success = Color(0xFF8FD9A8)
    val Danger = Color(0xFFD98F8F)
}

private val QuickNotesColorScheme = darkColorScheme(
    primary = Nocturne.Accent,
    onPrimary = Nocturne.OnAccent,
    background = Nocturne.Background,
    onBackground = Nocturne.TextPrimary,
    surface = Nocturne.Background,
    onSurface = Nocturne.TextPrimary,
    surfaceVariant = Nocturne.Surface,
    onSurfaceVariant = Nocturne.TextSecondary,
    secondaryContainer = Nocturne.AccentTint,
    onSecondaryContainer = Nocturne.AccentText,
    error = Nocturne.Danger,
    outline = Nocturne.BorderSubtle
)

@Composable
fun QuickNotesTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = QuickNotesColorScheme, content = content)
}
