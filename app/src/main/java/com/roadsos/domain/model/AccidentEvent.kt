package com.roadsos.domain.model

data class AccidentEvent(
    val eventId: String = "",
    val userId: String = "",
    val deviceId: String = "arduino_uno_1",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val gpsAccuracy: Float = 0f,
    val timestamp: Long = System.currentTimeMillis(),
    val impactLevel: String = "UNKNOWN",
    val sensorData: String = "{}",
    val accidentStatus: String = "CONFIRMED", // CONFIRMED, CANCELLED
    val emergencyContactNotificationStatus: String = "PENDING", // PENDING, SENDING, SENT, FAILED, NOT_CONFIGURED
    val hospitalNotificationStatus: String = "PENDING"
)
