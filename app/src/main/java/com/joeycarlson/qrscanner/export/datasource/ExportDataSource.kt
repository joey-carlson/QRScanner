package com.joeycarlson.qrscanner.export.datasource

import java.time.LocalDate

/**
 * Base interface for export data sources.
 * This abstraction allows the export system to work with any type of data
 * without being coupled to specific repositories or data structures.
 */
interface ExportDataSource {
    
    /**
     * Get the export type identifier for this data source
     */
    fun getExportType(): String
    
    /**
     * Get the display name for this data source (shown in UI)
     */
    fun getDisplayName(): String
    
    /**
     * Check if this data source supports date range filtering
     */
    fun supportsDateRange(): Boolean
    
    /**
     * Get data for a specific date range
     * @param startDate Start of the date range (inclusive)
     * @param endDate End of the date range (inclusive)
     * @return Map of dates to their respective data, serialized as strings
     */
    suspend fun getDataForDateRange(startDate: LocalDate, endDate: LocalDate): Map<LocalDate, String>
    
    /**
     * Get all available data (used for exports that don't support date ranges)
     * @return The data serialized as a string
     */
    suspend fun getAllData(): String
    
    /**
     * Get the filename prefix for exports from this data source
     * @param date The date for the export (may be null for non-date-based exports)
     */
    fun getFilenamePrefix(date: LocalDate? = null): String
    
    /**
     * Get supported export formats for this data source
     */
    fun getSupportedFormats(): List<ExportFormat>
    
    /**
     * Check if this data source has any data to export
     */
    suspend fun hasData(): Boolean
    
    /**
     * Get record count for display purposes
     * @param startDate Start of the date range (optional)
     * @param endDate End of the date range (optional)
     */
    suspend fun getRecordCount(startDate: LocalDate? = null, endDate: LocalDate? = null): Int
}

/**
 * Export format with additional metadata
 */
data class ExportFormat(
    val format: com.joeycarlson.qrscanner.export.ExportFormat,
    val displayName: String,
    val description: String,
    val isDefault: Boolean = false
)
