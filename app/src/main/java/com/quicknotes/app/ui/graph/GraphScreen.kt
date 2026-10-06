package com.quicknotes.app.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.components.EmptyHint
import com.quicknotes.app.ui.theme.Nocturne

private const val LABEL_MAX = 22

@Composable
fun GraphScreen(viewModel: GraphViewModel, onOpenNote: (Long) -> Unit, onCreateNote: (String) -> Unit) {
    val data by viewModel.graph.collectAsState()
    val filterTagId by viewModel.filterTagId.collectAsState()
    val layout = viewModel.layout
    val density = LocalDensity.current
    val noteRadius = with(density) { 20.dp.toPx() }
    val tagHalf = with(density) { 9.dp.toPx() }
    val minTouch = with(density) { 24.dp.toPx() }
    val textMeasurer = rememberTextMeasurer()
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var frame by remember { mutableIntStateOf(0) } // bumped per simulated frame so the Canvas redraws
    var wake by remember { mutableIntStateOf(0) }  // bumped on drag start to restart a settled simulation
    var dragKey by remember { mutableStateOf<String?>(null) }
    // ponytail: GraphData() is indistinguishable from an empty graph, so hold the empty hint back briefly instead of flashing it.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(300); settled = true }
    val labels = remember(data) {
        data.nodes.associate { node ->
            val text = if (node.label.length > LABEL_MAX) node.label.take(LABEL_MAX) + "…" else node.label
            node.key to textMeasurer.measure(text, TextStyle(color = Nocturne.TextSecondary, fontSize = 10.5.sp))
        }
    }
    val onTap by rememberUpdatedState<(GraphNode) -> Unit> { node ->
        when (node.kind) {
            NodeKind.NOTE -> onOpenNote(node.noteId!!)
            NodeKind.TAG -> viewModel.toggleTagFilter(node.tagId!!)
            NodeKind.GHOST -> onCreateNote(node.label)
        }
    }

    fun toScreen(body: ForceLayout.Body) =
        Offset(canvasSize.width / 2f + offset.x + body.x * scale, canvasSize.height / 2f + offset.y + body.y * scale)

    fun hitTest(p: Offset): GraphNode? = data.nodes.lastOrNull { node ->
        val body = layout.body(node.key) ?: return@lastOrNull false
        (toScreen(body) - p).getDistance() <= maxOf(noteRadius * scale, minTouch)
    }

    // Lazy updates: runs when the graph changes or a drag starts, stops once settled (or after ~10 s).
    LaunchedEffect(data, wake) {
        layout.sync(data.nodes.map { it.key }, data.edges.map { it.from to it.to })
        var idleFrames = 0
        while (true) {
            withFrameNanos { }
            val margin = 200f
            // Spec caps work at 10 iterations/frame; off-screen bodies are frozen (culling).
            val moving = layout.step(iterations = 5) { body ->
                val p = toScreen(body)
                p.x in -margin..canvasSize.width + margin && p.y in -margin..canvasSize.height + margin
            }
            frame++
            if (dragKey != null) idleFrames = 0 else if (!moving || ++idleFrames > 600) break
        }
    }

    Box(Modifier.fillMaxSize().background(Nocturne.Canvas)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .testTag("graph_canvas")
                .semantics { contentDescription = "Grafo com ${data.nodes.size} nós" }
                .onSizeChanged { canvasSize = it }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val hit = hitTest(down.position)
                        var moved = false
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            if (pressed.isEmpty()) break
                            if (pressed.size >= 2) { // pinch zoom + two-finger pan
                                moved = true
                                dragKey?.let { layout.release(it); dragKey = null }
                                scale = (scale * event.calculateZoom()).coerceIn(0.3f, 3f)
                                offset += event.calculatePan()
                            } else {
                                val change = pressed.first()
                                if (!moved && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) {
                                    moved = true
                                    if (hit != null) { dragKey = hit.key; wake++ }
                                }
                                if (moved) {
                                    val delta = change.positionChange()
                                    val key = dragKey
                                    val body = key?.let(layout::body)
                                    if (key != null && body != null) layout.pin(key, body.x + delta.x / scale, body.y + delta.y / scale)
                                    else offset += delta // one finger on empty space pans
                                }
                            }
                            event.changes.forEach { it.consume() }
                        }
                        dragKey?.let { layout.release(it); dragKey = null }
                        if (!moved && hit != null) onTap(hit)
                    }
                }
        ) {
            frame // read so every simulated frame invalidates the draw
            val stroke = 1.5.dp.toPx()
            for (edge in data.edges) {
                val a = layout.body(edge.from) ?: continue
                val b = layout.body(edge.to) ?: continue
                drawLine(
                    if (edge.isTagEdge) Nocturne.Success.copy(alpha = .35f) else Nocturne.TextMuted,
                    toScreen(a), toScreen(b), strokeWidth = stroke
                )
            }
            for (node in data.nodes) {
                val body = layout.body(node.key) ?: continue
                val c = toScreen(body)
                val r = noteRadius * scale
                if (c.x < -r || c.y < -r || c.x > size.width + r || c.y > size.height + r) continue // off-screen
                when (node.kind) {
                    NodeKind.NOTE -> drawCircle(Nocturne.Accent, r, c)
                    NodeKind.GHOST -> {
                        drawCircle(Nocturne.Accent, r, c, alpha = .5f)
                        drawCircle(
                            Nocturne.AccentText, r, c, alpha = .5f,
                            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
                        )
                    }
                    NodeKind.TAG -> {
                        val h = tagHalf * scale
                        drawRect(
                            if (node.tagId == filterTagId) Nocturne.Warning else Nocturne.Success,
                            topLeft = Offset(c.x - h, c.y - h), size = Size(2 * h, 2 * h)
                        )
                    }
                }
                labels[node.key]?.let { label ->
                    drawText(
                        label,
                        topLeft = Offset(c.x - label.size.width / 2f, c.y + maxOf(r, tagHalf * scale) + 4.dp.toPx()),
                        alpha = if (node.kind == NodeKind.GHOST) .5f else 1f
                    )
                }
            }
        }
        if (settled && data.nodes.isEmpty()) {
            EmptyHint("Nenhuma nota ainda. Escreva [[Título]] numa nota para criar ligações.")
        }
    }
}
