package com.quicknotes.app.domain

import com.quicknotes.app.domain.model.Folder
import com.quicknotes.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeFolderRepository : FolderRepository {
    private var nextId = 1L
    val folders = MutableStateFlow<List<Folder>>(emptyList())
    override fun observeFolders(): Flow<List<Folder>> = folders
    override suspend fun createFolder(name: String, parentId: Long?): Long {
        val id = nextId++
        folders.value = folders.value + Folder(id, name, parentId)
        return id
    }
    override suspend fun deleteFolder(id: Long) { folders.value = folders.value.filterNot { it.id == id } }
}
