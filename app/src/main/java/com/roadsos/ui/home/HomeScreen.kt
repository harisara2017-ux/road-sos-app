package com.roadsos.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.roadsos.ui.components.RoadSOSHeaderLogo
import com.roadsos.ui.emergency.EmergencyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    emergencyViewModel: EmergencyViewModel = viewModel(),
    onNavigateToMap: () -> Unit,
    onNavigateToContacts: () -> Unit,
    onNavigateToReports: () -> Unit,
    onNavigateToSafetyScore: () -> Unit,
    onTestAccident: () -> Unit,
    onLogout: () -> Unit
) {
    val currentUser = Firebase.auth.currentUser
    val userEmail = currentUser?.email ?: "Rider"

    Scaffold(
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RoadSOSHeaderLogo()

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userEmail.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = onLogout) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Logout",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Current Safety Status Card
            SafetyStatusCard(emergencyViewModel = emergencyViewModel)

            // 2. Emergency SOS Section
            EmergencySosSection(
                onTriggerTest = onTestAccident
            )

            // 3. Grid / Feature Cards (E-commerce Card Style)
            Text(
                text = "SAFETY SERVICES & DISPATCH",
                style = MaterialTheme.typography.labelMedium,
                letterSpacing = 1.2.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Safe Route Planner Card
            DashboardActionCard(
                title = "Safe Route Planner",
                description = "Navigate through Chennai & Tamil Nadu with real-time accident blackspot avoidance",
                tag = "BLACKSPOT AVOIDANCE",
                tagColor = Color(0xFF059669),
                icon = Icons.Default.Place,
                onClick = onNavigateToMap
            )

            // Emergency Contacts Card
            DashboardActionCard(
                title = "Emergency Contacts Network",
                description = "Configure priority responders and automated crash broadcast contacts",
                tag = "SMS DISPATCH",
                tagColor = Color(0xFF0284C7),
                icon = Icons.Default.Phone,
                onClick = onNavigateToContacts
            )

            // Accident Reports Card
            DashboardActionCard(
                title = "Accident Reports & Timeline",
                description = "View real-time event logs, telemetry evidence, and hospital portal statuses",
                tag = "AUDIT TIMELINE",
                tagColor = Color(0xFFD97706),
                icon = Icons.Default.Info,
                onClick = onNavigateToReports
            )

            // Ride Safety Score Card
            DashboardActionCard(
                title = "Ride Safety Index",
                description = "Monitor driving smoothness, harsh braking events, and trip safety rating",
                tag = "94 / 100 RATING",
                tagColor = Color(0xFF16A34A),
                icon = Icons.Default.ThumbUp,
                onClick = onNavigateToSafetyScore
            )

            // Nearby Hospitals Emergency Card
            DashboardActionCard(
                title = "Nearby Emergency Hospitals",
                description = "Live emergency departments and trauma centers connected to RoadSOS dispatch",
                tag = "TRAUMA CENTRES",
                tagColor = Color(0xFFDC2626),
                icon = Icons.Default.Star,
                onClick = onNavigateToMap
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun SafetyStatusCard(
    emergencyViewModel: EmergencyViewModel = viewModel()
) {
    val accel by emergencyViewModel.accelerometerData.collectAsState()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Status indicator & Protection Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (accel.isSpike) Color(0xFFDC2626) else Color(0xFF10B981))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (accel.isSpike) "IMPACT SPIKE DETECTED" else "SAFETY ENGINE ACTIVE",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp,
                        color = if (accel.isSpike) Color(0xFFDC2626) else Color(0xFF059669)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (accel.isSpike) Color(0xFFFEE2E2) else Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = if (accel.isSpike) "CRITICAL" else "PROTECTED",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (accel.isSpike) Color(0xFFB91C1C) else Color(0xFF166534)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Continuous Rider Protection",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Real-time 3-axis accelerometer telemetry & automated impact detection.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            // =========================================================
            // LIVE ACCELEROMETER SENSOR TELEMETRY BOX
            // =========================================================
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF0B132B).copy(alpha = 0.05f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Sensor Header & Dynamic State Pill
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ACCELEROMETER SENSOR (50Hz)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 0.8.sp
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when {
                                accel.magnitude >= 32.0f -> Color(0xFFDC2626)
                                accel.magnitude >= 20.0f -> Color(0xFFD97706)
                                accel.magnitude >= 14.0f -> Color(0xFF0284C7)
                                else -> Color(0xFF059669)
                            }
                        ) {
                            Text(
                                text = accel.status,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 10.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Primary G-Force / Resultant Acceleration Measure
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Column {
                            Text(
                                text = "RESULTANT FORCE",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = String.format("%.2f", accel.magnitude),
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (accel.magnitude >= 25f) Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "m/s²",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = String.format("%.2f G-Force", accel.gForce),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // 3-Axis Measures (X, Y, Z)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AxisMetricChip(
                            axis = "X",
                            label = "LATERAL",
                            value = accel.x,
                            modifier = Modifier.weight(1f)
                        )
                        AxisMetricChip(
                            axis = "Y",
                            label = "PITCH/BRAKE",
                            value = accel.y,
                            modifier = Modifier.weight(1f)
                        )
                        AxisMetricChip(
                            axis = "Z",
                            label = "GRAVITY",
                            value = accel.z,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dynamic Force Intensity Gauge
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Dynamic Load (Crash Trigger: 24 m/s²)",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${((accel.magnitude / 24f).coerceIn(0f, 1f) * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (accel.magnitude / 24f).coerceIn(0.04f, 1f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                accel.magnitude >= 24f -> Color(0xFFDC2626)
                                accel.magnitude >= 16f -> Color(0xFFD97706)
                                else -> Color(0xFF059669)
                            },
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subsystem Health Indicators
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusIndicatorItem(label = "Sensors", value = "Live 50Hz", isOk = true)
                StatusIndicatorItem(label = "GPS", value = "Acquired", isOk = true)
                StatusIndicatorItem(label = "Crash Engine", value = "Autonomous", isOk = true)
            }
        }
    }
}

@Composable
fun AxisMetricChip(
    axis: String,
    label: String,
    value: Float,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = axis,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = String.format("%+.2f", value),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "m/s²",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatusIndicatorItem(label: String, value: String, isOk: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = if (isOk) Color(0xFF10B981) else Color(0xFFD97706),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun EmergencySosSection(
    onTriggerTest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EMERGENCY RESPONSE",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.2.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFDC2626)
                )

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFFEE2E2)
                ) {
                    Text(
                        text = "1-TAP ESCALATION",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF991B1B)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Prominent Red SOS Button
            Button(
                onClick = onTriggerTest,
                modifier = Modifier
                    .size(110.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "SOS",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Tap SOS or simulate a crash event to trigger immediate verification countdown and hospital notification.",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF7F1D1D),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onTriggerTest,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF87171))
            ) {
                Text(
                    text = "SIMULATE ACCIDENT DETECTION",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

@Composable
fun DashboardActionCard(
    title: String,
    description: String,
    tag: String,
    tagColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = tagColor.copy(alpha = 0.12f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tagColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = tagColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = tag,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = tagColor,
                        fontSize = 10.sp
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
