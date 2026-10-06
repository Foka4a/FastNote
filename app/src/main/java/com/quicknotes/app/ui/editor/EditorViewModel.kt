package com.quicknotes.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Folder
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.model.NoteLink
import com.quicknotes.app.domain.model.NoteRef
import com.quicknotes.app.domain.model.Tag
import com.quicknotes.app.domain.repository.FolderRepository
import com.quicknotes.app.domain.repository.NoteRepository
import com.quicknotes.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
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
    val createdAt: Long = System.currentTimeMillis(),
    val captureSource: CaptureSource = CaptureSource.APP
)

class EditorViewModel(
    private val noteRepository: NoteRepository,
    tagRepository: TagRepository,
    folderRepository: FolderRepository,
    private val noteId: Long?,
    prefillContent: String? = null,
    prefillTitle: String? = null
) : ViewModel() {
    // Prefills only apply to a brand new note (voice transcription, or a ghost link being created).
    private val _uiState = MutableStateFlow(
        if (noteId == null) EditorUiState(title = prefillTitle.orEmpty(), content = prefillContent.orEmpty())
        else EditorUiState()
    )
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    val allTags: StateFlow<List<Tag>> = tagRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allFolders: StateFlow<List<Folder>> = folderRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val outgoingLinks: StateFlow<List<NoteLink>> =
        (if (noteId == null) flowOf(emptyList()) else noteRepository.observeOutgoingLinks(noteId))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val backlinks: StateFlow<List<NoteRef>> =
        (if (noteId == null) flowOf(emptyList()) else noteRepository.observeBacklinks(noteId))
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        if (noteId != null) {
            viewModelScope.launch {
                noteRepository.getNote(noteId)?.let { note ->
                    _uiState.value = EditorUiState(
                        id = note.id, title = note.title, content = note.content,
                        folderId = note.folderId, tagIds = note.tagIds,
                        favorite = note.favorite, archived = note.archived, inbox = note.inbox,
                        createdAt = note.createdAt, captureSource = note.captureSource
                    )
                }
            }
        }
    }

    fun updateTitle(title: String) { _uiState.value = _uiState.value.copy(title = title) }
    fun updateContent(content: String) { _uiState.value = _uiState.value.copy(content = content) }
    fun pickFolder(folderId: Long?) { _uiState.value = _uiState.value.copy(folderId = folderId) }
    fun addTag(tagId: Long) { _uiState.value = _uiState.value.copy(tagIds = _uiState.value.tagIds + tagId) }
    fun removeTag(tagId: Long) { _uiState.value = _uiState.value.copy(tagIds = _uiState.value.tagIds - tagId) }
    fun toggleFavorite() { _uiState.value = _uiState.value.copy(favorite = !_uiState.value.favorite) }
    fun toggleArchived() { _uiState.value = _uiState.value.copy(archived = !_uiState.value.archived, inbox = _uiState.value.archived) }
    fun toggleInbox() { _uiState.value = _uiState.value.copy(inbox = !_uiState.value.inbox) }

    fun delete(onDeleted: () -> Unit) {
        val id = _uiState.value.id
        if (id == 0L) { onDeleted(); return }
        viewModelScope.launch { noteRepository.deleteNote(id); onDeleted() }
    }

    fun save(onSaved: () -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val now = System.currentTimeMillis()
            noteRepository.saveNote(
                Note(
                    id = state.id, title = state.title, content = state.content,
                    createdAt = state.createdAt, updatedAt = now, folderId = state.folderId,
                    favorite = state.favorite, archived = state.archived, inbox = state.inbox,
                    captureSource = if (state.id == 0L) CaptureSource.APP else state.captureSource,
                    tagIds = state.tagIds
                )
            )
            onSaved()
        }
    }
}
