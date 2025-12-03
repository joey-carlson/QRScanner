package com.joeycarlson.qrscanner.util

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.MockitoJUnitRunner
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

/**
 * Unit tests for PermissionManager following ClineRules testing guidelines.
 * Uses AAA pattern (Arrange, Act, Assert) for clear, maintainable tests.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.Q]) // Test with Android 10 (Q)
class PermissionManagerTest {
    
    @Mock
    private lateinit var mockActivity: Activity
    
    private lateinit var permissionManager: PermissionManager
    
    @Before
    fun setup() {
        mockActivity = mock(Activity::class.java)
    }
    
    /**
     * Gist Test: Verify core functionality of PermissionManager
     * Tests the primary use case of checking and requesting permissions
     */
    @Test
    fun gistTest_permissionManagerCoreFlow() {
        // Arrange: Create PermissionManager with mock activity
        permissionManager = PermissionManager(mockActivity)
        
        // Act & Assert: Verify required permissions list contains camera
        val requiredPermissions = PermissionManager.getRequiredPermissions()
        assertTrue(requiredPermissions.contains(Manifest.permission.CAMERA))
        
        // Act & Assert: Verify Android 10+ only requires camera (not storage)
        assertEquals(1, requiredPermissions.size)
    }
    
    @Test
    fun getRequiredPermissions_android10Plus_returnsCameraOnly() {
        // Arrange: Already on Android 10+ (from @Config)
        
        // Act
        val permissions = PermissionManager.getRequiredPermissions()
        
        // Assert
        assertEquals(1, permissions.size)
        assertEquals(Manifest.permission.CAMERA, permissions[0])
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // Android 9
    fun getRequiredPermissions_android9AndBelow_includesStorage() {
        // Arrange: Running on Android 9
        
        // Act
        val permissions = PermissionManager.getRequiredPermissions()
        
        // Assert
        assertEquals(2, permissions.size)
        assertTrue(permissions.contains(Manifest.permission.CAMERA))
        assertTrue(permissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))
    }
    
    @Test
    fun areAllPermissionsGranted_allGranted_returnsTrue() {
        // Arrange
        `when`(mockActivity.checkSelfPermission(Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        permissionManager = PermissionManager(mockActivity)
        
        // Act
        val result = permissionManager.areAllPermissionsGranted()
        
        // Assert
        assertTrue(result)
    }
    
    @Test
    fun areAllPermissionsGranted_cameraDenied_returnsFalse() {
        // Arrange
        `when`(mockActivity.checkSelfPermission(Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        permissionManager = PermissionManager(mockActivity)
        
        // Act
        val result = permissionManager.areAllPermissionsGranted()
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    @Config(sdk = [Build.VERSION_CODES.P]) // Android 9
    fun areAllPermissionsGranted_android9_checksStoragePermission() {
        // Arrange
        `when`(mockActivity.checkSelfPermission(Manifest.permission.CAMERA))
            .thenReturn(PackageManager.PERMISSION_GRANTED)
        `when`(mockActivity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE))
            .thenReturn(PackageManager.PERMISSION_DENIED)
        permissionManager = PermissionManager(mockActivity)
        
        // Act
        val result = permissionManager.areAllPermissionsGranted()
        
        // Assert
        assertFalse(result)
    }
    
    @Test
    fun setPermissionCallbacks_onGranted_invokesCallback() {
        // Arrange
        var callbackInvoked = false
        permissionManager = PermissionManager(mockActivity)
        permissionManager.setPermissionCallbacks(
            onPermissionsGranted = { callbackInvoked = true },
            onPermissionsDenied = { }
        )
        
        // Act
        permissionManager.handlePermissionResult(
            permissions = arrayOf(Manifest.permission.CAMERA),
            grantResults = intArrayOf(PackageManager.PERMISSION_GRANTED)
        )
        
        // Assert
        assertTrue(callbackInvoked)
    }
    
    @Test
    fun setPermissionCallbacks_onDenied_invokesCallbackWithDeniedList() {
        // Arrange
        var deniedPermissions: List<String>? = null
        permissionManager = PermissionManager(mockActivity)
        permissionManager.setPermissionCallbacks(
            onPermissionsGranted = { },
            onPermissionsDenied = { denied -> deniedPermissions = denied }
        )
        
        // Act
        permissionManager.handlePermissionResult(
            permissions = arrayOf(Manifest.permission.CAMERA),
            grantResults = intArrayOf(PackageManager.PERMISSION_DENIED)
        )
        
        // Assert
        assertEquals(1, deniedPermissions?.size)
        assertEquals(Manifest.permission.CAMERA, deniedPermissions?.get(0))
    }
}
