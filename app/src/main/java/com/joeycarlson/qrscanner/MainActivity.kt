package com.joeycarlson.qrscanner

import android.animation.ObjectAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
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
import com.joeycarlson.qrscanner.export.UniversalExportManager
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.ui.HapticManager
import com.joeycarlson.qrscanner.ui.ScanState
import com.joeycarlson.qrscanner.ui.ScanViewModel
import com.joeycarlson.qrscanner.ui.ScanViewModelFactory
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.util.CameraManager
import com.joeycarlson.qrscanner.util.WindowInsetsHelper
import com.joeycarlson.qrscanner.util.PermissionManager
import com.joeycarlson.qrscanner.util.ErrorReporter
import com.joeycarlson.qrscanner.data.ScanHistoryItem
import com.joeycarlson.qrscanner.data.ScanHistoryManager
import com.joeycarlson.qrscanner.ui.ScanHistoryAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.tabs.TabLayout
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ScanViewModel
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraManager: CameraManager
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var hapticManager: HapticManager
    private lateinit var historyManager: ScanHistoryManager
    private lateinit var historyAdapter: ScanHistoryAdapter
    private lateinit var permissionManager: PermissionManager
    
    private var isHistoryTabActive = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Set up edge-to-edge display and handle system UI insets
        WindowInsetsHelper.setupWindowInsets(this)
        
        // Apply padding to the root view to avoid system UI overlap
        WindowInsetsHelper.applySystemWindowInsetsPadding(binding.root)
        
        // Set up action bar with back navigation
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.check_out_mode_title)
        }
        
        val repository = CheckoutRepository(this)
        val factory = ScanViewModelFactory(application, repository)
        viewModel = ViewModelProvider(this, factory)[ScanViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraManager = CameraManager(this, this, cameraExecutor)
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
        
        // Initialize history manager and adapter
        historyManager = ScanHistoryManager.getInstance(this)
        setupHistoryRecyclerView()
        setupTabLayout()
        
        setupObservers()
        setupClickListeners()
        
        // Log error report paths for easy access (especially useful in Android Studio)
        android.util.Log.i("ErrorReporter", "=== ERROR REPORT LOCATIONS ===")
        ErrorReporter.getErrorReportPaths().forEach { path ->
            android.util.Log.i("ErrorReporter", path)
        }
        android.util.Log.i("ErrorReporter", "=== END ERROR REPORT LOCATIONS ===")
        
        // Initialize permission manager
        permissionManager = PermissionManager(this)
        
        // Set up permission callbacks
        permissionManager.setPermissionCallbacks(
            onPermissionsGranted = {
                startCamera()
            },
            onPermissionsDenied = { deniedPermissions ->
                DialogUtils.showErrorToast(this, getString(R.string.camera_permission_required))
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
    
    private fun setupHistoryRecyclerView() {
        historyAdapter = ScanHistoryAdapter(
            onItemEdit = { itemId, newValue ->
                historyManager.updateHistoryItem(
                    ScanHistoryItem.ActivityType.CHECKOUT,
                    itemId,
                    newValue
                )
            },
            onItemDelete = { itemId ->
                historyManager.deleteHistoryItem(
                    ScanHistoryItem.ActivityType.CHECKOUT,
                    itemId
                )
                loadHistory()
            }
        )
        
        binding.historyRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = historyAdapter
        }
    }
    
    private fun setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        // Scan tab selected
                        showScanView()
                    }
                    1 -> {
                        // History tab selected
                        showHistoryView()
                    }
                }
            }
            
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
        
        // Select the Scan tab by default
        binding.tabLayout.selectTab(binding.tabLayout.getTabAt(0))
    }
    
    private fun showScanView() {
        isHistoryTabActive = false
        binding.scanViewContainer.visibility = View.VISIBLE
        binding.historyViewContainer.visibility = View.GONE
        
        // Resume scanning if not in review state
        if (viewModel.scanState.value != ScanState.REVIEW_PENDING) {
            viewModel.resumeScanning()
        }
    }
    
    private fun showHistoryView() {
        isHistoryTabActive = true
        binding.scanViewContainer.visibility = View.GONE
        binding.historyViewContainer.visibility = View.VISIBLE
        
        // Pause scanning when viewing history
        viewModel.pauseScanning()
        
        // Load and display history
        loadHistory()
    }
    
    private fun loadHistory() {
        val history = historyManager.loadHistory(ScanHistoryItem.ActivityType.CHECKOUT)
        historyAdapter.submitList(history)
        
        // Show empty state if no history
        binding.emptyHistoryText.visibility = if (history.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    
    private fun addScanToHistory(scannedValue: String, scanType: ScanHistoryItem.ScanType) {
        val historyItem = ScanHistoryItem(
            value = scannedValue,
            scanType = scanType,
            activityType = ScanHistoryItem.ActivityType.CHECKOUT
        )
        historyManager.addToHistory(historyItem)
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
                "Are you sure you want to undo the last checkout?"
            ) {
                viewModel.undoLastCheckout()
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
            // Use the unified export manager for checkout exports
            UniversalExportManager.getInstance(this)
                .startExport(AppConfig.EXPORT_TYPE_CHECKOUT, this)
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
        cameraManager.startCamera(
            previewView = binding.previewView,
            imageAnalyzer = BarcodeAnalyzer(),
            onError = { exception ->
                Log.e(TAG, "Camera initialization failed", exception)
                DialogUtils.showErrorToast(this, "Camera initialization failed")
            }
        )
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
                                // Add to history before processing
                                if (!isHistoryTabActive) {
                                    addScanToHistory(barcodeData, ScanHistoryItem.ScanType.BARCODE)
                                }
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
