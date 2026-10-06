package com.quicknotes.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.usecase.SearchNotesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(private val searchNotesUseCase: SearchNotesUseCase) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<Note>>(emptyList())
    val results: StateFlow<List<Note>> = _results.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        viewModelScope.launch { _results.value = searchNotesUseCase(newQuery) }
    }
}
