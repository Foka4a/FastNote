package com.quicknotes.app.widget

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.QuickNotesApp
import com.quicknotes.app.domain.model.CaptureSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickNoteWidgetTest {
    @Test
    fun recentNotesAreFetchedFromRepositoryForTheWidget() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<QuickNotesApp>()
        app.container.createNoteUseCase(title = "Nota recente", content = "", captureSource = CaptureSource.APP)

        val recent = app.container.noteRepository.observeRecent(5).first()

        assertTrue(recent.any { it.title == "Nota recente" })
    }
}
