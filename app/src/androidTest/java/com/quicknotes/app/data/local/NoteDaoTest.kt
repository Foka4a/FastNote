package com.quicknotes.app.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() { db.close() }

    private fun note(title: String, inbox: Boolean = true) = NoteEntity(
        title = title, content = "content $title", createdAt = 1L, updatedAt = 1L,
        folderId = null, favorite = false, archived = false, inbox = inbox, captureSource = "APP"
    )

    @Test
    fun insertAndObserveInbox() = runBlocking {
        db.noteDao().insert(note("A"))
        db.noteDao().insert(note("B", inbox = false))

        val inbox = db.noteDao().observeInbox().first()

        assertEquals(1, inbox.size)
        assertEquals("A", inbox[0].title)
    }

    @Test
    fun searchMatchesTitleContentTagAndFolder() = runBlocking {
        val folderId = db.folderDao().insert(com.quicknotes.app.data.local.entity.FolderEntity(name = "Projetos", parentId = null))
        val noteId = db.noteDao().insert(note("Reunião").copy(folderId = folderId))
        val tagId = db.tagDao().insert(com.quicknotes.app.data.local.entity.TagEntity(name = "trabalho"))
        db.noteDao().insertNoteTags(listOf(com.quicknotes.app.data.local.entity.NoteTagEntity(noteId, tagId)))

        assertEquals(1, db.noteDao().search("Reunião").size)
        assertEquals(1, db.noteDao().search("trabalho").size)
        assertEquals(1, db.noteDao().search("Projetos").size)
        assertEquals(0, db.noteDao().search("inexistente").size)
    }

    @Test
    fun observeByFolderReturnsOnlyActiveNotesOfThatFolderNewestFirst() = runBlocking {
        val folderId = db.folderDao().insert(com.quicknotes.app.data.local.entity.FolderEntity(name = "Pasta", parentId = null))
        db.noteDao().insert(note("velha").copy(folderId = folderId, updatedAt = 1L))
        db.noteDao().insert(note("nova").copy(folderId = folderId, updatedAt = 2L))
        db.noteDao().insert(note("arquivada").copy(folderId = folderId, archived = true))
        db.noteDao().insert(note("solta"))

        assertEquals(listOf("nova", "velha"), db.noteDao().observeByFolder(folderId).first().map { it.title })
    }

    @Test
    fun archivingRemovesNoteFromInbox() = runBlocking {
        val id = db.noteDao().insert(note("A"))
        db.noteDao().setArchived(id, true)

        assertEquals(0, db.noteDao().observeInbox().first().size)
        assertEquals(1, db.noteDao().observeArchived().first().size)
    }

    @Test
    fun unarchiveReturnsNoteToInbox() = runBlocking {
        val id = db.noteDao().insert(note("A", inbox = false).copy(archived = true))
        db.noteDao().setArchived(id, false)

        assertEquals(listOf("A"), db.noteDao().observeInbox().first().map { it.title })
    }

    @Test
    fun archivedNotesHiddenFromRecentAndFavoritesButStillSearchable() = runBlocking {
        db.noteDao().insert(note("Oculta").copy(archived = true, favorite = true))

        assertEquals(0, db.noteDao().observeRecent(10).first().size)
        assertEquals(0, db.noteDao().observeFavorites().first().size)
        assertEquals(1, db.noteDao().search("Oculta").size)
    }
}
