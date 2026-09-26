package com.roadsos.domain.model

data class SafeRoute(
    val routeId: String = "",
    val userId: String = "",
    val source: String = "",
    val destination: String = "",
    val distance: String = "",
    val duration: String = "",
    val overallRisk: String = "LOW",
    val highRiskZoneName: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
