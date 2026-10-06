package com.quicknotes.app.domain.usecase

import com.quicknotes.app.domain.FakeNoteRepository
import com.quicknotes.app.domain.model.CaptureSource
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateNoteUseCaseTest {
    @Test
    fun createdNoteLandsInInboxWithGivenCaptureSource() = runTest {
        val repository = FakeNoteRepository()
        val useCase = CreateNoteUseCase(repository)

        val id = useCase(title = "Comprar filtro de óleo", content = "", captureSource = CaptureSource.WIDGET_VOICE)

        val saved = repository.getNote(id)!!
        assertTrue(saved.inbox)
        assertTrue(saved.captureSource == CaptureSource.WIDGET_VOICE)
    }
}
