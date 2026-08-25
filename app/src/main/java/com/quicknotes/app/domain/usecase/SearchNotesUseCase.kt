package com.quicknotes.app.domain.usecase

import com.quicknotes.app.domain.model.Note
import com.quicknotes.app.domain.repository.NoteRepository

class SearchNotesUseCase(private val repository: NoteRepository) {
    suspend operator fun invoke(query: String): List<Note> {
        if (query.isBlank()) return emptyList()
        return repository.search(query)
    }
}
