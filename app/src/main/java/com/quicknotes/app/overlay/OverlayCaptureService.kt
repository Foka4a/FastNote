package com.quicknotes.app.overlay

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.quicknotes.app.QuickNotesApp
import com.quicknotes.app.domain.model.CaptureSource
import com.quicknotes.app.domain.voice.VoiceCaptureController
import com.quicknotes.app.voice.AndroidSpeechRecognizerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OverlayCaptureService : Service() {
    companion object {
        const val EXTRA_MODE = "mode"
        const val MODE_TEXT = "text"
        const val MODE_VOICE = "voice"
        private const val CHANNEL_ID = "capture_overlay"
        private const val NOTIFICATION_ID = 1

        fun textIntent(context: Context) = Intent(context, OverlayCaptureService::class.java).putExtra(EXTRA_MODE, MODE_TEXT)
        fun voiceIntent(context: Context) = Intent(context, OverlayCaptureService::class.java).putExtra(EXTRA_MODE, MODE_VOICE)
    }

    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification())
        when (intent?.getStringExtra(EXTRA_MODE)) {
            MODE_TEXT -> showTextCapture()
            MODE_VOICE -> showVoiceCapture()
            else -> showTextCapture()
        }
        return START_NOT_STICKY
    }

    private fun showTextCapture() {
        showOverlay { onDismiss ->
            TextCaptureOverlay(
                onSave = { text -> saveNote(text, CaptureSource.WIDGET_TEXT) },
                onDismiss = onDismiss
            )
        }
    }

    private fun showVoiceCapture() {
        val container = (application as QuickNotesApp).container
        val controller = VoiceCaptureController(AndroidSpeechRecognizerAdapter(this))

        showOverlay { onDismiss ->
            val behavior by androidx.compose.runtime.produceState(
                initialValue = com.quicknotes.app.domain.model.VoiceCaptureBehavior.REVIEW_BEFORE_SAVE
            ) {
                value = container.settingsRepository.voiceCaptureBehavior.first()
            }
            VoiceCaptureOverlay(
                controller = controller,
                behavior = behavior,
                onSave = { text -> saveNote(text, CaptureSource.WIDGET_VOICE) },
                onContinueEditing = { text ->
                    startActivity(
                        Intent(this, com.quicknotes.app.MainActivity::class.java)
                            .putExtra("route", "editor?noteId=0")
                            .putExtra("prefillContent", text)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                onDismiss = { closeOverlay() }
            )
        }
    }

    private fun showOverlay(content: @androidx.compose.runtime.Composable (onDismiss: () -> Unit) -> Unit) {
        if (composeView != null) return
        val owner = OverlayLifecycleOwner().apply { attach() }
        lifecycleOwner = owner

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent { content { closeOverlay() } }
        }
        composeView = view

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        }
        windowManager.addView(view, params)
    }

    private fun saveNote(text: String, source: CaptureSource) {
        val container = (application as QuickNotesApp).container
        scope.launch {
            container.createNoteUseCase(title = text.take(60), content = text, captureSource = source)
            closeOverlay()
        }
    }

    private fun closeOverlay() {
        composeView?.let { windowManager.removeView(it) }
        composeView = null
        lifecycleOwner?.detach()
        lifecycleOwner = null
        stopSelf()
    }

    private fun buildNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Captura rápida", NotificationManager.IMPORTANCE_MIN)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Quick Notes")
            .setContentText("Capturando nota...")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.let { windowManager.removeView(it) }
        lifecycleOwner?.detach()
    }
}
