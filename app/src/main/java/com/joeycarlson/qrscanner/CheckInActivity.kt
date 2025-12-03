package com.joeycarlson.qrscanner

import android.Manifest
import android.animation.ObjectAnimator
import android.content.Intent
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
import com.joeycarlson.qrscanner.data.CheckInRepository
import com.joeycarlson.qrscanner.databinding.ActivityCheckinBinding
import com.joeycarlson.qrscanner.export.UniversalExportManager
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.ui.HapticManager
import com.joeycarlson.qrscanner.ui.CheckInViewModel
import com.joeycarlson.qrscanner.ui.CheckInViewModelFactory
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.util.WindowInsetsHelper
import com.joeycarlson.qrscanner.util.PermissionManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity for Check In mode - simplified version that only scans kit IDs.
 * No user scanning required, just kit scanning and immediate check-in.
 */
class CheckInActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityCheckinBinding
    private lateinit var viewModel: CheckInViewModel
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var hapticManager: HapticManager
    private lateinit var permissionManager: PermissionManager
    
    private var imageAnalyzer: ImageAnalysis? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCheckinBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up edge-to-edge display and handle system UI insets
        WindowInsetsHelper.setupWindowInsets(this)
        
        // Apply padding to the root view to avoid system UI overlap
        WindowInsetsHelper.applySystemWindowInsetsPadding(binding.root)
        
        // Set up action bar with back navigation
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.check_in_mode_title)
        }
        
        val repository = CheckInRepository(this)
        val factory = CheckInViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[CheckInViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        hapticManager = HapticManager(this)
        
        // Initialize ML Kit barcode scanner with support for QR codes and common 1D barcodes
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_QR_CODE,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_CODE_93,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8
            )
            .build()
        barcodeScanner = BarcodeScanning.getClient(options)
        
        setupObservers()
        setupClickListeners()
        
        // Initialize permission manager
        permissionManager = PermissionManager(this)
        
        // Set up permission callbacks
        permissionManager.setPermissionCallbacks(
            onPermissionsGranted = {
                startCamera()
            },
            onPermissionsDenied = { deniedPermissions ->
                Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
                finish()
            }
        )
        
        // Request permissions
        if (permissionManager.areAllPermissionsGranted()) {
            startCamera()
        } else {
            permissionManager.requestAllRequiredPermissions()
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
                
                // Collect undo button visibility
                launch {
                    viewModel.showUndoButton.collect { showUndo ->
                        binding.undoButton.visibility = if (showUndo) {
                            View.VISIBLE
                        } else {
                            View.GONE
                        }
                    }
                }
                
                // Collect check-in confirmation display
                launch {
                    viewModel.showCheckInConfirmation.collect { showConfirmation ->
                        if (showConfirmation) {
                            showCheckInConfirmation()
                        } else {
                            hideCheckInConfirmation()
                        }
                    }
                }
                
                // Collect check-in confirmation message
                launch {
                    viewModel.checkInConfirmationMessage.collect { message ->
                        val lines = message.split("\n")
                        if (lines.size >= 2) {
                            binding.confirmationText.text = lines[0] // "CHECK-IN COMPLETE"
                            binding.confirmationDetails.text = lines.drop(1).joinToString("\n") // Kit info
                        } else {
                            binding.confirmationText.text = message
                            binding.confirmationDetails.text = ""
                        }
                    }
                }
            }
        }
    }
    
    private fun triggerFlashAnimation() {
        binding.flashOverlay.visibility = View.VISIBLE
        binding.flashOverlay.alpha = 0f
        binding.flashOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
        
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
        binding.flashOverlay.setBackgroundColor(ContextCompat.getColor(this, R.color.scan_failure_flash))
        
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
    
    private fun showCheckInConfirmation() {
        binding.confirmationOverlay.visibility = View.VISIBLE
        
        // Animate fade in
        val fadeInAnimator = ObjectAnimator.ofFloat(binding.confirmationOverlay, "alpha", 0f, 1f)
        fadeInAnimator.duration = 300
        fadeInAnimator.start()
        
        // Trigger success haptic feedback for check-in completion
        hapticManager.performSuccessHaptic()
    }
    
    private fun hideCheckInConfirmation() {
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
        // Clear button removed from UI - functionality commented out
        /*
        binding.clearButton.setOnClickListener {
            viewModel.clearState()
        }
        */
        
        binding.undoButton.setOnClickListener {
            DialogUtils.showWarningDialog(
                this,
                "Confirm Undo",
                "Are you sure you want to undo the last check-in?"
            ) {
                viewModel.undoLastCheckIn()
            }
        }
        
        // Settings button removed - only accessible from Home Screen
        /*
        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        */
        
        binding.exportButton.setOnClickListener {
            // Use the unified export manager for check-in exports
            UniversalExportManager.getInstance(this)
                .startExport(AppConfig.EXPORT_TYPE_CHECKIN, this)
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
                    it.setAnalyzer(cameraExecutor, BarcodeAnalyzer())
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
    
    
    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null && viewModel.isScanning.value) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { barcodeData ->
                            runOnUiThread {
                                viewModel.processBarcode(barcodeData)
                            }
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Barcode scanning failed", it)
                    }
                    .addOnCompleteListener {
                        imageProxy.close()
                    }
            } else {
                imageProxy.close()
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        barcodeScanner.close()
    }
    
    companion object {
        private const val TAG = "CheckInScanner"
    }
}
