package com.joeycarlson.qrscanner.export.strategy

import android.content.Context
import android.net.Uri
import com.joeycarlson.qrscanner.data.CheckoutRecord
import java.io.File
import java.time.LocalDate

/**
 * Strategy interface for different export methods.
 * Implements the Strategy pattern to allow flexible export implementations.
 */
interface ExportStrategy {
    
    /**
     * Export records for a date range using this strategy
     */
    suspend fun export(
        context: Context,
        records: Map<LocalDate, List<CheckoutRecord>>,
        locationId: String
    ): ExportResult
    
    /**
     * Get the display name for this export strategy
     */
    fun getDisplayName(): String
    
    /**
     * Get the description for this export strategy
     */
    fun getDescription(): String
    
    /**
     * Get the icon resource ID for this export strategy
     */
    fun getIconResId(): Int
    
    /**
     * Whether this strategy requires network connectivity
     */
    fun requiresNetwork(): Boolean = false
    
    /**
     * Whether this strategy requires additional configuration
     */
    fun requiresConfiguration(context: Context): Boolean = false
    
    /**
     * Get configuration requirements if any
     */
    fun getConfigurationRequirements(): List<String> = emptyList()
}

/**
 * Result of an export operation
 */
sealed class ExportResult {
    data class Success(
        val message: String,
        val fileUris: List<Uri> = emptyList(),
        val additionalData: Map<String, Any> = emptyMap()
    ) : ExportResult()
    
    data class ShareReady(
        val fileUris: List<Uri>,
        val tempFiles: List<File>,
        val mimeType: String = "application/json",
        val additionalData: Map<String, Any> = emptyMap()
    ) : ExportResult()
    
    data class IntentReady(
        val intentData: IntentData,
        val tempFiles: List<File> = emptyList()
    ) : ExportResult()
    
    data class ConfigurationRequired(
        val requirements: List<String>,
        val message: String
    ) : ExportResult()
    
    data class NetworkRequired(
        val message: String = "This export method requires network connectivity"
    ) : ExportResult()
    
    object NoData : ExportResult()
    
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : ExportResult()
}

/**
 * Intent data for export strategies that use Android Intents
 */
data class IntentData(
    val action: String,
    val type: String? = null,
    val extras: Map<String, Any> = emptyMap(),
    val fileUris: List<Uri> = emptyList(),
    val flags: Int = 0
)

/**
 * Export format enumeration
 */
enum class ExportFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
    XML("xml", "application/xml"),
    TXT("txt", "text/plain")
}

/**
 * Export destination enumeration
 */
enum class ExportDestination {
    LOCAL_STORAGE,
    CLOUD_STORAGE,
    EMAIL,
    SMS,
    SHARE_INTENT,
    CUSTOM
}
