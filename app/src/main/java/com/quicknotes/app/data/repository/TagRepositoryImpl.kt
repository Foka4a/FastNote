package com.quicknotes.app.data.repository

import com.quicknotes.app.data.local.dao.TagDao
import com.quicknotes.app.data.local.entity.TagEntity
import com.quicknotes.app.domain.model.Tag
import com.quicknotes.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepositoryImpl(private val tagDao: TagDao) : TagRepository {
    override fun observeTags(): Flow<List<Tag>> =
        tagDao.observeAll().map { list -> list.map { Tag(it.id, it.name) } }

    override suspend fun createTag(name: String): Long = tagDao.insert(TagEntity(name = name))
    override suspend fun deleteTag(id: Long) = tagDao.delete(id)
}
