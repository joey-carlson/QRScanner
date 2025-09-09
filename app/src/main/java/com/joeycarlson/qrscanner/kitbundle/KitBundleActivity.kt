package com.joeycarlson.qrscanner.kitbundle

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.preference.PreferenceManager
import kotlinx.coroutines.launch
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.data.KitRepository
import com.joeycarlson.qrscanner.databinding.ActivityKitBundleBinding
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.ui.HapticManager
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.ocr.*
import com.joeycarlson.qrscanner.SettingsActivity
import com.joeycarlson.qrscanner.export.*
import java.time.LocalDate
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.lifecycle.Lifecycle

class KitBundleActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityKitBundleBinding
    private lateinit var viewModel: KitBundleViewModel
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var hapticManager: HapticManager
    private lateinit var hybridAnalyzer: HybridScanAnalyzer
    private lateinit var repository: KitRepository
    
    private var imageAnalyzer: ImageAnalysis? = null
    private var currentScanMode = ScanMode.BARCODE_ONLY
    private var isDialogShowing = false
    
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // Only request storage permission for Android 9 and below
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ uses MediaStore API, no storage permission needed
                startCamera()
            } else {
                requestStoragePermission()
            }
        } else {
            Toast.makeText(this, getString(com.joeycarlson.qrscanner.R.string.camera_permission_required), Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startCamera()
        } else {
            Toast.makeText(this, "Storage permission is required to save files to Downloads", Toast.LENGTH_LONG).show()
            finish()
        }
    }
    
    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestStoragePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKitBundleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up action bar with back navigation
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(com.joeycarlson.qrscanner.R.string.kit_bundle_mode_title)
        }
        
        repository = KitRepository(this)
        val factory = KitBundleViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[KitBundleViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        hapticManager = HapticManager(this)
        
        // Initialize hybrid analyzer for barcode and OCR scanning
        hybridAnalyzer = HybridScanAnalyzer(
            scanMode = currentScanMode,
            onScanResult = { result ->
                handleScanResult(result)
            },
            onError = { exception ->
                Log.e(TAG, "Scanning error", exception)
            }
        )
        
        setupObservers()
        setupClickListeners()
        setupScanModeSelector()
        
        // Request permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                // Collect status message changes
                launch {
                    viewModel.statusMessage.collect { message ->
                        binding.statusText.text = message
                    }
                }
                
                // Collect instruction text changes
                launch {
                    viewModel.instructionText.collect { instruction ->
                        binding.instructionText.text = instruction
                    }
                }
                
                // Collect scanning state changes
                launch {
                    viewModel.isScanning.collect { isScanning ->
                        binding.scanOverlay.visibility = if (isScanning) {
                            View.VISIBLE
                        } else {
                            View.INVISIBLE
                        }
                    }
                }
                
                // Collect scan success events
                launch {
                    viewModel.scanSuccess.collect { success ->
                        if (success) {
                            triggerFlashAnimation()
                        }
                    }
                }
                
                // Collect scan failure events
                launch {
                    viewModel.scanFailure.collect { failure ->
                        if (failure) {
                            triggerFailureFlash()
                        }
                    }
                }
                
                // Collect save button visibility
                launch {
                    viewModel.showSaveButton.collect { show ->
                        binding.saveButton.visibility = if (show) View.VISIBLE else View.GONE
                    }
                }
                
                // Skip button is not used in current implementation
                // Remove or implement skipCurrentComponent in ViewModel if needed
                
                // Collect bundle confirmation display
                launch {
                    viewModel.showBundleConfirmation.collect { showConfirmation ->
                        if (showConfirmation) {
                            showBundleConfirmation()
                        } else {
                            hideBundleConfirmation()
                        }
                    }
                }
                
                // Collect bundle confirmation message
                launch {
                    viewModel.bundleConfirmationMessage.collect { message ->
                        binding.confirmationText.text = message
                    }
                }
                
                // Collect component summary
                launch {
                    viewModel.componentSummary.collect { summary ->
                        binding.componentSummaryText.text = summary
                        binding.componentSummaryCard.visibility = if (summary.isNotEmpty()) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
                
                // Collect requirement progress
                launch {
                    viewModel.requirementProgress.collect { progress ->
                        if (progress.isNotEmpty()) {
                            // Show progress in a dedicated text view or update existing UI
                            binding.requirementProgressText.text = progress
                            binding.requirementProgressCard.visibility = View.VISIBLE
                        } else {
                            binding.requirementProgressCard.visibility = View.GONE
                        }
                    }
                }
                
                // Collect component detection results
                launch {
                    viewModel.componentDetectionResult.collect { result ->
                        result?.let {
                            when (it.confidenceLevel) {
                                DsnValidator.ConfidenceLevel.MEDIUM -> {
                                    showComponentConfirmationDialog(it)
                                }
                                DsnValidator.ConfidenceLevel.LOW -> {
                                    showComponentSelectionDialog(it.dsn)
                                }
                                else -> {
                                    // High confidence handled automatically
                                }
                            }
                        }
                    }
                }
                
                // Collect review mode state
                launch {
                    viewModel.isReviewMode.collect { isReviewMode ->
                        if (isReviewMode) {
                            binding.reviewPanel.visibility = View.VISIBLE
                            binding.reviewButtonSection.visibility = View.VISIBLE
                            binding.scanModeSelector.visibility = View.GONE
                            binding.bottomButtonSection.visibility = View.GONE
                            
                            // Animate fade in
                            val fadeInAnimator = ObjectAnimator.ofFloat(binding.reviewPanel, "alpha", 0f, 1f)
                            fadeInAnimator.duration = 300
                            fadeInAnimator.start()
                        } else {
                            // Animate fade out
                            if (binding.reviewPanel.visibility == View.VISIBLE) {
                                val fadeOutAnimator = ObjectAnimator.ofFloat(binding.reviewPanel, "alpha", 1f, 0f)
                                fadeOutAnimator.duration = 300
                                fadeOutAnimator.start()
                                binding.reviewPanel.postDelayed({
                                    binding.reviewPanel.visibility = View.GONE
                                    binding.reviewButtonSection.visibility = View.GONE
                                    binding.scanModeSelector.visibility = View.VISIBLE
                                    binding.bottomButtonSection.visibility = View.VISIBLE
                                }, 300)
                            }
                        }
                    }
                }
                
                // Collect review kit code
                launch {
                    viewModel.reviewKitCode.collect { kitCode ->
                        if (binding.reviewKitCodeInput.text.toString() != kitCode) {
                            binding.reviewKitCodeInput.setText(kitCode)
                        }
                    }
                }
                
                // Collect review components
                launch {
                    viewModel.reviewComponents.collect { components ->
                        updateComponentInputs(components)
                    }
                }
                
                // Collect duplicate component results
                launch {
                    viewModel.duplicateComponentResult.collect { result ->
                        result?.let {
                            showDuplicateComponentDialog(it)
                        }
                    }
                }
            }
        }
    }
    
    private fun triggerFlashAnimation() {
        binding.flashOverlay.visibility = View.VISIBLE
        binding.flashOverlay.alpha = 0f
        binding.flashOverlay.setBackgroundColor(ContextCompat.getColor(this, com.joeycarlson.qrscanner.R.color.white))
        
        // Trigger success haptic feedback - single light tap
        hapticManager.performSuccessHaptic()
        
        // Longer, more noticeable flash with stronger visibility
        val flashAnimator = ObjectAnimator.ofFloat(binding.flashOverlay, "alpha", 0f, 0.8f, 0f)
        flashAnimator.duration = AppConfig.FLASH_ANIMATION_DURATION
        flashAnimator.start()
        
        // Hide the overlay after animation
        binding.flashOverlay.postDelayed({
            binding.flashOverlay.visibility = View.GONE
        }, AppConfig.FLASH_ANIMATION_DURATION)
    }
    
    private fun triggerFailureFlash() {
        binding.flashOverlay.visibility = View.VISIBLE
        binding.flashOverlay.alpha = 0f
        binding.flashOverlay.setBackgroundColor(ContextCompat.getColor(this, com.joeycarlson.qrscanner.R.color.scan_failure_flash))
        
        // Trigger failure haptic feedback - double buzz pattern
        hapticManager.performFailureHaptic()
        
        // Red flash for failure with strong visibility
        val flashAnimator = ObjectAnimator.ofFloat(binding.flashOverlay, "alpha", 0f, 0.8f, 0f)
        flashAnimator.duration = AppConfig.FLASH_ANIMATION_DURATION
        flashAnimator.start()
        
        // Hide the overlay after animation
        binding.flashOverlay.postDelayed({
            binding.flashOverlay.visibility = View.GONE
        }, AppConfig.FLASH_ANIMATION_DURATION)
    }
    
    private fun showBundleConfirmation() {
        binding.confirmationOverlay.visibility = View.VISIBLE
        
        // Animate fade in
        val fadeInAnimator = ObjectAnimator.ofFloat(binding.confirmationOverlay, "alpha", 0f, 1f)
        fadeInAnimator.duration = 300
        fadeInAnimator.start()
        
        // Trigger success haptic feedback for bundle completion
        hapticManager.performSuccessHaptic()
    }
    
    private fun hideBundleConfirmation() {
        // Animate fade out
        val fadeOutAnimator = ObjectAnimator.ofFloat(binding.confirmationOverlay, "alpha", 1f, 0f)
        fadeOutAnimator.duration = 300
        fadeOutAnimator.start()
        
        // Hide the overlay after animation
        binding.confirmationOverlay.postDelayed({
            binding.confirmationOverlay.visibility = View.GONE
        }, 300)
    }
    
    private fun setupClickListeners() {
        binding.clearButton.setOnClickListener {
            viewModel.clearState()
        }
        
        binding.saveButton.setOnClickListener {
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val locationId = prefs.getString("location_id", "")
            
            if (locationId.isNullOrEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Configuration Required")
                    .setMessage("Please configure Location ID in Settings before saving bundles.")
                    .setPositiveButton("OK", null)
                    .show()
            } else {
                viewModel.saveKitBundle()
            }
        }
        
        binding.skipButton.setOnClickListener {
            // Skip functionality not implemented in current version
            // Hide button or implement in ViewModel if needed
        }
        
        // Review panel button click listeners
        binding.reviewConfirmButton.setOnClickListener {
            viewModel.confirmReview()
        }
        
        binding.reviewCancelButton.setOnClickListener {
            viewModel.cancelReview()
        }
        
        // Text watcher for review kit code input
        binding.reviewKitCodeInput.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                s?.let {
                    val newText = it.toString()
                    if (newText != viewModel.reviewKitCode.value) {
                        viewModel.updateReviewKitCode(newText)
                    }
                }
            }
        })
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }
            
            imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, hybridAnalyzer)
                }
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
            
        }, ContextCompat.getMainExecutor(this))
    }
    
    private fun allPermissionsGranted(): Boolean {
        val cameraGranted = ContextCompat.checkSelfPermission(
            baseContext, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ only needs camera permission
            cameraGranted
        } else {
            // Android 9 and below needs both camera and storage permissions
            cameraGranted && ContextCompat.checkSelfPermission(
                baseContext, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun setupScanModeSelector() {
        binding.scanModeSelector.setOnModeChangeListener { mode ->
            currentScanMode = mode
            hybridAnalyzer.setScanMode(mode)
            
            // Update instruction text based on mode
            val instruction = when (mode) {
                ScanMode.BARCODE_ONLY -> "Position the barcode within the frame"
                ScanMode.OCR_ONLY -> "Position the serial number text within the frame"
                ScanMode.HYBRID -> "Position the barcode or serial number within the frame"
            }
            runOnUiThread {
                binding.instructionText.text = instruction
            }
        }
    }
    
    private fun handleScanResult(result: ScanResult) {
        if (viewModel.isScanning.value != true) return
        
        runOnUiThread {
            when (result) {
                is ScanResult.BarcodeResult -> {
                    // Direct barcode scan - process immediately
                    viewModel.processBarcode(result.rawValue)
                }
                
                is ScanResult.OcrResult -> {
                    // OCR result - show verification dialog if needed
                    if (result.requiresManualVerification) {
                        showOcrVerificationDialog(result)
                    } else {
                        viewModel.processBarcode(result.text)
                    }
                }
                
                is ScanResult.ManualInputRequired -> {
                    // Manual input dialog commented out due to dialog closure issues
                    // TODO: Implement better manual input handling in future update
                    // showManualInputDialog(result.reason)
                    
                    // For now, just show a toast and resume scanning
                    Toast.makeText(
                        this, 
                        "Unable to scan. Please try repositioning the barcode/text.", 
                        Toast.LENGTH_LONG
                    ).show()
                    viewModel.resumeScanning()
                }
            }
        }
    }
    
    private fun showOcrVerificationDialog(ocrResult: ScanResult.OcrResult) {
        val dialog = OcrVerificationDialog.newInstance(
            ocrResult = ocrResult.text,
            confidence = ocrResult.confidence,
            componentType = ocrResult.inferredComponentType
        )
        
        dialog.setOnConfirmListener { verifiedText ->
            viewModel.processBarcode(verifiedText)
        }
        
        dialog.setOnCancelListener {
            // User cancelled - resume scanning
            viewModel.resumeScanning()
        }
        
        dialog.show(supportFragmentManager, "ocr_verification")
    }
    
    private fun showManualInputDialog(reason: String) {
        // Prevent multiple dialogs
        if (isDialogShowing) return
        
        isDialogShowing = true
        viewModel.pauseScanning()
        
        val editText = android.widget.EditText(this).apply {
            hint = "Enter serial number"
            inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        
        val dialog = AlertDialog.Builder(this)
            .setTitle("Manual Entry Required")
            .setMessage(reason)
            .setView(editText)
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ ->
                val manualEntry = editText.text.toString().trim()
                if (manualEntry.isNotEmpty()) {
                    val validator = DsnValidator()
                    val validationResult = validator.validateManualEntry(manualEntry)
                    
                    if (validationResult.isValid) {
                        viewModel.processBarcode(validationResult.normalizedDsn ?: manualEntry)
                    } else {
                        Toast.makeText(this, validationResult.error, Toast.LENGTH_SHORT).show()
                        viewModel.resumeScanning()
                    }
                } else {
                    viewModel.resumeScanning()
                }
            }
            .setNegativeButton("Cancel") { _, _ ->
                viewModel.resumeScanning()
            }
            .setOnDismissListener {
                isDialogShowing = false
            }
            .create()
        
        dialog.show()
    }
    
    private fun showComponentConfirmationDialog(detectionResult: ComponentDetectionResult) {
        val dialog = ComponentConfirmationDialog.newInstance(
            dsn = detectionResult.dsn,
            componentType = detectionResult.componentType,
            suggestedSlot = detectionResult.suggestedSlot
        )
        
        dialog.setOnComponentConfirmListener(object : ComponentConfirmationDialog.OnComponentConfirmListener {
            override fun onConfirm(dsn: String, slot: String) {
                // User confirmed the detected component type
                viewModel.confirmComponentAssignment(dsn, slot)
            }
            
            override fun onCancel() {
                // User rejected - show manual selection
                showComponentSelectionDialog(detectionResult.dsn)
            }
        })
        
        dialog.show(supportFragmentManager, "component_confirmation")
    }
    
    private fun showComponentSelectionDialog(dsn: String) {
        // Get available slots from the kit bundle state
        val availableSlots = getAvailableComponentSlots()
        
        val dialog = ComponentSelectionDialog.newInstance(
            dsn = dsn,
            availableSlots = availableSlots
        )
        
        dialog.setOnComponentSelectListener(object : ComponentSelectionDialog.OnComponentSelectListener {
            override fun onSelect(dsn: String, slot: String) {
                // User selected a component slot
                viewModel.confirmComponentAssignment(dsn, slot)
            }
            
            override fun onCancel() {
                // User cancelled - resume scanning
                viewModel.resumeScanning()
            }
        })
        
        dialog.show(supportFragmentManager, "component_selection")
    }
    
    private fun showDuplicateComponentDialog(duplicateResult: DuplicateComponentResult) {
        val suggestedSlotDisplayName = duplicateResult.suggestedNewSlot?.let { slot ->
            getSlotDisplayName(slot)
        }
        
        val dialog = DuplicateComponentDialog.newInstance(
            dsn = duplicateResult.dsn,
            currentSlot = duplicateResult.currentSlot,
            currentSlotDisplayName = duplicateResult.currentSlotDisplayName,
            suggestedNewSlot = duplicateResult.suggestedNewSlot,
            suggestedSlotDisplayName = suggestedSlotDisplayName
        )
        
        dialog.setOnDuplicateActionListener(object : DuplicateComponentDialog.OnDuplicateActionListener {
            override fun onIgnore() {
                viewModel.ignoreDuplicateComponent()
            }
            
            override fun onReassign(newSlot: String) {
                viewModel.reassignDuplicateComponent(newSlot)
            }
        })
        
        dialog.show(supportFragmentManager, "duplicate_component")
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
    
    private fun getAvailableComponentSlots(): List<ComponentSlot> {
        // Create all possible component slots
        val allSlots = listOf(
            ComponentSlot("glasses", "Glasses", DsnValidator.ComponentType.GLASSES),
            ComponentSlot("controller", "Controller", DsnValidator.ComponentType.CONTROLLER),
            ComponentSlot("battery01", "Battery 01", DsnValidator.ComponentType.BATTERY_01),
            ComponentSlot("battery02", "Battery 02", DsnValidator.ComponentType.BATTERY_02),
            ComponentSlot("battery03", "Battery 03", DsnValidator.ComponentType.BATTERY_03),
            ComponentSlot("pads", "Pads", DsnValidator.ComponentType.PADS),
            ComponentSlot("unused01", "Unused 01", DsnValidator.ComponentType.UNUSED_01),
            ComponentSlot("unused02", "Unused 02", DsnValidator.ComponentType.UNUSED_02)
        )
        
        // For now, return all slots. In the future, you could filter based on what's already scanned
        return allSlots
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_kit_bundle, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressedDispatcher.onBackPressed()
                true
            }
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_export_labels -> {
                exportKitLabels()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun exportKitLabels() {
        lifecycleScope.launch {
            try {
                val bundles = repository.getAllKitBundles()
                
                if (bundles.isEmpty()) {
                    Toast.makeText(
                        this@KitBundleActivity,
                        "No kit bundles to export",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }
                
                val prefs = PreferenceManager.getDefaultSharedPreferences(this@KitBundleActivity)
                val deviceName = prefs.getString("device_name", null)
                val locationId = prefs.getString("location_id", null)
                
                // Generate the CSV content
                val contentGenerator = ContentGenerator()
                val csvContent = contentGenerator.generateKitBundleContent(
                    bundles,
                    ExportFormat.KIT_LABELS_CSV,
                    locationId ?: "Unknown"
                )
                
                // Generate filename using FileNamingService
                val fileNamingService = FileNamingService()
                val filename = fileNamingService.generateKitLabelFilename(
                    LocalDate.now(),
                    deviceName,
                    locationId
                )
                
                // Start export method selection activity
                val intent = Intent(this@KitBundleActivity, ExportMethodActivity::class.java).apply {
                    putExtra("export_type", "kit_labels")
                    putExtra("csv_content", csvContent)
                    putExtra("filename", filename)
                }
                startActivity(intent)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error exporting kit labels", e)
                Toast.makeText(
                    this@KitBundleActivity,
                    "Error exporting kit labels: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun updateComponentInputs(components: Map<String, String>) {
        binding.componentInputsContainer.removeAllViews()
        
        val componentSlots = listOf(
            "glasses" to "Glasses",
            "controller" to "Controller",
            "battery01" to "Battery 01",
            "battery02" to "Battery 02",
            "battery03" to "Battery 03",
            "pads" to "Pads",
            "unused01" to "Unused 01",
            "unused02" to "Unused 02"
        )
        
        componentSlots.forEach { (slot, displayName) ->
            val componentValue = components[slot]
            if (componentValue != null) {
                val inputLayout = com.google.android.material.textfield.TextInputLayout(this).apply {
                    layoutParams = android.widget.LinearLayout.LayoutParams(
                        android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                        android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = resources.getDimensionPixelSize(R.dimen.spacing_small)
                    }
                    hint = displayName
                    boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
                    setBoxStrokeColorStateList(
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this@KitBundleActivity, android.R.color.white)
                        )
                    )
                    setHintTextColor(
                        android.content.res.ColorStateList.valueOf(
                            ContextCompat.getColor(this@KitBundleActivity, android.R.color.white)
                        )
                    )
                }
                
                val inputEditText = com.google.android.material.textfield.TextInputEditText(this).apply {
                    setText(componentValue)
                    setTextColor(ContextCompat.getColor(this@KitBundleActivity, android.R.color.white))
                    addTextChangedListener(object : android.text.TextWatcher {
                        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                        override fun afterTextChanged(s: android.text.Editable?) {
                            s?.let {
                                val newText = it.toString()
                                viewModel.updateReviewComponent(slot, newText)
                            }
                        }
                    })
                }
                
                inputLayout.addView(inputEditText)
                binding.componentInputsContainer.addView(inputLayout)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        hybridAnalyzer.close()
    }
    
    companion object {
        private const val TAG = "KitBundleScanner"
    }
}
