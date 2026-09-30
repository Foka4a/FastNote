package com.quicknotes.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.repository.NoteRepository
import com.quicknotes.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class InboxNoteUi(val note: Note, val tagNames: List<String>)

/** A one-shot event for the Inbox's snackbar: a message, and an optional action to undo it. */
data class InboxEvent(val message: String, val undo: (() -> Unit)? = null)

class InboxViewModel(
    private val noteRepository: NoteRepository,
    tagRepository: TagRepository
) : ViewModel() {
    val notes: StateFlow<List<InboxNoteUi>> = noteRepository.observeInbox()
        .combine(tagRepository.observeTags()) { notes, tags ->
            val names = tags.associate { it.id to it.name }
            notes.map { note -> InboxNoteUi(note, note.tagIds.mapNotNull { names[it] }) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selected = MutableStateFlow<Set<Long>>(emptySet())
    val selected: StateFlow<Set<Long>> = _selected.asStateFlow()

    private val _events = MutableSharedFlow<InboxEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    fun toggleSelect(id: Long) {
        _selected.value = if (id in _selected.value) _selected.value - id else _selected.value + id
    }

    fun selectAll() {
        _selected.value = if (_selected.value.isNotEmpty()) emptySet() else notes.value.map { it.note.id }.toSet()
    }

    fun clearSelection() { _selected.value = emptySet() }

    fun setFavorite(id: Long, favorite: Boolean) {
        viewModelScope.launch {
            noteRepository.setFavorite(id, favorite)
            emit(if (favorite) "Favoritado" else "Removido dos favoritos") { setFavorite(id, !favorite) }
        }
    }

    fun setArchived(id: Long, archived: Boolean) {
        viewModelScope.launch {
            noteRepository.setArchived(id, archived)
            emit(if (archived) "Arquivado" else "Desarquivado") { setArchived(id, !archived) }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val note = noteRepository.getNote(id) ?: return@launch
            noteRepository.deleteNote(id)
            emit("Nota excluída") { viewModelScope.launch { noteRepository.saveNote(note) } }
        }
    }

    fun batchFavorite() = batch("favoritadas") { id -> noteRepository.setFavorite(id, true) }
    fun batchArchive() = batch("arquivadas") { id -> noteRepository.setArchived(id, true) }
    fun batchDelete() = batch("excluídas") { id -> noteRepository.deleteNote(id) }

    private inline fun batch(verb: String, crossinline action: suspend (Long) -> Unit) {
        val ids = _selected.value
        _selected.value = emptySet()
        viewModelScope.launch {
            ids.forEach { action(it) }
            _events.emit(InboxEvent("${ids.size} $verb"))
        }
    }

    private suspend fun emit(message: String, undo: (() -> Unit)? = null) {
        _events.emit(InboxEvent(message, undo))
    }
}
