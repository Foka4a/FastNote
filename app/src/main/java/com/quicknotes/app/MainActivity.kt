package com.quicknotes.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.glance.appwidget.updateAll
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quicknotes.app.overlay.OverlayPermission
import com.quicknotes.app.ui.nav.QuickNotesNavHost
import com.quicknotes.app.ui.onboarding.OnboardingScreen
import com.quicknotes.app.widget.QuickNoteWidget

class MainActivity : ComponentActivity() {
    // Set only by onNewIntent (widget/overlay tap while the app is already running);
    // the NavHost consumes it and clears it back to null.
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as QuickNotesApp).container
        val startRoute = routeFrom(intent)
        setContent {
            var showOnboarding by remember { mutableStateOf(!OverlayPermission.isGranted(this)) }
            val requestAudioPermission = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { /* no-op: if denied, voice capture surfaces its existing "unavailable" error path */ }
            LaunchedEffect(Unit) {
                val granted = this@MainActivity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                if (!granted) requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
            // Glance widgets don't re-render just because the app was reinstalled/updated;
            // refreshing here keeps the home-screen widget's recent-notes list current.
            LaunchedEffect(Unit) { QuickNoteWidget().updateAll(this@MainActivity) }
            // Granting SYSTEM_ALERT_WINDOW happens in system Settings, which only resumes
            // this activity instead of recreating it — re-check on every resume so the user
            // isn't stuck on onboarding after granting.
            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME && OverlayPermission.isGranted(this@MainActivity)) {
                        showOnboarding = false
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }
            MaterialTheme {
                Surface {
                    if (showOnboarding) {
                        OnboardingScreen(
                            onRequestPermission = { startActivity(OverlayPermission.requestIntent(this)) },
                            onSkip = { showOnboarding = false }
                        )
                    } else {
                        QuickNotesNavHost(
                            container = container,
                            startRoute = startRoute,
                            pendingRoute = pendingRoute,
                            onRouteHandled = { pendingRoute = null }
                        )
                    }
                }
            }
        }
    }

    // With launchMode="singleTop" a widget tap on the already-running app arrives here
    // instead of onCreate, so the route has to be applied to the live NavHost.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = routeFrom(intent)
    }

    // "prefillContent" (voice capture with CONTINUE_EDITING) rides along as an optional
    // query arg on the editor route so the Editor's ViewModel can start from that text.
    private fun routeFrom(intent: Intent): String {
        val route = intent.getStringExtra("route") ?: "inbox"
        val prefill = intent.getStringExtra("prefillContent")
        return if (prefill.isNullOrEmpty() || !route.startsWith("editor")) route
        else "$route&prefillContent=${Uri.encode(prefill)}"
    }
}
