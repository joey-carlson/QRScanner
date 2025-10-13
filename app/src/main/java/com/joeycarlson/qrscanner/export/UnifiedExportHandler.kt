package com.joeycarlson.qrscanner.export

import android.content.Context
import android.content.Intent
import com.google.gson.Gson
import com.joeycarlson.qrscanner.config.PreferenceKeys
import com.joeycarlson.qrscanner.export.datasource.*
import com.joeycarlson.qrscanner.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Handles export operations for the unified export system.
 * This class processes export requests from UnifiedExportActivity using the appropriate data sources.
 */
class UnifiedExportHandler(private val context: Context) {
    
    private val fileManager = FileManager(context)
    private val contentGenerator = ContentGenerator()
    private val fileNamingService = FileNamingService()
    private val tempFileManager = TempFileManager(context)
    private val intentFactory = IntentFactory()
    private val s3ExportManager = S3ExportManager(context)
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yy")
    
    /**
     * Create a data source based on the export type
     */
    fun createDataSource(exportType: String): ExportDataSource {
        return when (exportType) {
            "checkout" -> CheckoutDataSource(context)
            "checkin" -> CheckInDataSource(context)
            "kit_bundle" -> KitBundleDataSource(context)
            "inventory" -> InventoryDataSource(context)
            "logs" -> LogsDataSource(context)
            else -> throw IllegalArgumentException("Unknown export type: $exportType")
        }
    }
    
    /**
     * Export data to Downloads folder
     */
    suspend fun exportToDownloads(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val locationId = getLocationId()
            if (locationId.isEmpty() && dataSource.getExportType() != "logs") {
                return@withContext ExportResult.Error("Location ID not configured")
            }
            
            val exportedFiles = mutableListOf<android.net.Uri>()
            
            if (dataSource.supportsDateRange()) {
                val dataByDate = dataSource.getDataForDateRange(startDate, endDate)
                
                for ((date, jsonData) in dataByDate) {
                    val records = parseRecords(jsonData, dataSource.getExportType())
                    val filename = generateFilename(dataSource, date, locationId, format)
                    val content = generateContent(records, format, locationId, dataSource.getExportType())
                    
                    val result = fileManager.saveToDownloads(filename, content, format.mimeType)
                    when (result) {
                        is FileManager.FileResult.Success -> exportedFiles.add(result.data)
                        is FileManager.FileResult.Error -> throw Exception(result.message)
                    }
                }
            } else {
                // For data sources that don't support date ranges (like inventory)
                val jsonData = dataSource.getAllData()
                if (jsonData.isNotEmpty()) {
                    val filename = generateFilename(dataSource, LocalDate.now(), locationId, format)
                    val content = when (dataSource.getExportType()) {
                        "inventory" -> jsonData // Inventory already returns formatted JSON
                        "logs" -> jsonData // Logs are plain text
                        else -> generateContent(parseRecords(jsonData, dataSource.getExportType()), format, locationId, dataSource.getExportType())
                    }
                    
                    val result = fileManager.saveToDownloads(filename, content, format.mimeType)
                    when (result) {
                        is FileManager.FileResult.Success -> exportedFiles.add(result.data)
                        is FileManager.FileResult.Error -> throw Exception(result.message)
                    }
                }
            }
            
            if (exportedFiles.isEmpty()) {
                ExportResult.NoData
            } else {
                ExportResult.Success(exportedFiles)
            }
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Export failed")
        }
    }
    
    /**
     * Export data via sharing
     */
    suspend fun exportViaShare(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            val locationId = getLocationId()
            if (locationId.isEmpty() && dataSource.getExportType() != "logs") {
                return@withContext ExportResult.Error("Location ID not configured")
            }
            
            val tempFiles = mutableListOf<File>()
            val fileUris = mutableListOf<android.net.Uri>()
            
            if (dataSource.supportsDateRange()) {
                val dataByDate = dataSource.getDataForDateRange(startDate, endDate)
                
                for ((date, jsonData) in dataByDate) {
                    val records = parseRecords(jsonData, dataSource.getExportType())
                    val filename = generateFilename(dataSource, date, locationId, format)
                    val content = generateContent(records, format, locationId, dataSource.getExportType())
                    
                    val file = tempFileManager.createTempFile(filename, content)
                    tempFiles.add(file)
                    
                    val uri = tempFileManager.getUriForFile(file)
                    fileUris.add(uri)
                }
            } else {
                val jsonData = dataSource.getAllData()
                if (jsonData.isNotEmpty()) {
                    val filename = generateFilename(dataSource, LocalDate.now(), locationId, format)
                    val content = when (dataSource.getExportType()) {
                        "inventory" -> jsonData
                        "logs" -> jsonData
                        else -> generateContent(parseRecords(jsonData, dataSource.getExportType()), format, locationId, dataSource.getExportType())
                    }
                    
                    val file = tempFileManager.createTempFile(filename, content)
                    tempFiles.add(file)
                    
                    val uri = tempFileManager.getUriForFile(file)
                    fileUris.add(uri)
                }
            }
            
            if (fileUris.isEmpty()) {
                ExportResult.NoData
            } else {
                ExportResult.ShareReady(fileUris, tempFiles, format.mimeType)
            }
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Export failed")
        }
    }
    
    /**
     * Export to S3
     */
    suspend fun exportToS3(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat
    ): ExportResult {
        // For now, delegate to the existing S3ExportManager
        // This can be refactored later to use data sources directly
        return if (format == ExportFormat.CSV) {
            s3ExportManager.exportCsvToS3(startDate, endDate)
        } else {
            s3ExportManager.exportToS3(startDate, endDate)
        }
    }
    
    /**
     * Parse JSON records based on export type
     */
    private fun parseRecords(jsonData: String, exportType: String): List<Any> {
        return when (exportType) {
            "checkout" -> gson.fromJson(jsonData, Array<com.joeycarlson.qrscanner.data.CheckoutRecord>::class.java).toList()
            "checkin" -> gson.fromJson(jsonData, Array<com.joeycarlson.qrscanner.data.CheckInRecord>::class.java).toList()
            "kit_bundle" -> gson.fromJson(jsonData, Array<com.joeycarlson.qrscanner.data.KitBundle>::class.java).toList()
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
            "checkout" -> contentGenerator.generateCsvContent(
                records as List<com.joeycarlson.qrscanner.data.CheckoutRecord>,
                locationId
            )
            "checkin" -> contentGenerator.generateCheckInCsvContent(
                records as List<com.joeycarlson.qrscanner.data.CheckInRecord>,
                locationId
            )
            "kit_bundle" -> contentGenerator.generateKitBundleCsvContent(
                records as List<com.joeycarlson.qrscanner.data.KitBundle>
            )
            else -> ""
        }
    }
    
    /**
     * Generate TXT content based on record type
     */
    private fun generateTxtContent(records: List<Any>, exportType: String, locationId: String): String {
        return when (exportType) {
            "checkout" -> contentGenerator.generateTxtContent(
                records as List<com.joeycarlson.qrscanner.data.CheckoutRecord>,
                locationId
            )
            "checkin" -> contentGenerator.generateCheckInTxtContent(
                records as List<com.joeycarlson.qrscanner.data.CheckInRecord>,
                locationId
            )
            else -> ""
        }
    }
    
    /**
     * Generate XML content
     */
    private fun generateXmlContent(records: List<Any>, exportType: String, locationId: String): String {
        return contentGenerator.generateXmlContent(
            records as List<com.joeycarlson.qrscanner.data.CheckoutRecord>,
            locationId
        )
    }
    
    /**
     * Generate kit labels CSV content
     */
    private fun generateKitLabelsCsvContent(records: List<Any>): String {
        return contentGenerator.generateKitLabelsCsvContent(
            records as List<com.joeycarlson.qrscanner.data.KitBundle>
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
    
    /**
     * Get configured location ID
     */
    private fun getLocationId(): String {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PreferenceKeys.LOCATION_ID, "") ?: ""
    }
}
