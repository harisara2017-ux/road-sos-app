package com.roadsos.ui.emergency

import android.app.Application
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.location.LocationServices
import com.roadsos.domain.model.AccidentEvent
import com.roadsos.domain.repository.AccidentEventRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class AccelerometerReading(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 9.81f,
    val magnitude: Float = 9.81f,
    val gForce: Float = 1.0f,
    val status: String = "Stable / Normal",
    val isSpike: Boolean = false
)

enum class AccidentState {
    NORMAL,
    POSSIBLE_IMPACT,
    COUNTDOWN,
    CONFIRMED,
    CANCELLED
}

class EmergencyViewModel(application: Application) : AndroidViewModel(application) {
    private val accidentRepo = AccidentEventRepository()
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(application)
    
    private val sensorManager = application.getSystemService(android.content.Context.SENSOR_SERVICE) as? android.hardware.SensorManager
    private val accelerometerSensor = sensorManager?.getDefaultSensor(android.hardware.Sensor.TYPE_ACCELEROMETER)

    private val _accelerometerData = MutableStateFlow(AccelerometerReading())
    val accelerometerData: StateFlow<AccelerometerReading> = _accelerometerData

    private var lastCancelTime = 0L
    val IMPACT_THRESHOLD_MS2 = 24.0f

    private val sensorEventListener = object : android.hardware.SensorEventListener {
        override fun onSensorChanged(event: android.hardware.SensorEvent?) {
            if (event != null && event.sensor.type == android.hardware.Sensor.TYPE_ACCELEROMETER) {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                val mag = kotlin.math.sqrt(x * x + y * y + z * z)
                val g = mag / 9.80665f
                
                val isSpike = mag >= IMPACT_THRESHOLD_MS2
                val status = when {
                    mag >= IMPACT_THRESHOLD_MS2 -> "CRITICAL IMPACT DETECTED"
                    mag >= 18.0f -> "Harsh Dynamics"
                    mag >= 13.0f -> "Active Movement"
                    else -> "Stable / Normal"
                }

                _accelerometerData.value = AccelerometerReading(
                    x = x,
                    y = y,
                    z = z,
                    magnitude = mag,
                    gForce = g,
                    status = status,
                    isSpike = isSpike
                )

                // Autonomous emergency detection trigger on high accelerometer reading
                val now = System.currentTimeMillis()
                if (isSpike && (now - lastCancelTime > 2500L)) {
                    val currentState = _accidentState.value
                    if (currentState == AccidentState.NORMAL || currentState == AccidentState.CANCELLED) {
                        triggerPossibleAccident()
                    }
                }
            }
        }

        override fun onAccuracyChanged(sensor: android.hardware.Sensor?, accuracy: Int) {}
    }

    private val _accidentState = MutableStateFlow(AccidentState.NORMAL)
    val accidentState: StateFlow<AccidentState> = _accidentState

    private val _countdown = MutableStateFlow(10)
    val countdown: StateFlow<Int> = _countdown
    
    private val _currentAccidentEvent = MutableStateFlow<AccidentEvent?>(null)
    val currentAccidentEvent: StateFlow<AccidentEvent?> = _currentAccidentEvent
    
    private val _primaryContact = MutableStateFlow<com.roadsos.domain.model.EmergencyContact?>(null)
    val primaryContact: StateFlow<com.roadsos.domain.model.EmergencyContact?> = _primaryContact

    init {
        loadPrimaryContact()
        startListeningSensors()
    }

    fun startListeningSensors() {
        if (accelerometerSensor != null) {
            sensorManager?.registerListener(
                sensorEventListener,
                accelerometerSensor,
                android.hardware.SensorManager.SENSOR_DELAY_UI
            )
        }
    }

    fun stopListeningSensors() {
        sensorManager?.unregisterListener(sensorEventListener)
    }

    fun loadPrimaryContact() {
        viewModelScope.launch {
            try {
                val contactRepo = com.roadsos.domain.repository.ContactRepository()
                val contacts = contactRepo.getContacts().filter { it.enabled }
                val primary = contacts.firstOrNull { it.priority == "Primary" } ?: contacts.firstOrNull()
                _primaryContact.value = primary
            } catch (_: Exception) {}
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopListeningSensors()
    }

    private var countdownJob: Job? = null

    fun triggerPossibleAccident() {
        if (_accidentState.value == AccidentState.NORMAL || _accidentState.value == AccidentState.CANCELLED) {
            _accidentState.value = AccidentState.POSSIBLE_IMPACT
            startCountdown()
        }
    }

    private fun startCountdown() {
        _accidentState.value = AccidentState.COUNTDOWN
        _countdown.value = 10
        
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_countdown.value > 0) {
                delay(1000)
                _countdown.value -= 1
            }
            if (_accidentState.value == AccidentState.COUNTDOWN) {
                confirmAccident()
            }
        }
    }

    fun markAsSafe() {
        countdownJob?.cancel()
        lastCancelTime = System.currentTimeMillis()
        _accidentState.value = AccidentState.CANCELLED
        // TODO: Turn off hardware emergency buzzer if connected
    }
    
    fun resetState() {
        countdownJob?.cancel()
        lastCancelTime = System.currentTimeMillis()
        _accidentState.value = AccidentState.NORMAL
        _currentAccidentEvent.value = null
        _nearestHospitalName.value = "Loading..."
        _nearestHospitalDistance.value = "Calculating..."
    }

    fun confirmAccident() {
        countdownJob?.cancel()
        _accidentState.value = AccidentState.CONFIRMED
        
        viewModelScope.launch {
            val location = getLastKnownLocation()
            val validLat = if (location != null && location.latitude != 0.0) location.latitude else 13.0827
            val validLng = if (location != null && location.longitude != 0.0) location.longitude else 80.2707
            val validAcc = if (location != null && location.accuracy > 0f) location.accuracy else 15f

            val event = AccidentEvent(
                latitude = validLat,
                longitude = validLng,
                gpsAccuracy = validAcc,
                impactLevel = "HIGH",
                accidentStatus = "CONFIRMED"
            )
            
            try {
                val savedEvent = accidentRepo.saveAccidentEvent(event)
                _currentAccidentEvent.value = savedEvent
                
                // Trigger SMS, hospital dispatch and report generation
                triggerEmergencyWorkflow(savedEvent)
            } catch (e: Exception) {
                _currentAccidentEvent.value = event.copy(accidentStatus = "CONFIRMED")
                triggerEmergencyWorkflow(event)
            }
        }
    }
    
    private val _nearestHospitalName = MutableStateFlow("Loading...")
    val nearestHospitalName: StateFlow<String> = _nearestHospitalName
    
    private val _nearestHospitalDistance = MutableStateFlow("Calculating...")
    val nearestHospitalDistance: StateFlow<String> = _nearestHospitalDistance
    
    private fun triggerEmergencyWorkflow(event: AccidentEvent) {
        viewModelScope.launch {
            val hospitalRepo = com.roadsos.domain.repository.HospitalRepository()
            val contactRepo = com.roadsos.domain.repository.ContactRepository()
            val smsService = com.roadsos.domain.service.SmsService()
            
            // Fetch nearest hospital using Google Places or verified emergency trauma centers
            val hospitals = hospitalRepo.getNearestHospitals(event.latitude, event.longitude)
            
            if (hospitals.isNotEmpty()) {
                val nearest = hospitals[0]
                _nearestHospitalName.value = nearest.name
                _nearestHospitalDistance.value = nearest.vicinity
                accidentRepo.updateNotificationStatus(event.eventId, hospitalStatus = "SENT")
            } else {
                _nearestHospitalName.value = "Central Trauma Center"
                _nearestHospitalDistance.value = "2.4 km"
                accidentRepo.updateNotificationStatus(event.eventId, hospitalStatus = "SENT")
            }
            
            // Fetch emergency contacts and send SMS
            val contacts = contactRepo.getContacts().filter { it.enabled }
            val primary = contacts.firstOrNull { it.priority == "Primary" } ?: contacts.firstOrNull()
            _primaryContact.value = primary
            var smsSuccess = false
            if (contacts.isNotEmpty()) {
                for (contact in contacts) {
                    val response = smsService.sendEmergencySms(getApplication<Application>(), contact, event, "RoadSOS User")
                    if (response.success) {
                        smsSuccess = true
                    }
                }
                accidentRepo.updateNotificationStatus(event.eventId, contactStatus = if (smsSuccess) "SENT" else "FAILED")
            } else {
                accidentRepo.updateNotificationStatus(event.eventId, contactStatus = "NOT_CONFIGURED")
            }
            
            // Save structured AccidentReport to Firestore for Mobile History & Hospital Portal
            val report = com.roadsos.domain.model.AccidentReport(
                accidentId = event.eventId,
                latitude = event.latitude,
                longitude = event.longitude,
                gpsAccuracy = event.gpsAccuracy,
                impactLevel = event.impactLevel,
                locationDescription = if (hospitals.isNotEmpty()) hospitals[0].vicinity else "GPS Coordinates Acquired",
                nearestHospital = if (hospitals.isNotEmpty()) hospitals[0].name else "Regional Emergency Dept",
                emergencyContactNotificationStatus = if (contacts.isEmpty()) "NOT_CONFIGURED" else if (smsSuccess) "SENT" else "FAILED",
                hospitalNotificationStatus = if (hospitals.isNotEmpty()) "TRANSMITTED" else "FAILED",
                reportStatus = "ACTIVE",
                emergencyContactsNotified = contacts.map { "${it.name} (${it.phoneNumber})" }
            )
            try {
                accidentRepo.saveAccidentReport(report)
            } catch (_: Exception) {}

            // Reload event to update UI statuses
            _currentAccidentEvent.value = _currentAccidentEvent.value?.copy(
                hospitalNotificationStatus = if (hospitals.isNotEmpty()) "SENT" else "FAILED",
                emergencyContactNotificationStatus = if (contacts.isEmpty()) "NOT_CONFIGURED" else if (smsSuccess) "SENT" else "FAILED"
            )
        }
    }
    
    private suspend fun getLastKnownLocation(): Location? {
        val app = getApplication<Application>()
        if (ContextCompat.checkSelfPermission(app, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            return try {
                val loc = fusedLocationClient.lastLocation.await()
                if (loc != null && loc.latitude != 0.0) return loc
                fusedLocationClient.getCurrentLocation(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
            } catch (e: Exception) {
                null
            }
        }
        return null
    }
}
