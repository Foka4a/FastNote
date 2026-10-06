package com.quicknotes.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.quicknotes.app.AppContainer
import com.quicknotes.app.ui.archive.ArchiveViewModel
import com.quicknotes.app.ui.editor.EditorViewModel
import com.quicknotes.app.ui.favorites.FavoritesViewModel
import com.quicknotes.app.ui.folders.FolderNotesViewModel
import com.quicknotes.app.ui.folders.FoldersViewModel
import com.quicknotes.app.ui.inbox.InboxViewModel
import com.quicknotes.app.ui.search.SearchViewModel
import com.quicknotes.app.ui.settings.SettingsViewModel
import com.quicknotes.app.ui.tags.TagsViewModel

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        InboxViewModel::class.java -> InboxViewModel(container.noteRepository, container.tagRepository) as T
        SearchViewModel::class.java -> SearchViewModel(container.searchNotesUseCase) as T
        TagsViewModel::class.java -> TagsViewModel(container.tagRepository) as T
        FoldersViewModel::class.java -> FoldersViewModel(container.folderRepository) as T
        FavoritesViewModel::class.java -> FavoritesViewModel(container.noteRepository) as T
        ArchiveViewModel::class.java -> ArchiveViewModel(container.noteRepository) as T
        SettingsViewModel::class.java -> SettingsViewModel(container.settingsRepository) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}

fun editorViewModelFactory(
    container: AppContainer,
    noteId: Long?,
    prefillContent: String? = null,
    prefillTitle: String? = null
): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            EditorViewModel(
                container.noteRepository, container.tagRepository, container.folderRepository,
                noteId, prefillContent, prefillTitle
            ) as T
    }

fun folderNotesViewModelFactory(container: AppContainer, folderId: Long): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FolderNotesViewModel(container.noteRepository, folderId) as T
    }
