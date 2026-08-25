package com.quicknotes.app

import android.app.Application
import androidx.datastore.preferences.preferencesDataStore
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.repository.FolderRepositoryImpl
import com.quicknotes.app.data.repository.NoteRepositoryImpl
import com.quicknotes.app.data.repository.TagRepositoryImpl
import com.quicknotes.app.data.settings.SettingsRepository
import com.quicknotes.app.domain.repository.FolderRepository
import com.quicknotes.app.domain.repository.NoteRepository
import com.quicknotes.app.domain.repository.TagRepository
import com.quicknotes.app.domain.usecase.CreateNoteUseCase
import com.quicknotes.app.domain.usecase.SearchNotesUseCase

private val android.content.Context.dataStore by preferencesDataStore(name = "quicknotes_settings")

class AppContainer(
    val database: AppDatabase,
    val noteRepository: NoteRepository,
    val tagRepository: TagRepository,
    val folderRepository: FolderRepository,
    val createNoteUseCase: CreateNoteUseCase,
    val searchNotesUseCase: SearchNotesUseCase,
    val settingsRepository: SettingsRepository
)

class QuickNotesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.build(this)
        val noteRepository = NoteRepositoryImpl(database)
        container = AppContainer(
            database = database,
            noteRepository = noteRepository,
            tagRepository = TagRepositoryImpl(database.tagDao()),
            folderRepository = FolderRepositoryImpl(database.folderDao()),
            createNoteUseCase = CreateNoteUseCase(noteRepository),
            searchNotesUseCase = SearchNotesUseCase(noteRepository),
            settingsRepository = SettingsRepository(dataStore)
        )
    }
}
