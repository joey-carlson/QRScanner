package com.joeycarlson.qrscanner.export

import android.content.Context
import android.content.Intent
import androidx.preference.PreferenceManager
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.config.PreferenceKeys
import com.joeycarlson.qrscanner.export.datasource.*
import com.joeycarlson.qrscanner.util.LogManager

/**
 * Universal export manager that provides a centralized entry point for all export operations.
 * This class manages the entire export flow regardless of the data source type.
 */
class UniversalExportManager private constructor(private val context: Context) {
    
    private val logManager = LogManager.getInstance(context)
    
    companion object {
        @Volatile
        private var INSTANCE: UniversalExportManager? = null
        
        fun getInstance(context: Context): UniversalExportManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UniversalExportManager(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
    
    /**
     * Start the export flow for a specific data source type
     * @param exportType The type of data to export (from AppConfig.EXPORT_TYPE_*)
     * @param activity The calling activity for starting the export UI
     * @param additionalData Optional additional data for special export types
     */
    fun startExport(
        exportType: String,
        activity: android.app.Activity,
        additionalData: Map<String, Any>? = null
    ) {
        // Log the export request
        logManager.log("UniversalExportManager", "Starting export for type: $exportType")
        
        // Check if location ID is configured (required for most exports)
        if (exportType != AppConfig.EXPORT_TYPE_LOGS && !isLocationConfigured()) {
            showLocationConfigurationDialog(activity)
            return
        }
        
        // Create the appropriate data source
        val dataSource = createDataSource(exportType)
        
        // Launch the unified export activity with the data source
        launchExportActivity(activity, dataSource, additionalData)
    }
    
    /**
     * Start export with a custom data source
     * This allows for future extensibility with custom data sources
     */
    fun startExportWithDataSource(
        dataSource: ExportDataSource,
        activity: android.app.Activity,
        additionalData: Map<String, Any>? = null
    ) {
        logManager.log("UniversalExportManager", "Starting export with custom data source: ${dataSource.getDisplayName()}")
        
        if (!isLocationConfigured() && dataSource.getExportType() != AppConfig.EXPORT_TYPE_LOGS) {
            showLocationConfigurationDialog(activity)
            return
        }
        
        launchExportActivity(activity, dataSource, additionalData)
    }
    
    /**
     * Create the appropriate data source based on export type
     */
    private fun createDataSource(exportType: String): ExportDataSource {
        return when (exportType) {
            AppConfig.EXPORT_TYPE_CHECKOUT -> CheckoutDataSource(context)
            AppConfig.EXPORT_TYPE_CHECKIN -> CheckInDataSource(context)
            AppConfig.EXPORT_TYPE_KIT_BUNDLE -> KitBundleDataSource(context)
            AppConfig.EXPORT_TYPE_INVENTORY -> InventoryDataSource(context)
            AppConfig.EXPORT_TYPE_LOGS -> LogsDataSource(context)
            else -> throw IllegalArgumentException("Unknown export type: $exportType")
        }
    }
    
    /**
     * Launch the unified export activity
     */
    private fun launchExportActivity(
        activity: android.app.Activity,
        dataSource: ExportDataSource,
        additionalData: Map<String, Any>?
    ) {
        val intent = Intent(activity, UnifiedExportActivity::class.java).apply {
            // Pass the data source type
            putExtra("export_type", dataSource.getExportType())
            putExtra("export_display_name", dataSource.getDisplayName())
            putExtra("supports_date_range", dataSource.supportsDateRange())
            
            // Pass any additional data
            additionalData?.forEach { (key, value) ->
                when (value) {
                    is String -> putExtra(key, value)
                    is Int -> putExtra(key, value)
                    is Boolean -> putExtra(key, value)
                    is Long -> putExtra(key, value)
                    is Float -> putExtra(key, value)
                    is Double -> putExtra(key, value)
                    // Add more types as needed
                }
            }
        }
        
        activity.startActivity(intent)
    }
    
    /**
     * Check if the location ID is configured
     */
    private fun isLocationConfigured(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val locationId = prefs.getString(PreferenceKeys.LOCATION_ID, "")
        return !locationId.isNullOrEmpty()
    }
    
    /**
     * Show dialog prompting user to configure location ID
     */
    private fun showLocationConfigurationDialog(activity: android.app.Activity) {
        androidx.appcompat.app.AlertDialog.Builder(activity)
            .setTitle("Configuration Required")
            .setMessage("Please configure Location ID in Settings before exporting.")
            .setPositiveButton("Go to Settings") { _, _ ->
                val intent = Intent(activity, com.joeycarlson.qrscanner.SettingsActivity::class.java)
                activity.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    /**
     * Get available export methods for a specific data source type
     */
    fun getAvailableExportMethods(exportType: String): List<ExportMethodInfo> {
        val dataSource = createDataSource(exportType)
        val methods = mutableListOf<ExportMethodInfo>()
        
        // Add format-specific export methods
        dataSource.getSupportedFormats().forEach { format ->
            // Save to Downloads
            methods.add(
                ExportMethodInfo(
                    id = "download_${format.format.extension}",
                    displayName = "Save as ${format.displayName}",
                    description = "Save ${format.displayName} files to Downloads folder",
                    icon = "📁",
                    requiresNetwork = false,
                    format = format.format
                )
            )
            
            // Share via Android
            methods.add(
                ExportMethodInfo(
                    id = "share_${format.format.extension}",
                    displayName = "Share ${format.displayName}",
                    description = "Share ${format.displayName} files via installed apps",
                    icon = "📤",
                    requiresNetwork = false,
                    format = format.format
                )
            )
        }
        
        // Add email option if supported
        if (dataSource.getSupportedFormats().isNotEmpty()) {
            methods.add(
                ExportMethodInfo(
                    id = "email",
                    displayName = "Email",
                    description = "Send files as email attachments",
                    icon = "📧",
                    requiresNetwork = false,
                    format = dataSource.getSupportedFormats().first().format
                )
            )
            
            // Add SMS option
            methods.add(
                ExportMethodInfo(
                    id = "sms",
                    displayName = "SMS/Text",
                    description = "Send file links via text message",
                    icon = "💬",
                    requiresNetwork = false,
                    format = dataSource.getSupportedFormats().first().format
                )
            )
        }
        
        // Add S3 options if configured
        if (isS3Configured()) {
            dataSource.getSupportedFormats().forEach { format ->
                if (format.format == ExportFormat.JSON || format.format == ExportFormat.CSV) {
                    methods.add(
                        ExportMethodInfo(
                            id = "s3_${format.format.extension}",
                            displayName = "S3 ${format.displayName}",
                            description = "Upload ${format.displayName} to AWS S3",
                            icon = "☁️",
                            requiresNetwork = true,
                            format = format.format
                        )
                    )
                }
            }
        }
        
        return methods
    }
    
    /**
     * Check if S3 is configured
     */
    private fun isS3Configured(): Boolean {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return !prefs.getString("s3_bucket_name", "").isNullOrEmpty() &&
               !prefs.getString("s3_region", "").isNullOrEmpty()
    }
}

/**
 * Information about an export method
 */
data class ExportMethodInfo(
    val id: String,
    val displayName: String,
    val description: String,
    val icon: String,
    val requiresNetwork: Boolean,
    val format: ExportFormat
)

/**
 * Special data source for exporting diagnostic logs
 */
class LogsDataSource(private val context: Context) : ExportDataSource {
    
    private val logManager = LogManager.getInstance(context)
    
    override fun getExportType(): String = AppConfig.EXPORT_TYPE_LOGS
    
    override fun getDisplayName(): String = "Diagnostic Logs"
    
    override fun supportsDateRange(): Boolean = false
    
    override suspend fun getDataForDateRange(startDate: java.time.LocalDate, endDate: java.time.LocalDate): Map<java.time.LocalDate, String> {
        return mapOf(java.time.LocalDate.now() to logManager.exportLogs())
    }
    
    override suspend fun getAllData(): String {
        return logManager.exportLogs()
    }
    
    override fun getFilenamePrefix(date: java.time.LocalDate?): String {
        return "qrscanner_logs"
    }
    
    override fun getSupportedFormats(): List<com.joeycarlson.qrscanner.export.datasource.ExportFormat> {
        return listOf(
            com.joeycarlson.qrscanner.export.datasource.ExportFormat(
                format = ExportFormat.TXT,
                displayName = "Text",
                description = "Plain text log file",
                isDefault = true
            )
        )
    }
    
    override suspend fun hasData(): Boolean {
        return logManager.hasLogs()
    }
    
    override suspend fun getRecordCount(startDate: java.time.LocalDate?, endDate: java.time.LocalDate?): Int {
        return if (logManager.hasLogs()) 1 else 0
    }
}
