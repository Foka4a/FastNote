package com.quicknotes.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.compose.ui.unit.dp
import com.quicknotes.app.MainActivity
import com.quicknotes.app.QuickNotesApp
import com.quicknotes.app.overlay.OverlayCaptureService
import kotlinx.coroutines.flow.first

class StartTextCaptureAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startForegroundService(OverlayCaptureService.textIntent(context))
    }
}

class StartVoiceCaptureAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startForegroundService(OverlayCaptureService.voiceIntent(context))
    }
}

class OpenInboxAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putExtra("route", "inbox")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

val NoteIdKey = androidx.glance.action.ActionParameters.Key<Long>("noteId")

class OpenNoteAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val noteId = parameters[NoteIdKey] ?: return
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .putExtra("route", "editor?noteId=$noteId")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

class QuickNoteWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as QuickNotesApp).container
        val recentNotes = container.noteRepository.observeRecent(5).first()

        provideContent {
            Column(modifier = androidx.glance.GlanceModifier.padding(12.dp)) {
                Row {
                    Text("Texto", modifier = androidx.glance.GlanceModifier.clickable(actionRunCallback<StartTextCaptureAction>()))
                    Text("Voz", modifier = androidx.glance.GlanceModifier.clickable(actionRunCallback<StartVoiceCaptureAction>()))
                    Text("Inbox", modifier = androidx.glance.GlanceModifier.clickable(actionRunCallback<OpenInboxAction>()))
                }
                recentNotes.forEach { note ->
                    Text(
                        note.title.ifBlank { note.content.take(30) },
                        modifier = androidx.glance.GlanceModifier
                            .fillMaxWidth()
                            .clickable(actionRunCallback<OpenNoteAction>(
                                androidx.glance.action.actionParametersOf(NoteIdKey to note.id)
                            ))
                    )
                }
            }
        }
    }
}
