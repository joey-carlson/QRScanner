package com.joeycarlson.qrscanner.kitbundle

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.data.KitBundle
import com.joeycarlson.qrscanner.data.KitRepository
import com.joeycarlson.qrscanner.ocr.DsnValidator
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
    
    private val dsnValidator = DsnValidator()
    
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
    
    private val _showBundleConfirmation = MutableStateFlow(false)
    val showBundleConfirmation: StateFlow<Boolean> = _showBundleConfirmation.asStateFlow()
    
    private val _bundleConfirmationMessage = MutableStateFlow("")
    val bundleConfirmationMessage: StateFlow<String> = _bundleConfirmationMessage.asStateFlow()
    
    private val _componentSummary = MutableStateFlow("")
    val componentSummary: StateFlow<String> = _componentSummary.asStateFlow()
    
    // New state flows for smart detection
    private val _requirementProgress = MutableStateFlow("")
    val requirementProgress: StateFlow<String> = _requirementProgress.asStateFlow()
    
    private val _componentDetectionResult = MutableStateFlow<ComponentDetectionResult?>(null)
    val componentDetectionResult: StateFlow<ComponentDetectionResult?> = _componentDetectionResult.asStateFlow()
    
    // Bundle state using the new models
    private var kitBundleState: KitBundleState? = null
    
    fun processBarcode(barcodeData: String, ocrConfidence: Float = 1.0f) {
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
            
            if (kitBundleState == null) {
                // First scan - expecting kit code
                handleKitScan(sanitizedData)
            } else {
                // Subsequent scans - expecting components
                handleComponentScan(sanitizedData, ocrConfidence)
            }
        }
    }
    
    private fun handleKitScan(kitCode: String) {
        // Initialize kit bundle state
        kitBundleState = KitBundleState(
            baseKitCode = kitCode,
            requirements = KitRequirements()
        )
        
        _statusMessage.value = "Kit scanned: $kitCode"
        _instructionText.value = "Now scan any component (glasses, controller, or battery)"
        _scanSuccess.value = true
        
        // Update requirement progress
        updateRequirementProgress()
        
        // Reset scan success after animation
        viewModelScope.launch {
            delay(600)
            _scanSuccess.value = false
            // Re-enable scanning
            delay(900)
            _isScanning.value = true
        }
    }
    
    private fun handleComponentScan(componentDsn: String, ocrConfidence: Float) {
        val state = kitBundleState ?: return
        
        // Check for duplicate DSN
        if (state.isDuplicateDsn(componentDsn)) {
            _statusMessage.value = "⚠️ Duplicate DSN - already scanned in this kit"
            _scanFailure.value = true
            
            viewModelScope.launch {
                delay(600)
                _scanFailure.value = false
                delay(900)
                _isScanning.value = true
            }
            return
        }
        
        // Detect component type with confidence
        val (componentType, patternConfidence) = dsnValidator.inferComponentTypeWithConfidence(componentDsn)
        val overallConfidence = dsnValidator.getDetectionConfidence(componentDsn, ocrConfidence)
        
        // Create detection result
        val detectionResult = ComponentDetectionResult(
            dsn = componentDsn,
            componentType = componentType,
            confidenceLevel = overallConfidence,
            requiresConfirmation = overallConfidence == DsnValidator.ConfidenceLevel.MEDIUM,
            suggestedSlot = getSuggestedSlot(componentType, state)
        )
        
        when (overallConfidence) {
            DsnValidator.ConfidenceLevel.HIGH -> {
                // Auto-assign with high confidence
                autoAssignComponent(detectionResult)
            }
            DsnValidator.ConfidenceLevel.MEDIUM -> {
                // Show confirmation dialog
                _componentDetectionResult.value = detectionResult
                _isScanning.value = false
            }
            DsnValidator.ConfidenceLevel.LOW -> {
                // Show manual selection dialog
                _componentDetectionResult.value = detectionResult
                _isScanning.value = false
            }
        }
    }
    
    private fun getSuggestedSlot(componentType: DsnValidator.ComponentType?, state: KitBundleState): String? {
        return when (componentType) {
            DsnValidator.ComponentType.GLASSES -> "glasses"
            DsnValidator.ComponentType.CONTROLLER -> "controller"
            DsnValidator.ComponentType.BATTERY_01,
            DsnValidator.ComponentType.BATTERY_02,
            DsnValidator.ComponentType.BATTERY_03 -> state.getNextAvailableBatterySlot()
            DsnValidator.ComponentType.PADS -> "pads"
            DsnValidator.ComponentType.UNUSED_01 -> "unused01"
            DsnValidator.ComponentType.UNUSED_02 -> "unused02"
            null -> null
        }
    }
    
    fun confirmComponentAssignment(dsn: String, slot: String) {
        val state = kitBundleState ?: return
        val detectionResult = _componentDetectionResult.value ?: return
        
        // Get component type for the slot
        val componentType = getComponentTypeForSlot(slot)
        
        // Create scanned component
        val scannedComponent = ScannedComponent(
            dsn = dsn,
            componentType = componentType,
            assignedSlot = slot
        )
        
        // Update state
        val updatedComponents = state.scannedComponents.toMutableMap()
        updatedComponents[slot] = scannedComponent
        
        val updatedDsns = state.scannedDsns.toMutableSet()
        updatedDsns.add(dsn)
        
        kitBundleState = state.copy(
            scannedComponents = updatedComponents,
            scannedDsns = updatedDsns
        )
        
        // Update UI
        val displayName = getSlotDisplayName(slot)
        _statusMessage.value = "$displayName: ${dsn.takeLast(8)}... ✓"
        _scanSuccess.value = true
        
        // Clear detection result
        _componentDetectionResult.value = null
        
        // Update progress and summary
        updateRequirementProgress()
        updateComponentSummary()
        
        // Check if minimum requirements are met
        checkCompletionStatus()
        
        // Resume scanning
        viewModelScope.launch {
            delay(600)
            _scanSuccess.value = false
            delay(900)
            _isScanning.value = true
        }
    }
    
    private fun autoAssignComponent(detectionResult: ComponentDetectionResult) {
        val slot = detectionResult.suggestedSlot ?: return
        
        // For high confidence, auto-confirm
        confirmComponentAssignment(detectionResult.dsn, slot)
    }
    
    private fun getComponentTypeForSlot(slot: String): DsnValidator.ComponentType? {
        return when (slot) {
            "glasses" -> DsnValidator.ComponentType.GLASSES
            "controller" -> DsnValidator.ComponentType.CONTROLLER
            "battery01" -> DsnValidator.ComponentType.BATTERY_01
            "battery02" -> DsnValidator.ComponentType.BATTERY_02
            "battery03" -> DsnValidator.ComponentType.BATTERY_03
            "pads" -> DsnValidator.ComponentType.PADS
            "unused01" -> DsnValidator.ComponentType.UNUSED_01
            "unused02" -> DsnValidator.ComponentType.UNUSED_02
            else -> null
        }
    }
    
    private fun getSlotDisplayName(slot: String): String {
        return when (slot) {
            "glasses" -> "Glasses"
            "controller" -> "Controller"
            "battery01" -> "Battery 01"
            "battery02" -> "Battery 02"
            "battery03" -> "Battery 03"
            "pads" -> "Pads"
            "unused01" -> "Unused 01"
            "unused02" -> "Unused 02"
            else -> slot
        }
    }
    
    private fun updateRequirementProgress() {
        val state = kitBundleState ?: return
        val status = state.getRequirementStatus()
        
        _requirementProgress.value = status.getProgressMessage()
        
        // Update instruction text based on what's needed
        val missingMessage = status.getMissingComponentsMessage()
        _instructionText.value = if (missingMessage != null) {
            missingMessage
        } else {
            "✓ Minimum requirements met - scan more or save kit"
        }
    }
    
    private fun updateComponentSummary() {
        val state = kitBundleState ?: return
        val summary = StringBuilder()
        
        state.scannedComponents.forEach { (slot, component) ->
            val displayName = getSlotDisplayName(slot)
            val shortDsn = component.dsn.takeLast(8)
            summary.append("$displayName: ...$shortDsn\n")
        }
        
        _componentSummary.value = summary.toString().trimEnd()
    }
    
    private fun checkCompletionStatus() {
        val state = kitBundleState ?: return
        val status = state.getRequirementStatus()
        
        _showSaveButton.value = status.isComplete
    }
    
    fun cancelComponentDetection() {
        _componentDetectionResult.value = null
        _isScanning.value = true
    }
    
    fun saveKitBundle() {
        viewModelScope.launch {
            val state = kitBundleState ?: return@launch
            
            if (state.scannedComponents.isNotEmpty()) {
                val bundle = createKitBundle(state)
                
                val success = repository.saveKitBundle(bundle)
                
                if (success) {
                    _bundleConfirmationMessage.value = 
                        "Kit Bundle Complete\n\nKit ID: ${bundle.kitId}\nComponents: ${bundle.getFilledComponentCount()}"
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
            }
        }
    }
    
    private fun createKitBundle(state: KitBundleState): KitBundle {
        val kitId = KitBundle.generateKitId(state.baseKitCode)
        val creationDate = KitBundle.extractCreationDate(kitId)
        
        return KitBundle(
            kitId = kitId,
            baseKitCode = state.baseKitCode,
            creationDate = creationDate,
            glasses = state.scannedComponents["glasses"]?.dsn,
            controller = state.scannedComponents["controller"]?.dsn,
            battery01 = state.scannedComponents["battery01"]?.dsn,
            battery02 = state.scannedComponents["battery02"]?.dsn,
            battery03 = state.scannedComponents["battery03"]?.dsn,
            pads = state.scannedComponents["pads"]?.dsn,
            unused01 = state.scannedComponents["unused01"]?.dsn,
            unused02 = state.scannedComponents["unused02"]?.dsn
        )
    }
    
    private fun handleInvalidBarcode(message: String) {
        _statusMessage.value = message
        _scanFailure.value = true
        
        // Reset scan failure state after animation
        viewModelScope.launch {
            delay(600)
            _scanFailure.value = false
            delay(900)
            _isScanning.value = true
        }
    }
    
    fun clearState() {
        kitBundleState = null
        
        _statusMessage.value = "Scan Kit QR Code"
        _instructionText.value = "Position the kit QR code within the frame"
        _isScanning.value = true
        _showSaveButton.value = false
        _componentSummary.value = ""
        _requirementProgress.value = ""
        _scanSuccess.value = false
        _scanFailure.value = false
        _showBundleConfirmation.value = false
        _componentDetectionResult.value = null
    }
    
    fun resumeScanning() {
        _isScanning.value = true
    }
    
    fun pauseScanning() {
        _isScanning.value = false
    }
}
