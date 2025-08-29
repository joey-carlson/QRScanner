package com.joeycarlson.qrscanner.export

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Service responsible for generating consistent filenames for export files.
 * Follows the pattern: qr_checkouts_MM-dd-yy_[LocationID].[extension]
 */
class FileNamingService {
    
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yy")
    
    /**
     * Generates a filename based on the date, location ID, and format
     * Pattern: qr_checkouts_MM-dd-yy_[LocationID].[extension]
     */
    fun generateFilename(
        date: LocalDate, 
        locationId: String, 
        format: ExportFormat
    ): String {
        val dateStr = date.format(dateFormatter)
        val sanitizedLocationId = sanitizeLocationId(locationId)
        return "qr_checkouts_${dateStr}_$sanitizedLocationId.${format.extension}"
    }
    
    /**
     * Generates a filename for S3 uploads with folder structure
     * Pattern: [LocationID]/[Year]/[Month]/qr_checkouts_MM-dd-yy_[LocationID].[extension]
     */
    fun generateS3Key(
        date: LocalDate,
        locationId: String,
        format: ExportFormat
    ): String {
        val sanitizedLocationId = sanitizeLocationId(locationId)
        val year = date.year
        val month = String.format("%02d", date.monthValue)
        val filename = generateFilename(date, locationId, format)
        
        return "$sanitizedLocationId/$year/$month/$filename"
    }
    
    /**
     * Generates a descriptive filename for multi-day exports
     * Pattern: qr_checkouts_[StartDate]_to_[EndDate]_[LocationID].[extension]
     */
    fun generateRangeFilename(
        startDate: LocalDate,
        endDate: LocalDate,
        locationId: String,
        format: ExportFormat
    ): String {
        val startStr = startDate.format(dateFormatter)
        val endStr = endDate.format(dateFormatter)
        val sanitizedLocationId = sanitizeLocationId(locationId)
        return "qr_checkouts_${startStr}_to_${endStr}_$sanitizedLocationId.${format.extension}"
    }
    
    /**
     * Generates a temporary filename with timestamp for uniqueness
     * Pattern: temp_qr_checkouts_MM-dd-yy_[LocationID]_[timestamp].[extension]
     */
    fun generateTempFilename(
        date: LocalDate,
        locationId: String,
        format: ExportFormat
    ): String {
        val baseFilename = generateFilename(date, locationId, format)
        val timestamp = System.currentTimeMillis()
        return "temp_${baseFilename.removeSuffix(".${format.extension}")}_$timestamp.${format.extension}"
    }
    
    /**
     * Sanitizes location ID to ensure filesystem compatibility
     * Removes/replaces characters that might cause issues in filenames
     */
    private fun sanitizeLocationId(locationId: String): String {
        return locationId
            .replace(" ", "_")
            .replace("/", "-")
            .replace("\\", "-")
            .replace(":", "-")
            .replace("*", "")
            .replace("?", "")
            .replace("\"", "")
            .replace("<", "")
            .replace(">", "")
            .replace("|", "")
            .trim()
    }
    
    /**
     * Extracts date from a filename following the standard pattern
     * Returns null if the filename doesn't match the expected pattern
     */
    fun extractDateFromFilename(filename: String): LocalDate? {
        val regex = Regex("qr_checkouts_(\\d{2}-\\d{2}-\\d{2})_.*")
        val matchResult = regex.find(filename)
        
        return matchResult?.let {
            val dateStr = it.groupValues[1]
            try {
                LocalDate.parse(dateStr, dateFormatter)
            } catch (e: Exception) {
                null
            }
        }
    }
    
    /**
     * Extracts location ID from a filename following the standard pattern
     * Returns null if the filename doesn't match the expected pattern
     */
    fun extractLocationIdFromFilename(filename: String): String? {
        val regex = Regex("qr_checkouts_\\d{2}-\\d{2}-\\d{2}_(.*?)\\.(\\w+)")
        val matchResult = regex.find(filename)
        
        return matchResult?.groupValues?.get(1)
    }
}
