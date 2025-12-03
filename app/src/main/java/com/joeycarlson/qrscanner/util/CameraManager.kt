package com.joeycarlson.qrscanner.util

import android.content.Context
import android.util.Log
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.ExecutorService

/**
 * Centralized camera management utility for consistent camera operations across activities.
 * Handles camera initialization, preview setup, and image analysis binding.
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Manages camera lifecycle only
 * - Open/Closed: Extensible through callbacks
 * - Dependency Inversion: Uses abstractions (callbacks) instead of concrete implementations
 */
class CameraManager(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val cameraExecutor: ExecutorService
) {
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    
    /**
     * Starts the camera with the provided configuration.
     * 
     * @param previewView The PreviewView to display the camera preview
     * @param imageAnalyzer The ImageAnalysis.Analyzer to process camera frames
     * @param onSuccess Callback invoked when camera starts successfully
     * @param onError Callback invoked when camera initialization fails
     */
    fun startCamera(
        previewView: PreviewView,
        imageAnalyzer: ImageAnalysis.Analyzer,
        onSuccess: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Build preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // Build image analyzer
                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, imageAnalyzer)
                    }
                
                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // Unbind all use cases before rebinding
                cameraProvider?.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                
                onSuccess?.invoke()
                
            } catch (exc: Exception) {
                Log.e(TAG, "Camera initialization failed", exc)
                onError?.invoke(exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * Starts the camera with custom image analysis configuration.
     * 
     * @param previewView The PreviewView to display the camera preview
     * @param imageAnalyzer The ImageAnalysis.Analyzer to process camera frames
     * @param targetResolution Optional target resolution for image analysis
     * @param onSuccess Callback invoked when camera starts successfully
     * @param onError Callback invoked when camera initialization fails
     */
    fun startCameraWithConfig(
        previewView: PreviewView,
        imageAnalyzer: ImageAnalysis.Analyzer,
        targetResolution: android.util.Size? = null,
        onSuccess: (() -> Unit)? = null,
        onError: ((Exception) -> Unit)? = null
    ) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                
                // Build preview
                val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }
                
                // Build image analyzer with optional target resolution
                val analysisBuilder = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                
                targetResolution?.let {
                    analysisBuilder.setTargetResolution(it)
                }
                
                val imageAnalysis = analysisBuilder.build()
                    .also {
                        it.setAnalyzer(cameraExecutor, imageAnalyzer)
                    }
                
                // Select back camera
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                // Unbind all use cases before rebinding
                cameraProvider?.unbindAll()
                
                // Bind use cases to camera
                camera = cameraProvider?.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                
                onSuccess?.invoke()
                
            } catch (exc: Exception) {
                Log.e(TAG, "Camera initialization failed", exc)
                onError?.invoke(exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    
    /**
     * Stops the camera and unbinds all use cases.
     */
    fun stopCamera() {
        cameraProvider?.unbindAll()
        camera = null
    }
    
    /**
     * Gets the current camera instance.
     * 
     * @return The current Camera instance, or null if camera is not initialized
     */
    fun getCamera(): Camera? = camera
    
    companion object {
        private const val TAG = "CameraManager"
    }
}
