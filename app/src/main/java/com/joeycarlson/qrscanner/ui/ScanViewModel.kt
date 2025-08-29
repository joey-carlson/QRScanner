package com.joeycarlson.qrscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joeycarlson.qrscanner.data.CheckoutRepository
import com.joeycarlson.qrscanner.util.BarcodeValidator
import com.joeycarlson.qrscanner.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

enum class ScanState {
    IDLE,           // Ready to scan either user or kit
    USER_SCANNED,   // User scanned, waiting for kit
    KIT_SCANNED     // Kit scanned, waiting for user
}

class ScanViewModel(
    application: Application,
    private val repository: CheckoutRepository
) : AndroidViewModel(application) {
    
    private val _scanState = MutableStateFlow(ScanState.IDLE)
    val scanState: StateFlow<ScanState> = _scanState.asStateFlow()
    
    private val _statusMessage = MutableStateFlow("Ready to scan")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _scanSuccess = MutableStateFlow(false)
    val scanSuccess: StateFlow<Boolean> = _scanSuccess.asStateFlow()
    
    private val _scanFailure = MutableStateFlow(false)
    val scanFailure: StateFlow<Boolean> = _scanFailure.asStateFlow()
    
    private val _showUndoButton = MutableStateFlow(false)
    val showUndoButton: StateFlow<Boolean> = _showUndoButton.asStateFlow()
    
    private val _showCheckoutConfirmation = MutableStateFlow(false)
    val showCheckoutConfirmation: StateFlow<Boolean> = _showCheckoutConfirmation.asStateFlow()
    
    private val _checkoutConfirmationMessage = MutableStateFlow("")
    val checkoutConfirmationMessage: StateFlow<String> = _checkoutConfirmationMessage.asStateFlow()
    
    private var pendingUserId: String? = null
    private var pendingKitId: String? = null
    private var lastCheckoutUserId: String? = null
    private var lastCheckoutKitId: String? = null
    private var undoTimerJob: Job? = null
    
    fun processBarcode(barcodeData: String) {
        // Enhanced validation with format detection
        val validationResult = BarcodeValidator.validateBarcodeData(barcodeData)
        
        if (!validationResult.isValid) {
            _statusMessage.value = validationResult.errorMessage ?: "Invalid barcode format"
            _scanFailure.value = true
            return
        }
        
        val sanitizedData = validationResult.sanitizedData
        val formatName = getFormatDisplayName(validationResult.format)
        val barcodeType = getBarcodeType(sanitizedData)
        
        when (_scanState.value) {
            ScanState.IDLE -> {
                when (barcodeType) {
                    "USER" -> {
                        pendingUserId = sanitizedData
                        _scanState.value = ScanState.USER_SCANNED
                        _statusMessage.value = "User scanned ($formatName): $sanitizedData\nScan kit barcode"
                        _scanSuccess.value = true
                    }
                    "KIT" -> {
                        pendingKitId = sanitizedData
                        _scanState.value = ScanState.KIT_SCANNED
                        _statusMessage.value = "Kit scanned ($formatName): $sanitizedData\nScan user barcode"
                        _scanSuccess.value = true
                    }
                    "OTHER" -> {
                        // Save OTHER type immediately
                        saveOtherEntry(sanitizedData, formatName)
                        _scanSuccess.value = true
                    }
                }
            }
            
            ScanState.USER_SCANNED -> {
                when (barcodeType) {
                    "USER" -> {
                        // Another user barcode - replace the pending one
                        pendingUserId = sanitizedData
                        _statusMessage.value = "User updated ($formatName): $sanitizedData\nScan kit barcode"
                        _scanSuccess.value = true
                    }
                    "KIT" -> {
                        // Kit barcode - complete the checkout
                        pendingKitId = sanitizedData
                        _scanSuccess.value = true
                        completeCheckout()
                    }
                    "OTHER" -> {
                        // Save OTHER type immediately
                        saveOtherEntry(sanitizedData, formatName)
                        _scanSuccess.value = true
                    }
                }
            }
            
            ScanState.KIT_SCANNED -> {
                when (barcodeType) {
                    "USER" -> {
                        // User barcode - complete the checkout
                        pendingUserId = sanitizedData
                        _scanSuccess.value = true
                        completeCheckout()
                    }
                    "KIT" -> {
                        // Another kit barcode - replace the pending one
                        pendingKitId = sanitizedData
                        _statusMessage.value = "Kit updated ($formatName): $sanitizedData\nScan user barcode"
                        _scanSuccess.value = true
                    }
                    "OTHER" -> {
                        // Save OTHER type immediately
                        saveOtherEntry(sanitizedData, formatName)
                        _scanSuccess.value = true
                    }
                }
            }
            
        }
    }
    
    private fun completeCheckout() {
        val userId = pendingUserId
        val kitId = pendingKitId
        
        if (userId != null && kitId != null) {
            _isScanning.value = false
            _statusMessage.value = "Processing checkout..."
            
            viewModelScope.launch {
                val success = repository.saveCheckout(userId, kitId)
                if (success) {
                    _statusMessage.value = "✓ User $userId checked out kit $kitId"
                    
                    // Show visual confirmation overlay
                    _checkoutConfirmationMessage.value = "CHECKOUT COMPLETE\nUser: $userId\nKit: $kitId"
                    _showCheckoutConfirmation.value = true
                    
                    // Hide confirmation after 2 seconds
                    kotlinx.coroutines.delay(2000)
                    _showCheckoutConfirmation.value = false
                    
                    // Store checkout info for undo and show undo button
                    lastCheckoutUserId = userId
                    lastCheckoutKitId = kitId
                    showUndoButtonWithTimer()
                } else {
                    _statusMessage.value = "✗ Failed to save checkout"
                }
                
                // Reset state after brief delay
                kotlinx.coroutines.delay(1000)
                resetScanState()
            }
        }
    }
    
    private fun resetScanState() {
        pendingUserId = null
        pendingKitId = null
        _scanState.value = ScanState.IDLE
        _statusMessage.value = "Ready to scan"
        _isScanning.value = true
    }
    
    
    private fun getBarcodeType(data: String): String {
        val upperData = data.uppercase()
        return when {
            upperData.startsWith("U") || upperData.startsWith("USER") -> "USER"
            upperData.startsWith("K") || upperData.startsWith("KIT") -> "KIT"
            else -> "OTHER"
        }
    }
    
    private fun getFormatDisplayName(format: BarcodeValidator.BarcodeFormat): String {
        return when (format) {
            BarcodeValidator.BarcodeFormat.QR_CODE -> "QR"
            BarcodeValidator.BarcodeFormat.CODE_128 -> "Code 128"
            BarcodeValidator.BarcodeFormat.CODE_39 -> "Code 39"
            BarcodeValidator.BarcodeFormat.CODE_93 -> "Code 93"
            BarcodeValidator.BarcodeFormat.UPC_A -> "UPC-A"
            BarcodeValidator.BarcodeFormat.UPC_E -> "UPC-E"
            BarcodeValidator.BarcodeFormat.EAN_13 -> "EAN-13"
            BarcodeValidator.BarcodeFormat.EAN_8 -> "EAN-8"
            BarcodeValidator.BarcodeFormat.UNKNOWN -> "Unknown"
        }
    }
    
    private fun isUserBarcode(data: String): Boolean {
        return getBarcodeType(data) == "USER"
    }
    
    private fun saveOtherEntry(value: String, formatName: String) {
        _isScanning.value = false
        _statusMessage.value = "Processing other entry..."
        
        viewModelScope.launch {
            val success = repository.saveOtherEntry(value)
            if (success) {
                _statusMessage.value = "✓ Other entry saved ($formatName): $value"
            } else {
                _statusMessage.value = "✗ Failed to save other entry"
            }
            
            // Reset state after brief delay
            kotlinx.coroutines.delay(1000)
            resetScanState()
        }
    }
    
    private fun showUndoButtonWithTimer() {
        // Cancel any existing timer
        undoTimerJob?.cancel()
        
        // Show undo button
        _showUndoButton.value = true
        
        // Start timer to hide button after timeout
        undoTimerJob = viewModelScope.launch {
            kotlinx.coroutines.delay(Constants.UNDO_BUTTON_TIMEOUT)
            hideUndoButton()
        }
    }
    
    private fun hideUndoButton() {
        _showUndoButton.value = false
        lastCheckoutUserId = null
        lastCheckoutKitId = null
        undoTimerJob?.cancel()
        undoTimerJob = null
    }
    
    fun undoLastCheckout() {
        val userId = lastCheckoutUserId
        val kitId = lastCheckoutKitId
        
        if (userId != null && kitId != null) {
            viewModelScope.launch {
                val success = repository.deleteLastCheckout()
                if (success) {
                    _statusMessage.value = "✓ Undid checkout: User $userId / Kit $kitId"
                    hideUndoButton()
                    
                    // Reset state after brief delay
                    kotlinx.coroutines.delay(2000)
                    resetScanState()
                } else {
                    _statusMessage.value = "✗ Failed to undo checkout"
                    hideUndoButton()
                }
            }
        } else {
            hideUndoButton()
        }
    }
    
    fun clearState() {
        hideUndoButton()
        resetScanState()
    }
}
