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
    
    private var pendingUserId: String? = null
    private var pendingKitId: String? = null
    
    fun processBarcode(barcodeData: String) {
        val trimmedData = barcodeData.trim()
        
        // Simple validation for barcode data
        if (!isValidBarcodeData(trimmedData)) {
            _statusMessage.value = "Invalid barcode format"
            return
        }
        
        when (_scanState.value) {
            ScanState.IDLE -> {
                // First scan - determine if it's user or kit based on content
                if (isUserBarcode(trimmedData)) {
                    pendingUserId = trimmedData
                    _scanState.value = ScanState.USER_SCANNED
                    _statusMessage.value = "User scanned: $trimmedData\nScan kit barcode"
                    _scanSuccess.value = true
                } else {
                    pendingKitId = trimmedData
                    _scanState.value = ScanState.KIT_SCANNED
                    _statusMessage.value = "Kit scanned: $trimmedData\nScan user barcode"
                    _scanSuccess.value = true
                }
            }
            
            ScanState.USER_SCANNED -> {
                if (isUserBarcode(trimmedData)) {
                    // Another user barcode - replace the pending one
                    pendingUserId = trimmedData
                    _statusMessage.value = "User updated: $trimmedData\nScan kit barcode"
                    _scanSuccess.value = true
                } else {
                    // Kit barcode - complete the checkout
                    pendingKitId = trimmedData
                    _scanSuccess.value = true
                    completeCheckout()
                }
            }
            
            ScanState.KIT_SCANNED -> {
                if (isUserBarcode(trimmedData)) {
                    // User barcode - complete the checkout
                    pendingUserId = trimmedData
                    _scanSuccess.value = true
                    completeCheckout()
                } else {
                    // Another kit barcode - replace the pending one
                    pendingKitId = trimmedData
                    _statusMessage.value = "Kit updated: $trimmedData\nScan user barcode"
                    _scanSuccess.value = true
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
        // Accept alphanumeric data and common barcode characters (hyphens, periods)
        return data.isNotEmpty() && data.matches(Regex("^[A-Za-z0-9._-]+$"))
    }
    
    private fun isUserBarcode(data: String): Boolean {
        // Simple heuristic: assume user codes start with 'U' or 'USER'
        // This can be customized based on actual barcode format
        return data.uppercase().startsWith("U") || data.uppercase().startsWith("USER")
    }
    
    fun clearState() {
        resetScanState()
    }
}
