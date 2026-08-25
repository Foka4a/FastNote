package com.quicknotes.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.quicknotes.app.overlay.OverlayPermission
import com.quicknotes.app.ui.nav.QuickNotesNavHost
import com.quicknotes.app.ui.onboarding.OnboardingScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as QuickNotesApp).container
        val startRoute = intent.getStringExtra("route") ?: "inbox"
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
            MaterialTheme {
                Surface {
                    if (showOnboarding) {
                        OnboardingScreen(
                            onRequestPermission = { startActivity(OverlayPermission.requestIntent(this)) },
                            onSkip = { showOnboarding = false }
                        )
                    } else {
                        QuickNotesNavHost(container, startRoute)
                    }
                }
            }
        }
    }
}
