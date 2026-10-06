package com.quicknotes.app.overlay

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quicknotes.app.QuickNotesApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OverlayCaptureServiceTest {
    @Test
    fun textIntentCarriesTextMode() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = OverlayCaptureService.textIntent(context)
        assertEquals(OverlayCaptureService.MODE_TEXT, intent.getStringExtra(OverlayCaptureService.EXTRA_MODE))
    }

    @Test
    fun savingFromOverlayCreatesInboxNoteWithWidgetTextSource() = runBlocking {
        val app = ApplicationProvider.getApplicationContext<QuickNotesApp>()
        val before = app.container.noteRepository.observeInbox()

        // This exercises the same code path the service calls on save, without needing
        // WindowManager permission granted in the test environment.
        app.container.createNoteUseCase(
            title = "Nota do overlay",
            content = "",
            captureSource = com.quicknotes.app.domain.model.CaptureSource.WIDGET_TEXT
        )

        val inbox = before.first()
        assertTrue(inbox.any { it.title == "Nota do overlay" && it.captureSource == com.quicknotes.app.domain.model.CaptureSource.WIDGET_TEXT })
    }
}
