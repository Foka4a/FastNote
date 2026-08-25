package com.quicknotes.app.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingScreen(onRequestPermission: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.padding(24.dp)) {
        Text("Quick Notes precisa de uma permissão para mostrar o popup de captura rápida por cima de outros apps.")
        Text("Sem ela, a captura pelo widget abre o app inteiro em vez do popup instantâneo.")
        Button(onClick = onRequestPermission) { Text("Liberar permissão") }
        Button(onClick = onSkip) { Text("Agora não") }
    }
}
