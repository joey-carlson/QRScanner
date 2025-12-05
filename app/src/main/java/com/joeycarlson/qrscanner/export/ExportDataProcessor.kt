package com.joeycarlson.qrscanner.export

import com.google.gson.Gson
import com.joeycarlson.qrscanner.data.CheckInRecord
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.data.KitBundle
import com.joeycarlson.qrscanner.export.datasource.ExportDataSource
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Processes export data for different data sources and formats.
 * Handles data collection, parsing, and content generation.
 * 
 * Following SOLID principles:
 * - Single Responsibility: Focuses only on data processing
 * - Open/Closed: Extensible for new data types
 */
class ExportDataProcessor(
    private val contentGenerator: ContentGenerator
) {
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yy")
    
    /**
     * Data class representing processed export data for a single date
     */
    data class ProcessedExport(
        val filename: String,
        val content: String,
        val date: LocalDate
    )
    
    /**
     * Process data source into export-ready content
     */
    suspend fun processDataSource(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat,
        locationId: String
    ): List<ProcessedExport> {
        return if (dataSource.supportsDateRange()) {
            processDateRangeData(dataSource, startDate, endDate, format, locationId)
        } else {
            processSingleData(dataSource, format, locationId)
        }
    }
    
    /**
     * Process data source that supports date ranges
     */
    private suspend fun processDateRangeData(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat,
        locationId: String
    ): List<ProcessedExport> {
        val dataByDate = dataSource.getDataForDateRange(startDate, endDate)
        val results = mutableListOf<ProcessedExport>()
        
        for ((date, jsonData) in dataByDate) {
            val records = parseRecords(jsonData, dataSource.getExportType())
            val filename = generateFilename(dataSource, date, locationId, format)
            val content = generateContent(records, format, locationId, dataSource.getExportType())
            
            results.add(ProcessedExport(filename, content, date))
        }
        
        return results
    }
    
    /**
     * Process data source that doesn't support date ranges (e.g., inventory, logs)
     */
    private suspend fun processSingleData(
        dataSource: ExportDataSource,
        format: ExportFormat,
        locationId: String
    ): List<ProcessedExport> {
        val jsonData = dataSource.getAllData()
        
        if (jsonData.isEmpty()) {
            return emptyList()
        }
        
        val filename = generateFilename(dataSource, LocalDate.now(), locationId, format)
        val content = when (dataSource.getExportType()) {
            "inventory" -> jsonData // Inventory already returns formatted JSON
            "logs" -> jsonData // Logs are plain text
            else -> {
                val records = parseRecords(jsonData, dataSource.getExportType())
                generateContent(records, format, locationId, dataSource.getExportType())
            }
        }
        
        return listOf(ProcessedExport(filename, content, LocalDate.now()))
    }
    
    /**
     * Parse JSON records based on export type
     */
    private fun parseRecords(jsonData: String, exportType: String): List<Any> {
        return when (exportType) {
            "checkout" -> gson.fromJson(jsonData, Array<CheckoutRecord>::class.java).toList()
            "checkin" -> gson.fromJson(jsonData, Array<CheckInRecord>::class.java).toList()
            "kit_bundle" -> gson.fromJson(jsonData, Array<KitBundle>::class.java).toList()
            else -> emptyList()
        }
    }
    
    /**
     * Generate content in the specified format
     */
    private fun generateContent(
        records: List<Any>,
        format: ExportFormat,
        locationId: String,
        exportType: String
    ): String {
        return when (format) {
            ExportFormat.JSON -> gson.toJson(records)
            ExportFormat.CSV -> generateCsvContent(records, exportType, locationId)
            ExportFormat.TXT -> generateTxtContent(records, exportType, locationId)
            ExportFormat.XML -> generateXmlContent(records, exportType, locationId)
            ExportFormat.KIT_LABELS_CSV -> generateKitLabelsCsvContent(records)
        }
    }
    
    /**
     * Generate CSV content based on record type
     */
    private fun generateCsvContent(records: List<Any>, exportType: String, locationId: String): String {
        return when (exportType) {
            "checkout" -> contentGenerator.generateContent(
                records as List<CheckoutRecord>,
                ExportFormat.CSV,
                locationId
            )
            "checkin" -> contentGenerator.generateCheckInContent(
                records as List<CheckInRecord>,
                ExportFormat.CSV,
                locationId
            )
            "kit_bundle" -> contentGenerator.generateKitBundleContent(
                records as List<KitBundle>,
                ExportFormat.CSV,
                locationId
            )
            else -> ""
        }
    }
    
    /**
     * Generate TXT content based on record type
     */
    private fun generateTxtContent(records: List<Any>, exportType: String, locationId: String): String {
        return when (exportType) {
            "checkout" -> contentGenerator.generateContent(
                records as List<CheckoutRecord>,
                ExportFormat.TXT,
                locationId
            )
            "checkin" -> contentGenerator.generateCheckInContent(
                records as List<CheckInRecord>,
                ExportFormat.TXT,
                locationId
            )
            else -> ""
        }
    }
    
    /**
     * Generate XML content
     */
    private fun generateXmlContent(records: List<Any>, exportType: String, locationId: String): String {
        return contentGenerator.generateContent(
            records as List<CheckoutRecord>,
            ExportFormat.XML,
            locationId
        )
    }
    
    /**
     * Generate kit labels CSV content
     */
    private fun generateKitLabelsCsvContent(records: List<Any>): String {
        return contentGenerator.generateKitBundleContent(
            records as List<KitBundle>,
            ExportFormat.KIT_LABELS_CSV,
            "" // locationId not needed for kit labels
        )
    }
    
    /**
     * Generate filename for export
     */
    private fun generateFilename(
        dataSource: ExportDataSource,
        date: LocalDate,
        locationId: String,
        format: ExportFormat
    ): String {
        val prefix = dataSource.getFilenamePrefix(date)
        val dateStr = date.format(dateFormatter)
        return "${prefix}_${dateStr}_${locationId}.${format.extension}"
    }
}
