package com.quicknotes.app.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.local.entity.NoteEntity
import com.quicknotes.app.data.local.entity.NoteLinkEntity
import com.quicknotes.app.data.local.entity.NoteTagEntity
import com.quicknotes.app.data.local.entity.TagEntity
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.NoteTagRef
import com.quicknotes.app.domain.model.Tag
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private fun note(title: String, content: String = "", id: Long = 0) = Note(
        id = id, title = title, content = content, createdAt = 1, updatedAt = 1, folderId = null,
        favorite = false, archived = false, inbox = true, captureSource = CaptureSource.APP
    )

    private suspend fun links(sourceId: Long) = db.noteLinkDao().getForSource(sourceId)

    @Test
    fun savingStoresOrderedLinksResolvedCaseAndAccentInsensitively() = runBlocking {
        val reuniao = repository.saveNote(note("Reunião"))
        val src = repository.saveNote(note("Diário", "[[REUNIÃO]] depois [[Futura]]"))

        assertEquals(listOf(NoteLinkEntity(src, 0, reuniao, "REUNIÃO"), NoteLinkEntity(src, 1, null, "Futura")), links(src))

        repository.saveNote(note("Diário", "só [[ reunião ]]", id = src))
        assertEquals(listOf(NoteLinkEntity(src, 0, reuniao, "reunião")), links(src))
    }

    @Test
    fun creatingNoteResolvesPendingGhosts() = runBlocking {
        val src = repository.saveNote(note("Diário", "[[Futura Nota]]"))
        assertEquals(null, links(src).single().targetId)

        val futura = repository.saveNote(note("  futura NOTA "))

        assertEquals(futura, links(src).single().targetId)
        assertEquals(listOf(NoteRef(src, "Diário")), repository.observeBacklinks(futura).first())
    }

    @Test
    fun renamePropagatesToLinkingNotesWithSpecialCharacters() = runBlocking {
        val alvo = repository.saveNote(note("C++ & 100% (rascunho)"))
        val a = repository.saveNote(note("A", "ver [[c++ & 100% (rascunho)]] e [[Outra]] e C++ & 100% (rascunho)"))

        repository.saveNote(note("Nova (v2) \$1", id = alvo))

        assertEquals("ver [[Nova (v2) \$1]] e [[Outra]] e C++ & 100% (rascunho)", repository.getNote(a)!!.content)
        assertEquals(1L, repository.getNote(a)!!.updatedAt) // a rename is not an edit of the linking note
        assertEquals(listOf(NoteLinkEntity(a, 0, alvo, "Nova (v2) \$1"), NoteLinkEntity(a, 1, null, "Outra")), links(a))
        assertEquals(listOf(NoteRef(a, "A")), repository.observeBacklinks(alvo).first())
    }

    @Test
    fun blankTitlesNeverResolveAndBlankRenameKeepsText() = runBlocking {
        repository.saveNote(note("", "captura rápida sem título"))
        val src = repository.saveNote(note("A", "[[ ]] [[Alvo]]"))
        assertEquals(listOf(NoteLinkEntity(src, 0, null, "Alvo")), links(src))

        val alvo = repository.saveNote(note("Alvo"))
        assertEquals(alvo, links(src).single().targetId)

        repository.saveNote(note("   ", id = alvo))

        assertEquals("[[ ]] [[Alvo]]", repository.getNote(src)!!.content)
        assertEquals(listOf(NoteLinkEntity(src, 0, null, "Alvo")), links(src))
    }

    @Test
    fun deletingTargetTurnsLinksIntoGhostsAndSourceDeleteRemovesLinks() = runBlocking {
        val alvo = repository.saveNote(note("Alvo"))
        val src = repository.saveNote(note("Fonte", "[[Alvo]]"))

        repository.deleteNote(alvo)
        assertEquals(listOf(NoteLinkEntity(src, 0, null, "Alvo")), links(src))

        repository.deleteNote(src)
        assertTrue(links(src).isEmpty())
    }

    @Test
    fun savingDeletedNoteReinsertsItWithItsLinks() = runBlocking {
        val alvo = repository.saveNote(note("Alvo"))
        val fonte = repository.saveNote(note("Fonte", "[[Alvo]]"))
        repository.deleteNote(fonte)

        assertEquals(fonte, repository.saveNote(note("Fonte", "[[Alvo]]", id = fonte)))

        assertEquals("Fonte", repository.getNote(fonte)!!.title)
        assertEquals(listOf(NoteLinkEntity(fonte, 0, alvo, "Alvo")), links(fonte))
    }

    @Test
    fun renamingToTitleOwnedByAnotherNoteTurnsLinksIntoGhosts() = runBlocking {
        val dono = repository.saveNote(note("Plano"))
        val alvo = repository.saveNote(note("Rascunho"))
        val src = repository.saveNote(note("Fonte", "[[Rascunho]]"))

        repository.saveNote(note("plano", id = alvo))

        assertEquals("[[Rascunho]]", repository.getNote(src)!!.content)
        assertEquals(listOf(NoteLinkEntity(src, 0, null, "Rascunho")), links(src))
        assertEquals(dono, repository.saveNote(note("Plano", id = dono)))
    }
}
