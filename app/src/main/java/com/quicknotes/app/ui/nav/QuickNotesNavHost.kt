package com.quicknotes.app.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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

private const val EDITOR_ROUTE = "editor?noteId={noteId}&prefillContent={prefillContent}"

private val MENU_DESTINATIONS = listOf(
    "search" to "Buscar",
    "tags" to "Tags",
    "folders" to "Pastas",
    "favorites" to "Favoritos",
    "archive" to "Arquivo",
    "settings" to "Configurações"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickNotesNavHost(
    container: AppContainer,
    startRoute: String = "inbox",
    pendingRoute: String? = null,
    onRouteHandled: () -> Unit = {}
) {
    val navController = rememberNavController()
    val factory = ViewModelFactory(container)
    val currentEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentEntry?.destination?.route

    // Inbox is always the root of the back stack; a widget/overlay deep link opens on top of
    // it, so back (and save) from a deep-linked Editor always land somewhere sensible instead
    // of on an empty screen.
    // rememberSaveable so a rotation doesn't push the deep link a second time.
    var deepLinkHandled by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!deepLinkHandled) {
            deepLinkHandled = true
            if (startRoute != "inbox") navController.navigate(startRoute)
        }
    }

    // A widget/overlay tap while the app is already running arrives via onNewIntent,
    // which sets pendingRoute; navigate and let MainActivity clear it.
    LaunchedEffect(pendingRoute) {
        pendingRoute?.let {
            // singleTop keeps repeated widget taps from stacking duplicates, but for the editor
            // route it also reuses the current EditorViewModel (wrong note's content/id) instead
            // of loading the newly-deep-linked note or a fresh transcription — so never singleTop
            // the editor route; menu destinations have no arguments, so singleTop is safe there.
            navController.navigate(it) { launchSingleTop = !it.startsWith("editor") }
            onRouteHandled()
        }
    }

    Scaffold(
        topBar = {
            var menuOpen by remember { mutableStateOf(false) }
            TopAppBar(
                title = { Text("Quick Notes") },
                actions = {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.testTag("nav_menu_button")
                    ) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        MENU_DESTINATIONS.forEach { (route, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    menuOpen = false
                                    navController.navigate(route) { launchSingleTop = true }
                                }
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (currentRoute == "inbox") {
                FloatingActionButton(
                    onClick = { navController.navigate("editor?noteId=0") },
                    modifier = Modifier.testTag("new_note_fab")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Nova nota")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "inbox",
            modifier = Modifier.padding(padding)
        ) {
            composable("inbox") {
                val vm: InboxViewModel = viewModel(factory = factory)
                InboxScreen(vm, onNoteClick = { id -> navController.navigate("editor?noteId=$id") })
            }
            composable(
                EDITOR_ROUTE,
                arguments = listOf(
                    navArgument("noteId") { type = NavType.LongType; defaultValue = 0L },
                    navArgument("prefillContent") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it != 0L }
                val prefill = backStackEntry.arguments?.getString("prefillContent")
                val vm: EditorViewModel = viewModel(factory = editorViewModelFactory(container, noteId, prefill))
                // Inbox is always underneath (see the deep-link effect above), so this
                // never leaves the user staring at an empty NavHost.
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
}
