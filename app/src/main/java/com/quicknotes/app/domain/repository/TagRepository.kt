package com.quicknotes.app.domain.repository

import com.quicknotes.app.domain.model.Tag
import kotlinx.coroutines.flow.Flow

interface TagRepository {
    fun observeTags(): Flow<List<Tag>>
    suspend fun createTag(name: String): Long
    suspend fun deleteTag(id: Long)
}
