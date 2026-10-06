package com.quicknotes.app.widget

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.quicknotes.app.MainActivity
import com.quicknotes.app.QuickNotesApp
import com.quicknotes.app.R
import com.quicknotes.app.overlay.OverlayCaptureService
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Locale

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

val NoteIdKey = ActionParameters.Key<Long>("noteId")

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

// "Nocturne" palette from the QuickNotes design (dark theme, fixed — not adaptive to system light/dark).
private val WidgetBackground = ColorProvider(Color(0xFF232532))
private val PanelBackground = ColorProvider(Color(0xFF1A1C29))
private val PillBackground = ColorProvider(Color(0xFF1A1C29))
private val CardBackground = ColorProvider(Color(0xFF232532))
private val Primary = ColorProvider(Color(0xFFB9AFE8))
private val PrimaryContainer = ColorProvider(Color(0xFF9184D9))
private val OnPrimaryContainer = ColorProvider(Color(0xFF161826))
private val OnSurfaceVariant = ColorProvider(Color(0xFFE9E9ED).copy(alpha = .5f))

private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

class QuickNoteWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as QuickNotesApp).container
        val recentNotes = container.noteRepository.observeRecent(3).first()

        provideContent {
            Row(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(WidgetBackground)
                    .cornerRadius(24.dp)
                    .padding(12.dp)
            ) {
                // Left: quick capture
                Column(modifier = GlanceModifier.defaultWeight().padding(end = 12.dp)) {
                    Text(
                        "Captura Rápida",
                        style = TextStyle(color = Primary, fontWeight = FontWeight.Medium, fontSize = 16.sp)
                    )
                    Spacer(GlanceModifier.height(12.dp))
                    Row(
                        modifier = GlanceModifier.fillMaxWidth().background(PillBackground).cornerRadius(24.dp).padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = GlanceModifier
                                .size(40.dp)
                                .background(PrimaryContainer)
                                .cornerRadius(20.dp)
                                .clickable(actionRunCallback<StartVoiceCaptureAction>()),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_mic),
                                contentDescription = "Gravar áudio",
                                colorFilter = ColorFilter.tint(OnPrimaryContainer),
                                modifier = GlanceModifier.size(20.dp)
                            )
                        }
                        Spacer(GlanceModifier.width(8.dp))
                        Text(
                            "Escreva uma nota...",
                            style = TextStyle(color = OnSurfaceVariant, fontSize = 13.sp),
                            modifier = GlanceModifier.defaultWeight().clickable(actionRunCallback<StartTextCaptureAction>())
                        )
                    }
                }

                Spacer(GlanceModifier.width(12.dp))

                // Right: recent notes
                Column(modifier = GlanceModifier.defaultWeight().fillMaxHeight().background(PanelBackground).padding(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "RECENTES",
                            style = TextStyle(color = OnSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 10.sp),
                            modifier = GlanceModifier.defaultWeight()
                        )
                        Image(
                            provider = ImageProvider(R.drawable.ic_arrow_forward),
                            contentDescription = "Abrir Inbox",
                            colorFilter = ColorFilter.tint(OnSurfaceVariant),
                            modifier = GlanceModifier.size(14.dp).clickable(actionRunCallback<OpenInboxAction>())
                        )
                    }
                    Spacer(GlanceModifier.height(6.dp))
                    recentNotes.forEach { note ->
                        Column(
                            modifier = GlanceModifier
                                .fillMaxWidth()
                                .background(CardBackground)
                                .cornerRadius(8.dp)
                                .padding(6.dp)
                                .clickable(actionRunCallback<OpenNoteAction>(actionParametersOf(NoteIdKey to note.id)))
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    note.title.ifBlank { note.content.take(20) },
                                    maxLines = 1,
                                    style = TextStyle(color = Primary, fontWeight = FontWeight.Medium, fontSize = 12.sp),
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Text(
                                    timeFormat.format(java.util.Date(note.createdAt)),
                                    style = TextStyle(color = OnSurfaceVariant, fontSize = 9.sp)
                                )
                            }
                            Text(
                                note.content.take(30),
                                maxLines = 1,
                                style = TextStyle(color = OnSurfaceVariant, fontSize = 10.sp)
                            )
                        }
                        Spacer(GlanceModifier.height(4.dp))
                    }
                }
            }
        }
    }
}
