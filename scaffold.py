import os

base_dir = r'c:\Users\harik\OneDrive\Documents\Desktop\ep pbl'

dirs = [
    'app/src/main/java/com/roadsos/data/firebase',
    'app/src/main/java/com/roadsos/data/maps',
    'app/src/main/java/com/roadsos/data/location',
    'app/src/main/java/com/roadsos/data/bluetooth',
    'app/src/main/java/com/roadsos/data/repository',
    'app/src/main/java/com/roadsos/domain/model',
    'app/src/main/java/com/roadsos/domain/repository',
    'app/src/main/java/com/roadsos/ui/theme',
    'app/src/main/java/com/roadsos/ui/auth',
    'app/src/main/java/com/roadsos/ui/home',
    'app/src/main/java/com/roadsos/ui/map',
    'app/src/main/java/com/roadsos/ui/sos',
    'app/src/main/java/com/roadsos/ui/profile',
    'app/src/main/java/com/roadsos/ui/hardware',
    'app/src/main/java/com/roadsos/ui/common',
    'app/src/main/java/com/roadsos/navigation',
    'app/src/main/java/com/roadsos/utils',
    'app/src/main/res/values',
    'app/src/main/res/layout',
    'app/src/main/res/drawable',
    'app/src/main/res/mipmap-hdpi',
    'gradle/wrapper'
]

for d in dirs:
    os.makedirs(os.path.join(base_dir, d), exist_ok=True)

build_gradle_project = """// Top-level build file
plugins {
    id("com.android.application") version "8.2.0" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}
"""

build_gradle_app = """plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.roadsos"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.roadsos"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.08.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    
    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")

    // Google Maps Compose
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")
}
"""

settings_gradle = """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RoadSOS"
include(":app")
"""

manifest = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.roadsos">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
    <uses-permission android:name="android.permission.BLUETOOTH" />
    <uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="RoadSOS"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.RoadSOS">
        <meta-data
            android:name="com.google.android.geo.API_KEY"
            android:value="YOUR_API_KEY_HERE" />
        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/Theme.RoadSOS">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>
"""

files = {
    'build.gradle.kts': build_gradle_project,
    'settings.gradle.kts': settings_gradle,
    'app/build.gradle.kts': build_gradle_app,
    'app/src/main/AndroidManifest.xml': manifest
}

for path, content in files.items():
    with open(os.path.join(base_dir, path), 'w') as f:
        f.write(content.strip())

print('Android Base Scaffolding Complete!')
