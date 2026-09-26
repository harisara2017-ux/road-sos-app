package com.roadsos.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.roadsos.ui.auth.AuthScreen
import com.roadsos.ui.emergency.ContactsScreen
import com.roadsos.ui.emergency.EmergencyCountdownScreen
import com.roadsos.ui.emergency.EmergencyStatusScreen
import com.roadsos.ui.emergency.EmergencyViewModel
import com.roadsos.ui.home.HomeScreen
import com.roadsos.ui.map.MapScreen
import com.roadsos.ui.reports.AccidentReportsScreen
import com.roadsos.ui.safety.SafetyScoreScreen

sealed class BottomNavItem(val route: String, val title: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Default.Home)
    object SafeRoute : BottomNavItem("map", "Safe Route", Icons.Default.Place)
    object Reports : BottomNavItem("reports", "Reports", Icons.Default.List)
    object Contacts : BottomNavItem("contacts", "Contacts", Icons.Default.Phone)
    object SafetyScore : BottomNavItem("safety", "Safety Index", Icons.Default.CheckCircle)
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RoadSOSApp() {
    val navController = rememberNavController()
    val emergencyViewModel: EmergencyViewModel = viewModel()

    val permissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.SEND_SMS
        )
    )

    LaunchedEffect(Unit) {
        permissionState.launchMultiplePermissionRequest()
    }

    val startDest = remember {
        if (Firebase.auth.currentUser != null) "home" else "auth"
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val accidentState by emergencyViewModel.accidentState.collectAsState()

    LaunchedEffect(accidentState) {
        if (accidentState == com.roadsos.ui.emergency.AccidentState.COUNTDOWN || 
            accidentState == com.roadsos.ui.emergency.AccidentState.POSSIBLE_IMPACT) {
            val destination = navController.currentBackStackEntry?.destination?.route
            if (destination != "countdown" && destination != "status") {
                navController.navigate("countdown")
            }
        }
    }

    val bottomNavItems = listOf(
        BottomNavItem.Home,
        BottomNavItem.SafeRoute,
        BottomNavItem.Reports,
        BottomNavItem.Contacts,
        BottomNavItem.SafetyScore
    )

    val showBottomBar = currentRoute in listOf("home", "map", "reports", "contacts", "safety")

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 6.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 11.sp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                indicatorColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            NavHost(navController = navController, startDestination = startDest) {
                composable("auth") {
                    AuthScreen(
                        onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("auth") { inclusive = true }
                            }
                        }
                    )
                }

                composable("home") {
                    HomeScreen(
                        emergencyViewModel = emergencyViewModel,
                        onNavigateToMap = { navController.navigate("map") },
                        onNavigateToContacts = { navController.navigate("contacts") },
                        onNavigateToReports = { navController.navigate("reports") },
                        onNavigateToSafetyScore = { navController.navigate("safety") },
                        onTestAccident = {
                            emergencyViewModel.triggerPossibleAccident()
                            navController.navigate("countdown")
                        },
                        onLogout = {
                            Firebase.auth.signOut()
                            navController.navigate("auth") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }

                composable("map") {
                    MapScreen(
                        onBack = { navController.navigate("home") }
                    )
                }

                composable("reports") {
                    AccidentReportsScreen(
                        onBack = { navController.navigate("home") }
                    )
                }

                composable("contacts") {
                    ContactsScreen(
                        onBack = { navController.navigate("home") }
                    )
                }

                composable("safety") {
                    SafetyScoreScreen(
                        onBack = { navController.navigate("home") }
                    )
                }

                composable("countdown") {
                    EmergencyCountdownScreen(
                        viewModel = emergencyViewModel,
                        onCancelled = {
                            emergencyViewModel.resetState()
                            navController.popBackStack()
                        },
                        onConfirmed = {
                            navController.navigate("status") {
                                popUpTo("countdown") { inclusive = true }
                            }
                        }
                    )
                }

                composable("status") {
                    EmergencyStatusScreen(
                        viewModel = emergencyViewModel,
                        onNavigateBack = {
                            emergencyViewModel.resetState()
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }
}
