package com.quicknotes.app.domain

import com.quicknotes.app.domain.link.LinkParser
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.model.NoteGraph
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.NoteTagRef
import com.quicknotes.app.domain.model.Tag
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class FakeNoteRepository : NoteRepository {
    private var nextId = 1L
    val notes = MutableStateFlow<List<Note>>(emptyList())
    val tags = MutableStateFlow<List<Tag>>(emptyList())

    // Derived from content on every emission, so deleting a target turns its links into ghosts.
    private fun resolvedLinks(all: List<Note>): List<NoteLink> {
        val titles = LinkParser.titleIndex(all.map { NoteRef(it.id, it.title) })
        return all.sortedBy { it.id }.flatMap { note ->
            LinkParser.extractLinks(note.content).map { NoteLink(note.id, it.index, titles[LinkParser.normalize(it.title)], it.title) }
        }
    }

    override fun observeOutgoingLinks(noteId: Long): Flow<List<NoteLink>> =
        notes.map { all -> resolvedLinks(all).filter { it.sourceId == noteId } }

    override fun observeBacklinks(noteId: Long): Flow<List<NoteRef>> = notes.map { all ->
        val sources = resolvedLinks(all).filter { it.targetId == noteId && it.sourceId != noteId }.map { it.sourceId }.toSet()
        all.filter { it.id in sources }.sortedBy { it.id }.map { NoteRef(it.id, it.title) }
    }

    override fun observeGraph(): Flow<NoteGraph> = combine(notes, tags) { all, allTags ->
        NoteGraph(
            notes = all.map { NoteRef(it.id, it.title) },
            links = resolvedLinks(all),
            tags = allTags,
            noteTags = all.flatMap { note -> note.tagIds.map { NoteTagRef(note.id, it) } }
        )
    }

    override fun observeInbox(): Flow<List<Note>> = notes
    override fun observeFavorites(): Flow<List<Note>> = notes
    override fun observeArchived(): Flow<List<Note>> = notes
    override fun observeByFolder(folderId: Long): Flow<List<Note>> =
        notes.map { list -> list.filter { it.folderId == folderId && !it.archived }.sortedByDescending { it.updatedAt } }
    override fun observeRecent(limit: Int): Flow<List<Note>> = notes

    override suspend fun getNote(id: Long): Note? = notes.value.find { it.id == id }

    override suspend fun search(query: String): List<Note> =
        notes.value.filter { it.title.contains(query, ignoreCase = true) || it.content.contains(query, ignoreCase = true) }

    override suspend fun saveNote(note: Note): Long {
        val id = if (note.id == 0L) nextId++ else note.id
        val saved = note.copy(id = id)
        notes.value = notes.value.filterNot { it.id == id } + saved
        return id
    }

    override suspend fun deleteNote(id: Long) { notes.value = notes.value.filterNot { it.id == id } }
    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        notes.value = notes.value.map { if (it.id == id) it.copy(favorite = favorite) else it }
    }
    override suspend fun setArchived(id: Long, archived: Boolean) {
        notes.value = notes.value.map { if (it.id == id) it.copy(archived = archived, inbox = it.inbox || !archived) else it }
    }
}
