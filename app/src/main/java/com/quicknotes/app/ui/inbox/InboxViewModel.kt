package com.quicknotes.app.ui.inbox

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InboxViewModel(private val noteRepository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<Note>> = noteRepository.observeInbox()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun delete(id: Long) {
        viewModelScope.launch { noteRepository.deleteNote(id) }
    }
}
