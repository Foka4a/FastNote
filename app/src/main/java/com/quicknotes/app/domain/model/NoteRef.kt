package com.quicknotes.app.domain.model

/** Lightweight note reference (id + title) for links, backlinks, pickers and the graph. */
data class NoteRef(val id: Long, val title: String)
