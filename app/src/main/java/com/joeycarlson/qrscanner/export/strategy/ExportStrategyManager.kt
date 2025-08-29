package com.joeycarlson.qrscanner.export.strategy

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.preference.PreferenceManager
import com.joeycarlson.qrscanner.config.PreferenceKeys
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.data.CheckoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Manager class that coordinates all export strategies and provides a unified interface.
 * Handles strategy selection, validation, and execution.
 */
class ExportStrategyManager(private val context: Context) {
    
    private val repository = CheckoutRepository(context)
    
    /**
     * Get all available export strategies
     */
    fun getAllStrategies(): List<ExportStrategy> {
        return buildList {
            // Local storage strategies
            addAll(LocalStorageExportStrategyFactory.getAllStrategies())
            
            // Share strategies
            addAll(ShareExportStrategyFactory.getAllStrategies())
            
            // TODO: Add more strategies as they're implemented
            // - EmailExportStrategy
            // - SMSExportStrategy  
            // - S3ExportStrategy
            // - SlackExportStrategy
        }
    }
    
    /**
     * Get strategies filtered by availability and configuration
     */
    fun getAvailableStrategies(): List<ExportStrategy> {
        return getAllStrategies().filter { strategy ->
            // Filter out strategies that require network when offline
            if (strategy.requiresNetwork() && !isNetworkAvailable()) {
                false
            } else {
                // Include strategies that don't require configuration or are properly configured
                !strategy.requiresConfiguration(context)
            }
        }
    }
    
    /**
     * Get strategies that require configuration
     */
    fun getUnconfiguredStrategies(): List<ExportStrategy> {
        return getAllStrategies().filter { strategy ->
            strategy.requiresConfiguration(context)
        }
    }
    
    /**
     * Execute export using specified strategy
     */
    suspend fun executeExport(
        strategy: ExportStrategy,
        startDate: LocalDate,
        endDate: LocalDate
    ): ExportResult = withContext(Dispatchers.IO) {
        
        try {
            // Pre-flight checks
            val locationId = getLocationId()
            if (locationId.isBlank()) {
                return@withContext ExportResult.ConfigurationRequired(
                    requirements = listOf("Location ID"),
                    message = "Location ID must be configured in settings before exporting"
                )
            }
            
            // Check network requirement
            if (strategy.requiresNetwork() && !isNetworkAvailable()) {
                return@withContext ExportResult.NetworkRequired()
            }
            
            // Check configuration requirement
            if (strategy.requiresConfiguration(context)) {
                return@withContext ExportResult.ConfigurationRequired(
                    requirements = strategy.getConfigurationRequirements(),
                    message = "This export method requires additional configuration"
                )
            }
            
            // Load records for date range
            val records = loadRecordsForDateRange(startDate, endDate)
            if (records.isEmpty() || records.values.all { it.isEmpty() }) {
                return@withContext ExportResult.NoData
            }
            
            // Execute the strategy
            strategy.export(context, records, locationId)
            
        } catch (e: Exception) {
            ExportResult.Error("Export execution failed: ${e.message}", e)
        }
    }
    
    /**
     * Load records for a date range
     */
    private suspend fun loadRecordsForDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<LocalDate, List<CheckoutRecord>> {
        val records = mutableMapOf<LocalDate, List<CheckoutRecord>>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            val dateRecords = repository.getRecordsForDate(currentDate)
            if (dateRecords.isNotEmpty()) {
                records[currentDate] = dateRecords
            }
            currentDate = currentDate.plusDays(1)
        }
        
        return records
    }
    
    /**
     * Get location ID from preferences
     */
    private fun getLocationId(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PreferenceKeys.LOCATION_ID, "") ?: ""
    }
    
    /**
     * Check if network is available
     * Uses NetworkCapabilities for API 23+ and deprecated NetworkInfo for older versions
     */
    @Suppress("DEPRECATION")
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected == true
        }
    }
    
    /**
     * Handle export result and create appropriate response
     */
    fun handleExportResult(result: ExportResult): ExportResultHandler {
        return ExportResultHandler(context, result)
    }
    
    /**
     * Get strategy by display name
     */
    fun getStrategyByName(displayName: String): ExportStrategy? {
        return getAllStrategies().find { it.getDisplayName() == displayName }
    }
    
    /**
     * Get strategies by format
     */
    fun getStrategiesByFormat(format: ExportFormat): List<ExportStrategy> {
        return getAllStrategies().filter { strategy ->
            when (strategy) {
                is LocalStorageExportStrategy -> strategy.format == format
                is ShareExportStrategy -> strategy.format == format
                else -> false
            }
        }
    }
    
    /**
     * Get strategies by destination
     */
    fun getStrategiesByDestination(destination: ExportDestination): List<ExportStrategy> {
        return getAllStrategies().filter { strategy ->
            when (destination) {
                ExportDestination.LOCAL_STORAGE -> strategy is LocalStorageExportStrategy
                ExportDestination.SHARE_INTENT -> strategy is ShareExportStrategy
                ExportDestination.CLOUD_STORAGE -> false // TODO: Implement cloud strategies
                ExportDestination.EMAIL -> false // TODO: Implement email strategy
                ExportDestination.SMS -> false // TODO: Implement SMS strategy
                ExportDestination.CUSTOM -> false // TODO: Implement custom strategies
            }
        }
    }
}

/**
 * Helper class to handle export results and provide appropriate actions
 */
class ExportResultHandler(
    private val context: Context,
    private val result: ExportResult
) {
    
    /**
     * Get user-friendly message for the result
     */
    fun getMessage(): String {
        return when (result) {
            is ExportResult.Success -> result.message
            is ExportResult.ShareReady -> result.additionalData["message"]?.toString() ?: "Files ready for sharing"
            is ExportResult.IntentReady -> "Ready to launch external app"
            is ExportResult.ConfigurationRequired -> result.message
            is ExportResult.NetworkRequired -> result.message
            is ExportResult.NoData -> "No data found for the selected date range"
            is ExportResult.Error -> result.message
        }
    }
    
    /**
     * Check if result requires user action
     */
    fun requiresUserAction(): Boolean {
        return when (result) {
            is ExportResult.ShareReady -> true
            is ExportResult.IntentReady -> true
            is ExportResult.ConfigurationRequired -> true
            is ExportResult.NetworkRequired -> true
            else -> false
        }
    }
    
    /**
     * Get action intent if available
     */
    fun getActionIntent(): Intent? {
        return when (result) {
            is ExportResult.ShareReady -> {
                ShareExportStrategy.createShareIntent(result)
            }
            is ExportResult.IntentReady -> {
                Intent(result.intentData.action).apply {
                    result.intentData.type?.let { type = it }
                    result.intentData.extras.forEach { (key, value) ->
                        when (value) {
                            is String -> putExtra(key, value)
                            is Int -> putExtra(key, value)
                            is Boolean -> putExtra(key, value)
                            is Long -> putExtra(key, value)
                            // Add more types as needed
                        }
                    }
                    if (result.intentData.fileUris.isNotEmpty()) {
                        if (result.intentData.fileUris.size == 1) {
                            putExtra(Intent.EXTRA_STREAM, result.intentData.fileUris[0])
                        } else {
                            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(result.intentData.fileUris))
                        }
                    }
                    flags = result.intentData.flags
                }
            }
            else -> null
        }
    }
    
    /**
     * Get cleanup action for temporary files
     */
    fun getCleanupAction(): (() -> Unit)? {
        return when (result) {
            is ExportResult.ShareReady -> {
                if (result.tempFiles.isNotEmpty()) {
                    {
                        val fileManager = com.joeycarlson.qrscanner.util.FileManager(context)
                        fileManager.cleanupTempFiles(result.tempFiles)
                    }
                } else null
            }
            is ExportResult.IntentReady -> {
                if (result.tempFiles.isNotEmpty()) {
                    {
                        val fileManager = com.joeycarlson.qrscanner.util.FileManager(context)
                        fileManager.cleanupTempFiles(result.tempFiles)
                    }
                } else null
            }
            else -> null
        }
    }
    
    /**
     * Check if result indicates success
     */
    fun isSuccess(): Boolean {
        return when (result) {
            is ExportResult.Success -> true
            is ExportResult.ShareReady -> true
            is ExportResult.IntentReady -> true
            else -> false
        }
    }
    
    /**
     * Check if result indicates error
     */
    fun isError(): Boolean {
        return result is ExportResult.Error
    }
    
    /**
     * Get error details if available
     */
    fun getError(): Throwable? {
        return (result as? ExportResult.Error)?.exception
    }
}
