package com.roadsos.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.*
import com.roadsos.domain.model.RiskLevel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    viewModel: MapViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasLocationPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    val chennaiCenter = LatLng(13.0827, 80.2707)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(chennaiCenter, 12f)
    }

    var showWarningDialog by remember { mutableStateOf(false) }
    var sourceText by remember { mutableStateOf("Chennai") }
    var destText by remember { mutableStateOf("Tambaram") }
    var isInputsExpanded by remember { mutableStateOf(false) }

    val API_KEY = com.roadsos.BuildConfig.MAPS_API_KEY.ifBlank { "AIzaSyCiEufyiOWDSvEB2wflG-Mvbkm8h_I-5q4" }

    val routePoints by viewModel.routePoints
    val isLoading by viewModel.isLoading
    val distance by viewModel.routeDistance
    val duration by viewModel.routeDuration
    val overallRisk by viewModel.overallRisk
    val collisionZone by viewModel.highRiskCollisionZone
    val routeRiskZones by viewModel.routeRiskZones
    val error by viewModel.error
    val accidentZones by viewModel.accidentZones

    val snackbarHostState = remember { SnackbarHostState() }

    // Helper to zoom and frame the entire route on the map
    fun fitRouteBounds() {
        if (routePoints.isNotEmpty()) {
            coroutineScope.launch {
                try {
                    val builder = LatLngBounds.builder()
                    routePoints.forEach { builder.include(it) }
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngBounds(builder.build(), 140),
                        800
                    )
                } catch (e: Exception) {
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(routePoints.first(), 12f),
                        600
                    )
                }
            }
        }
    }

    // Auto-fit route in view whenever a new route is received
    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            fitRouteBounds()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(
                message = "Notice: $it",
                duration = SnackbarDuration.Long
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Safe Route Navigation",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = if (routePoints.isNotEmpty()) "$sourceText → $destText" else "Real-time Blackspot Avoidance",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (routePoints.isNotEmpty()) {
                        IconButton(onClick = { fitRouteBounds() }) {
                            Icon(
                                Icons.Default.Place,
                                contentDescription = "Fit Route",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Google Map View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(
                    isMyLocationEnabled = hasLocationPermission
                ),
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = hasLocationPermission,
                    compassEnabled = true
                )
            ) {
                // 1. Dual-stroke Navigation Polyline
                if (routePoints.isNotEmpty()) {
                    // Outer border casing for high contrast against maps
                    Polyline(
                        points = routePoints,
                        color = Color(0xFF0F172A).copy(alpha = 0.85f),
                        width = 18f
                    )
                    // Inner glowing route color (Blue for Safe, Red for High Risk)
                    Polyline(
                        points = routePoints,
                        color = if (overallRisk == "CRITICAL" || overallRisk == "HIGH") Color(0xFFDC2626) else Color(0xFF2563EB),
                        width = 12f
                    )

                    // Origin Pin (Green)
                    val startPos = routePoints.first()
                    val startMarkerState = remember(startPos) { MarkerState(position = startPos) }
                    Marker(
                        state = startMarkerState,
                        title = "🟢 Start: $sourceText",
                        snippet = "Trip origin",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                    )

                    // Destination Pin (Red)
                    val endPos = routePoints.last()
                    val endMarkerState = remember(endPos) { MarkerState(position = endPos) }
                    Marker(
                        state = endMarkerState,
                        title = "🏁 Destination: $destText",
                        snippet = if (distance.isNotEmpty()) "$distance • $duration" else "Arrival point",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                    )
                }

                // 2. High-Risk Collision Alert Zone Marker (if on active route)
                collisionZone?.let { zone ->
                    val zonePos = LatLng(zone.centerLatitude, zone.centerLongitude)
                    val alertMarkerState = remember(zone.zoneId) { MarkerState(position = zonePos) }
                    Marker(
                        state = alertMarkerState,
                        title = "⚠️ ${zone.riskLevel} ACCIDENT ZONE",
                        snippet = "Documented crashes: ${zone.accidentCount} recorded",
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)
                    )
                }

                // 3. Documented Accident Blackspots along the route
                val displayRiskZones = if (routeRiskZones.isNotEmpty()) {
                    routeRiskZones
                } else {
                    accidentZones.filter { it.riskLevel == RiskLevel.HIGH || it.riskLevel == RiskLevel.CRITICAL }.take(6)
                }

                displayRiskZones.forEach { zone ->
                    val zonePos = LatLng(zone.centerLatitude, zone.centerLongitude)
                    val zoneMarkerState = remember(zone.zoneId) { MarkerState(position = zonePos) }
                    Marker(
                        state = zoneMarkerState,
                        title = "${zone.riskLevel} Blackspot",
                        snippet = "${zone.accidentCount} historical incidents",
                        icon = BitmapDescriptorFactory.defaultMarker(
                            if (zone.riskLevel == RiskLevel.CRITICAL) BitmapDescriptorFactory.HUE_MAGENTA
                            else BitmapDescriptorFactory.HUE_ORANGE
                        )
                    )
                }
            }

            // Floating "Fit Whole Route" Button on top-right below map tools
            if (routePoints.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { fitRouteBounds() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 16.dp, end = 16.dp)
                        .size(44.dp),
                    shape = CircleShape,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary,
                    elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Center Route",
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // Route Panel: Interactive Bottom Sheet / Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(14.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Header Row with collapse/expand toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (routePoints.isNotEmpty()) "$sourceText → $destText" else "PLAN SAFE JOURNEY",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (routePoints.isNotEmpty() && distance.isNotEmpty()) {
                                    Text(
                                        text = "$distance • $duration",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (routePoints.isNotEmpty()) {
                            TextButton(
                                onClick = { isInputsExpanded = !isInputsExpanded },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isInputsExpanded) "Hide" else "Change",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Collapsible Input Section
                    AnimatedVisibility(
                        visible = isInputsExpanded || routePoints.isEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = sourceText,
                                    onValueChange = { sourceText = it },
                                    label = { Text("Start") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                OutlinedTextField(
                                    value = destText,
                                    onValueChange = { destText = it },
                                    label = { Text("Destination") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    shape = RoundedCornerShape(12.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    isInputsExpanded = false
                                    viewModel.getDirections(sourceText, destText, API_KEY)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isLoading,
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                if (isLoading) {
                                    Text("Calculating Route & Hazards...", fontWeight = FontWeight.Bold)
                                } else {
                                    Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Find Safe Route", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Active Route Details & GPS Launch Action
                    if (routePoints.isNotEmpty() && distance.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))

                        // Risk status alert badge
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = when (overallRisk) {
                                "CRITICAL", "HIGH" -> Color(0xFFFEE2E2)
                                "MEDIUM" -> Color(0xFFFEF3C7)
                                else -> Color(0xFFDCFCE7)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = if (overallRisk == "CRITICAL" || overallRisk == "HIGH") Icons.Default.Warning else Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = if (overallRisk == "CRITICAL" || overallRisk == "HIGH") Color(0xFFDC2626) else Color(0xFF059669),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (collisionZone != null) "Passes near ${collisionZone!!.riskLevel} blackspot!"
                                               else "No high-accident blackspots on path",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = if (overallRisk == "CRITICAL" || overallRisk == "HIGH") Color(0xFF991B1B) else Color(0xFF166534)
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = when (overallRisk) {
                                        "CRITICAL", "HIGH" -> Color(0xFFDC2626)
                                        "MEDIUM" -> Color(0xFFD97706)
                                        else -> Color(0xFF059669)
                                    }
                                ) {
                                    Text(
                                        text = "$overallRisk RISK",
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Button to launch live Google Maps Turn-by-Turn GPS Navigation
                        Button(
                            onClick = {
                                val navUri = Uri.parse("google.navigation:q=" + Uri.encode(destText))
                                val mapIntent = Intent(Intent.ACTION_VIEW, navUri).apply {
                                    setPackage("com.google.android.apps.maps")
                                }
                                try {
                                    context.startActivity(mapIntent)
                                } catch (_: Exception) {
                                    val fallbackIntent = Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://www.google.com/maps/dir/?api=1&origin=" + Uri.encode(sourceText) + "&destination=" + Uri.encode(destText))
                                    )
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (overallRisk == "CRITICAL" || overallRisk == "HIGH") Color(0xFFDC2626) else Color(0xFF059669)
                            )
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("START LIVE TURN-BY-TURN GPS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }

            // High-Risk Collision Alert Modal
            LaunchedEffect(collisionZone) {
                if (collisionZone != null) {
                    showWarningDialog = true
                }
            }

            if (showWarningDialog && collisionZone != null) {
                val zone = collisionZone!!
                AlertDialog(
                    onDismissRequest = { showWarningDialog = false },
                    icon = {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Alert",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(36.dp)
                        )
                    },
                    title = {
                        Text(
                            text = "⚠️ Road Hazard Advisory",
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    text = {
                        Column {
                            Text("The selected route traverses an identified accident cluster:")
                            Spacer(Modifier.height(8.dp))
                            Text("• Severity Level: ${zone.riskLevel}", fontWeight = FontWeight.Bold)
                            Text("• Historic Incidents: ${zone.accidentCount} crashes logged")
                            Text("• Advisory: Reduce velocity and keep safe braking separation.")
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showWarningDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Acknowledge & Continue")
                        }
                    }
                )
            }
        }
    }
}
