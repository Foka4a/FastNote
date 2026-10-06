package com.quicknotes.app.domain.repository

import com.quicknotes.app.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun observeFolders(): Flow<List<Folder>>
    suspend fun createFolder(name: String, parentId: Long?): Long
    suspend fun deleteFolder(id: Long)
}
