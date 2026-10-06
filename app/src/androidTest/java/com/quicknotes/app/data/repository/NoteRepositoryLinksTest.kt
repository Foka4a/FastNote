package com.quicknotes.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.local.entity.NoteEntity
import com.quicknotes.app.data.local.entity.NoteLinkEntity
import com.quicknotes.app.data.local.entity.NoteTagEntity
import com.quicknotes.app.data.local.entity.TagEntity
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.NoteTagRef
import com.quicknotes.app.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteRepositoryLinksTest {
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

    private suspend fun rawNote(title: String, content: String = ""): Long = db.noteDao().insert(
        NoteEntity(title = title, content = content, createdAt = 1, updatedAt = 1, folderId = null,
            favorite = false, archived = false, inbox = true, captureSource = "APP")
    )

    @Test
    fun outgoingLinksAreOrderedAndBacklinksAreDistinctWithoutSelf() = runBlocking {
        val a = rawNote("A")
        val b = rawNote("B")
        db.noteLinkDao().insertAll(listOf(
            NoteLinkEntity(b, 2, a, "a"),
            NoteLinkEntity(b, 0, a, "A"),
            NoteLinkEntity(b, 1, null, "Futura"),
            NoteLinkEntity(a, 0, a, "A") // self link
        ))

        assertEquals(
            listOf(NoteLink(b, 0, a, "A"), NoteLink(b, 1, null, "Futura"), NoteLink(b, 2, a, "a")),
            repository.observeOutgoingLinks(b).first()
        )
        assertEquals(listOf(NoteRef(b, "B")), repository.observeBacklinks(a).first())
        assertEquals(true, repository.observeOutgoingLinks(b).first()[1].isGhost)
    }

    @Test
    fun graphCombinesNotesLinksTagsAndNoteTags() = runBlocking {
        val a = rawNote("A")
        val b = rawNote("B")
        val tag = db.tagDao().insert(TagEntity(name = "ideia"))
        db.noteDao().insertNoteTags(listOf(NoteTagEntity(a, tag)))
        db.noteLinkDao().insertAll(listOf(NoteLinkEntity(b, 0, a, "A")))

        val graph = repository.observeGraph().first()

        assertEquals(listOf(NoteRef(a, "A"), NoteRef(b, "B")), graph.notes.sortedBy { it.id })
        assertEquals(listOf(NoteLink(b, 0, a, "A")), graph.links)
        assertEquals(listOf(Tag(tag, "ideia")), graph.tags)
        assertEquals(listOf(NoteTagRef(a, tag)), graph.noteTags)
    }
}
