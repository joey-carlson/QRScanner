package com.joeycarlson.qrscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joeycarlson.qrscanner.data.CheckInRepository
import com.joeycarlson.qrscanner.util.BarcodeValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Check In feature.
 * Simplified version that only handles kit scanning (no user scanning required).
 */
class CheckInViewModel(
    application: Application,
    private val repository: CheckInRepository
) : AndroidViewModel(application) {
    
    // UI state flows
    private val _statusMessage = MutableStateFlow("Ready to scan kit")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _scanSuccess = MutableStateFlow(false)
    val scanSuccess: StateFlow<Boolean> = _scanSuccess.asStateFlow()
    
    private val _scanFailure = MutableStateFlow(false)
    val scanFailure: StateFlow<Boolean> = _scanFailure.asStateFlow()
    
    private val _showUndoButton = MutableStateFlow(false)
    val showUndoButton: StateFlow<Boolean> = _showUndoButton.asStateFlow()
    
    private val _showCheckInConfirmation = MutableStateFlow(false)
    val showCheckInConfirmation: StateFlow<Boolean> = _showCheckInConfirmation.asStateFlow()
    
    private val _checkInConfirmationMessage = MutableStateFlow("")
    val checkInConfirmationMessage: StateFlow<String> = _checkInConfirmationMessage.asStateFlow()
    
    // Track last scanned kit for potential undo
    private var lastScannedKitId: String? = null
    
    /**
     * Process a scanned barcode - simplified for kit-only scanning
     */
    fun processBarcode(barcodeData: String) {
        if (!_isScanning.value) return
        
        viewModelScope.launch {
            // Validate the barcode format
            val validationResult = BarcodeValidator.validateBarcodeData(barcodeData)
            if (!validationResult.isValid) {
                handleInvalidBarcode()
                return@launch
            }
            
            // Process the kit check-in
            processKitCheckIn(barcodeData)
        }
    }
    
    private suspend fun processKitCheckIn(kitId: String) {
        _isScanning.value = false
        _statusMessage.value = "Processing check-in..."
        
        val success = repository.saveCheckIn(kitId)
        
        if (success) {
            lastScannedKitId = kitId
            _statusMessage.value = "✓ Kit $kitId checked in"
            _scanSuccess.value = true
            _showUndoButton.value = true
            
            // Show confirmation overlay
            _checkInConfirmationMessage.value = "CHECK-IN COMPLETE\nKit $kitId\nChecked In"
            _showCheckInConfirmation.value = true
            
            // Hide confirmation after delay
            kotlinx.coroutines.delay(2000)
            _showCheckInConfirmation.value = false
            
            // Return to ready state
            kotlinx.coroutines.delay(1000)
            clearState()
        } else {
            _statusMessage.value = "✗ Failed to save check-in"
            _scanFailure.value = true
            kotlinx.coroutines.delay(2000)
            clearState()
        }
    }
    
    private fun handleInvalidBarcode() {
        _scanFailure.value = true
        _statusMessage.value = "Invalid QR code format"
        
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            clearState()
        }
    }
    
    /**
     * Clear state and return to ready to scan
     */
    fun clearState() {
        _statusMessage.value = "Ready to scan kit"
        _isScanning.value = true
        _scanSuccess.value = false
        _scanFailure.value = false
    }
    
    /**
     * Undo the last check-in
     */
    fun undoLastCheckIn() {
        viewModelScope.launch {
            val success = repository.deleteLastCheckIn()
            
            if (success) {
                _statusMessage.value = "Check-in undone"
                _showUndoButton.value = false
                lastScannedKitId = null
                
                kotlinx.coroutines.delay(2000)
                clearState()
            } else {
                _statusMessage.value = "Failed to undo check-in"
                kotlinx.coroutines.delay(2000)
                clearState()
            }
        }
    }
}
