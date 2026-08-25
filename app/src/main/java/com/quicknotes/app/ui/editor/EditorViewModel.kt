package com.quicknotes.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class EditorUiState(
    val id: Long = 0,
    val title: String = "",
    val content: String = "",
    val folderId: Long? = null,
    val tagIds: List<Long> = emptyList(),
    val favorite: Boolean = false,
    val archived: Boolean = false,
    val inbox: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

class EditorViewModel(
    private val noteRepository: NoteRepository,
    private val noteId: Long?
) : ViewModel() {
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    init {
        if (noteId != null) {
            viewModelScope.launch {
                noteRepository.getNote(noteId)?.let { note ->
                    _uiState.value = EditorUiState(
                        id = note.id, title = note.title, content = note.content,
                        folderId = note.folderId, tagIds = note.tagIds,
                        favorite = note.favorite, archived = note.archived, inbox = note.inbox,
                        createdAt = note.createdAt
                    )
                }
            }
        }
    }

    fun updateTitle(title: String) { _uiState.value = _uiState.value.copy(title = title) }
    fun updateContent(content: String) { _uiState.value = _uiState.value.copy(content = content) }
    fun updateFolder(folderId: Long?) { _uiState.value = _uiState.value.copy(folderId = folderId) }
    fun updateTags(tagIds: List<Long>) { _uiState.value = _uiState.value.copy(tagIds = tagIds) }
    fun toggleFavorite() { _uiState.value = _uiState.value.copy(favorite = !_uiState.value.favorite) }
    fun toggleArchived() { _uiState.value = _uiState.value.copy(archived = !_uiState.value.archived) }
    fun toggleInbox() { _uiState.value = _uiState.value.copy(inbox = !_uiState.value.inbox) }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val now = System.currentTimeMillis()
            noteRepository.saveNote(
                Note(
                    id = state.id, title = state.title, content = state.content,
                    createdAt = state.createdAt, updatedAt = now, folderId = state.folderId,
                    favorite = state.favorite, archived = state.archived, inbox = state.inbox,
                    captureSource = CaptureSource.APP, tagIds = state.tagIds
                )
            )
            onSaved()
        }
    }
}
