package com.quicknotes.app.data.repository

import androidx.room.withTransaction
import com.quicknotes.app.data.local.dao.NoteDao
import com.quicknotes.app.data.local.dao.NoteLinkDao
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.local.entity.NoteEntity
import com.quicknotes.app.data.local.entity.NoteTagEntity
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.model.NoteGraph
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.Tag
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class NoteRepositoryImpl(
    private val database: AppDatabase,
    private val noteDao: NoteDao = database.noteDao(),
    private val linkDao: NoteLinkDao = database.noteLinkDao()
) : NoteRepository {

    override fun observeInbox(): Flow<List<Note>> = noteDao.observeInbox().map { list -> list.map { it.toDomainWithTags() } }
    override fun observeFavorites(): Flow<List<Note>> = noteDao.observeFavorites().map { list -> list.map { it.toDomainWithTags() } }
    override fun observeArchived(): Flow<List<Note>> = noteDao.observeArchived().map { list -> list.map { it.toDomainWithTags() } }
    override fun observeByFolder(folderId: Long): Flow<List<Note>> = noteDao.observeByFolder(folderId).map { list -> list.map { it.toDomainWithTags() } }
    override fun observeRecent(limit: Int): Flow<List<Note>> = noteDao.observeRecent(limit).map { list -> list.map { it.toDomainWithTags() } }

    override suspend fun getNote(id: Long): Note? = noteDao.getById(id)?.toDomainWithTags()

    override suspend fun search(query: String): List<Note> = noteDao.search(query).map { it.toDomainWithTags() }

    override suspend fun saveNote(note: Note): Long = database.withTransaction {
        val entity = NoteEntity(
            id = note.id, title = note.title, content = note.content,
            createdAt = note.createdAt, updatedAt = note.updatedAt, folderId = note.folderId,
            favorite = note.favorite, archived = note.archived, inbox = note.inbox,
            captureSource = note.captureSource.name
        )
        val id = if (note.id == 0L) noteDao.insert(entity) else { noteDao.update(entity); note.id }
        noteDao.clearNoteTags(id)
        if (note.tagIds.isNotEmpty()) {
            noteDao.insertNoteTags(note.tagIds.map { NoteTagEntity(id, it) })
        }
        id
    }

    override suspend fun deleteNote(id: Long) = noteDao.delete(id)
    override suspend fun setFavorite(id: Long, favorite: Boolean) = noteDao.setFavorite(id, favorite)
    override suspend fun setArchived(id: Long, archived: Boolean) = noteDao.setArchived(id, archived)

    override fun observeOutgoingLinks(noteId: Long): Flow<List<NoteLink>> = linkDao.observeOutgoing(noteId)
    override fun observeBacklinks(noteId: Long): Flow<List<NoteRef>> = linkDao.observeBacklinks(noteId)
    override fun observeGraph(): Flow<NoteGraph> = combine(
        linkDao.observeNoteRefs(), linkDao.observeAll(), database.tagDao().observeAll(), linkDao.observeNoteTags()
    ) { notes, links, tags, noteTags -> NoteGraph(notes, links, tags.map { Tag(it.id, it.name) }, noteTags) }

    private suspend fun NoteEntity.toDomainWithTags(): Note = Note(
        id = id, title = title, content = content, createdAt = createdAt, updatedAt = updatedAt,
        folderId = folderId, favorite = favorite, archived = archived, inbox = inbox,
        captureSource = CaptureSource.valueOf(captureSource),
        tagIds = noteDao.getTagIdsForNote(id)
    )
}
