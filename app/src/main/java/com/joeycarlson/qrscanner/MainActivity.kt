package com.joeycarlson.qrscanner

import android.Manifest
import android.animation.ObjectAnimator
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.joeycarlson.qrscanner.databinding.ActivityMainBinding
import com.joeycarlson.qrscanner.ui.ScanViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: ScanViewModel
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner
    
    private var imageCapture: ImageCapture? = null
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
        
        viewModel = ViewModelProvider(this)[ScanViewModel::class.java]
        cameraExecutor = Executors.newSingleThreadExecutor()
        
        // Initialize ML Kit barcode scanner
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
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
        viewModel.statusMessage.observe(this) { message ->
            binding.statusText.text = message
        }
        
        viewModel.isScanning.observe(this) { isScanning ->
            binding.scanOverlay.visibility = if (isScanning) {
                android.view.View.VISIBLE
            } else {
                android.view.View.INVISIBLE
            }
        }
        
        viewModel.scanSuccess.observe(this) { success ->
            if (success) {
                triggerFlashAnimation()
            }
        }
    }
    
    private fun triggerFlashAnimation() {
        binding.flashOverlay.visibility = View.VISIBLE
        binding.flashOverlay.alpha = 0f
        
        val flashAnimator = ObjectAnimator.ofFloat(binding.flashOverlay, "alpha", 0f, 1f, 0f)
        flashAnimator.duration = 300 // 300ms flash
        flashAnimator.start()
        
        // Hide the overlay after animation
        binding.flashOverlay.postDelayed({
            binding.flashOverlay.visibility = View.GONE
        }, 300)
    }
    
    private fun setupClickListeners() {
        binding.clearButton.setOnClickListener {
            viewModel.clearState()
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
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer())
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
    
    private inner class QRCodeAnalyzer : ImageAnalysis.Analyzer {
        
        @androidx.camera.core.ExperimentalGetImage
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image
            if (mediaImage != null && viewModel.isScanning.value == true) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        barcodes.firstOrNull()?.rawValue?.let { qrData ->
                            runOnUiThread {
                                viewModel.processQRCode(qrData)
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
