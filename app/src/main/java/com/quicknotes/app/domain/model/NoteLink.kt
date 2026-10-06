package com.quicknotes.app.domain.model

/** A `[[targetTitle]]` in note `sourceId`, at position `index`. targetId == null -> fantasma. */
data class NoteLink(val sourceId: Long, val index: Int, val targetId: Long?, val targetTitle: String) {
    val isGhost: Boolean get() = targetId == null
}

data class NoteTagRef(val noteId: Long, val tagId: Long)

/** Raw graph snapshot; the UI decides what becomes a node (folders never do). */
data class NoteGraph(
    val notes: List<NoteRef> = emptyList(),
    val links: List<NoteLink> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val noteTags: List<NoteTagRef> = emptyList()
)
