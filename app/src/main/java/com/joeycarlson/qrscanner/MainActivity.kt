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
import android.text.Editable
import android.text.TextWatcher
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.joeycarlson.qrscanner.data.CheckoutRepository
import com.joeycarlson.qrscanner.databinding.ActivityMainBinding
import com.joeycarlson.qrscanner.export.ExportActivity
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.ui.HapticManager
import com.joeycarlson.qrscanner.ui.ScanState
import com.joeycarlson.qrscanner.ui.ScanViewModel
import com.joeycarlson.qrscanner.ui.ScanViewModelFactory
import com.joeycarlson.qrscanner.config.AppConfig
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ScanViewModel
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var hapticManager: HapticManager
    
    private var imageAnalyzer: ImageAnalysis? = null
    
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
            Toast.makeText(this, getString(R.string.camera_permission_required), Toast.LENGTH_LONG).show()
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
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up action bar with back navigation
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.check_out_mode_title)
        }
        
        val repository = CheckoutRepository(this)
        val factory = ScanViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[ScanViewModel::class.java]
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
                
                // Collect scanning state changes
                launch {
                    viewModel.isScanning.collect { isScanning ->
                        binding.scanOverlay.visibility = if (isScanning) {
                            android.view.View.VISIBLE
                        } else {
                            android.view.View.INVISIBLE
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
                
                // Collect checkout confirmation display
                launch {
                    viewModel.showCheckoutConfirmation.collect { showConfirmation ->
                        if (showConfirmation) {
                            showCheckoutConfirmation()
                        } else {
                            hideCheckoutConfirmation()
                        }
                    }
                }
                
                // Collect checkout confirmation message
                launch {
                    viewModel.checkoutConfirmationMessage.collect { message ->
                        val lines = message.split("\n")
                        if (lines.size >= 3) {
                            binding.confirmationText.text = lines[0] // "CHECKOUT COMPLETE"
                            binding.confirmationDetails.text = "${lines[1]}\n${lines[2]}" // User and Kit info
                        } else {
                            binding.confirmationText.text = message
                            binding.confirmationDetails.text = ""
                        }
                    }
                }
                
                // Collect scan state to show/hide review panel
                launch {
                    viewModel.scanState.collect { state ->
                        when (state) {
                            ScanState.REVIEW_PENDING -> {
                                binding.reviewPanel.visibility = View.VISIBLE
                                // Show review panel with animation
                                val fadeInAnimator = ObjectAnimator.ofFloat(binding.reviewPanel, "alpha", 0f, 1f)
                                fadeInAnimator.duration = 300
                                fadeInAnimator.start()
                            }
                            else -> {
                                // Hide review panel if visible
                                if (binding.reviewPanel.visibility == View.VISIBLE) {
                                    val fadeOutAnimator = ObjectAnimator.ofFloat(binding.reviewPanel, "alpha", 1f, 0f)
                                    fadeOutAnimator.duration = 300
                                    fadeOutAnimator.start()
                                    binding.reviewPanel.postDelayed({
                                        binding.reviewPanel.visibility = View.GONE
                                    }, 300)
                                }
                            }
                        }
                    }
                }
                
                // Collect review user ID
                launch {
                    viewModel.reviewUserId.collect { userId ->
                        if (binding.reviewUserIdInput.text.toString() != userId) {
                            binding.reviewUserIdInput.setText(userId)
                        }
                    }
                }
                
                // Collect review kit ID
                launch {
                    viewModel.reviewKitId.collect { kitId ->
                        if (binding.reviewKitIdInput.text.toString() != kitId) {
                            binding.reviewKitIdInput.setText(kitId)
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
    
    private fun showCheckoutConfirmation() {
        binding.confirmationOverlay.visibility = View.VISIBLE
        
        // Animate fade in
        val fadeInAnimator = ObjectAnimator.ofFloat(binding.confirmationOverlay, "alpha", 0f, 1f)
        fadeInAnimator.duration = 300
        fadeInAnimator.start()
        
        // Trigger success haptic feedback for checkout completion
        hapticManager.performSuccessHaptic()
    }
    
    private fun hideCheckoutConfirmation() {
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
        
        binding.undoButton.setOnClickListener {
            DialogUtils.showWarningDialog(
                this,
                "Confirm Undo",
                "Are you sure you want to undo the last checkout?"
            ) {
                viewModel.undoLastCheckout()
            }
        }
        
        binding.settingsButton.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }
        
        binding.exportButton.setOnClickListener {
            // Check if location ID is configured
            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            val locationId = prefs.getString("location_id", "")
            
            if (locationId.isNullOrEmpty()) {
                AlertDialog.Builder(this)
                    .setTitle("Configuration Required")
                    .setMessage("Please configure Location ID in Settings before exporting.")
                    .setPositiveButton("Go to Settings") { _, _ ->
                        val intent = Intent(this, SettingsActivity::class.java)
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            } else {
                val intent = Intent(this, ExportActivity::class.java)
                startActivity(intent)
            }
        }
        
        // Review panel button click listeners
        binding.reviewConfirmButton.setOnClickListener {
            viewModel.confirmReview()
        }
        
        binding.reviewCancelButton.setOnClickListener {
            viewModel.clearState()
        }
        
        // Text watchers for review input fields
        binding.reviewUserIdInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val newText = it.toString()
                    if (newText != viewModel.reviewUserId.value) {
                        viewModel.updateReviewUserId(newText)
                    }
                }
            }
        })
        
        binding.reviewKitIdInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val newText = it.toString()
                    if (newText != viewModel.reviewKitId.value) {
                        viewModel.updateReviewKitId(newText)
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
    
    private inner class BarcodeAnalyzer : ImageAnalysis.Analyzer {
        
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null && viewModel.isScanning.value == true) {
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
        private const val TAG = "QRScanner"
    }
}
