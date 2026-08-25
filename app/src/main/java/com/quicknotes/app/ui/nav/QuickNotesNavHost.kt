package com.quicknotes.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quicknotes.app.AppContainer
import com.quicknotes.app.ui.ViewModelFactory
import com.quicknotes.app.ui.archive.ArchiveScreen
import com.quicknotes.app.ui.archive.ArchiveViewModel
import com.quicknotes.app.ui.editor.EditorScreen
import com.quicknotes.app.ui.editor.EditorViewModel
import com.quicknotes.app.ui.editorViewModelFactory
import com.quicknotes.app.ui.favorites.FavoritesScreen
import com.quicknotes.app.ui.favorites.FavoritesViewModel
import com.quicknotes.app.ui.folders.FoldersScreen
import com.quicknotes.app.ui.folders.FoldersViewModel
import com.quicknotes.app.ui.inbox.InboxScreen
import com.quicknotes.app.ui.inbox.InboxViewModel
import com.quicknotes.app.ui.search.SearchScreen
import com.quicknotes.app.ui.search.SearchViewModel
import com.quicknotes.app.ui.settings.SettingsScreen
import com.quicknotes.app.ui.settings.SettingsViewModel
import com.quicknotes.app.ui.tags.TagsScreen
import com.quicknotes.app.ui.tags.TagsViewModel

@Composable
fun QuickNotesNavHost(container: AppContainer, startRoute: String = "inbox") {
    val navController = rememberNavController()
    val factory = ViewModelFactory(container)

    NavHost(navController = navController, startDestination = startRoute) {
        composable("inbox") {
            val vm: InboxViewModel = viewModel(factory = factory)
            InboxScreen(vm, onNoteClick = { id -> navController.navigate("editor?noteId=$id") })
        }
        composable(
            "editor?noteId={noteId}",
            arguments = listOf(navArgument("noteId") { type = NavType.LongType; defaultValue = 0L })
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it != 0L }
            val vm: EditorViewModel = viewModel(factory = editorViewModelFactory(container, noteId))
            EditorScreen(vm, onSaved = { navController.popBackStack() })
        }
        composable("search") {
            val vm: SearchViewModel = viewModel(factory = factory)
            SearchScreen(vm)
        }
        composable("tags") {
            val vm: TagsViewModel = viewModel(factory = factory)
            TagsScreen(vm)
        }
        composable("folders") {
            val vm: FoldersViewModel = viewModel(factory = factory)
            FoldersScreen(vm)
        }
        composable("favorites") {
            val vm: FavoritesViewModel = viewModel(factory = factory)
            FavoritesScreen(vm)
        }
        composable("archive") {
            val vm: ArchiveViewModel = viewModel(factory = factory)
            ArchiveScreen(vm)
        }
        composable("settings") {
            val vm: SettingsViewModel = viewModel(factory = factory)
            SettingsScreen(vm)
        }
    }
}
