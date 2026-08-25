package com.quicknotes.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quicknotes.app.AppContainer
import com.quicknotes.app.ui.editor.EditorViewModel
import com.quicknotes.app.ui.inbox.InboxViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        InboxViewModel::class.java -> InboxViewModel(container.noteRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}

fun editorViewModelFactory(container: AppContainer, noteId: Long?): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditorViewModel(container.noteRepository, noteId) as T
    }
