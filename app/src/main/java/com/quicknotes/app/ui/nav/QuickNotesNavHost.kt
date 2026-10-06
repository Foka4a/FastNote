package com.quicknotes.app.ui.nav

import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quicknotes.app.AppContainer
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.VoiceCaptureBehavior
import com.quicknotes.app.ui.ViewModelFactory
import com.quicknotes.app.ui.archive.ArchiveScreen
import com.quicknotes.app.ui.archive.ArchiveViewModel
import com.quicknotes.app.ui.components.HeaderIconButton
import com.quicknotes.app.ui.components.LocalSnackbarHostState
import com.quicknotes.app.ui.editor.EditorScreen
import com.quicknotes.app.ui.editor.EditorViewModel
import com.quicknotes.app.ui.editorViewModelFactory
import com.quicknotes.app.ui.folderNotesViewModelFactory
import com.quicknotes.app.ui.favorites.FavoritesScreen
import com.quicknotes.app.ui.favorites.FavoritesViewModel
import com.quicknotes.app.ui.folders.FolderNotesScreen
import com.quicknotes.app.ui.folders.FoldersScreen
import com.quicknotes.app.ui.folders.FolderNotesViewModel
import com.quicknotes.app.ui.folders.FoldersViewModel
import com.quicknotes.app.ui.inbox.InboxScreen
import com.quicknotes.app.ui.inbox.InboxViewModel
import com.quicknotes.app.ui.search.SearchScreen
import com.quicknotes.app.ui.search.SearchViewModel
import com.quicknotes.app.ui.settings.SettingsScreen
import com.quicknotes.app.ui.settings.SettingsViewModel
import com.quicknotes.app.ui.tags.TagsScreen
import com.quicknotes.app.ui.tags.TagsViewModel
import com.quicknotes.app.ui.theme.Nocturne
import kotlinx.coroutines.launch

private const val EDITOR_ROUTE = "editor?noteId={noteId}&prefillContent={prefillContent}&prefillTitle={prefillTitle}"
private const val FOLDER_ROUTE = "folder/{folderId}"

private data class TabDef(val route: String, val label: String, val icon: ImageVector)
private val TABS = listOf(
    TabDef("inbox", "Inbox", Icons.Filled.Inbox),
    TabDef("search", "Busca", Icons.Filled.Search),
    TabDef("tags", "Tags", Icons.Filled.Tag),
    TabDef("folders", "Pastas", Icons.Filled.Folder)
)

private data class ScreenHeader(val title: String, val subtitle: String)
private val HEADERS = mapOf(
    "inbox" to ScreenHeader("Inbox", "capturar primeiro, organizar depois"),
    "search" to ScreenHeader("Busca", "título, conteúdo, tags e pastas"),
    "tags" to ScreenHeader("Tags", "uma nota pode ter várias"),
    "folders" to ScreenHeader("Pastas", "hierarquia livre"),
    "favorites" to ScreenHeader("Favoritos", "acesso rápido às importantes"),
    "archive" to ScreenHeader("Arquivados", "fora da visão principal"),
    "settings" to ScreenHeader("Ajustes", "captura por voz, widget e permissões"),
    "onboarding" to ScreenHeader("Sobreposição", "permissão necessária uma única vez")
)

private data class MenuEntry(val route: String, val label: String, val icon: ImageVector)
private val MENU_ENTRIES = listOf(
    MenuEntry("favorites", "Favoritos", Icons.Filled.Star),
    MenuEntry("archive", "Arquivados", Icons.Filled.Archive),
    MenuEntry("settings", "Ajustes", Icons.Filled.Settings)
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
    val isEditor = currentRoute?.startsWith("editor") == true
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Folder route header shows the folder's name; remember so the flow isn't recreated per recomposition.
    val folders by remember { container.folderRepository.observeFolders() }.collectAsState(initial = emptyList())

    var menuOpen by remember { mutableStateOf(false) }
    var voiceSheetOpen by remember { mutableStateOf(false) }
    val voiceBehavior by container.settingsRepository.voiceCaptureBehavior
        .collectAsState(initial = VoiceCaptureBehavior.REVIEW_BEFORE_SAVE)

    CompositionLocalProvider(LocalSnackbarHostState provides snackbarHostState) {
        Scaffold(
            containerColor = Nocturne.Background,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!isEditor) {
                    val header = if (currentRoute == FOLDER_ROUTE) {
                        val folderId = currentEntry?.arguments?.getLong("folderId")
                        ScreenHeader(folders.find { it.id == folderId }?.name ?: "Pasta", "notas da pasta")
                    } else HEADERS[currentRoute] ?: ScreenHeader("Quick Notes", "")
                    Row(
                        Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(header.title, color = Nocturne.TextPrimary, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                            Text(header.subtitle, color = Nocturne.TextMuted, fontSize = 11.5.sp, modifier = Modifier.padding(top = 2.dp))
                        }
                        HeaderIconButton(Icons.Filled.Search, "Buscar", onClick = { navController.navigate("search") { launchSingleTop = true } })
                        HeaderIconButton(
                            Icons.Filled.MoreVert, "Menu",
                            modifier = Modifier.testTag("nav_menu_button"),
                            onClick = { menuOpen = true }
                        )
                    }
                }
            },
            bottomBar = {
                if (!isEditor) {
                    BottomBar(
                        currentRoute = currentRoute,
                        onTabClick = { route -> navController.navigate(route) { launchSingleTop = true } },
                        onNewNote = { navController.navigate("editor?noteId=0") },
                        onHoldVoice = { voiceSheetOpen = true }
                    )
                }
            }
        ) { padding ->
            Box(Modifier.padding(padding)) {
                NavHost(
                    navController = navController,
                    startDestination = "inbox",
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable("inbox") {
                        val vm: InboxViewModel = viewModel(factory = factory)
                        InboxScreen(vm, onNoteClick = { id -> navController.navigate("editor?noteId=$id") })
                    }
                    composable(
                        EDITOR_ROUTE,
                        arguments = listOf(
                            navArgument("noteId") { type = NavType.LongType; defaultValue = 0L },
                            navArgument("prefillContent") { type = NavType.StringType; nullable = true; defaultValue = null },
                            navArgument("prefillTitle") { type = NavType.StringType; nullable = true; defaultValue = null }
                        )
                    ) { backStackEntry ->
                        val noteId = backStackEntry.arguments?.getLong("noteId")?.takeIf { it != 0L }
                        val prefill = backStackEntry.arguments?.getString("prefillContent")
                        val prefillTitle = backStackEntry.arguments?.getString("prefillTitle")
                        val vm: EditorViewModel = viewModel(factory = editorViewModelFactory(container, noteId, prefill, prefillTitle))
                        // Inbox is always underneath (see the deep-link effect above), so this
                        // never leaves the user staring at an empty NavHost.
                        // Opening a linked note pushes a new editor, so back returns to the note being read.
                        EditorScreen(
                            vm,
                            onSaved = { navController.popBackStack() },
                            onOpenNote = { id -> navController.navigate("editor?noteId=$id") },
                            onCreateNote = { title -> navController.navigate("editor?noteId=0&prefillTitle=${Uri.encode(title)}") }
                        )
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
                        FoldersScreen(vm, onFolderClick = { id -> navController.navigate("folder/$id") })
                    }
                    composable(FOLDER_ROUTE, arguments = listOf(navArgument("folderId") { type = NavType.LongType })) { backStackEntry ->
                        val folderId = backStackEntry.arguments!!.getLong("folderId")
                        val vm: FolderNotesViewModel = viewModel(factory = folderNotesViewModelFactory(container, folderId))
                        FolderNotesScreen(vm, onNoteClick = { id -> navController.navigate("editor?noteId=$id") })
                    }
                    composable("favorites") {
                        val vm: FavoritesViewModel = viewModel(factory = factory)
                        FavoritesScreen(vm, onNoteClick = { id -> navController.navigate("editor?noteId=$id") })
                    }
                    composable("archive") {
                        val vm: ArchiveViewModel = viewModel(factory = factory)
                        ArchiveScreen(vm, onNoteClick = { id -> navController.navigate("editor?noteId=$id") })
                    }
                    composable("settings") {
                        val vm: SettingsViewModel = viewModel(factory = factory)
                        SettingsScreen(vm, onOpenOnboarding = { navController.navigate("onboarding") { launchSingleTop = true } })
                    }
                    composable("onboarding") {
                        val context = LocalContext.current
                        com.quicknotes.app.ui.onboarding.OnboardingScreen(
                            onRequestPermission = {
                                context.startActivity(com.quicknotes.app.overlay.OverlayPermission.requestIntent(context))
                            },
                            onSkip = { navController.popBackStack() }
                        )
                    }
                }

                if (menuOpen) {
                    ModalBottomSheet(onDismissRequest = { menuOpen = false }, containerColor = Nocturne.Surface) {
                        Column(Modifier.padding(bottom = 18.dp)) {
                            MENU_ENTRIES.forEach { entry ->
                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable {
                                            menuOpen = false
                                            navController.navigate(entry.route) { launchSingleTop = true }
                                        }
                                        .padding(horizontal = 8.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                                ) {
                                    Icon(entry.icon, contentDescription = null, tint = Nocturne.AccentText)
                                    Text(entry.label, color = Nocturne.TextPrimary, fontSize = 14.5.sp, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }

                if (voiceSheetOpen) {
                    VoiceCaptureSheet(
                        behavior = voiceBehavior,
                        onSave = { text ->
                            voiceSheetOpen = false
                            scope.launch {
                                container.createNoteUseCase(title = text.take(60), content = text, captureSource = CaptureSource.APP)
                                snackbarHostState.showSnackbar("Nota salva na Inbox")
                            }
                        },
                        onContinueEditing = { text ->
                            voiceSheetOpen = false
                            navController.navigate("editor?noteId=0&prefillContent=${Uri.encode(text)}")
                        },
                        onDismiss = { voiceSheetOpen = false }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BottomBar(
    currentRoute: String?,
    onTabClick: (String) -> Unit,
    onNewNote: () -> Unit,
    onHoldVoice: () -> Unit
) {
    Box(Modifier.fillMaxWidth().height(74.dp).background(Nocturne.BottomBar)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TABS.forEach { tab ->
                val active = currentRoute == tab.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .testTag(if (tab.route == "inbox") "tab_inbox" else "tab_${tab.route}")
                        .clickable { onTabClick(tab.route) }
                        .padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(tab.icon, contentDescription = tab.label, tint = if (active) Nocturne.AccentText else Nocturne.TextMuted, modifier = Modifier.size(21.dp))
                    Text(tab.label, color = if (active) Nocturne.AccentText else Nocturne.TextMuted, fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.width(76.dp))
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 22.dp)
                .size(60.dp)
                .background(Nocturne.Accent, RoundedCornerShape(20.dp))
                .testTag("new_note_fab")
                .combinedClickable(onClick = onNewNote, onLongClick = onHoldVoice),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Nova nota", tint = Nocturne.OnAccent, modifier = Modifier.size(26.dp))
        }
    }
}
