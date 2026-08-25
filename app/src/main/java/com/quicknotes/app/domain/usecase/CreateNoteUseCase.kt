package com.quicknotes.app.domain.usecase

import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.repository.NoteRepository

class CreateNoteUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(
        title: String,
        content: String,
        captureSource: CaptureSource,
        folderId: Long? = null,
        tagIds: List<Long> = emptyList()
    ): Long {
        val now = System.currentTimeMillis()
        return repository.saveNote(
            Note(
                title = title, content = content, createdAt = now, updatedAt = now,
                folderId = folderId, favorite = false, archived = false, inbox = true,
                captureSource = captureSource, tagIds = tagIds
            )
        )
    }
}
