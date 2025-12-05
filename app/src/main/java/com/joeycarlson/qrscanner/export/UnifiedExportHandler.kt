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
    private val dataProcessor = ExportDataProcessor(contentGenerator)
    private val tempFileManager = TempFileManager(context)
    private val s3UploadManager = S3UploadManager(context)
    
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
            
            val processedExports = dataProcessor.processDataSource(
                dataSource, startDate, endDate, format, locationId
            )
            
            if (processedExports.isEmpty()) {
                return@withContext ExportResult.NoData
            }
            
            val exportedFiles = mutableListOf<android.net.Uri>()
            
            for (export in processedExports) {
                val result = fileManager.saveToDownloads(export.filename, export.content, format.mimeType)
                when (result) {
                    is FileManager.FileResult.Success -> exportedFiles.add(result.data)
                    is FileManager.FileResult.Error -> throw Exception(result.message)
                }
            }
            
            ExportResult.Success(exportedFiles)
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
            
            val processedExports = dataProcessor.processDataSource(
                dataSource, startDate, endDate, format, locationId
            )
            
            if (processedExports.isEmpty()) {
                return@withContext ExportResult.NoData
            }
            
            val tempFiles = mutableListOf<File>()
            val fileUris = mutableListOf<android.net.Uri>()
            
            for (export in processedExports) {
                val file = tempFileManager.createTempFile(export.filename, export.content)
                tempFiles.add(file)
                
                val uri = tempFileManager.getUriForFile(file)
                fileUris.add(uri)
            }
            
            ExportResult.ShareReady(fileUris, tempFiles, format.mimeType)
        } catch (e: Exception) {
            ExportResult.Error(e.message ?: "Export failed")
        }
    }
    
    /**
     * Export to S3 using the universal S3UploadManager
     */
    suspend fun exportToS3(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat
    ): ExportResult {
        // Use the new universal S3UploadManager that works with all data sources
        return s3UploadManager.uploadToS3(dataSource, startDate, endDate, format)
    }
    
    /**
     * Get configured location ID
     */
    private fun getLocationId(): String {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PreferenceKeys.LOCATION_ID, "") ?: ""
    }
}
