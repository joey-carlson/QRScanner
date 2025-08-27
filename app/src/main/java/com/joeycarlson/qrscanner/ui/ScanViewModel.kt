package com.joeycarlson.qrscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joeycarlson.qrscanner.data.CheckoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    
    private var pendingUserId: String? = null
    private var pendingKitId: String? = null
    
    fun processBarcode(barcodeData: String) {
        val trimmedData = barcodeData.trim()
        
        // Simple validation for barcode data
        if (!isValidBarcodeData(trimmedData)) {
            _statusMessage.value = "Invalid barcode format"
            _scanFailure.value = true
            return
        }
        
        val barcodeType = getBarcodeType(trimmedData)
        
        when (_scanState.value) {
            ScanState.IDLE -> {
                when (barcodeType) {
                    "USER" -> {
                        pendingUserId = trimmedData
                        _scanState.value = ScanState.USER_SCANNED
                        _statusMessage.value = "User scanned: $trimmedData\nScan kit barcode"
                        _scanSuccess.value = true
                    }
                    "KIT" -> {
                        pendingKitId = trimmedData
                        _scanState.value = ScanState.KIT_SCANNED
                        _statusMessage.value = "Kit scanned: $trimmedData\nScan user barcode"
                        _scanSuccess.value = true
                    }
                    "OTHER" -> {
                        // Save OTHER type immediately
                        saveOtherEntry(trimmedData)
                        _scanSuccess.value = true
                    }
                }
            }
            
            ScanState.USER_SCANNED -> {
                when (barcodeType) {
                    "USER" -> {
                        // Another user barcode - replace the pending one
                        pendingUserId = trimmedData
                        _statusMessage.value = "User updated: $trimmedData\nScan kit barcode"
                        _scanSuccess.value = true
                    }
                    "KIT" -> {
                        // Kit barcode - complete the checkout
                        pendingKitId = trimmedData
                        _scanSuccess.value = true
                        completeCheckout()
                    }
                    "OTHER" -> {
                        // Save OTHER type immediately
                        saveOtherEntry(trimmedData)
                        _scanSuccess.value = true
                    }
                }
            }
            
            ScanState.KIT_SCANNED -> {
                when (barcodeType) {
                    "USER" -> {
                        // User barcode - complete the checkout
                        pendingUserId = trimmedData
                        _scanSuccess.value = true
                        completeCheckout()
                    }
                    "KIT" -> {
                        // Another kit barcode - replace the pending one
                        pendingKitId = trimmedData
                        _statusMessage.value = "Kit updated: $trimmedData\nScan user barcode"
                        _scanSuccess.value = true
                    }
                    "OTHER" -> {
                        // Save OTHER type immediately
                        saveOtherEntry(trimmedData)
                        _scanSuccess.value = true
                    }
                }
            }
            
            null -> {
                _scanState.value = ScanState.IDLE
                processBarcode(barcodeData)
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
    
    private fun isValidBarcodeData(data: String): Boolean {
        // Security validations to prevent injection attacks
        if (data.isEmpty()) return false
        
        // Length limit to prevent buffer overflow and performance issues
        if (data.length > 200) return false
        
        // Strict character whitelist - only alphanumeric and basic symbols
        // Excludes potentially dangerous characters: quotes, brackets, semicolons, etc.
        if (!data.matches(Regex("^[A-Za-z0-9._-]+$"))) return false
        
        // Prevent common injection patterns
        val dangerousPatterns = listOf(
            "script", "javascript", "vbscript", "onload", "onerror",
            "alert", "eval", "document", "window", "location",
            "<%", "%>", "<?", "?>", "{{", "}}", "${", "}",
            "drop", "delete", "insert", "update", "select",
            "union", "exec", "execute", "xp_", "sp_"
        )
        
        val lowerData = data.lowercase()
        if (dangerousPatterns.any { lowerData.contains(it) }) return false
        
        return true
    }
    
    private fun getBarcodeType(data: String): String {
        val upperData = data.uppercase()
        return when {
            upperData.startsWith("U") || upperData.startsWith("USER") -> "USER"
            upperData.startsWith("K") || upperData.startsWith("KIT") -> "KIT"
            else -> "OTHER"
        }
    }
    
    private fun isUserBarcode(data: String): Boolean {
        return getBarcodeType(data) == "USER"
    }
    
    private fun saveOtherEntry(value: String) {
        _isScanning.value = false
        _statusMessage.value = "Processing other entry..."
        
        viewModelScope.launch {
            val success = repository.saveOtherEntry(value)
            if (success) {
                _statusMessage.value = "✓ Other entry saved: $value"
            } else {
                _statusMessage.value = "✗ Failed to save other entry"
            }
            
            // Reset state after brief delay
            kotlinx.coroutines.delay(1000)
            resetScanState()
        }
    }
    
    fun clearState() {
        resetScanState()
    }
}
