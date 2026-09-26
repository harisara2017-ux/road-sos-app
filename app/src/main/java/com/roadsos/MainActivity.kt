package com.roadsos

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.google.firebase.FirebaseApp
import com.roadsos.ui.theme.RoadSOSTheme
import com.roadsos.navigation.RoadSOSApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Verify Firebase Initialization
        try {
            FirebaseApp.initializeApp(this)
            Log.d("RoadSOS_Firebase", "Firebase initialized successfully with existing project.")
        } catch (e: Exception) {
            Log.e("RoadSOS_Firebase", "Firebase initialization failed: \${e.message}")
        }
        
        setContent {
            RoadSOSTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RoadSOSApp()
                }
            }
        }
    }
}

