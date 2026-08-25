package com.quicknotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.quicknotes.app.ui.nav.QuickNotesNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as QuickNotesApp).container
        val startRoute = intent.getStringExtra("route") ?: "inbox"
        setContent {
            MaterialTheme {
                Surface { QuickNotesNavHost(container, startRoute) }
            }
        }
    }
}
