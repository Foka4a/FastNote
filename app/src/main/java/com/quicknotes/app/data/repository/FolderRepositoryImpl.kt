package com.quicknotes.app.data.repository

import com.quicknotes.app.data.local.dao.FolderDao
import com.quicknotes.app.data.local.entity.FolderEntity
import com.quicknotes.app.domain.model.Folder
import com.quicknotes.app.domain.repository.FolderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FolderRepositoryImpl(private val folderDao: FolderDao) : FolderRepository {
    override fun observeFolders(): Flow<List<Folder>> =
        folderDao.observeAll().map { list -> list.map { Folder(it.id, it.name, it.parentId) } }

    override suspend fun createFolder(name: String, parentId: Long?): Long =
        folderDao.insert(FolderEntity(name = name, parentId = parentId))

    override suspend fun deleteFolder(id: Long) = folderDao.delete(id)
}
