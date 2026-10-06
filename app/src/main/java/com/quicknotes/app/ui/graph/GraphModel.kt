package com.quicknotes.app.ui.graph

import com.quicknotes.app.domain.link.LinkParser
import com.quicknotes.app.domain.model.NoteGraph

enum class NodeKind { NOTE, TAG, GHOST }

data class GraphNode(val key: String, val label: String, val kind: NodeKind, val noteId: Long? = null, val tagId: Long? = null)

data class GraphEdge(val from: String, val to: String, val isTagEdge: Boolean)

data class GraphData(val nodes: List<GraphNode> = emptyList(), val edges: List<GraphEdge> = emptyList())

private fun noteKey(id: Long) = "n:$id"
private fun tagKey(id: Long) = "t:$id"
private fun ghostKey(title: String) = "g:${LinkParser.normalize(title)}"

/** Notes + tags + ghosts. Folders are never nodes. An unknown [filterTagId] (tag deleted) shows the full graph. */
fun buildGraph(graph: NoteGraph, filterTagId: Long? = null): GraphData {
    val noteIds = graph.notes.map { it.id }.toSet()
    val tagIds = graph.tags.map { it.id }.toSet()
    val nodes = LinkedHashMap<String, GraphNode>()
    graph.notes.sortedBy { it.id }.forEach { nodes[noteKey(it.id)] = GraphNode(noteKey(it.id), it.title.ifBlank { "(sem título)" }, NodeKind.NOTE, noteId = it.id) }
    graph.tags.sortedBy { it.id }.forEach { nodes[tagKey(it.id)] = GraphNode(tagKey(it.id), "#${it.name}", NodeKind.TAG, tagId = it.id) }

    val edges = LinkedHashSet<GraphEdge>()
    for (link in graph.links) {
        if (link.sourceId !in noteIds) continue
        val target = link.targetId
        val to = when {
            target == null -> ghostKey(link.targetTitle).also { key ->
                nodes.getOrPut(key) { GraphNode(key, link.targetTitle, NodeKind.GHOST) }
            }
            target == link.sourceId || target !in noteIds -> continue
            else -> noteKey(target)
        }
        edges += GraphEdge(noteKey(link.sourceId), to, isTagEdge = false)
    }
    for (pair in graph.noteTags) {
        if (pair.noteId in noteIds && pair.tagId in tagIds) edges += GraphEdge(noteKey(pair.noteId), tagKey(pair.tagId), isTagEdge = true)
    }
    val full = GraphData(nodes.values.toList(), edges.toList())
    if (filterTagId == null || filterTagId !in tagIds) return full

    val tagged = graph.noteTags.filter { it.tagId == filterTagId && it.noteId in noteIds }.map { noteKey(it.noteId) }.toSet()
    val keep = tagged + tagKey(filterTagId) + edges.filter { it.isTagEdge && it.from in tagged }.map { it.to }
    return GraphData(full.nodes.filter { it.key in keep }, full.edges.filter { it.from in keep && it.to in keep })
}
