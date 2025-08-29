package com.joeycarlson.qrscanner.export.strategy

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.util.FileManager
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Base implementation for export strategies providing common functionality.
 * Handles file naming, content generation, and common validation.
 */
abstract class BaseExportStrategy : ExportStrategy {
    
    protected val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    protected val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(AppConfig.DATE_FORMAT_FILENAME)
    protected val displayDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern(AppConfig.DATE_FORMAT_DISPLAY)
    
    /**
     * Generate filename for a specific date and location
     */
    protected fun generateFilename(
        date: LocalDate,
        locationId: String,
        format: ExportFormat = ExportFormat.JSON
    ): String {
        val dateStr = date.format(dateFormatter)
        return "${AppConfig.EXPORT_FILE_PREFIX}_${dateStr}_$locationId.${format.extension}"
    }
    
    /**
     * Generate JSON content for records
     */
    protected fun generateJsonContent(records: List<CheckoutRecord>): String {
        return gson.toJson(records)
    }
    
    /**
     * Generate CSV content for records
     */
    protected fun generateCsvContent(records: List<CheckoutRecord>, locationId: String): String {
        val stringBuilder = StringBuilder()
        
        // CSV header
        stringBuilder.appendLine(AppConfig.CSV_HEADER)
        
        // CSV data rows
        records.forEach { record ->
            stringBuilder.append("\"${record.userId ?: ""}\",")
            stringBuilder.append("\"${record.kitId ?: ""}\",")
            stringBuilder.append("\"${record.timestamp}\",")
            stringBuilder.appendLine("\"$locationId\"")
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * Validate records before export
     */
    protected fun validateRecords(records: Map<LocalDate, List<CheckoutRecord>>): ExportResult? {
        if (records.isEmpty() || records.values.all { it.isEmpty() }) {
            return ExportResult.NoData
        }
        
        val totalRecords = records.values.sumOf { it.size }
        if (totalRecords > AppConfig.Export.MAX_RECORDS_PER_FILE * AppConfig.Export.MAX_FILES_PER_EXPORT) {
            return ExportResult.Error("Too many records to export: $totalRecords. Maximum allowed: ${AppConfig.Export.MAX_RECORDS_PER_FILE * AppConfig.Export.MAX_FILES_PER_EXPORT}")
        }
        
        return null // No validation errors
    }
    
    /**
     * Validate location ID
     */
    protected fun validateLocationId(locationId: String): ExportResult? {
        if (locationId.isBlank()) {
            return ExportResult.ConfigurationRequired(
                requirements = listOf("Location ID"),
                message = "Location ID must be configured in settings before exporting"
            )
        }
        return null
    }
    
    /**
     * Check if external storage is available
     */
    protected fun checkStorageAvailability(context: Context): ExportResult? {
        val fileManager = FileManager(context)
        if (!fileManager.isExternalStorageWritable()) {
            return ExportResult.Error("External storage is not available or writable")
        }
        return null
    }
    
    /**
     * Generate export summary message
     */
    protected fun generateSummaryMessage(
        records: Map<LocalDate, List<CheckoutRecord>>,
        locationId: String,
        format: ExportFormat
    ): String {
        val totalRecords = records.values.sumOf { it.size }
        val fileCount = records.count { it.value.isNotEmpty() }
        val dateRange = if (records.size == 1) {
            records.keys.first().format(displayDateFormatter)
        } else {
            val sortedDates = records.keys.sorted()
            "${sortedDates.first().format(displayDateFormatter)} to ${sortedDates.last().format(displayDateFormatter)}"
        }
        
        return "Exported $totalRecords records from $locationId ($dateRange) in $fileCount ${format.extension.uppercase()} file(s)"
    }
    
    /**
     * Get content for specific format
     */
    protected fun getFormattedContent(
        records: List<CheckoutRecord>,
        locationId: String,
        format: ExportFormat
    ): String {
        return when (format) {
            ExportFormat.JSON -> generateJsonContent(records)
            ExportFormat.CSV -> generateCsvContent(records, locationId)
            ExportFormat.XML -> generateXmlContent(records, locationId)
            ExportFormat.TXT -> generateTxtContent(records, locationId)
        }
    }
    
    /**
     * Generate XML content for records
     */
    private fun generateXmlContent(records: List<CheckoutRecord>, locationId: String): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        stringBuilder.appendLine("<checkouts location=\"$locationId\">")
        
        records.forEach { record ->
            stringBuilder.appendLine("  <checkout>")
            stringBuilder.appendLine("    <user>${record.userId ?: ""}</user>")
            stringBuilder.appendLine("    <kit>${record.kitId ?: ""}</kit>")
            stringBuilder.appendLine("    <timestamp>${record.timestamp}</timestamp>")
            stringBuilder.appendLine("    <type>${record.type}</type>")
            if (!record.value.isNullOrBlank()) {
                stringBuilder.appendLine("    <value><![CDATA[${record.value}]]></value>")
            }
            stringBuilder.appendLine("  </checkout>")
        }
        
        stringBuilder.appendLine("</checkouts>")
        return stringBuilder.toString()
    }
    
    /**
     * Generate plain text content for records
     */
    private fun generateTxtContent(records: List<CheckoutRecord>, locationId: String): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("QR Checkout Records - Location: $locationId")
        stringBuilder.appendLine("=" * 50)
        stringBuilder.appendLine()
        
        records.forEach { record ->
            stringBuilder.appendLine("Timestamp: ${record.timestamp}")
            stringBuilder.appendLine("User: ${record.userId ?: "N/A"}")
            stringBuilder.appendLine("Kit: ${record.kitId ?: "N/A"}")
            stringBuilder.appendLine("Type: ${record.type}")
            if (!record.value.isNullOrBlank()) {
                stringBuilder.appendLine("Value: ${record.value}")
            }
            stringBuilder.appendLine("-" * 30)
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * Get file size estimate for records
     */
    protected fun estimateFileSize(records: List<CheckoutRecord>, format: ExportFormat): Long {
        // Rough estimation based on format
        val avgRecordSize = when (format) {
            ExportFormat.JSON -> 200 // bytes per record
            ExportFormat.CSV -> 100
            ExportFormat.XML -> 300
            ExportFormat.TXT -> 150
        }
        return records.size * avgRecordSize.toLong()
    }
    
    /**
     * Check if estimated file size is within limits
     */
    protected fun validateFileSize(records: List<CheckoutRecord>, format: ExportFormat): ExportResult? {
        val estimatedSize = estimateFileSize(records, format)
        val maxSizeBytes = AppConfig.Export.MAX_RECORDS_PER_FILE * 1024L // Rough limit
        
        if (estimatedSize > maxSizeBytes) {
            return ExportResult.Error("File size would be too large: ${estimatedSize / 1024}KB. Maximum: ${maxSizeBytes / 1024}KB")
        }
        
        return null
    }
}

/**
 * String repeat extension for Kotlin
 */
private operator fun String.times(count: Int): String {
    return this.repeat(count)
}
