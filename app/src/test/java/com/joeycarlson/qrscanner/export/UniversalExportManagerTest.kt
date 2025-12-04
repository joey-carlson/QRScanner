package com.joeycarlson.qrscanner.export

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.joeycarlson.qrscanner.config.AppConfig
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Comprehensive test suite for UniversalExportManager
 * Verifies that share buttons from all features correctly navigate to UnifiedExportActivity
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class UniversalExportManagerTest {
    
    private lateinit var context: Context
    private lateinit var exportManager: UniversalExportManager
    private lateinit var testActivity: Activity
    
    @Before
    fun setup() {
        context = RuntimeEnvironment.getApplication()
        exportManager = UniversalExportManager.getInstance(context)
        testActivity = Robolectric.buildActivity(Activity::class.java).create().get()
    }
    
    /**
     * Test that checkout export starts UnifiedExportActivity with correct parameters
     * This verifies the share button from MainActivity (Kit Check Out)
     */
    @Test
    fun testCheckoutExportStartsUnifiedExportActivity() {
        // Act
        exportManager.startExport(AppConfig.EXPORT_TYPE_CHECKOUT, testActivity)
        
        // Assert
        val expectedIntent = Intent(testActivity, UnifiedExportActivity::class.java)
        val actualIntent = shadowOf(testActivity).nextStartedActivity
        
        assert(actualIntent != null) { "No activity was started" }
        assert(actualIntent.component?.className == UnifiedExportActivity::class.java.name) {
            "Expected UnifiedExportActivity but got ${actualIntent.component?.className}"
        }
        assert(actualIntent.getStringExtra("export_type") == AppConfig.EXPORT_TYPE_CHECKOUT) {
            "Expected export_type to be ${AppConfig.EXPORT_TYPE_CHECKOUT}"
        }
    }
    
    /**
     * Test that checkin export starts UnifiedExportActivity with correct parameters
     * This verifies the share button from CheckInActivity (Kit Check In)
     */
    @Test
    fun testCheckinExportStartsUnifiedExportActivity() {
        // Act
        exportManager.startExport(AppConfig.EXPORT_TYPE_CHECKIN, testActivity)
        
        // Assert
        val actualIntent = shadowOf(testActivity).nextStartedActivity
        
        assert(actualIntent != null) { "No activity was started" }
        assert(actualIntent.component?.className == UnifiedExportActivity::class.java.name) {
            "Expected UnifiedExportActivity but got ${actualIntent.component?.className}"
        }
        assert(actualIntent.getStringExtra("export_type") == AppConfig.EXPORT_TYPE_CHECKIN) {
            "Expected export_type to be ${AppConfig.EXPORT_TYPE_CHECKIN}"
        }
    }
    
    /**
     * Test that kit bundle export starts UnifiedExportActivity with correct parameters
     * This verifies the share button from KitBundleActivity
     */
    @Test
    fun testKitBundleExportStartsUnifiedExportActivity() {
        // Act
        exportManager.startExport(AppConfig.EXPORT_TYPE_KIT_BUNDLE, testActivity)
        
        // Assert
        val actualIntent = shadowOf(testActivity).nextStartedActivity
        
        assert(actualIntent != null) { "No activity was started" }
        assert(actualIntent.component?.className == UnifiedExportActivity::class.java.name) {
            "Expected UnifiedExportActivity but got ${actualIntent.component?.className}"
        }
        assert(actualIntent.getStringExtra("export_type") == AppConfig.EXPORT_TYPE_KIT_BUNDLE) {
            "Expected export_type to be ${AppConfig.EXPORT_TYPE_KIT_BUNDLE}"
        }
    }
    
    /**
     * Test that inventory export starts UnifiedExportActivity with correct parameters
     * This verifies the share button from InventoryManagementActivity
     */
    @Test
    fun testInventoryExportStartsUnifiedExportActivity() {
        // Act
        exportManager.startExport(AppConfig.EXPORT_TYPE_INVENTORY, testActivity)
        
        // Assert
        val actualIntent = shadowOf(testActivity).nextStartedActivity
        
        assert(actualIntent != null) { "No activity was started" }
        assert(actualIntent.component?.className == UnifiedExportActivity::class.java.name) {
            "Expected UnifiedExportActivity but got ${actualIntent.component?.className}"
        }
        assert(actualIntent.getStringExtra("export_type") == AppConfig.EXPORT_TYPE_INVENTORY) {
            "Expected export_type to be ${AppConfig.EXPORT_TYPE_INVENTORY}"
        }
    }
    
    /**
     * Test that all export types consistently navigate to the same UnifiedExportActivity
     * This is a "gist test" that covers the fundamental functionality
     */
    @Test
    fun testAllExportTypesUseUnifiedExportActivity() {
        val exportTypes = listOf(
            AppConfig.EXPORT_TYPE_CHECKOUT,
            AppConfig.EXPORT_TYPE_CHECKIN,
            AppConfig.EXPORT_TYPE_KIT_BUNDLE,
            AppConfig.EXPORT_TYPE_INVENTORY
        )
        
        exportTypes.forEach { exportType ->
            // Arrange - create fresh activity for each test
            val activity = Robolectric.buildActivity(Activity::class.java).create().get()
            
            // Act
            exportManager.startExport(exportType, activity)
            
            // Assert
            val intent = shadowOf(activity).nextStartedActivity
            assert(intent != null) { "No activity started for $exportType" }
            assert(intent.component?.className == UnifiedExportActivity::class.java.name) {
                "Export type $exportType should start UnifiedExportActivity but started ${intent.component?.className}"
            }
            assert(intent.getStringExtra("export_type") == exportType) {
                "Export type should be $exportType"
            }
        }
    }
    
    /**
     * Test that export intent includes required metadata
     */
    @Test
    fun testExportIntentIncludesRequiredMetadata() {
        // Act
        exportManager.startExport(AppConfig.EXPORT_TYPE_CHECKOUT, testActivity)
        
        // Assert
        val intent = shadowOf(testActivity).nextStartedActivity
        assert(intent.hasExtra("export_type")) { "Intent should have export_type extra" }
        assert(intent.hasExtra("export_display_name")) { "Intent should have export_display_name extra" }
        assert(intent.hasExtra("supports_date_range")) { "Intent should have supports_date_range extra" }
    }
    
    /**
     * Test singleton pattern - verify same instance is returned
     */
    @Test
    fun testSingletonPattern() {
        val instance1 = UniversalExportManager.getInstance(context)
        val instance2 = UniversalExportManager.getInstance(context)
        
        assert(instance1 === instance2) { "UniversalExportManager should return same instance" }
    }
}
