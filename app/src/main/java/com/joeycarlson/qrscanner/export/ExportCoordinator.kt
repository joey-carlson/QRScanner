package com.joeycarlson.qrscanner.export

import android.content.Context
import android.net.Uri
import com.joeycarlson.qrscanner.config.PreferenceKeys
import com.joeycarlson.qrscanner.data.CheckoutRepository
import com.joeycarlson.qrscanner.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Coordinates export operations across date ranges and manages the overall export workflow.
 * This class orchestrates the export process but delegates specific tasks to specialized services.
 */
class ExportCoordinator(private val context: Context) {
    
    private val repository = CheckoutRepository(context)
    private val fileManager = FileManager(context)
    private val contentGenerator = ContentGenerator()
    private val fileNamingService = FileNamingService()
    private val tempFileManager = TempFileManager(context)
    
    /**
     * Exports data to Downloads folder for the specified date range
     */
    suspend fun exportToDownloads(
        startDate: LocalDate, 
        endDate: LocalDate, 
        format: ExportFormat = ExportFormat.JSON
    ): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val exportedFiles = mutableListOf<Uri>()
                
                iterateDateRange(startDate, endDate) { date, records ->
                    val filename = fileNamingService.generateFilename(date, locationId, format)
                    val content = contentGenerator.generateContent(records, format, locationId)
                    
                    val result = fileManager.saveToDownloads(filename, content, format.mimeType)
                    when (result) {
                        is FileManager.FileResult.Success -> exportedFiles.add(result.data)
                        is FileManager.FileResult.Error -> throw Exception(result.message)
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
    }
    
    /**
     * Exports data via Android sharing system for the specified date range
     */
    suspend fun exportViaShare(
        startDate: LocalDate, 
        endDate: LocalDate, 
        format: ExportFormat = ExportFormat.JSON
    ): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val tempFiles = mutableListOf<File>()
                val fileUris = mutableListOf<Uri>()
                
                iterateDateRange(startDate, endDate) { date, records ->
                    val filename = fileNamingService.generateFilename(date, locationId, format)
                    val content = contentGenerator.generateContent(records, format, locationId)
                    
                    val file = tempFileManager.createTempFile(filename, content)
                    tempFiles.add(file)
                    
                    val uri = tempFileManager.getUriForFile(file)
                    fileUris.add(uri)
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
    }
    
    /**
     * Exports data via email for the specified date range
     */
    suspend fun exportViaEmail(
        startDate: LocalDate, 
        endDate: LocalDate, 
        format: ExportFormat = ExportFormat.JSON
    ): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val tempFiles = mutableListOf<File>()
                val fileUris = mutableListOf<Uri>()
                var totalRecords = 0
                
                iterateDateRange(startDate, endDate) { date, records ->
                    totalRecords += records.size
                    val filename = fileNamingService.generateFilename(date, locationId, format)
                    val content = contentGenerator.generateContent(records, format, locationId)
                    
                    val file = tempFileManager.createTempFile(filename, content)
                    tempFiles.add(file)
                    
                    val uri = tempFileManager.getUriForFile(file)
                    fileUris.add(uri)
                }
                
                if (fileUris.isEmpty()) {
                    ExportResult.NoData
                } else {
                    val emailData = EmailExportData(
                        fileUris = fileUris,
                        tempFiles = tempFiles,
                        locationId = locationId,
                        startDate = startDate,
                        endDate = endDate,
                        totalRecords = totalRecords,
                        format = format
                    )
                    ExportResult.EmailReady(emailData)
                }
            } catch (e: Exception) {
                ExportResult.Error(e.message ?: "Export failed")
            }
        }
    }
    
    /**
     * Exports data via SMS/MMS for the specified date range
     */
    suspend fun exportViaSMS(
        startDate: LocalDate, 
        endDate: LocalDate, 
        format: ExportFormat = ExportFormat.JSON
    ): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val tempFiles = mutableListOf<File>()
                val fileUris = mutableListOf<Uri>()
                var totalRecords = 0
                
                iterateDateRange(startDate, endDate) { date, records ->
                    totalRecords += records.size
                    val filename = fileNamingService.generateFilename(date, locationId, format)
                    val content = contentGenerator.generateContent(records, format, locationId)
                    
                    val file = tempFileManager.createTempFile(filename, content)
                    tempFiles.add(file)
                    
                    val uri = tempFileManager.getUriForFile(file)
                    fileUris.add(uri)
                }
                
                if (fileUris.isEmpty()) {
                    ExportResult.NoData
                } else {
                    val smsData = SMSExportData(
                        fileUris = fileUris,
                        tempFiles = tempFiles,
                        locationId = locationId,
                        startDate = startDate,
                        endDate = endDate,
                        totalRecords = totalRecords,
                        format = format
                    )
                    ExportResult.SMSReady(smsData)
                }
            } catch (e: Exception) {
                ExportResult.Error(e.message ?: "Export failed")
            }
        }
    }
    
    /**
     * Iterates through a date range and processes records for each date that has data
     */
    private suspend inline fun iterateDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
        processor: (LocalDate, List<com.joeycarlson.qrscanner.data.CheckoutRecord>) -> Unit
    ) {
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            val records = repository.getRecordsForDate(currentDate)
            if (records.isNotEmpty()) {
                processor(currentDate, records)
            }
            currentDate = currentDate.plusDays(1)
        }
    }
    
    /**
     * Gets the configured location ID from preferences
     */
    private fun getLocationId(): String {
        val prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PreferenceKeys.LOCATION_ID, "") ?: ""
    }
}

/**
 * Supported export formats
 */
enum class ExportFormat(val extension: String, val mimeType: String) {
    JSON("json", "application/json"),
    CSV("csv", "text/csv"),
    XML("xml", "application/xml"),
    TXT("txt", "text/plain"),
    KIT_LABELS_CSV("csv", "text/csv")
}
