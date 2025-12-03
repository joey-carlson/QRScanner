package com.joeycarlson.qrscanner.inventory

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.data.InventoryRepository
import com.joeycarlson.qrscanner.databinding.ActivityInventoryManagementBinding
import com.joeycarlson.qrscanner.export.UniversalExportManager
import com.joeycarlson.qrscanner.ocr.HybridScanAnalyzer
import com.joeycarlson.qrscanner.ocr.ScanMode
import com.joeycarlson.qrscanner.ocr.ScanResult
import com.joeycarlson.qrscanner.ui.DialogUtils
import com.joeycarlson.qrscanner.ui.HapticManager
import com.joeycarlson.qrscanner.util.FileManager
import com.joeycarlson.qrscanner.util.PermissionManager
import com.joeycarlson.qrscanner.util.WindowInsetsHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class InventoryManagementActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityInventoryManagementBinding
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var hapticManager: HapticManager
    private lateinit var permissionManager: PermissionManager
    private var camera: Camera? = null
    private lateinit var hybridAnalyzer: HybridScanAnalyzer
    private var currentScanMode = ScanMode.BARCODE_ONLY
    
    private val viewModel: InventoryViewModel by viewModels {
        InventoryViewModelFactory(
            InventoryRepository(applicationContext, FileManager(applicationContext))
        )
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInventoryManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        // Handle window insets
        WindowInsetsHelper.setupWindowInsets(this)
        WindowInsetsHelper.applySystemWindowInsetsPadding(binding.contentContainer)
        
        supportActionBar?.title = "Inventory Management"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        
        hapticManager = HapticManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        
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
                DialogUtils.showErrorToast(this, "Permissions not granted by the user.")
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
        // Observe scan success
        viewModel.scanSuccess.observe(this) { isSuccess ->
            if (isSuccess) {
                hapticManager.performSuccessHaptic()
                binding.flashOverlay.visibility = View.VISIBLE
                binding.flashOverlay.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .withEndAction {
                        binding.flashOverlay.animate()
                            .alpha(0f)
                            .setDuration(500)
                            .withEndAction {
                                binding.flashOverlay.visibility = View.GONE
                            }
                            .start()
                    }
                    .start()
            }
        }
        
        // Observe scan failure
        viewModel.scanFailure.observe(this) { isFailure ->
            if (isFailure) {
                hapticManager.performFailureHaptic()
                binding.flashOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.holo_red_light))
                binding.flashOverlay.visibility = View.VISIBLE
                binding.flashOverlay.animate()
                    .alpha(1f)
                    .setDuration(100)
                    .withEndAction {
                        binding.flashOverlay.animate()
                            .alpha(0f)
                            .setDuration(500)
                            .withEndAction {
                                binding.flashOverlay.visibility = View.GONE
                                binding.flashOverlay.setBackgroundColor(ContextCompat.getColor(this, android.R.color.white))
                            }
                            .start()
                    }
                    .start()
            }
        }
        
        // Observe status message
        viewModel.statusMessage.observe(this) { message ->
            binding.statusText.text = message
        }
        
        // Observe scan count
        viewModel.scanCount.observe(this) { count ->
            binding.scanCountText.text = "Scanned: $count devices"
        }
        
        // Observe current component type
        viewModel.currentComponentType.observe(this) { type ->
            updateComponentTypeSelection(type)
        }
        
        // Observe is scanning state
        viewModel.isScanning.observe(this) { isScanning ->
            // The analyzer automatically processes frames when camera is active
            // No manual start/stop needed
        }
    }
    
    private fun setupClickListeners() {
        // Component type selection
        binding.glassesChip.setOnClickListener {
            viewModel.setComponentType(ComponentType.GLASSES)
        }
        
        binding.controllerChip.setOnClickListener {
            viewModel.setComponentType(ComponentType.CONTROLLER)
        }
        
        binding.batteryChip.setOnClickListener {
            viewModel.setComponentType(ComponentType.BATTERY)
        }
        
        // Scan mode buttons
        binding.barcodeButton.setOnClickListener {
            currentScanMode = ScanMode.BARCODE_ONLY
            hybridAnalyzer.setScanMode(currentScanMode)
            binding.instructionText.text = "Position the barcode within the frame"
            binding.barcodeButton.alpha = 1.0f
            binding.ocrButton.alpha = 0.6f
        }
        
        binding.ocrButton.setOnClickListener {
            currentScanMode = ScanMode.OCR_ONLY
            hybridAnalyzer.setScanMode(currentScanMode)
            binding.instructionText.text = "Position the text within the frame"
            binding.barcodeButton.alpha = 0.6f
            binding.ocrButton.alpha = 1.0f
        }
        
        // Export button
        binding.exportFab.setOnClickListener {
            viewModel.exportInventory()
            // Use the unified export manager for inventory exports
            UniversalExportManager.getInstance(this)
                .startExport(AppConfig.EXPORT_TYPE_INVENTORY, this)
        }
        
        // Clear All button removed from UI - functionality commented out
        /*
        binding.clearButton.setOnClickListener {
            viewModel.clearInventory()
        }
        */
    }
    
    private fun updateComponentTypeSelection(type: ComponentType) {
        // Reset all chips
        listOf(binding.glassesChip, binding.controllerChip, binding.batteryChip).forEach { chip ->
            chip.isChecked = false
        }
        
        // Check the selected chip
        when (type) {
            ComponentType.GLASSES -> binding.glassesChip.isChecked = true
            ComponentType.CONTROLLER -> binding.controllerChip.isChecked = true
            ComponentType.BATTERY -> binding.batteryChip.isChecked = true
        }
    }
    
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.viewFinder.surfaceProvider)
                }
            
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            
            hybridAnalyzer = HybridScanAnalyzer(
                scanMode = currentScanMode,
                context = this,
                onScanResult = { scanResult ->
                    runOnUiThread {
                        when (scanResult) {
                            is ScanResult.BarcodeResult -> {
                                viewModel.processScan(scanResult.rawValue)
                            }
                            is ScanResult.OcrResult -> {
                                viewModel.processScan(scanResult.text)
                            }
                            is ScanResult.ManualInputRequired -> {
                                Log.w(TAG, "Manual input required: ${scanResult.reason}")
                            }
                        }
                    }
                },
                onError = { error ->
                    runOnUiThread {
                        Log.e(TAG, "Scan error: $error")
                    }
                }
            )
            
            imageAnalyzer.setAnalyzer(cameraExecutor, hybridAnalyzer)
            
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            
            try {
                cameraProvider.unbindAll()
                camera = cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
                DialogUtils.showErrorToast(this, "Camera initialization failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }
    
    
    
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        if (::hybridAnalyzer.isInitialized) {
            hybridAnalyzer.close()
        }
    }
    
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    
    companion object {
        private const val TAG = "InventoryManagement"
    }
}
