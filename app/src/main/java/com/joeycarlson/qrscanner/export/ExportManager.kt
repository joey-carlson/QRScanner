package com.joeycarlson.qrscanner.export

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.data.CheckoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ExportManager(private val context: Context) {
    
    private val repository = CheckoutRepository(context)
    private val gson: Gson = GsonBuilder().create()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yy")
    
    suspend fun exportToDownloads(startDate: LocalDate, endDate: LocalDate): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val exportedFiles = mutableListOf<Uri>()
                var currentDate = startDate
                
                while (!currentDate.isAfter(endDate)) {
                    val records = repository.getRecordsForDate(currentDate)
                    if (records.isNotEmpty()) {
                        val uri = saveToDownloads(records, currentDate, locationId)
                        uri?.let { exportedFiles.add(it) }
                    }
                    currentDate = currentDate.plusDays(1)
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
    
    suspend fun exportViaShare(startDate: LocalDate, endDate: LocalDate): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val tempFiles = mutableListOf<File>()
                val fileUris = mutableListOf<Uri>()
                var currentDate = startDate
                
                while (!currentDate.isAfter(endDate)) {
                    val records = repository.getRecordsForDate(currentDate)
                    if (records.isNotEmpty()) {
                        val file = saveToTempFile(records, currentDate, locationId)
                        tempFiles.add(file)
                        
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        fileUris.add(uri)
                    }
                    currentDate = currentDate.plusDays(1)
                }
                
                if (fileUris.isEmpty()) {
                    ExportResult.NoData
                } else {
                    ExportResult.ShareReady(fileUris, tempFiles)
                }
            } catch (e: Exception) {
                ExportResult.Error(e.message ?: "Export failed")
            }
        }
    }
    
    suspend fun exportCsvToDownloads(startDate: LocalDate, endDate: LocalDate): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val exportedFiles = mutableListOf<Uri>()
                var currentDate = startDate
                
                while (!currentDate.isAfter(endDate)) {
                    val records = repository.getRecordsForDate(currentDate)
                    if (records.isNotEmpty()) {
                        val uri = saveCsvToDownloads(records, currentDate, locationId)
                        uri?.let { exportedFiles.add(it) }
                    }
                    currentDate = currentDate.plusDays(1)
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
    
    suspend fun exportCsvViaShare(startDate: LocalDate, endDate: LocalDate): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val tempFiles = mutableListOf<File>()
                val fileUris = mutableListOf<Uri>()
                var currentDate = startDate
                
                while (!currentDate.isAfter(endDate)) {
                    val records = repository.getRecordsForDate(currentDate)
                    if (records.isNotEmpty()) {
                        val file = saveCsvToTempFile(records, currentDate, locationId)
                        tempFiles.add(file)
                        
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        fileUris.add(uri)
                    }
                    currentDate = currentDate.plusDays(1)
                }
                
                if (fileUris.isEmpty()) {
                    ExportResult.NoData
                } else {
                    ExportResult.ShareReady(fileUris, tempFiles, "text/csv")
                }
            } catch (e: Exception) {
                ExportResult.Error(e.message ?: "Export failed")
            }
        }
    }
    
    private fun saveToDownloads(records: List<CheckoutRecord>, date: LocalDate, locationId: String): Uri? {
        val filename = generateFilename(date, locationId)
        val jsonContent = gson.toJson(records)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ using MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(jsonContent)
                    }
                }
                
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            
            uri
        } else {
            // Android 9 and below using direct file access
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos).use { writer ->
                    writer.write(jsonContent)
                }
            }
            
            Uri.fromFile(file)
        }
    }
    
    private fun saveToTempFile(records: List<CheckoutRecord>, date: LocalDate, locationId: String): File {
        val filename = generateFilename(date, locationId)
        val tempDir = File(context.cacheDir, "exports")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        
        val file = File(tempDir, filename)
        val jsonContent = gson.toJson(records)
        
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos).use { writer ->
                writer.write(jsonContent)
            }
        }
        
        return file
    }
    
    private fun generateFilename(date: LocalDate, locationId: String, format: String = "json"): String {
        val dateStr = date.format(dateFormatter)
        return "qr_checkouts_${dateStr}_$locationId.$format"
    }
    
    private fun generateCsvContent(records: List<CheckoutRecord>): String {
        val locationId = getLocationId()
        val stringBuilder = StringBuilder()
        
        // CSV header
        stringBuilder.appendLine("User,Kit,Timestamp,Location")
        
        // CSV data rows
        records.forEach { record ->
            stringBuilder.append("\"${record.userId}\",")
            stringBuilder.append("\"${record.kitId}\",")
            stringBuilder.append("\"${record.timestamp}\",")
            stringBuilder.appendLine("\"$locationId\"")
        }
        
        return stringBuilder.toString()
    }
    
    private fun saveCsvToDownloads(records: List<CheckoutRecord>, date: LocalDate, locationId: String): Uri? {
        val filename = generateFilename(date, locationId, "csv")
        val csvContent = generateCsvContent(records)
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ using MediaStore
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, "text/csv")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(csvContent)
                    }
                }
                
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            
            uri
        } else {
            // Android 9 and below using direct file access
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos).use { writer ->
                    writer.write(csvContent)
                }
            }
            
            Uri.fromFile(file)
        }
    }
    
    private fun saveCsvToTempFile(records: List<CheckoutRecord>, date: LocalDate, locationId: String): File {
        val filename = generateFilename(date, locationId, "csv")
        val tempDir = File(context.cacheDir, "exports")
        if (!tempDir.exists()) {
            tempDir.mkdirs()
        }
        
        val file = File(tempDir, filename)
        val csvContent = generateCsvContent(records)
        
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos).use { writer ->
                writer.write(csvContent)
            }
        }
        
        return file
    }
    
    private fun getLocationId(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("location_id", "") ?: ""
    }
    
    fun createShareIntent(uris: List<Uri>): Intent {
        return if (uris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "application/json"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    
    suspend fun exportViaEmail(startDate: LocalDate, endDate: LocalDate): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val tempFiles = mutableListOf<File>()
                val fileUris = mutableListOf<Uri>()
                var currentDate = startDate
                var totalRecords = 0
                
                while (!currentDate.isAfter(endDate)) {
                    val records = repository.getRecordsForDate(currentDate)
                    if (records.isNotEmpty()) {
                        totalRecords += records.size
                        val file = saveToTempFile(records, currentDate, locationId)
                        tempFiles.add(file)
                        
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        fileUris.add(uri)
                    }
                    currentDate = currentDate.plusDays(1)
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
                        totalRecords = totalRecords
                    )
                    ExportResult.EmailReady(emailData)
                }
            } catch (e: Exception) {
                ExportResult.Error(e.message ?: "Export failed")
            }
        }
    }
    
    suspend fun exportViaSMS(startDate: LocalDate, endDate: LocalDate): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                val tempFiles = mutableListOf<File>()
                val fileUris = mutableListOf<Uri>()
                var currentDate = startDate
                var totalRecords = 0
                
                while (!currentDate.isAfter(endDate)) {
                    val records = repository.getRecordsForDate(currentDate)
                    if (records.isNotEmpty()) {
                        totalRecords += records.size
                        val file = saveToTempFile(records, currentDate, locationId)
                        tempFiles.add(file)
                        
                        val uri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                        )
                        fileUris.add(uri)
                    }
                    currentDate = currentDate.plusDays(1)
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
                        totalRecords = totalRecords
                    )
                    ExportResult.SMSReady(smsData)
                }
            } catch (e: Exception) {
                ExportResult.Error(e.message ?: "Export failed")
            }
        }
    }
    
    fun createEmailIntent(emailData: EmailExportData): Intent {
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        val subject = "QR Checkout Export - ${emailData.locationId} (${emailData.startDate.format(dateFormatter)} to ${emailData.endDate.format(dateFormatter)})"
        
        val body = buildString {
            appendLine("QR Checkout Data Export")
            appendLine("========================")
            appendLine()
            appendLine("Location: ${emailData.locationId}")
            appendLine("Date Range: ${emailData.startDate.format(dateFormatter)} to ${emailData.endDate.format(dateFormatter)}")
            appendLine("Total Records: ${emailData.totalRecords}")
            appendLine("Files Attached: ${emailData.fileUris.size}")
            appendLine()
            appendLine("The attached JSON files contain checkout records for the specified date range.")
            appendLine("Each file represents one day of checkout data.")
            appendLine()
            appendLine("File format: qr_checkouts_MM-dd-yy_[LocationID].json")
        }
        
        return if (emailData.fileUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, emailData.fileUris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(emailData.fileUris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    
    fun createSMSIntent(smsData: SMSExportData): Intent {
        val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
        val body = "QR Export - ${smsData.locationId}: ${smsData.totalRecords} records (${smsData.startDate.format(dateFormatter)}-${smsData.endDate.format(dateFormatter)}). ${smsData.fileUris.size} file(s) attached."
        
        return if (smsData.fileUris.size == 1) {
            Intent(Intent.ACTION_SEND).apply {
                type = "*/*"
                putExtra("sms_body", body)
                putExtra(Intent.EXTRA_TEXT, body)
                putExtra(Intent.EXTRA_STREAM, smsData.fileUris[0])
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        } else {
            Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                type = "*/*"
                putExtra("sms_body", body)
                putExtra(Intent.EXTRA_TEXT, body)
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(smsData.fileUris))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
    }
    
    fun cleanupTempFiles(files: List<File>) {
        files.forEach { it.delete() }
    }
}

data class EmailExportData(
    val fileUris: List<Uri>,
    val tempFiles: List<File>,
    val locationId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalRecords: Int
)

data class SMSExportData(
    val fileUris: List<Uri>,
    val tempFiles: List<File>,
    val locationId: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val totalRecords: Int
)

sealed class ExportResult {
    data class Success(val fileUris: List<Uri>) : ExportResult()
    data class ShareReady(val fileUris: List<Uri>, val tempFiles: List<File>, val mimeType: String = "application/json") : ExportResult()
    data class EmailReady(val emailData: EmailExportData) : ExportResult()
    data class SMSReady(val smsData: SMSExportData) : ExportResult()
    data class S3Success(val uploadedFiles: List<String>, val bucketName: String, val region: String) : ExportResult()
    object NoData : ExportResult()
    data class Error(val message: String) : ExportResult()
}
