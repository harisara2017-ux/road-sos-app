package com.roadsos.domain.model

data class EmergencyContact(
    val contactId: String = "",
    val name: String = "",
    val phoneNumber: String = "",
    val relationship: String = "",
    val priority: String = "Primary", // Primary, Secondary, Other
    val enabled: Boolean = true
)
