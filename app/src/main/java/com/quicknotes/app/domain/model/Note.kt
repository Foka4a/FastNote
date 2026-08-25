package com.quicknotes.app.domain.model

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val folderId: Long?,
    val favorite: Boolean,
    val archived: Boolean,
    val inbox: Boolean,
    val captureSource: CaptureSource,
    val tagIds: List<Long> = emptyList()
)
