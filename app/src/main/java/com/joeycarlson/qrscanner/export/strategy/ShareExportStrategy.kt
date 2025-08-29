package com.joeycarlson.qrscanner.export.strategy

import android.content.Context
import android.content.Intent
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.util.FileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate

/**
 * Export strategy for sharing files via Android's share system.
 * Creates temporary files and provides them via FileProvider for sharing.
 */
class ShareExportStrategy(
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
        
        try {
            val fileManager = FileManager(context)
            val tempFiles = mutableListOf<File>()
            val fileUris = mutableListOf<android.net.Uri>()
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
                    
                    when (val result = fileManager.saveToTempFile(filename, content)) {
                        is FileManager.FileResult.Success -> {
                            val tempFile = result.data
                            tempFiles.add(tempFile)
                            
                            // Create FileProvider URI for sharing
                            val uri = fileManager.createFileProviderUri(tempFile)
                            fileUris.add(uri)
                        }
                        is FileManager.FileResult.Error -> {
                            errors.add("$date: ${result.message}")
                        }
                    }
                }
            }
            
            // Return results
            when {
                fileUris.isEmpty() && errors.isNotEmpty() -> {
                    // Clean up any temp files that were created
                    fileManager.cleanupTempFiles(tempFiles)
                    ExportResult.Error("All exports failed: ${errors.joinToString("; ")}")
                }
                fileUris.isEmpty() -> {
                    ExportResult.NoData
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
                    ExportResult.ShareReady(
                        fileUris = fileUris,
                        tempFiles = tempFiles,
                        mimeType = format.mimeType,
                        additionalData = mapOf(
                            "message" to "$successMessage. Some files failed: ${errors.joinToString("; ")}",
                            "errors" to errors,
                            "format" to format.name
                        )
                    )
                }
                else -> {
                    ExportResult.ShareReady(
                        fileUris = fileUris,
                        tempFiles = tempFiles,
                        mimeType = format.mimeType,
                        additionalData = mapOf(
                            "message" to generateSummaryMessage(records, locationId, format),
                            "format" to format.name
                        )
                    )
                }
            }
        } catch (e: Exception) {
            ExportResult.Error("Share preparation failed: ${e.message}", e)
        }
    }
    
    override fun getDisplayName(): String {
        return when (format) {
            ExportFormat.JSON -> "Share JSON Files"
            ExportFormat.CSV -> "Share CSV Files"
            ExportFormat.XML -> "Share XML Files"
            ExportFormat.TXT -> "Share Text Files"
        }
    }
    
    override fun getDescription(): String {
        return when (format) {
            ExportFormat.JSON -> "Share JSON files via email, messaging, cloud storage, or other apps"
            ExportFormat.CSV -> "Share CSV files via email, messaging, cloud storage, or other apps"
            ExportFormat.XML -> "Share XML files via email, messaging, cloud storage, or other apps"
            ExportFormat.TXT -> "Share text files via email, messaging, cloud storage, or other apps"
        }
    }
    
    override fun getIconResId(): Int {
        return when (format) {
            ExportFormat.JSON -> R.drawable.ic_share
            ExportFormat.CSV -> R.drawable.ic_share
            ExportFormat.XML -> R.drawable.ic_share
            ExportFormat.TXT -> R.drawable.ic_share
        }
    }
    
    override fun requiresNetwork(): Boolean = false // Network requirement depends on chosen sharing app
    
    override fun requiresConfiguration(context: Context): Boolean = false
    
    override fun getConfigurationRequirements(): List<String> = emptyList()
    
    companion object {
        /**
         * Create Android share intent from ShareReady result
         */
        fun createShareIntent(shareResult: ExportResult.ShareReady): Intent {
            return if (shareResult.fileUris.size == 1) {
                Intent(Intent.ACTION_SEND).apply {
                    type = shareResult.mimeType
                    putExtra(Intent.EXTRA_STREAM, shareResult.fileUris[0])
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    
                    // Add subject and text if available
                    shareResult.additionalData["message"]?.let { message ->
                        putExtra(Intent.EXTRA_SUBJECT, "QR Checkout Export")
                        putExtra(Intent.EXTRA_TEXT, message.toString())
                    }
                }
            } else {
                Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = shareResult.mimeType
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(shareResult.fileUris))
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    
                    // Add subject and text if available
                    shareResult.additionalData["message"]?.let { message ->
                        putExtra(Intent.EXTRA_SUBJECT, "QR Checkout Export")
                        putExtra(Intent.EXTRA_TEXT, message.toString())
                    }
                }
            }
        }
    }
}

/**
 * Factory for creating share export strategies
 */
object ShareExportStrategyFactory {
    
    fun createJsonStrategy() = ShareExportStrategy(ExportFormat.JSON)
    
    fun createCsvStrategy() = ShareExportStrategy(ExportFormat.CSV)
    
    fun createXmlStrategy() = ShareExportStrategy(ExportFormat.XML)
    
    fun createTxtStrategy() = ShareExportStrategy(ExportFormat.TXT)
    
    fun getAllStrategies(): List<ShareExportStrategy> {
        return listOf(
            createJsonStrategy(),
            createCsvStrategy(),
            createXmlStrategy(),
            createTxtStrategy()
        )
    }
}
