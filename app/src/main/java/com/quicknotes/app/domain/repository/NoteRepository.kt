package com.quicknotes.app.domain.repository

import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.model.NoteGraph
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    fun observeInbox(): Flow<List<Note>>
    fun observeFavorites(): Flow<List<Note>>
    fun observeArchived(): Flow<List<Note>>
    fun observeByFolder(folderId: Long): Flow<List<Note>>
    fun observeRecent(limit: Int): Flow<List<Note>>
    suspend fun getNote(id: Long): Note?
    suspend fun search(query: String): List<Note>
    suspend fun saveNote(note: Note): Long
    suspend fun deleteNote(id: Long)
    suspend fun setFavorite(id: Long, favorite: Boolean)
    suspend fun setArchived(id: Long, archived: Boolean)
    fun observeOutgoingLinks(noteId: Long): Flow<List<NoteLink>>
    fun observeBacklinks(noteId: Long): Flow<List<NoteRef>>
    fun observeGraph(): Flow<NoteGraph>
}
