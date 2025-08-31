package com.joeycarlson.qrscanner.kitbundle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.data.KitBundle
import com.joeycarlson.qrscanner.data.KitRepository
import com.joeycarlson.qrscanner.util.BarcodeValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KitBundleViewModel(
    application: Application,
    private val repository: KitRepository
) : AndroidViewModel(application) {
    
    // Component types in order
    private val componentTypes = listOf(
        "Glasses",
        "Controller",
        "Battery 01",
        "Battery 02",
        "Battery 03",
        "Pads",
        "Unused 01",
        "Unused 02"
    )
    
    // UI state flows
    private val _statusMessage = MutableStateFlow("Scan Kit QR Code")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()
    
    private val _instructionText = MutableStateFlow("Position the kit QR code within the frame")
    val instructionText: StateFlow<String> = _instructionText.asStateFlow()
    
    private val _isScanning = MutableStateFlow(true)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()
    
    private val _scanSuccess = MutableStateFlow(false)
    val scanSuccess: StateFlow<Boolean> = _scanSuccess.asStateFlow()
    
    private val _scanFailure = MutableStateFlow(false)
    val scanFailure: StateFlow<Boolean> = _scanFailure.asStateFlow()
    
    private val _showSaveButton = MutableStateFlow(false)
    val showSaveButton: StateFlow<Boolean> = _showSaveButton.asStateFlow()
    
    private val _showSkipButton = MutableStateFlow(false)
    val showSkipButton: StateFlow<Boolean> = _showSkipButton.asStateFlow()
    
    private val _showBundleConfirmation = MutableStateFlow(false)
    val showBundleConfirmation: StateFlow<Boolean> = _showBundleConfirmation.asStateFlow()
    
    private val _bundleConfirmationMessage = MutableStateFlow("")
    val bundleConfirmationMessage: StateFlow<String> = _bundleConfirmationMessage.asStateFlow()
    
    private val _componentSummary = MutableStateFlow("")
    val componentSummary: StateFlow<String> = _componentSummary.asStateFlow()
    
    // Bundle state
    private var baseKitCode: String? = null
    private var currentComponentIndex = -1
    private val scannedComponents = mutableMapOf<String, String>()
    
    fun processBarcode(barcodeData: String) {
        if (!_isScanning.value) return
        
        viewModelScope.launch {
            val validationResult = BarcodeValidator.validateBarcodeData(barcodeData)
            
            if (!validationResult.isValid) {
                handleInvalidBarcode(validationResult.errorMessage ?: "Invalid barcode format")
                return@launch
            }
            
            val sanitizedData = validationResult.sanitizedData
            
            // Temporarily disable scanning
            _isScanning.value = false
            
            if (baseKitCode == null) {
                // First scan - expecting kit code
                handleKitScan(sanitizedData)
            } else {
                // Subsequent scans - expecting components
                handleComponentScan(sanitizedData)
            }
            
            // Re-enable scanning after a short delay
            delay(1500)
            _isScanning.value = true
        }
    }
    
    private fun handleKitScan(kitCode: String) {
        baseKitCode = kitCode
        currentComponentIndex = 0
        
        _statusMessage.value = "Kit scanned: $kitCode"
        _instructionText.value = "Now scan ${componentTypes[currentComponentIndex]}"
        _scanSuccess.value = true
        _showSkipButton.value = true
        
        // Reset scan success after animation
        viewModelScope.launch {
            delay(600)
            _scanSuccess.value = false
        }
    }
    
    private fun handleComponentScan(componentCode: String) {
        if (currentComponentIndex < componentTypes.size) {
            val componentType = componentTypes[currentComponentIndex]
            scannedComponents[componentType] = componentCode
            
            _statusMessage.value = "$componentType: $componentCode"
            updateComponentSummary()
            
            _scanSuccess.value = true
            
            // Reset scan success after animation
            viewModelScope.launch {
                delay(600)
                _scanSuccess.value = false
            }
            
            moveToNextComponent()
        }
    }
    
    private fun moveToNextComponent() {
        currentComponentIndex++
        
        if (currentComponentIndex < componentTypes.size) {
            _instructionText.value = "Scan ${componentTypes[currentComponentIndex]}"
            _showSkipButton.value = true
        } else {
            // All components scanned or skipped
            finishScanning()
        }
    }
    
    fun skipCurrentComponent() {
        if (currentComponentIndex < componentTypes.size) {
            _statusMessage.value = "${componentTypes[currentComponentIndex]} skipped"
            moveToNextComponent()
        }
    }
    
    private fun finishScanning() {
        _isScanning.value = false
        _showSkipButton.value = false
        _showSaveButton.value = true
        _instructionText.value = "All components scanned"
        _statusMessage.value = "Ready to save kit bundle"
    }
    
    fun saveKitBundle() {
        viewModelScope.launch {
            baseKitCode?.let { kitCode ->
                if (scannedComponents.isNotEmpty()) {
                    val bundle = createKitBundle(kitCode)
                    
                    val success = repository.saveKitBundle(bundle)
                    
                    if (success) {
                        _bundleConfirmationMessage.value = "Kit Bundle Complete\n\nKit ID: ${bundle.kitId}\nComponents: ${bundle.getFilledComponentCount()}"
                        _showBundleConfirmation.value = true
                        _showSaveButton.value = false
                        
                        // Hide confirmation after 2 seconds and reset
                        delay(2000)
                        _showBundleConfirmation.value = false
                        clearState()
                    } else {
                        _statusMessage.value = "✗ Failed to save kit bundle"
                        _scanFailure.value = true
                        
                        // Reset failure state
                        delay(600)
                        _scanFailure.value = false
                    }
                } else {
                    _statusMessage.value = "Kit bundle must have at least one component"
                    _scanFailure.value = true
                    
                    // Reset failure state
                    delay(600)
                    _scanFailure.value = false
                }
            }
        }
    }
    
    private fun createKitBundle(kitCode: String): KitBundle {
        val kitId = KitBundle.generateKitId(kitCode)
        val creationDate = KitBundle.extractCreationDate(kitId)
        
        return KitBundle(
            kitId = kitId,
            baseKitCode = kitCode,
            creationDate = creationDate,
            glasses = scannedComponents["Glasses"],
            controller = scannedComponents["Controller"],
            battery01 = scannedComponents["Battery 01"],
            battery02 = scannedComponents["Battery 02"],
            battery03 = scannedComponents["Battery 03"],
            pads = scannedComponents["Pads"],
            unused01 = scannedComponents["Unused 01"],
            unused02 = scannedComponents["Unused 02"]
        )
    }
    
    private fun updateComponentSummary() {
        val summary = StringBuilder()
        scannedComponents.forEach { (type, code) ->
            summary.append("$type: $code\n")
        }
        _componentSummary.value = summary.toString().trimEnd()
    }
    
    private fun handleInvalidBarcode(message: String) {
        _statusMessage.value = message
        _scanFailure.value = true
        
        // Reset scan failure state after animation
        viewModelScope.launch {
            delay(600)
            _scanFailure.value = false
        }
    }
    
    fun clearState() {
        baseKitCode = null
        currentComponentIndex = -1
        scannedComponents.clear()
        
        _statusMessage.value = "Scan Kit QR Code"
        _instructionText.value = "Position the kit QR code within the frame"
        _isScanning.value = true
        _showSaveButton.value = false
        _showSkipButton.value = false
        _componentSummary.value = ""
        _scanSuccess.value = false
        _scanFailure.value = false
        _showBundleConfirmation.value = false
    }
    
    fun resumeScanning() {
        _isScanning.value = true
    }
}
