package com.joeycarlson.qrscanner.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.joeycarlson.qrscanner.data.CheckoutRepository
import kotlinx.coroutines.launch

enum class ScanState {
    IDLE,           // Ready to scan either user or kit
    USER_SCANNED,   // User scanned, waiting for kit
    KIT_SCANNED     // Kit scanned, waiting for user
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = CheckoutRepository(application)
    
    private val _scanState = MutableLiveData<ScanState>(ScanState.IDLE)
    val scanState: LiveData<ScanState> = _scanState
    
    private val _statusMessage = MutableLiveData<String>()
    val statusMessage: LiveData<String> = _statusMessage
    
    private val _isScanning = MutableLiveData<Boolean>(true)
    val isScanning: LiveData<Boolean> = _isScanning
    
    private var pendingUserId: String? = null
    private var pendingKitId: String? = null
    
    fun processQRCode(qrData: String) {
        val trimmedData = qrData.trim()
        
        // Simple validation for alphanumeric strings
        if (!isValidQRData(trimmedData)) {
            _statusMessage.value = "Invalid QR code format"
            return
        }
        
        when (_scanState.value) {
            ScanState.IDLE -> {
                // First scan - determine if it's user or kit based on content
                if (isUserQR(trimmedData)) {
                    pendingUserId = trimmedData
                    _scanState.value = ScanState.USER_SCANNED
                    _statusMessage.value = "User scanned: $trimmedData\nScan kit QR code"
                } else {
                    pendingKitId = trimmedData
                    _scanState.value = ScanState.KIT_SCANNED
                    _statusMessage.value = "Kit scanned: $trimmedData\nScan user QR code"
                }
            }
            
            ScanState.USER_SCANNED -> {
                if (isUserQR(trimmedData)) {
                    // Another user QR - replace the pending one
                    pendingUserId = trimmedData
                    _statusMessage.value = "User updated: $trimmedData\nScan kit QR code"
                } else {
                    // Kit QR - complete the checkout
                    pendingKitId = trimmedData
                    completeCheckout()
                }
            }
            
            ScanState.KIT_SCANNED -> {
                if (isUserQR(trimmedData)) {
                    // User QR - complete the checkout
                    pendingUserId = trimmedData
                    completeCheckout()
                } else {
                    // Another kit QR - replace the pending one
                    pendingKitId = trimmedData
                    _statusMessage.value = "Kit updated: $trimmedData\nScan user QR code"
                }
            }
            
            null -> {
                _scanState.value = ScanState.IDLE
                processQRCode(qrData)
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
    
    private fun isValidQRData(data: String): Boolean {
        return data.isNotEmpty() && data.matches(Regex("^[A-Za-z0-9]+$"))
    }
    
    private fun isUserQR(data: String): Boolean {
        // Simple heuristic: assume user QRs start with 'U' or 'USER'
        // This can be customized based on actual QR code format
        return data.uppercase().startsWith("U") || data.uppercase().startsWith("USER")
    }
    
    fun clearState() {
        resetScanState()
    }
}
