package com.quicknotes.app.ui.folders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quicknotes.app.domain.model.Folder
import com.quicknotes.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoldersViewModel(private val folderRepository: FolderRepository) : ViewModel() {
    val folders: StateFlow<List<Folder>> = folderRepository.observeFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun create(name: String, parentId: Long?) { viewModelScope.launch { folderRepository.createFolder(name, parentId) } }
    fun delete(id: Long) { viewModelScope.launch { folderRepository.deleteFolder(id) } }
}
