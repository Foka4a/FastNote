package com.quicknotes.app.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.Tag
import com.quicknotes.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TagsViewModel(private val tagRepository: TagRepository) : ViewModel() {
    val tags: StateFlow<List<Tag>> = tagRepository.observeTags()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String) { viewModelScope.launch { tagRepository.createTag(name) } }
    fun delete(id: Long) { viewModelScope.launch { tagRepository.deleteTag(id) } }
}
