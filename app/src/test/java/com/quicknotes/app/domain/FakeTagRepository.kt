package com.quicknotes.app.domain

import com.quicknotes.app.domain.model.Tag
import com.quicknotes.app.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeTagRepository : TagRepository {
    private var nextId = 1L
    val tags = MutableStateFlow<List<Tag>>(emptyList())
    override fun observeTags(): Flow<List<Tag>> = tags
    override suspend fun createTag(name: String): Long {
        val id = nextId++
        tags.value = tags.value + Tag(id, name)
        return id
    }
    override suspend fun deleteTag(id: Long) { tags.value = tags.value.filterNot { it.id == id } }
}
