package com.roadsos.ui.reports

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.roadsos.domain.model.AccidentReport
import com.roadsos.domain.repository.AccidentEventRepository
import kotlinx.coroutines.launch

class AccidentReportsViewModel : ViewModel() {
    private val repository = AccidentEventRepository()

    private val _reports = mutableStateOf<List<AccidentReport>>(emptyList())
    val reports: State<List<AccidentReport>> = _reports

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> = _isLoading

    private val _selectedReport = mutableStateOf<AccidentReport?>(null)
    val selectedReport: State<AccidentReport?> = _selectedReport

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> = _error

    fun loadReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _reports.value = repository.getUserAccidentReports()
            } catch (e: Exception) {
                _error.value = "Failed to load accident reports: ${e.localizedMessage}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectReport(report: AccidentReport?) {
        _selectedReport.value = report
    }
}
