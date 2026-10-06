package com.quicknotes.app.ui.graph

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.repository.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Activity-scoped (see QuickNotesNavHost) so [layout] keeps node positions between tab switches. */
class GraphViewModel(noteRepository: NoteRepository) : ViewModel() {
    private val _filterTagId = MutableStateFlow<Long?>(null)
    val filterTagId: StateFlow<Long?> = _filterTagId.asStateFlow()

    val graph: StateFlow<GraphData> = combine(noteRepository.observeGraph(), _filterTagId) { graph, tagId -> buildGraph(graph, tagId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GraphData())

    val layout = ForceLayout()

    fun toggleTagFilter(tagId: Long) {
        _filterTagId.value = if (_filterTagId.value == tagId) null else tagId
    }
}
