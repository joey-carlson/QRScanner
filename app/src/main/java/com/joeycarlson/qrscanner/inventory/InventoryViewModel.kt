package com.joeycarlson.qrscanner.inventory

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.joeycarlson.qrscanner.data.InventoryRepository
import com.joeycarlson.qrscanner.ocr.ScanMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Inventory Management feature.
 * Manages scanning devices for inventory tracking.
 */
class InventoryViewModel(
    private val repository: InventoryRepository
) : ViewModel() {
    
    // UI state flows - using LiveData for compatibility with the Activity
    private val _statusMessage = MutableStateFlow("Select component type and start scanning")
    val statusMessage: LiveData<String> = _statusMessage.asLiveData()
    
    private val _isScanning = MutableStateFlow(true)
    val isScanning: LiveData<Boolean> = _isScanning.asLiveData()
    
    private val _scanSuccess = MutableStateFlow(false)
    val scanSuccess: LiveData<Boolean> = _scanSuccess.asLiveData()
    
    private val _scanFailure = MutableStateFlow(false)
    val scanFailure: LiveData<Boolean> = _scanFailure.asLiveData()
    
    private val _scanCount = MutableStateFlow(0)
    val scanCount: LiveData<Int> = _scanCount.asLiveData()
    
    private val _currentComponentType = MutableStateFlow(ComponentType.GLASSES)
    val currentComponentType: LiveData<ComponentType> = _currentComponentType.asLiveData()
    
    // Current scan mode (barcode or OCR)
    private var currentScanMode = ScanMode.BARCODE_ONLY
    
    /**
     * Set the current component type for scanning
     */
    fun setComponentType(type: ComponentType) {
        _currentComponentType.value = type
        _statusMessage.value = "Scanning ${type.displayName}"
    }
    
    /**
     * Set the current scan mode
     */
    fun setScanMode(mode: ScanMode) {
        currentScanMode = mode
    }
    
    /**
     * Process a scanned device ID
     */
    fun processScan(deviceId: String) {
        if (!_isScanning.value || deviceId.isBlank()) return
        
        viewModelScope.launch {
            _isScanning.value = false
            
            // Check if device is already scanned
            if (repository.isDeviceAlreadyScanned(deviceId)) {
                handleDuplicateScan(deviceId)
                return@launch
            }
            
            // Add the device to inventory
            val success = repository.addInventoryRecord(
                deviceId = deviceId,
                componentType = _currentComponentType.value,
                scanMode = currentScanMode
            )
            
            if (success) {
                handleSuccessfulScan(deviceId)
            } else {
                handleFailedScan()
            }
        }
    }
    
    private suspend fun handleSuccessfulScan(deviceId: String) {
        _scanSuccess.value = true
        _scanCount.value = repository.getRecordCount()
        _statusMessage.value = "✓ ${_currentComponentType.value.displayName}: $deviceId"
        
        // Reset success flag after a delay
        kotlinx.coroutines.delay(100)
        _scanSuccess.value = false
        
        // Resume scanning after a brief delay
        kotlinx.coroutines.delay(1500)
        _isScanning.value = true
        _statusMessage.value = "Scanning ${_currentComponentType.value.displayName}"
    }
    
    private suspend fun handleDuplicateScan(deviceId: String) {
        _scanFailure.value = true
        _statusMessage.value = "✗ Device already scanned: $deviceId"
        
        // Reset failure flag after a delay
        kotlinx.coroutines.delay(100)
        _scanFailure.value = false
        
        // Resume scanning after a longer delay
        kotlinx.coroutines.delay(2000)
        _isScanning.value = true
        _statusMessage.value = "Scanning ${_currentComponentType.value.displayName}"
    }
    
    private suspend fun handleFailedScan() {
        _scanFailure.value = true
        _statusMessage.value = "✗ Failed to save device"
        
        // Reset failure flag after a delay
        kotlinx.coroutines.delay(100)
        _scanFailure.value = false
        
        // Resume scanning after a delay
        kotlinx.coroutines.delay(2000)
        _isScanning.value = true
        _statusMessage.value = "Scanning ${_currentComponentType.value.displayName}"
    }
    
    /**
     * Export the current inventory data
     */
    fun exportInventory() {
        viewModelScope.launch {
            _isScanning.value = false
            _statusMessage.value = "Preparing export..."
            
            // The actual export will be handled by the export system
            // Just prepare the data here
            val jsonData = repository.exportInventoryData()
            if (jsonData != null) {
                _statusMessage.value = "Ready to export ${repository.getRecordCount()} devices"
            } else {
                _statusMessage.value = "Export preparation failed"
            }
        }
    }
    
    /**
     * Clear all inventory records
     */
    fun clearInventory() {
        viewModelScope.launch {
            _isScanning.value = false
            
            repository.clearInventory()
            _scanCount.value = 0
            _statusMessage.value = "Inventory cleared"
            
            // Resume scanning after a delay
            kotlinx.coroutines.delay(1500)
            _isScanning.value = true
            _statusMessage.value = "Scanning ${_currentComponentType.value.displayName}"
        }
    }
    
    /**
     * Get the breakdown of devices by component type
     */
    fun getInventorySummary(): String {
        val glassesCount = repository.getCountByType(ComponentType.GLASSES)
        val controllerCount = repository.getCountByType(ComponentType.CONTROLLER)
        val batteryCount = repository.getCountByType(ComponentType.BATTERY)
        
        return buildString {
            append("Inventory Summary:\n")
            append("Glasses: $glassesCount\n")
            append("Controllers: $controllerCount\n")
            append("Batteries: $batteryCount\n")
            append("Total: ${repository.getRecordCount()}")
        }
    }
    
    /**
     * Check if there are unsaved changes
     */
    fun hasUnsavedChanges(): Boolean = repository.hasUnsavedChanges()
}
