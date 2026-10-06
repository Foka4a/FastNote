package com.quicknotes.app.ui.graph

import com.quicknotes.app.domain.FakeNoteRepository
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.model.NoteGraph
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.NoteTagRef
import com.quicknotes.app.domain.model.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GraphModelTest {
    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private val sample = NoteGraph(
        notes = listOf(NoteRef(1, "A"), NoteRef(2, "B"), NoteRef(3, "")),
        links = listOf(
            NoteLink(1, 0, 2, "B"), NoteLink(1, 1, null, "Futura"), NoteLink(2, 0, null, "FUTURA"),
            NoteLink(1, 2, 1, "A"),   // self link
            NoteLink(2, 1, 99, "Sumiu") // target no longer exists
        ),
        tags = listOf(Tag(10, "ideia"), Tag(11, "aula")),
        noteTags = listOf(NoteTagRef(1, 10), NoteTagRef(1, 11), NoteTagRef(3, 11), NoteTagRef(2, 77)) // tag 77 deleted
    )

    @Test
    fun buildsNoteTagAndSharedGhostNodes() {
        val data = buildGraph(sample)

        assertEquals(
            listOf("n:1" to NodeKind.NOTE, "n:2" to NodeKind.NOTE, "n:3" to NodeKind.NOTE,
                "t:10" to NodeKind.TAG, "t:11" to NodeKind.TAG, "g:futura" to NodeKind.GHOST),
            data.nodes.map { it.key to it.kind }
        )
        assertEquals("(sem título)", data.nodes.first { it.key == "n:3" }.label)
        assertEquals("Futura", data.nodes.first { it.key == "g:futura" }.label)
        assertEquals(
            setOf(GraphEdge("n:1", "n:2", false), GraphEdge("n:1", "g:futura", false), GraphEdge("n:2", "g:futura", false),
                GraphEdge("n:1", "t:10", true), GraphEdge("n:1", "t:11", true), GraphEdge("n:3", "t:11", true)),
            data.edges.toSet()
        )
    }

    @Test
    fun selfLinksAreDroppedAndDeletedFilterTagShowsFullGraph() {
        val full = buildGraph(sample)
        assertEquals(false, full.edges.any { it.from == it.to })
        assertEquals(full, buildGraph(sample, filterTagId = 77))
    }

    @Test
    fun tagFilterKeepsTagItsNotesAndTheirOtherTags() {
        val data = buildGraph(sample, filterTagId = 10)

        assertEquals(setOf("t:10", "n:1", "t:11"), data.nodes.map { it.key }.toSet())
        assertEquals(setOf(GraphEdge("n:1", "t:10", true), GraphEdge("n:1", "t:11", true)), data.edges.toSet())
    }

    @Test
    fun viewModelFollowsRepositoryAndTogglesFilter() = runTest {
        val repository = FakeNoteRepository()
        val viewModel = GraphViewModel(repository)
        fun note(title: String, content: String = "", tagIds: List<Long> = emptyList()) = Note(
            title = title, content = content, createdAt = 1, updatedAt = 1, folderId = null, favorite = false,
            archived = false, inbox = true, captureSource = CaptureSource.APP, tagIds = tagIds
        )

        repository.tags.value = listOf(Tag(10, "ideia"))
        repository.saveNote(note("A", "[[B]]", tagIds = listOf(10)))
        assertEquals(setOf("n:1", "t:10", "g:b"), viewModel.graph.first { it.nodes.size == 3 }.nodes.map { it.key }.toSet())

        repository.saveNote(note("B")) // ghost resolves into a real note
        assertEquals(setOf("n:1", "n:2", "t:10"), viewModel.graph.first { "n:2" in it.nodes.map { n -> n.key } }.nodes.map { it.key }.toSet())

        viewModel.toggleTagFilter(10)
        assertEquals(setOf("n:1", "t:10"), viewModel.graph.first { it.nodes.size == 2 }.nodes.map { it.key }.toSet())
        viewModel.toggleTagFilter(10)
        assertEquals(null, viewModel.filterTagId.value)
    }
}
