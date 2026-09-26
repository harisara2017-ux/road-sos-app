package com.roadsos.domain.model

data class AccidentRecord(
    val accidentId: String,
    val latitude: Double,
    val longitude: Double,
    val date: String,
    val severity: String, // minor, major, fatal
    val cause: String,
    val riskScore: Double
)

enum class RiskLevel {
    LOW, MEDIUM, HIGH, CRITICAL
}

data class AccidentZone(
    val zoneId: String,
    val centerLatitude: Double,
    val centerLongitude: Double,
    val accidentCount: Int,
    val riskLevel: RiskLevel,
    val historicalPeriod: String // e.g. "2023-2025"
)
