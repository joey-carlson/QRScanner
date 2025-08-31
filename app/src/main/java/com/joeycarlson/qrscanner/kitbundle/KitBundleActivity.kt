package com.joeycarlson.qrscanner.kitbundle

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.joeycarlson.qrscanner.data.KitRepository
import com.joeycarlson.qrscanner.databinding.ActivityKitBundleBinding
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.ui.HapticManager
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.ocr.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class KitBundleActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityKitBundleBinding
    private lateinit var viewModel: KitBundleViewModel
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var hapticManager: HapticManager
    private lateinit var hybridAnalyzer: HybridScanAnalyzer
    
    private var imageAnalyzer: ImageAnalysis? = null
    private var currentScanMode = ScanMode.BARCODE_ONLY
    
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
        
        val repository = KitRepository(this)
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
                
                // Collect skip button visibility
                launch {
                    viewModel.showSkipButton.collect { show ->
                        binding.skipButton.visibility = if (show) View.VISIBLE else View.GONE
                    }
                }
                
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
            viewModel.skipCurrentComponent()
        }
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
                    // Both scanning methods failed - show manual input dialog
                    showManualInputDialog(result.reason)
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
        val editText = android.widget.EditText(this).apply {
            hint = "Enter serial number"
            inputType = android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
        }
        
        AlertDialog.Builder(this)
            .setTitle("Manual Entry Required")
            .setMessage(reason)
            .setView(editText)
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
            .show()
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
