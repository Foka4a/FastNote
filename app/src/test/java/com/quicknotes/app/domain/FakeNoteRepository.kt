package com.quicknotes.app.domain

import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeNoteRepository : NoteRepository {
    private var nextId = 1L
    val notes = MutableStateFlow<List<Note>>(emptyList())

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
