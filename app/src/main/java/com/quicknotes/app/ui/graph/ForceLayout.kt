package com.quicknotes.app.ui.graph

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Force-directed layout: Coulomb repulsion between every pair, Hooke springs on edges,
 * a weak pull to the origin and viscous damping. Held by GraphViewModel so positions
 * survive navigation. Tuning knobs are the constructor params.
 */
class ForceLayout(
    private val repulsion: Float = 12_000f,
    private val springLength: Float = 110f,
    private val springStrength: Float = 0.04f,
    private val gravity: Float = 0.01f,
    private val damping: Float = 0.85f,
    private val maxSpeed: Float = 40f
) {
    class Body(var x: Float, var y: Float) {
        var vx = 0f
        var vy = 0f
        var pinned = false
    }

    private val bodies = LinkedHashMap<String, Body>()
    private var edges: List<Pair<String, String>> = emptyList()

    val keys: Set<String> get() = bodies.keys
    fun body(key: String): Body? = bodies[key]

    /** Keeps positions of surviving keys, drops removed ones, seeds new ones on a golden-angle spiral. */
    fun sync(nodeKeys: List<String>, edgeKeys: List<Pair<String, String>>) {
        bodies.keys.retainAll(nodeKeys.toSet())
        for (key in nodeKeys) {
            if (key !in bodies) {
                val i = bodies.size
                val r = 40f * sqrt(i + 1f)
                val a = i * 2.39996f
                bodies[key] = Body(r * cos(a), r * sin(a))
            }
        }
        edges = edgeKeys.filter { (a, b) -> a != b && a in bodies && b in bodies }
    }

    fun pin(key: String, x: Float, y: Float) {
        bodies[key]?.apply { this.x = x; this.y = y; vx = 0f; vy = 0f; pinned = true }
    }

    fun release(key: String) { bodies[key]?.pinned = false }

    /**
     * Runs up to [iterations] steps (the screen caps this at 10 per frame). Bodies for which
     * [isActive] is false (off-screen culling) or that are pinned are not moved, but still push/pull.
     * ponytail: O(n²) repulsion, fine to ~300 nodes; Barnes-Hut if graphs grow beyond that.
     */
    fun step(iterations: Int = 1, restSpeed: Float = 0.05f, isActive: (Body) -> Boolean = { true }): Boolean {
        val list = bodies.values.toList()
        val indexOf = bodies.keys.withIndex().associate { it.value to it.index }
        val edgeIdx = edges.map { (a, b) -> indexOf.getValue(a) to indexOf.getValue(b) }
        var moving = false
        repeat(iterations) {
            val fx = FloatArray(list.size)
            val fy = FloatArray(list.size)
            for (i in list.indices) for (j in i + 1 until list.size) {
                var dx = list[i].x - list[j].x
                var dy = list[i].y - list[j].y
                var d2 = dx * dx + dy * dy
                if (d2 < 0.01f) { // coincident bodies: deterministic nudge instead of dividing by ~0
                    val a = (i * 31 + j) * 0.7f
                    dx = cos(a); dy = sin(a); d2 = 1f
                }
                val d = sqrt(d2)
                val f = repulsion / d2
                fx[i] += f * dx / d; fy[i] += f * dy / d
                fx[j] -= f * dx / d; fy[j] -= f * dy / d
            }
            for ((i, j) in edgeIdx) {
                val dx = list[j].x - list[i].x
                val dy = list[j].y - list[i].y
                val d = sqrt(maxOf(dx * dx + dy * dy, 0.01f))
                val f = springStrength * (d - springLength)
                fx[i] += f * dx / d; fy[i] += f * dy / d
                fx[j] -= f * dx / d; fy[j] -= f * dy / d
            }
            moving = false
            for ((i, body) in list.withIndex()) {
                if (body.pinned || !isActive(body)) { body.vx = 0f; body.vy = 0f; continue }
                body.vx = (body.vx + fx[i] - gravity * body.x) * damping
                body.vy = (body.vy + fy[i] - gravity * body.y) * damping
                val speed = sqrt(body.vx * body.vx + body.vy * body.vy)
                if (speed > maxSpeed) { body.vx *= maxSpeed / speed; body.vy *= maxSpeed / speed }
                body.x += body.vx
                body.y += body.vy
                if (speed > restSpeed) moving = true
            }
        }
        return moving
    }
}
