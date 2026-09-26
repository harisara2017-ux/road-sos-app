package com.roadsos.domain.model

data class HospitalContact(
    val hospitalId: String = "",
    val hospitalName: String = "",
    val phoneNumber: String = "",
    val enabled: Boolean = true,
    val verified: Boolean = false // Must be verified by backend/admin
)
