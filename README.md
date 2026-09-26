# RoadSOS - Intelligent Accident Prevention & Emergency Response System

RoadSOS is an Android mobile platform and emergency dispatch system developed to prevent road accidents, assist riders through real-time blackspot avoidance, and provide automated crash detection and emergency escalation.

---

## 🌟 Key Features

### 1. 🛡️ Real-Time Accelerometer Telemetry & Autonomous Crash Engine
- **Continuous 50Hz Sensor Sampling**: Uses Android's hardware `TYPE_ACCELEROMETER` to calculate real-time resultant force magnitude ($|a| = \sqrt{x^2 + y^2 + z^2}$) and G-Force.
- **3-Axis Split**: Displays lateral tilt ($X$), pitch/braking ($Y$), and vertical gravity ($Z$) telemetry.
- **Autonomous Emergency Detection**: Automatically detects severe impact dynamics ($\ge 24.0 \, m/s^2$) and initiates an emergency countdown.
- **Verification Countdown**: 10-second confirmation timer with `I'M SAFE` false-alarm cancellation and `SEND HELP NOW` escalation.

### 2. 🗺️ Safe Route Navigation & Blackspot Avoidance
- **Accident Risk Mapping**: Leverages spatial bounding-box indexing and clustering across Tamil Nadu accident datasets to highlight dangerous road corridors.
- **Dual-Stroke Route Polylines**: Navy casing with royal blue / alert red inner routing for high visibility.
- **Turn-by-Turn GPS Integration**: 1-tap launch into Google Maps navigation.

### 3. 🚨 Emergency Dispatch & Hospital Alert Network
- **Cloud Firestore Synchronization**: Records verified crash coordinates, timestamps, and severity levels.
- **Automated SMS Dispatch**: Dispatches emergency SMS with Google Maps crash location links to designated emergency contacts.
- **Emergency Trauma Center Locator**: Identifies the nearest verified hospitals with distance and direct call options.
- **One-Tap Emergency Calling**: Pre-configured direct dialing for designated priority contacts and `108 Ambulance Dispatch`.

---

## 📱 Tech Stack & Architecture

- **Operating System / Target**: Android 14+ (SDK 34)
- **UI Framework**: Jetpack Compose & Material 3
- **Language**: Kotlin with Coroutines & StateFlow
- **Location & Sensors**: Google Play Services Location (`FusedLocationProviderClient`) & Android Sensor API (`SensorManager`)
- **Mapping & Geodesics**: Google Maps Compose SDK
- **Backend & Database**: Firebase Authentication & Google Cloud Firestore
- **Emergency Telephony**: Android Telephony `SmsManager` & Action Intents

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Iguana / Ladybug or newer
- JDK 17
- Android SDK 34
- Google Maps Android API Key (configured in `local.properties` or environment)

### Build & Run
```bash
# Clone the repository
git clone https://github.com/harisara2017-ux/road-sos-app.git
cd road-sos-app

# Build debug APK
./gradlew assembleDebug

# Install on connected device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
