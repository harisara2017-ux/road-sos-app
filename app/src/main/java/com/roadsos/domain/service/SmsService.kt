package com.roadsos.domain.service

import android.content.Context
import android.telephony.SmsManager
import android.util.Log
import com.roadsos.domain.model.AccidentEvent
import com.roadsos.domain.model.EmergencyContact

data class SmsResponse(
    val success: Boolean,
    val error: String?
)

class SmsService {

    fun sendEmergencySms(
        context: Context,
        contact: EmergencyContact,
        event: AccidentEvent,
        userName: String
    ): SmsResponse {
        val cleanNumber = contact.phoneNumber.replace(" ", "").replace("-", "").trim()
        val mapLink = "https://maps.google.com/?q=${event.latitude},${event.longitude}"
        
        val messageBody = """
            🚨 ROADSOS EMERGENCY ALERT
            A road accident has been detected.
            
            Person: $userName
            Location: $mapLink
            Impact: ${event.impactLevel}
            
            Please check on them immediately!
        """.trimIndent()

        Log.d("RoadSOS_SMS", "Preparing SMS for ${contact.name} -> $cleanNumber")

        return try {
            val smsManager: SmsManager = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            
            val parts = smsManager.divideMessage(messageBody)
            smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
            Log.d("RoadSOS_SMS", "Successfully called sendMultipartTextMessage for $cleanNumber")
            
            SmsResponse(success = true, error = null)
        } catch (e: Exception) {
            Log.e("RoadSOS_SMS", "SMS dispatch failed: ${e.message}", e)
            SmsResponse(success = false, error = e.message ?: "Failed to send SMS")
        }
    }
}
