package com.roadsos.domain.model

data class AccidentReport(
    val reportId: String = "",
    val accidentId: String = "",
    val userId: String = "",
    val userName: String = "RoadSOS Rider",
    val userEmail: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gpsAccuracy: Float = 0f,
    val locationDescription: String = "Coordinates Acquired",
    val detectionStatus: String = "HIGH_CONFIDENCE_MULTI_SENSOR",
    val verificationStatus: String = "TIMER_ELAPSED_NO_RESPONSE",
    val impactLevel: String = "HIGH", // CRITICAL, HIGH, MODERATE
    val sensorTelemetry: SensorTelemetry = SensorTelemetry(),
    val emergencyContactNotificationStatus: String = "SENT", // SENT, FAILED, NOT_CONFIGURED
    val hospitalNotificationStatus: String = "TRANSMITTED", // TRANSMITTED, ACKNOWLEDGED, DISPATCHED
    val reportStatus: String = "ACTIVE", // ACTIVE, AMBULANCE_DISPATCHED, ADMITTED, RESOLVED
    val emergencyContactsNotified: List<String> = emptyList(),
    val nearestHospital: String = "Emergency Department"
)

data class SensorTelemetry(
    val peakGForce: Double = 3.8,
    val angularVelocityDps: Double = 145.2,
    val preImpactSpeedKmh: Double = 42.0,
    val postImpactSpeedKmh: Double = 0.0,
    val phoneOrientationTiltDeg: Double = 78.5,
    val verifiedStationaryDurationSec: Int = 10
)
