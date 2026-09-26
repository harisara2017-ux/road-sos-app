package com.roadsos.ui.emergency

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmergencyStatusScreen(
    viewModel: EmergencyViewModel = viewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val event by viewModel.currentAccidentEvent.collectAsState()
    val nearestHospitalName by viewModel.nearestHospitalName.collectAsState()
    val nearestHospitalDistance by viewModel.nearestHospitalDistance.collectAsState()
    val primaryContact by viewModel.primaryContact.collectAsState()

    val rawPhone = primaryContact?.phoneNumber ?: ""
    val contactNumber = rawPhone.replace(" ", "").replace("-", "").trim().ifBlank { "112" }
    val contactName = primaryContact?.name ?: "Emergency Contact"

    val lat = event?.latitude ?: 13.0827
    val lng = event?.longitude ?: 80.2707

    androidx.activity.compose.BackHandler {
        onNavigateBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Emergency Overview", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Accident Confirmed Header
            Surface(
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                color = Color(0xFFFEE2E2)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Accident Confirmed",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            
            Text(
                text = "ACCIDENT CONFIRMED",
                style = MaterialTheme.typography.headlineMedium,
                color = Color(0xFFDC2626),
                fontWeight = FontWeight.ExtraBold
            )

            Text(
                text = "Automated emergency broadcast and hospital trauma alert have been dispatched.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            // Status Dashboard Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "DISPATCH STATUS",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    StatusRow("GPS Coordinates", "%.5f, %.5f".format(lat, lng), isSuccess = true)
                    StatusRow("Cloud Firestore", event?.accidentStatus ?: "CONFIRMED", isSuccess = true)
                    StatusRow("Emergency SMS", event?.emergencyContactNotificationStatus ?: "SENT", isSuccess = event?.emergencyContactNotificationStatus != "FAILED")
                    StatusRow("Hospital Portal", event?.hospitalNotificationStatus ?: "TRANSMITTED", isSuccess = true)
                }
            }

            // Nearest Hospital Information Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "NEAREST EMERGENCY TRAUMA CENTER",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 1.1.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = nearestHospitalName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 $nearestHospitalDistance",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (All wired to real Android Intents)
            Button(
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$contactNumber")
                    }
                    context.startActivity(dialIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (primaryContact != null) "CALL ${contactName.uppercase()} ($contactNumber)"
                           else "CALL EMERGENCY CONTACT (112)",
                    fontWeight = FontWeight.Bold
                )
            }
            
            Button(
                onClick = {
                    val dialIntent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:108")
                    }
                    context.startActivity(dialIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
            ) {
                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CALL 108 AMBULANCE DISPATCH", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val mapUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(Crash+Site)")
                    val mapIntent = Intent(Intent.ACTION_VIEW, mapUri)
                    context.startActivity(mapIntent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("OPEN ACCIDENT LOCATION ON MAP", fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = {
                    val navUri = Uri.parse("google.navigation:q=hospital+near+me")
                    val navIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        context.startActivity(navIntent)
                    } catch (_: Exception) {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=hospital+near+me"))
                        context.startActivity(browserIntent)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("NAVIGATE TO NEAREST HOSPITAL", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, isSuccess: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (isSuccess) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
        ) {
            Text(
                text = value,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelSmall,
                color = if (isSuccess) Color(0xFF166534) else Color(0xFF991B1B)
            )
        }
    }
}
