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
    
    // Duplicate handling state
    private val _duplicateComponentResult = MutableStateFlow<DuplicateComponentResult?>(null)
    val duplicateComponentResult: StateFlow<DuplicateComponentResult?> = _duplicateComponentResult.asStateFlow()
    
    // Review state flows
    private val _isReviewMode = MutableStateFlow(false)
    val isReviewMode: StateFlow<Boolean> = _isReviewMode.asStateFlow()
    
    private val _reviewKitCode = MutableStateFlow("")
    val reviewKitCode: StateFlow<String> = _reviewKitCode.asStateFlow()
    
    private val _reviewComponents = MutableStateFlow<Map<String, String>>(emptyMap())
    val reviewComponents: StateFlow<Map<String, String>> = _reviewComponents.asStateFlow()
    
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
            // Find which slot currently has this DSN
            val currentEntry = state.scannedComponents.entries.find { (_, component) ->
                component.dsn == componentDsn
            }
            
            if (currentEntry != null) {
                val (currentSlot, currentComponent) = currentEntry
                
                // Detect component type for suggested new slot
                val (componentType, _) = dsnValidator.inferComponentTypeWithConfidence(componentDsn)
                val suggestedNewSlot = getSuggestedSlot(componentType ?: currentComponent.componentType, state)
                
                // Create duplicate result and show dialog
                _duplicateComponentResult.value = DuplicateComponentResult(
                    dsn = componentDsn,
                    currentSlot = currentSlot,
                    currentSlotDisplayName = getSlotDisplayName(currentSlot),
                    componentType = componentType ?: currentComponent.componentType,
                    suggestedNewSlot = suggestedNewSlot
                )
                _isScanning.value = false
            }
            return
        }
        
        // Detect component type with confidence
        val (componentType, _) = dsnValidator.inferComponentTypeWithConfidence(componentDsn)
        val overallConfidence = dsnValidator.getDetectionConfidence(componentDsn, ocrConfidence)
        
        // Create detection result and process based on confidence level
        val result = ComponentDetectionResult(
            dsn = componentDsn,
            componentType = componentType,
            confidenceLevel = overallConfidence,
            requiresConfirmation = overallConfidence == DsnValidator.ConfidenceLevel.MEDIUM,
            suggestedSlot = getSuggestedSlot(componentType, state)
        )
        
        when (overallConfidence) {
            DsnValidator.ConfidenceLevel.HIGH -> {
                // Auto-assign with high confidence
                autoAssignComponent(result)
            }
            DsnValidator.ConfidenceLevel.MEDIUM -> {
                // Show confirmation dialog
                _componentDetectionResult.value = result
                _isScanning.value = false
            }
            DsnValidator.ConfidenceLevel.LOW -> {
                // Show manual selection dialog
                _componentDetectionResult.value = result
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
        
        if (status.isComplete && !_isReviewMode.value) {
            // Enter review mode when minimum requirements are met
            enterReviewMode()
        }
    }
    
    private fun enterReviewMode() {
        val state = kitBundleState ?: return
        
        // Prepare review data
        _reviewKitCode.value = state.baseKitCode
        
        // Convert scanned components to review format (slot -> DSN)
        val reviewComponentsMap = mutableMapOf<String, String>()
        state.scannedComponents.forEach { (slot, component) ->
            reviewComponentsMap[slot] = component.dsn
        }
        _reviewComponents.value = reviewComponentsMap
        
        // Enter review mode
        _isReviewMode.value = true
        _isScanning.value = false
        _showSaveButton.value = true
        _statusMessage.value = "Review kit bundle before saving"
        _instructionText.value = "Check all components are correct"
    }
    
    fun updateReviewKitCode(kitCode: String) {
        _reviewKitCode.value = kitCode
    }
    
    fun updateReviewComponent(slot: String, dsn: String) {
        val currentComponents = _reviewComponents.value.toMutableMap()
        if (dsn.isNotBlank()) {
            currentComponents[slot] = dsn
        } else {
            currentComponents.remove(slot)
        }
        _reviewComponents.value = currentComponents
    }
    
    fun confirmReview() {
        // Update kit bundle state with reviewed values
        val reviewedKitCode = _reviewKitCode.value
        val reviewedComponents = _reviewComponents.value
        
        // Create new state with reviewed values
        val newComponents = mutableMapOf<String, ScannedComponent>()
        val newDsns = mutableSetOf<String>()
        
        reviewedComponents.forEach { (slot, dsn) ->
            val componentType = getComponentTypeForSlot(slot)
            newComponents[slot] = ScannedComponent(
                dsn = dsn,
                componentType = componentType,
                assignedSlot = slot
            )
            newDsns.add(dsn)
        }
        
        kitBundleState = KitBundleState(
            baseKitCode = reviewedKitCode,
            scannedComponents = newComponents,
            scannedDsns = newDsns
        )
        
        // Exit review mode
        _isReviewMode.value = false
        
        // Save the kit bundle
        saveKitBundle()
    }
    
    fun cancelReview() {
        // Exit review mode and resume scanning
        _isReviewMode.value = false
        _isScanning.value = true
        _showSaveButton.value = false
        _statusMessage.value = "Continue scanning components"
        
        // Update instruction based on current requirements
        updateRequirementProgress()
    }
    
    fun cancelComponentDetection() {
        _componentDetectionResult.value = null
        _isScanning.value = true
    }
    
    fun ignoreDuplicateComponent() {
        // Clear the duplicate result and resume scanning
        _duplicateComponentResult.value = null
        _isScanning.value = true
        _statusMessage.value = "Duplicate component ignored"
        
        // Show brief failure flash to indicate rejection
        _scanFailure.value = true
        viewModelScope.launch {
            delay(600)
            _scanFailure.value = false
        }
    }
    
    fun reassignDuplicateComponent(newSlot: String) {
        val duplicateResult = _duplicateComponentResult.value ?: return
        val state = kitBundleState ?: return
        
        // Remove the component from its current slot
        val updatedComponents = state.scannedComponents.toMutableMap()
        updatedComponents.remove(duplicateResult.currentSlot)
        
        // Assign to new slot
        val componentType = getComponentTypeForSlot(newSlot)
        val scannedComponent = ScannedComponent(
            dsn = duplicateResult.dsn,
            componentType = componentType,
            assignedSlot = newSlot
        )
        updatedComponents[newSlot] = scannedComponent
        
        // Update state
        kitBundleState = state.copy(
            scannedComponents = updatedComponents
        )
        
        // Clear duplicate result
        _duplicateComponentResult.value = null
        
        // Update UI
        val displayName = getSlotDisplayName(newSlot)
        val oldDisplayName = getSlotDisplayName(duplicateResult.currentSlot)
        _statusMessage.value = "Moved from $oldDisplayName to $displayName"
        _scanSuccess.value = true
        
        // Update progress and summary
        updateRequirementProgress()
        updateComponentSummary()
        checkCompletionStatus()
        
        // Resume scanning
        viewModelScope.launch {
            delay(600)
            _scanSuccess.value = false
            delay(900)
            _isScanning.value = true
        }
    }
    
    fun clearDuplicateResult() {
        _duplicateComponentResult.value = null
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
