package com.quicknotes.app.ui.graph

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class ForceLayoutTest {
    private fun ForceLayout.dist(a: String, b: String): Float {
        val p = body(a)!!; val q = body(b)!!
        return hypot(p.x - q.x, p.y - q.y)
    }

    @Test
    fun unconnectedNodesRepel() {
        val layout = ForceLayout().apply { sync(listOf("a", "b"), emptyList()) }
        val before = layout.dist("a", "b")
        layout.step(iterations = 20)
        assertTrue(layout.dist("a", "b") > before)
    }

    @Test
    fun connectedPairEndsCloserThanUnconnectedAndSettles() {
        val layout = ForceLayout().apply { sync(listOf("a", "b", "c"), listOf("a" to "b")) }
        layout.step(iterations = 2000)
        assertTrue(layout.dist("a", "b") < layout.dist("a", "c"))
        assertTrue(layout.dist("a", "b") < layout.dist("b", "c"))
        assertFalse(layout.step())
    }

    @Test
    fun coincidentNodesDoNotProduceNaN() {
        val layout = ForceLayout().apply { sync(listOf("a", "b", "c"), listOf("a" to "a", "a" to "b")) }
        listOf("a", "b", "c").forEach { layout.pin(it, 0f, 0f); layout.release(it) }
        layout.step(iterations = 10)
        layout.keys.forEach { key ->
            val body = layout.body(key)!!
            assertTrue(body.x.isFinite() && body.y.isFinite())
        }
        assertTrue(layout.dist("a", "b") > 0f)
    }

    @Test
    fun syncKeepsSurvivorsAndDropsRemovedKeys() {
        val layout = ForceLayout().apply { sync(listOf("a", "b"), emptyList()) }
        layout.pin("a", 123f, -45f)
        layout.sync(listOf("a", "c"), listOf("a" to "b")) // edge to a removed key is ignored
        assertEquals(setOf("a", "c"), layout.keys)
        assertEquals(123f, layout.body("a")!!.x)
        assertEquals(-45f, layout.body("a")!!.y)
    }

    @Test
    fun pinnedAndInactiveBodiesDoNotMove() {
        val layout = ForceLayout().apply { sync(listOf("a", "b", "c"), listOf("a" to "b")) }
        layout.pin("a", 10f, 10f)
        val c = layout.body("c")!!
        val cx = c.x; val cy = c.y
        layout.step(iterations = 50, isActive = { it !== c })
        assertEquals(10f, layout.body("a")!!.x)
        assertEquals(cx, c.x); assertEquals(cy, c.y)
    }
}
