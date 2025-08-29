package com.joeycarlson.qrscanner.export.strategy

import android.content.Context
import android.net.Uri
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Export strategy for saving files to local Downloads folder.
 * Supports both JSON and CSV formats.
 */
class LocalStorageExportStrategy(
    private val format: ExportFormat = ExportFormat.JSON
) : BaseExportStrategy() {
    
    override suspend fun export(
        context: Context,
        records: Map<LocalDate, List<CheckoutRecord>>,
        locationId: String
    ): ExportResult = withContext(Dispatchers.IO) {
        
        // Validation checks
        validateLocationId(locationId)?.let { return@withContext it }
        validateRecords(records)?.let { return@withContext it }
        checkStorageAvailability(context)?.let { return@withContext it }
        
        try {
            val fileManager = FileManager(context)
            val exportedUris = mutableListOf<Uri>()
            val errors = mutableListOf<String>()
            
            // Process each date with records
            records.forEach { (date, dateRecords) ->
                if (dateRecords.isNotEmpty()) {
                    // Validate file size for this date
                    validateFileSize(dateRecords, format)?.let { error ->
                        errors.add("$date: ${error.message}")
                        return@forEach
                    }
                    
                    val filename = generateFilename(date, locationId, format)
                    val content = getFormattedContent(dateRecords, locationId, format)
                    
                    when (val result = fileManager.saveToDownloads(filename, content, format.mimeType)) {
                        is FileManager.FileResult.Success -> {
                            exportedUris.add(result.data)
                        }
                        is FileManager.FileResult.Error -> {
                            errors.add("$date: ${result.message}")
                        }
                    }
                }
            }
            
            // Return results
            when {
                exportedUris.isEmpty() && errors.isNotEmpty() -> {
                    ExportResult.Error("All exports failed: ${errors.joinToString("; ")}")
                }
                errors.isNotEmpty() -> {
                    val successMessage = generateSummaryMessage(
                        records.filterKeys { date -> 
                            records[date]?.isNotEmpty() == true && 
                            !errors.any { it.startsWith(date.toString()) }
                        },
                        locationId,
                        format
                    )
                    ExportResult.Success(
                        message = "$successMessage. Some files failed: ${errors.joinToString("; ")}",
                        fileUris = exportedUris,
                        additionalData = mapOf(
                            "errors" to errors,
                            "format" to format.name
                        )
                    )
                }
                else -> {
                    ExportResult.Success(
                        message = generateSummaryMessage(records, locationId, format),
                        fileUris = exportedUris,
                        additionalData = mapOf(
                            "format" to format.name,
                            "destination" to "Downloads"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            ExportResult.Error("Export failed: ${e.message}", e)
        }
    }
    
    override fun getDisplayName(): String {
        return when (format) {
            ExportFormat.JSON -> "Save to Downloads (JSON)"
            ExportFormat.CSV -> "Save to Downloads (CSV)"
            ExportFormat.XML -> "Save to Downloads (XML)"
            ExportFormat.TXT -> "Save to Downloads (TXT)"
        }
    }
    
    override fun getDescription(): String {
        return when (format) {
            ExportFormat.JSON -> "Save JSON files to your device's Downloads folder"
            ExportFormat.CSV -> "Save CSV files to your device's Downloads folder for spreadsheet apps"
            ExportFormat.XML -> "Save XML files to your device's Downloads folder"
            ExportFormat.TXT -> "Save plain text files to your device's Downloads folder"
        }
    }
    
    override fun getIconResId(): Int {
        return when (format) {
            ExportFormat.JSON -> R.drawable.ic_download // You'll need to add these icons
            ExportFormat.CSV -> R.drawable.ic_table
            ExportFormat.XML -> R.drawable.ic_code
            ExportFormat.TXT -> R.drawable.ic_text_file
        }
    }
    
    override fun requiresNetwork(): Boolean = false
    
    override fun requiresConfiguration(context: Context): Boolean = false
    
    override fun getConfigurationRequirements(): List<String> = emptyList()
}

/**
 * Factory for creating local storage export strategies
 */
object LocalStorageExportStrategyFactory {
    
    fun createJsonStrategy() = LocalStorageExportStrategy(ExportFormat.JSON)
    
    fun createCsvStrategy() = LocalStorageExportStrategy(ExportFormat.CSV)
    
    fun createXmlStrategy() = LocalStorageExportStrategy(ExportFormat.XML)
    
    fun createTxtStrategy() = LocalStorageExportStrategy(ExportFormat.TXT)
    
    fun getAllStrategies(): List<LocalStorageExportStrategy> {
        return listOf(
            createJsonStrategy(),
            createCsvStrategy(),
            createXmlStrategy(),
            createTxtStrategy()
        )
    }
}
