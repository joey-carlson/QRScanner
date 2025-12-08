package com.joeycarlson.qrscanner.export

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.preference.PreferenceManager
import com.amazonaws.ClientConfiguration
import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.joeycarlson.qrscanner.config.PreferenceKeys
import com.joeycarlson.qrscanner.export.datasource.ExportDataSource
import com.joeycarlson.qrscanner.util.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.min
import kotlin.math.pow

/**
 * Universal S3 upload manager that works with any ExportDataSource
 * Includes progress tracking, retry logic, and network checks
 * Version: 2.9
 */
class S3UploadManager(private val context: Context) {
    
    private val s3Configuration = S3Configuration(context)
    private val contentGenerator = ContentGenerator()
    private val fileNamingService = FileNamingService()
    private val logManager = LogManager.getInstance(context)
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yy")
    
    companion object {
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val INITIAL_RETRY_DELAY_MS = 1000L
        private const val MAX_RETRY_DELAY_MS = 10000L
    }
    
    /**
     * Progress callback interface for upload status updates
     */
    interface UploadProgressListener {
        fun onUploadStarted(totalFiles: Int)
        fun onFileUploadStarted(filename: String, fileNumber: Int, totalFiles: Int)
        fun onFileUploadProgress(filename: String, bytesUploaded: Long, totalBytes: Long)
        fun onFileUploadCompleted(filename: String, s3Key: String)
        fun onFileUploadFailed(filename: String, error: String)
        fun onAllUploadsCompleted(uploadedFiles: List<String>)
        fun onUploadError(error: String)
    }
    
    /**
     * Upload data to S3 with progress tracking and retry logic
     */
    suspend fun uploadToS3(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat,
        progressListener: UploadProgressListener? = null
    ): ExportResult = withContext(Dispatchers.IO) {
        try {
            // Network connectivity check
            if (!isNetworkAvailable()) {
                val error = "No network connection available"
                progressListener?.onUploadError(error)
                return@withContext ExportResult.Error(error)
            }
            
            // Validate S3 configuration
            val validationResult = s3Configuration.validateConfiguration()
            if (!validationResult.isValid) {
                val errorMessage = buildString {
                    if (validationResult.missingFields.isNotEmpty()) {
                        append("Missing configuration: ${validationResult.missingFields.joinToString(", ")}")
                    }
                    if (validationResult.errors.isNotEmpty()) {
                        if (isNotEmpty()) append(". ")
                        append("Errors: ${validationResult.errors.joinToString(", ")}")
                    }
                }
                progressListener?.onUploadError(errorMessage)
                return@withContext ExportResult.Error(errorMessage)
            }
            
            val locationId = getLocationId()
            if (locationId.isEmpty() && dataSource.getExportType() != "logs") {
                val error = "Location ID not configured"
                progressListener?.onUploadError(error)
                return@withContext ExportResult.Error(error)
            }
            
            // Initialize S3 client
            val s3Client = createS3Client()
            if (s3Client == null) {
                val error = "Failed to initialize S3 client"
                progressListener?.onUploadError(error)
                return@withContext ExportResult.Error(error)
            }
            
            // Prepare files for upload
            val filesToUpload = prepareFilesForUpload(dataSource, startDate, endDate, format, locationId)
            
            if (filesToUpload.isEmpty()) {
                return@withContext ExportResult.NoData
            }
            
            progressListener?.onUploadStarted(filesToUpload.size)
            logManager.log("S3UploadManager", "Starting upload of ${filesToUpload.size} files to S3")
            
            val uploadedFiles = mutableListOf<String>()
            
            filesToUpload.forEachIndexed { index, fileData ->
                progressListener?.onFileUploadStarted(fileData.filename, index + 1, filesToUpload.size)
                
                val uploadResult = uploadFileWithRetry(
                    s3Client,
                    fileData,
                    progressListener
                )
                
                if (uploadResult != null) {
                    uploadedFiles.add(uploadResult)
                    progressListener?.onFileUploadCompleted(fileData.filename, uploadResult)
                    logManager.log("S3UploadManager", "Successfully uploaded: ${fileData.filename}")
                } else {
                    val error = "Failed to upload file: ${fileData.filename}"
                    progressListener?.onFileUploadFailed(fileData.filename, error)
                    logManager.log("S3UploadManager", error)
                    return@withContext ExportResult.Error(error)
                }
            }
            
            progressListener?.onAllUploadsCompleted(uploadedFiles)
            logManager.log("S3UploadManager", "All uploads completed successfully")
            
            ExportResult.S3Success(
                uploadedFiles = uploadedFiles,
                bucketName = s3Configuration.getBucketName() ?: "",
                region = s3Configuration.getRegion().name
            )
            
        } catch (e: Exception) {
            val error = "S3 upload failed: ${e.message}"
            progressListener?.onUploadError(error)
            logManager.log("S3UploadManager", "Upload error: ${e.message}")
            ExportResult.Error(error)
        }
    }
    
    /**
     * Prepare files for upload from data source
     */
    private suspend fun prepareFilesForUpload(
        dataSource: ExportDataSource,
        startDate: LocalDate,
        endDate: LocalDate,
        format: ExportFormat,
        locationId: String
    ): List<FileUploadData> {
        val files = mutableListOf<FileUploadData>()
        
        if (dataSource.supportsDateRange()) {
            // Date-based data sources
            val dataByDate = dataSource.getDataForDateRange(startDate, endDate)
            
            for ((date, jsonData) in dataByDate) {
                if (jsonData.isNotEmpty()) {
                    val prefix = dataSource.getFilenamePrefix(date)
                    val dateStr = date.format(dateFormatter)
                    val filename = "${prefix}_${dateStr}_${locationId}.${format.extension}"
                    
                    val content = generateContentForFormat(jsonData, format, locationId, dataSource.getExportType())
                    val s3Key = s3Configuration.generateS3Key(filename, locationId)
                    
                    files.add(
                        FileUploadData(
                            filename = filename,
                            content = content,
                            s3Key = s3Key,
                            mimeType = format.mimeType,
                            metadata = mapOf(
                                "location-id" to locationId,
                                "date" to date.toString(),
                                "export-type" to dataSource.getExportType()
                            )
                        )
                    )
                }
            }
        } else {
            // Non-date-based data sources (inventory, logs)
            val data = dataSource.getAllData()
            if (data.isNotEmpty()) {
                val prefix = dataSource.getFilenamePrefix(null)
                val dateStr = LocalDate.now().format(dateFormatter)
                val filename = "${prefix}_${dateStr}_${locationId}.${format.extension}"
                
                val content = when (dataSource.getExportType()) {
                    "inventory", "logs" -> data
                    else -> generateContentForFormat(data, format, locationId, dataSource.getExportType())
                }
                
                val s3Key = s3Configuration.generateS3Key(filename, locationId)
                
                files.add(
                    FileUploadData(
                        filename = filename,
                        content = content,
                        s3Key = s3Key,
                        mimeType = format.mimeType,
                        metadata = mapOf(
                            "location-id" to locationId,
                            "export-type" to dataSource.getExportType()
                        )
                    )
                )
            }
        }
        
        return files
    }
    
    /**
     * Generate content in the specified format using ContentGenerator
     */
    private fun generateContentForFormat(
        jsonData: String,
        format: ExportFormat,
        locationId: String,
        exportType: String
    ): String {
        // For JSON format, the data is already in JSON format from the data source
        if (format == ExportFormat.JSON) {
            return jsonData
        }
        
        // For other formats, use ContentGenerator
        return when (exportType) {
            "checkout" -> {
                val records = com.google.gson.Gson().fromJson(
                    jsonData,
                    Array<com.joeycarlson.qrscanner.data.CheckoutRecord>::class.java
                ).toList()
                contentGenerator.generateContent(records, format, locationId)
            }
            "checkin" -> {
                val records = com.google.gson.Gson().fromJson(
                    jsonData,
                    Array<com.joeycarlson.qrscanner.data.CheckInRecord>::class.java
                ).toList()
                contentGenerator.generateCheckInContent(records, format, locationId)
            }
            "kit_bundle" -> {
                val records = com.google.gson.Gson().fromJson(
                    jsonData,
                    Array<com.joeycarlson.qrscanner.data.KitBundle>::class.java
                ).toList()
                contentGenerator.generateKitBundleContent(records, format, locationId)
            }
            else -> jsonData
        }
    }
    
    /**
     * Upload file with exponential backoff retry logic
     */
    private suspend fun uploadFileWithRetry(
        s3Client: AmazonS3,
        fileData: FileUploadData,
        progressListener: UploadProgressListener?
    ): String? {
        var attempt = 0
        
        while (attempt < MAX_RETRY_ATTEMPTS) {
            try {
                // Check network before each attempt
                if (!isNetworkAvailable()) {
                    throw Exception("Network connection lost")
                }
                
                return uploadFile(s3Client, fileData, progressListener)
                
            } catch (e: Exception) {
                attempt++
                
                if (attempt < MAX_RETRY_ATTEMPTS) {
                    val delayMs = calculateRetryDelay(attempt)
                    logManager.log(
                        "S3UploadManager",
                        "Upload attempt $attempt failed for ${fileData.filename}, retrying in ${delayMs}ms: ${e.message}"
                    )
                    delay(delayMs)
                } else {
                    logManager.log(
                        "S3UploadManager",
                        "Upload failed after $MAX_RETRY_ATTEMPTS attempts for ${fileData.filename}: ${e.message}"
                    )
                }
            }
        }
        
        return null
    }
    
    /**
     * Calculate retry delay with exponential backoff
     */
    private fun calculateRetryDelay(attempt: Int): Long {
        val exponentialDelay = INITIAL_RETRY_DELAY_MS * (2.0.pow(attempt - 1)).toLong()
        return min(exponentialDelay, MAX_RETRY_DELAY_MS)
    }
    
    /**
     * Upload a single file to S3
     */
    private fun uploadFile(
        s3Client: AmazonS3,
        fileData: FileUploadData,
        progressListener: UploadProgressListener?
    ): String {
        val contentBytes = fileData.content.toByteArray(Charsets.UTF_8)
        val totalBytes = contentBytes.size.toLong()
        
        val metadata = ObjectMetadata().apply {
            contentLength = totalBytes
            contentType = fileData.mimeType
            fileData.metadata.forEach { (key, value) ->
                addUserMetadata(key, value)
            }
        }
        
        val putRequest = PutObjectRequest(
            s3Configuration.getBucketName(),
            fileData.s3Key,
            ByteArrayInputStream(contentBytes),
            metadata
        )
        
        // Report progress (simplified - AWS SDK v2 doesn't have built-in progress listeners)
        progressListener?.onFileUploadProgress(fileData.filename, 0, totalBytes)
        
        s3Client.putObject(putRequest)
        
        progressListener?.onFileUploadProgress(fileData.filename, totalBytes, totalBytes)
        
        return fileData.s3Key
    }
    
    /**
     * Create S3 client with configured credentials
     * Note: All AmazonS3Client constructors are deprecated in AWS Android SDK
     * but there's no alternative for Android SDK v2.x
     */
    @Suppress("DEPRECATION")
    private fun createS3Client(): AmazonS3? {
        return try {
            val credentials = s3Configuration.getCredentials() ?: return null
            val clientConfiguration = ClientConfiguration().apply {
                connectionTimeout = 30000
                socketTimeout = 30000
                maxErrorRetry = 3
            }
            val s3Client = AmazonS3Client(credentials, clientConfiguration)
            s3Client.setRegion(s3Configuration.getRegion())
            s3Client
        } catch (e: Exception) {
            logManager.log("S3UploadManager", "Failed to create S3 client: ${e.message}")
            null
        }
    }
    
    /**
     * Check if network is available
     */
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
               capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
    
    /**
     * Get location ID from preferences
     */
    private fun getLocationId(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(PreferenceKeys.LOCATION_ID, "") ?: ""
    }
    
    /**
     * Test S3 connection and permissions
     */
    suspend fun testS3Connection(): S3TestResult {
        return withContext(Dispatchers.IO) {
            try {
                if (!isNetworkAvailable()) {
                    return@withContext S3TestResult.Error("No network connection available")
                }
                
                val validationResult = s3Configuration.validateConfiguration()
                if (!validationResult.isValid) {
                    return@withContext S3TestResult.Error(
                        "Configuration invalid: ${validationResult.missingFields.joinToString(", ")}"
                    )
                }
                
                val s3Client = createS3Client()
                    ?: return@withContext S3TestResult.Error("Failed to create S3 client")
                
                val bucketName = s3Configuration.getBucketName()!!
                
                // Test bucket access
                try {
                    s3Client.listObjects(bucketName).objectSummaries
                } catch (e: Exception) {
                    return@withContext S3TestResult.Error(
                        "Bucket '$bucketName' does not exist or is not accessible: ${e.message}"
                    )
                }
                
                // Test write permissions with a small test file
                val testKey = "${s3Configuration.getFolderPrefix()}/test-connection-${System.currentTimeMillis()}.txt"
                val testContent = "QR Scanner S3 connection test - ${java.util.Date()}"
                val testBytes = testContent.toByteArray(Charsets.UTF_8)
                
                val metadata = ObjectMetadata().apply {
                    contentLength = testBytes.size.toLong()
                    contentType = "text/plain"
                }
                
                val putRequest = PutObjectRequest(
                    bucketName,
                    testKey,
                    ByteArrayInputStream(testBytes),
                    metadata
                )
                
                s3Client.putObject(putRequest)
                
                // Clean up test file
                s3Client.deleteObject(bucketName, testKey)
                
                logManager.log("S3UploadManager", "S3 connection test successful")
                S3TestResult.Success("S3 connection successful! Bucket '$bucketName' is accessible.")
                
            } catch (e: Exception) {
                logManager.log("S3UploadManager", "S3 connection test failed: ${e.message}")
                S3TestResult.Error("Connection failed: ${e.message}")
            }
        }
    }
}

/**
 * Data class for file upload information
 */
private data class FileUploadData(
    val filename: String,
    val content: String,
    val s3Key: String,
    val mimeType: String,
    val metadata: Map<String, String>
)
