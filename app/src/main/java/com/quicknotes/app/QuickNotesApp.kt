package com.quicknotes.app

import android.app.Application
import com.quicknotes.app.data.local.database.AppDatabase
import com.quicknotes.app.data.repository.FolderRepositoryImpl
import com.quicknotes.app.data.repository.NoteRepositoryImpl
import com.quicknotes.app.data.repository.TagRepositoryImpl
import com.quicknotes.app.domain.repository.FolderRepository
import com.quicknotes.app.domain.repository.NoteRepository
import com.quicknotes.app.domain.repository.TagRepository

class AppContainer(
    val database: AppDatabase,
    val noteRepository: NoteRepository,
    val tagRepository: TagRepository,
    val folderRepository: FolderRepository
)

class QuickNotesApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.build(this)
        container = AppContainer(
            database = database,
            noteRepository = NoteRepositoryImpl(database),
            tagRepository = TagRepositoryImpl(database.tagDao()),
            folderRepository = FolderRepositoryImpl(database.folderDao())
        )
    }
}
