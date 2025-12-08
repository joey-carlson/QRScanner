package com.joeycarlson.qrscanner.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Centralized permission management utility following ClineRules 02-solid-android
 * Eliminates code duplication across activities and provides single responsibility for permissions
 */
class PermissionManager(private val activity: Activity) {
    
    // Callback functions for permission results
    private var onPermissionsGrantedCallback: (() -> Unit)? = null
    private var onPermissionsDeniedCallback: ((List<String>) -> Unit)? = null
    
    companion object {
        const val REQUEST_CODE_PERMISSIONS = 10
        const val REQUEST_CODE_STORAGE_PERMISSION = 1001
        
        private val CAMERA_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
        private val STORAGE_PERMISSIONS = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        
        // Combined permissions for activities that need both
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        
        /**
         * Get required permissions based on Android version
         */
        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Android 10+ only needs camera permission
                CAMERA_PERMISSIONS
            } else {
                // Android 9 and below needs both camera and storage permissions
                REQUIRED_PERMISSIONS
            }
        }
    }
    
    /**
     * Check if camera permission is granted
     */
    fun isCameraPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            activity, Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    /**
     * Check if storage permission is granted (Android version aware)
     */
    fun isStoragePermissionGranted(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ uses MediaStore API, no storage permission needed
            true
        } else {
            ContextCompat.checkSelfPermission(
                activity, Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Check if all required permissions are granted for camera activities
     */
    fun areAllPermissionsGranted(): Boolean {
        val cameraGranted = isCameraPermissionGranted()
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ only needs camera permission
            cameraGranted
        } else {
            // Android 9 and below needs both camera and storage permissions
            cameraGranted && isStoragePermissionGranted()
        }
    }
    
    /**
     * Check if specific array of permissions are all granted
     */
    fun arePermissionsGranted(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Request camera permission
     */
    fun requestCameraPermission() {
        ActivityCompat.requestPermissions(
            activity, CAMERA_PERMISSIONS, REQUEST_CODE_PERMISSIONS
        )
    }
    
    /**
     * Request storage permission
     */
    fun requestStoragePermission() {
        ActivityCompat.requestPermissions(
            activity, STORAGE_PERMISSIONS, REQUEST_CODE_STORAGE_PERMISSION
        )
    }
    
    /**
     * Request all required permissions for camera activities
     */
    fun requestAllRequiredPermissions() {
        val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            CAMERA_PERMISSIONS
        } else {
            REQUIRED_PERMISSIONS
        }
        
        ActivityCompat.requestPermissions(
            activity, permissionsToRequest, REQUEST_CODE_PERMISSIONS
        )
    }
    
    /**
     * Request specific array of permissions
     */
    fun requestPermissions(permissions: Array<String>, requestCode: Int = REQUEST_CODE_PERMISSIONS) {
        ActivityCompat.requestPermissions(activity, permissions, requestCode)
    }
    
    /**
     * Handle permission result for camera activities
     */
    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit
    ) {
        when (requestCode) {
            REQUEST_CODE_PERMISSIONS -> {
                if (grantResults.isNotEmpty() && 
                    grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
            REQUEST_CODE_STORAGE_PERMISSION -> {
                if (grantResults.isNotEmpty() && 
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
        }
    }
    
    /**
     * Get user-friendly permission description for error messages
     */
    fun getPermissionDescription(permission: String): String {
        return when (permission) {
            Manifest.permission.CAMERA -> "Camera access is required to scan QR codes"
            Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Storage access is required to save files"
            else -> "This permission is required for the app to function properly"
        }
    }
    
    /**
     * Set callback functions for permission results
     */
    fun setPermissionCallbacks(
        onPermissionsGranted: () -> Unit,
        onPermissionsDenied: (List<String>) -> Unit
    ) {
        onPermissionsGrantedCallback = onPermissionsGranted
        onPermissionsDeniedCallback = onPermissionsDenied
    }
    
    /**
     * Handle permission result using callbacks
     */
    fun handlePermissionResult(
        _permissions: Array<String>,
        grantResults: IntArray
    ) {
        val deniedPermissions = mutableListOf<String>()
        
        for (i in _permissions.indices) {
            if (grantResults.getOrNull(i) != PackageManager.PERMISSION_GRANTED) {
                deniedPermissions.add(_permissions[i])
            }
        }
        
        if (deniedPermissions.isEmpty()) {
            onPermissionsGrantedCallback?.invoke()
        } else {
            onPermissionsDeniedCallback?.invoke(deniedPermissions)
        }
    }
    
    /**
     * Check if we should show rationale for permission request
     */
    fun shouldShowPermissionRationale(permission: String): Boolean {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
    }
}
