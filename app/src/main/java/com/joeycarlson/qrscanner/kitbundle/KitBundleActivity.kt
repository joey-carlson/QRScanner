package com.joeycarlson.qrscanner.kitbundle

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.databinding.ActivityKitBundleBinding
import com.joeycarlson.qrscanner.export.UniversalExportManager
import com.joeycarlson.qrscanner.ocr.HybridScanAnalyzer
import com.joeycarlson.qrscanner.ocr.ScanMode
import com.joeycarlson.qrscanner.ocr.ScanResult
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.ui.HapticManager
import com.joeycarlson.qrscanner.util.CameraManager
import com.joeycarlson.qrscanner.util.LogManager
import com.joeycarlson.qrscanner.util.PermissionManager
import com.joeycarlson.qrscanner.util.WindowInsetsHelper
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class KitBundleActivity : AppCompatActivity() {
    private lateinit var binding: ActivityKitBundleBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraManager: CameraManager
    private lateinit var hybridScanAnalyzer: HybridScanAnalyzer
    private lateinit var viewModel: KitBundleViewModel
    private lateinit var hapticManager: HapticManager
    private lateinit var permissionManager: PermissionManager
    private var currentScanMode = ScanMode.BARCODE_ONLY
    
    // Component slots adapter removed - using different UI approach
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKitBundleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val repository = com.joeycarlson.qrscanner.data.KitRepository(this)
        val factory = KitBundleViewModelFactory(application, repository)
        viewModel = viewModels<KitBundleViewModel> { factory }.value
        
        hapticManager = HapticManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraManager = CameraManager(this, this, cameraExecutor)
        
        WindowInsetsHelper.setupWindowInsets(this)
        WindowInsetsHelper.applySystemWindowInsetsPadding(binding.root)
        
        
        setupViews()
        setupObservers()
        
        // Initialize permission manager
        permissionManager = PermissionManager(this)
        
        // Set up permission callbacks
        permissionManager.setPermissionCallbacks(
            onPermissionsGranted = {
                startCamera()
            },
            onPermissionsDenied = { _ ->
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
    
    private fun setupViews() {
        // Set up scan mode buttons
        binding.barcodeButton.setOnClickListener {
            currentScanMode = ScanMode.BARCODE_ONLY
            if (::hybridScanAnalyzer.isInitialized) {
                hybridScanAnalyzer.setScanMode(currentScanMode)
            }
            updateUI(currentScanMode)
        }
        
        binding.ocrButton.setOnClickListener {
            currentScanMode = ScanMode.OCR_ONLY
            if (::hybridScanAnalyzer.isInitialized) {
                hybridScanAnalyzer.setScanMode(currentScanMode)
            }
            updateUI(currentScanMode)
        }

        // Clear button removed from UI - functionality commented out
        /*
        binding.clearButton.setOnClickListener {
            viewModel.clearState()
        }
        */

        binding.exportButton.setOnClickListener {
            viewModel.saveKitBundle()
            // Use the unified export manager for kit bundle exports
            UniversalExportManager.getInstance(this)
                .startExport(AppConfig.EXPORT_TYPE_KIT_BUNDLE, this)
        }
        
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                finish()
            }
        })
    }
    
    private fun setupObservers() {
        lifecycleScope.launch {
            viewModel.statusMessage.collect { message ->
                binding.statusText.text = message
            }
        }
        
        lifecycleScope.launch {
            viewModel.instructionText.collect { instruction ->
                binding.instructionText.text = instruction
            }
        }
        
        lifecycleScope.launch {
            viewModel.isScanning.collect { _ ->
                // Scanning is controlled by camera binding, no need to enable/disable analyzer
            }
        }
        
        lifecycleScope.launch {
            viewModel.showExportButton.collect { show ->
                binding.exportButton.visibility = if (show) View.VISIBLE else View.GONE
            }
        }
        
        lifecycleScope.launch {
            viewModel.scanSuccess.collect { success ->
                if (success) {
                    // Flash the overlay for visual feedback
                    binding.flashOverlay.visibility = View.VISIBLE
                    binding.flashOverlay.postDelayed({
                        binding.flashOverlay.visibility = View.GONE
                    }, 100)
                }
            }
        }
        
        lifecycleScope.launch {
            viewModel.errorMessage.collect { errorMsg ->
                errorMsg?.let {
                    DialogUtils.showErrorSnackbar(binding.root, it)
                }
            }
        }
    }
    
    // UI state handling simplified - using individual state flows from ViewModel
    
    private fun saveCurrentBundle() {
        viewModel.saveCurrentBundle()
        hapticManager.performSuccessHaptic()
        DialogUtils.showSuccessSnackbar(binding.root, "Bundle saved")
    }
    
    private fun skipToNext() {
        viewModel.skipToNextBundle()
    }
    
    private fun startCamera() {
        binding.previewView.visibility = View.VISIBLE
        
        // Create HybridScanAnalyzer with callbacks
        hybridScanAnalyzer = HybridScanAnalyzer(
            scanMode = currentScanMode,
            context = this,
            onScanResult = { scanResult ->
                runOnUiThread {
                    when (scanResult) {
                        is ScanResult.BarcodeResult -> {
                            handleBarcodeScan(scanResult.rawValue)
                        }
                        is ScanResult.OcrResult -> {
                            if (scanResult.requiresManualVerification) {
                                // Show confirmation dialog for low confidence OCR
                                val dialog = ComponentConfirmationDialog.newInstance(
                                    dsn = scanResult.text,
                                    componentType = null,
                                    suggestedSlot = null
                                )
                                dialog.setOnComponentConfirmListener(object : ComponentConfirmationDialog.OnComponentConfirmListener {
                                    override fun onConfirm(dsn: String, slot: String) {
                                        handleOcrScan(dsn)
                                    }
                                    override fun onCancel() {
                                        // Resume scanning if cancelled
                                        viewModel.resumeScanning()
                                    }
                                })
                                dialog.show(supportFragmentManager, "ComponentConfirmationDialog")
                            } else {
                                handleOcrScan(scanResult.text)
                            }
                        }
                        is ScanResult.ManualInputRequired -> {
                            LogManager.getInstance(this).log("KitBundleActivity", "Manual input required: ${scanResult.reason}")
                        }
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    hapticManager.performFailureHaptic()
                    viewModel.showError(error.message ?: "Scan error occurred")
                }
            }
        )
        
        // Start camera using CameraManager
        cameraManager.startCamera(
            previewView = binding.previewView,
            imageAnalyzer = hybridScanAnalyzer,
            onError = { exception ->
                LogManager.getInstance(this).logError("KitBundleActivity", "Camera initialization failed", exception)
                DialogUtils.showErrorSnackbar(binding.root, "Camera initialization failed")
            }
        )
    }
    
    private fun handleBarcodeScan(barcode: String) {
        hapticManager.performSuccessHaptic()
        viewModel.processBarcode(barcode)
    }
    
    private fun handleOcrScan(text: String) {
        hapticManager.performSuccessHaptic()
        viewModel.processBarcode(text)
    }
    
    private fun updateUI(mode: ScanMode) {
        // Update button states
        binding.barcodeButton.alpha = if (mode == ScanMode.BARCODE_ONLY) 1.0f else 0.6f
        binding.ocrButton.alpha = if (mode == ScanMode.OCR_ONLY) 1.0f else 0.6f
    }
    
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::hybridScanAnalyzer.isInitialized) {
            hybridScanAnalyzer.close()
        }
    }
    
    companion object {
        private const val TAG = "KitBundleActivity"
    }
}
