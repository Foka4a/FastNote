package com.quicknotes.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteRepositoryImplTest {
    private lateinit var db: AppDatabase
    private lateinit var repository: NoteRepositoryImpl

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = NoteRepositoryImpl(db)
    }

    @After
    fun tearDown() { db.close() }

    @Test
    fun saveNoteThenReadItBackWithTags() = runBlocking {
        val tagId = db.tagDao().insert(com.quicknotes.app.data.local.entity.TagEntity(name = "ideia"))
        val id = repository.saveNote(
            Note(
                title = "Testar overlay", content = "conteúdo", createdAt = 1, updatedAt = 1,
                folderId = null, favorite = false, archived = false, inbox = true,
                captureSource = CaptureSource.WIDGET_TEXT, tagIds = listOf(tagId)
            )
        )

        val saved = repository.getNote(id)

        assertEquals("Testar overlay", saved?.title)
        assertEquals(listOf(tagId), saved?.tagIds)
        assertTrue(repository.observeInbox().first().any { it.id == id })
    }

    @Test
    fun setFavoriteAndArchivedUpdateFlags() = runBlocking {
        val id = repository.saveNote(
            Note(title = "A", content = "", createdAt = 1, updatedAt = 1, folderId = null,
                favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP)
        )

        repository.setFavorite(id, true)
        assertTrue(repository.getNote(id)!!.favorite)

        repository.setArchived(id, true)
        assertTrue(repository.getNote(id)!!.archived)
    }
}
