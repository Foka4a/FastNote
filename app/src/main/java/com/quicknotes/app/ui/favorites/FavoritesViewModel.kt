package com.quicknotes.app.ui.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritesViewModel(private val noteRepository: NoteRepository) : ViewModel() {
    val notes: StateFlow<List<Note>> = noteRepository.observeFavorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun unfavorite(id: Long) { viewModelScope.launch { noteRepository.setFavorite(id, false) } }
}
