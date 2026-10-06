package com.quicknotes.app.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class FolderNotesViewModel(noteRepository: NoteRepository, folderId: Long) : ViewModel() {
    val notes: StateFlow<List<Note>> = noteRepository.observeByFolder(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
