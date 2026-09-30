package com.quicknotes.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quicknotes.app.ui.theme.Nocturne

private data class OnboardingStep(val number: String, val text: String)

private val steps = listOf(
    OnboardingStep("1", "Toque em Abrir Configurações — levamos você direto para “Sobrepor a outros apps”."),
    OnboardingStep("2", "Ative Quick Notes na lista e volte com o botão de voltar."),
    OnboardingStep("3", "Pronto: o widget passa a abrir o popup por cima de qualquer app.")
)

@Composable
fun OnboardingScreen(onRequestPermission: () -> Unit, onSkip: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Nocturne.Background).padding(20.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(Nocturne.Surface, RoundedCornerShape(14.dp))
                .border(1.dp, Nocturne.BorderSubtle, RoundedCornerShape(14.dp)),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Widgets, contentDescription = null, tint = Nocturne.TextFaint, modifier = Modifier.size(40.dp))
        }

        Text(
            "Anotar sem sair do que você está fazendo",
            color = Nocturne.TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Medium, lineHeight = 28.sp,
            modifier = Modifier.padding(top = 22.dp, bottom = 10.dp)
        )
        Text(
            "O Android não concede a permissão de sobreposição por um aviso simples — você precisa liberá-la uma vez nas Configurações do sistema. Sem ela, o widget ainda funciona: a captura abre o app.",
            color = Nocturne.TextSecondary, fontSize = 13.5.sp, lineHeight = 21.sp
        )

        Column(Modifier.padding(top = 24.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            steps.forEach { step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        Modifier
                            .size(24.dp)
                            .border(1.dp, Nocturne.AccentBorder, CircleShape),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(step.number, color = Nocturne.AccentText, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(step.text, color = Nocturne.TextPrimary.copy(alpha = .72f), fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }

        Text(
            "Abrir Configurações do Android",
            color = Nocturne.AccentText, fontSize = 14.sp, fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Nocturne.AccentBorder, RoundedCornerShape(12.dp))
                .clickable(onClick = onRequestPermission)
                .padding(14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Text(
            "Agora não",
            color = Nocturne.TextMuted, fontSize = 13.sp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onSkip)
                .padding(14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
