package com.roadsos.ui.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun EmergencyCountdownScreen(
    viewModel: EmergencyViewModel = viewModel(),
    onCancelled: () -> Unit,
    onConfirmed: () -> Unit
) {
    val state by viewModel.accidentState.collectAsState()
    val countdown by viewModel.countdown.collectAsState()

    androidx.activity.compose.BackHandler {
        viewModel.markAsSafe()
    }

    androidx.compose.runtime.LaunchedEffect(state) {
        if (state == AccidentState.CONFIRMED) {
            onConfirmed()
        } else if (state == AccidentState.CANCELLED) {
            onCancelled()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "⚠️ POSSIBLE ACCIDENT DETECTED",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Emergency alert will be sent in:",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = countdown.toString(),
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp),
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(
                onClick = { viewModel.markAsSafe() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
            ) {
                Text("I'M SAFE", style = MaterialTheme.typography.titleLarge)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { viewModel.confirmAccident() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("SEND HELP NOW", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
    }
}
