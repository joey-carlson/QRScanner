package com.joeycarlson.qrscanner.export

import android.net.Uri
import java.io.File
import java.time.LocalDate

/**
 * Data class for email export information
 */
data class EmailExportData(
    val fileUris: List<Uri>,
    val tempFiles: List<File>,
    val locationId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalRecords: Int,
    val format: ExportFormat = ExportFormat.JSON
)

/**
 * Data class for SMS/MMS export information
 */
data class SMSExportData(
    val fileUris: List<Uri>,
    val tempFiles: List<File>,
    val locationId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalRecords: Int,
    val format: ExportFormat = ExportFormat.JSON
)

/**
 * Data class for S3 export configuration
 */
data class S3ExportConfig(
    val bucketName: String,
    val region: String,
    val accessKeyId: String,
    val secretAccessKey: String,
    val folderPrefix: String? = null
)

/**
 * Data class for Slack export configuration
 */
data class SlackExportConfig(
    val webhookUrl: String,
    val channelName: String? = null,
    val username: String = "QR Scanner Export"
)

/**
 * Sealed class representing different export results
 */
sealed class ExportResult {
    /**
     * Successful export to local storage
     */
    data class Success(val fileUris: List<Uri>) : ExportResult()
    
    /**
     * Files ready for sharing via Android share sheet
     */
    data class ShareReady(
        val fileUris: List<Uri>, 
        val tempFiles: List<File>, 
        val mimeType: String = "application/json"
    ) : ExportResult()
    
    /**
     * Files ready for email with additional metadata
     */
    data class EmailReady(val emailData: EmailExportData) : ExportResult()
    
    /**
     * Files ready for SMS/MMS with concise message
     */
    data class SMSReady(val smsData: SMSExportData) : ExportResult()
    
    /**
     * Successful S3 upload
     */
    data class S3Success(
        val uploadedFiles: List<String>, 
        val bucketName: String, 
        val region: String
    ) : ExportResult()
    
    /**
     * Successful Slack upload
     */
    data class SlackSuccess(
        val messageUrl: String,
        val filesUploaded: Int
    ) : ExportResult()
    
    /**
     * No data available for the specified date range
     */
    object NoData : ExportResult()
    
    /**
     * Export operation failed with error message
     */
    data class Error(val message: String, val exception: Throwable? = null) : ExportResult()
    
    /**
     * Export operation was cancelled by user
     */
    object Cancelled : ExportResult()
}

/**
 * Export operation configuration
 */
data class ExportConfig(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val format: ExportFormat = ExportFormat.JSON,
    val includeEmptyDays: Boolean = false,
    val compressFiles: Boolean = false
)

/**
 * Export progress information for UI updates
 */
data class ExportProgress(
    val currentDate: LocalDate,
    val totalDays: Int,
    val processedDays: Int,
    val currentRecords: Int,
    val totalRecords: Int,
    val status: String
) {
    val percentComplete: Int
        get() = if (totalDays > 0) (processedDays * 100) / totalDays else 0
}

/**
 * Export statistics after completion
 */
data class ExportStatistics(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalDays: Int,
    val daysWithData: Int,
    val totalRecords: Int,
    val filesGenerated: Int,
    val totalSizeBytes: Long,
    val format: ExportFormat,
    val exportDurationMs: Long
) {
    val totalSizeMB: Double
        get() = totalSizeBytes / (1024.0 * 1024.0)
        
    val averageRecordsPerDay: Double
        get() = if (daysWithData > 0) totalRecords.toDouble() / daysWithData else 0.0
}

/**
 * Export validation result
 */
sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}
