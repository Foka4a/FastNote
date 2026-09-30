package com.quicknotes.app.domain.repository

import com.quicknotes.app.domain.model.Note
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
}
