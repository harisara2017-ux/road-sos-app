package com.roadsos.ui.emergency

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadsos.domain.model.EmergencyContact
import com.roadsos.domain.repository.ContactRepository
import kotlinx.coroutines.launch

class ContactsViewModel : ViewModel() {
    private val repository = ContactRepository()

    private val _contacts = mutableStateOf<List<EmergencyContact>>(emptyList())
    val contacts: State<List<EmergencyContact>> = _contacts

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    private val _statusMessage = mutableStateOf<String?>(null)
    val statusMessage: State<String?> = _statusMessage

    fun sendTestSms(context: android.content.Context, contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                val smsService = com.roadsos.domain.service.SmsService()
                val dummyEvent = com.roadsos.domain.model.AccidentEvent(
                    latitude = 13.0827,
                    longitude = 80.2707,
                    impactLevel = "HIGH (TEST ALERT)"
                )
                val resp = smsService.sendEmergencySms(context, contact, dummyEvent, "RoadSOS Test")
                if (resp.success) {
                    _statusMessage.value = "Test SMS sent to ${contact.name} (${contact.phoneNumber})!"
                } else {
                    _error.value = "SMS failed: ${resp.error}"
                }
            } catch (e: Exception) {
                _error.value = "SMS error: ${e.message}"
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun loadContacts() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _contacts.value = repository.getContacts()
            } catch (e: Exception) {
                _error.value = "Failed to load contacts"
            }
            _isLoading.value = false
        }
    }

    fun addContact(name: String, phone: String, relationship: String, priority: String) {
        viewModelScope.launch {
            try {
                val newContact = EmergencyContact(
                    name = name,
                    phoneNumber = phone,
                    relationship = relationship,
                    priority = priority,
                    enabled = true
                )
                repository.addContact(newContact)
                loadContacts()
            } catch (e: Exception) {
                _error.value = "Failed to add contact"
            }
        }
    }

    fun updateContact(contact: EmergencyContact) {
        viewModelScope.launch {
            try {
                repository.updateContact(contact)
                loadContacts()
            } catch (e: Exception) {
                _error.value = "Failed to update contact"
            }
        }
    }

    fun toggleContact(contact: EmergencyContact, enabled: Boolean) {
        viewModelScope.launch {
            try {
                repository.updateContact(contact.copy(enabled = enabled))
                loadContacts()
            } catch (e: Exception) {
                _error.value = "Failed to update contact"
            }
        }
    }

    fun deleteContact(contactId: String) {
        viewModelScope.launch {
            try {
                repository.deleteContact(contactId)
                loadContacts()
            } catch (e: Exception) {
                _error.value = "Failed to delete contact"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}
