package com.joeycarlson.qrscanner.export

import android.content.Context
import androidx.preference.PreferenceManager
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.data.CheckoutRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * S3 Export Manager for uploading QR checkout data to AWS S3
 * Follows the existing export architecture pattern
 */
class S3ExportManager(private val context: Context) {
    
    private val repository = CheckoutRepository(context)
    private val gson: Gson = GsonBuilder().create()
    private val dateFormatter = DateTimeFormatter.ofPattern("MM-dd-yy")
    private val s3Configuration = S3Configuration(context)
    
    /**
     * Export data to S3 bucket
     */
    suspend fun exportToS3(startDate: LocalDate, endDate: LocalDate): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
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
                    return@withContext ExportResult.Error(errorMessage)
                }
                
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                // Initialize S3 client
                val s3Client = createS3Client() ?: return@withContext ExportResult.Error("Failed to initialize S3 client")
                
                val uploadedFiles = mutableListOf<String>()
                var currentDate = startDate
                
                while (!currentDate.isAfter(endDate)) {
                    val records = repository.getRecordsForDate(currentDate)
                    if (records.isNotEmpty()) {
                        val uploadResult = uploadFileToS3(s3Client, records, currentDate, locationId)
                        if (uploadResult != null) {
                            uploadedFiles.add(uploadResult)
                        } else {
                            return@withContext ExportResult.Error("Failed to upload file for date: ${currentDate}")
                        }
                    }
                    currentDate = currentDate.plusDays(1)
                }
                
                if (uploadedFiles.isEmpty()) {
                    ExportResult.NoData
                } else {
                    ExportResult.S3Success(
                        uploadedFiles = uploadedFiles,
                        bucketName = s3Configuration.getBucketName() ?: "",
                        region = s3Configuration.getRegion().name
                    )
                }
            } catch (e: Exception) {
                ExportResult.Error("S3 upload failed: ${e.message}")
            }
        }
    }
    
    /**
     * Export CSV data to S3 bucket
     */
    suspend fun exportCsvToS3(startDate: LocalDate, endDate: LocalDate): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
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
                    return@withContext ExportResult.Error(errorMessage)
                }
                
                val locationId = getLocationId()
                if (locationId.isEmpty()) {
                    return@withContext ExportResult.Error("Location ID not configured")
                }
                
                // Initialize S3 client
                val s3Client = createS3Client() ?: return@withContext ExportResult.Error("Failed to initialize S3 client")
                
                val uploadedFiles = mutableListOf<String>()
                var currentDate = startDate
                
                while (!currentDate.isAfter(endDate)) {
                    val records = repository.getRecordsForDate(currentDate)
                    if (records.isNotEmpty()) {
                        val uploadResult = uploadCsvFileToS3(s3Client, records, currentDate, locationId)
                        if (uploadResult != null) {
                            uploadedFiles.add(uploadResult)
                        } else {
                            return@withContext ExportResult.Error("Failed to upload CSV file for date: ${currentDate}")
                        }
                    }
                    currentDate = currentDate.plusDays(1)
                }
                
                if (uploadedFiles.isEmpty()) {
                    ExportResult.NoData
                } else {
                    ExportResult.S3Success(
                        uploadedFiles = uploadedFiles,
                        bucketName = s3Configuration.getBucketName() ?: "",
                        region = s3Configuration.getRegion().name
                    )
                }
            } catch (e: Exception) {
                ExportResult.Error("S3 CSV upload failed: ${e.message}")
            }
        }
    }
    
    /**
     * Create S3 client with configured credentials
     */
    private fun createS3Client(): AmazonS3Client? {
        return try {
            val credentials = s3Configuration.getCredentials() ?: return null
            val s3Client = AmazonS3Client(credentials)
            s3Client.setRegion(s3Configuration.getRegion())
            s3Client
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Upload JSON file to S3
     */
    private fun uploadFileToS3(
        s3Client: AmazonS3Client,
        records: List<CheckoutRecord>,
        date: LocalDate,
        locationId: String
    ): String? {
        return try {
            val filename = generateFilename(date, locationId, "json")
            val jsonContent = gson.toJson(records)
            val contentBytes = jsonContent.toByteArray(Charsets.UTF_8)
            
            val metadata = ObjectMetadata().apply {
                contentLength = contentBytes.size.toLong()
                contentType = "application/json"
                addUserMetadata("location-id", locationId)
                addUserMetadata("date", date.toString())
                addUserMetadata("record-count", records.size.toString())
            }
            
            val s3Key = s3Configuration.generateS3Key(filename, locationId)
            val putRequest = PutObjectRequest(
                s3Configuration.getBucketName(),
                s3Key,
                ByteArrayInputStream(contentBytes),
                metadata
            )
            
            s3Client.putObject(putRequest)
            s3Key
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Upload CSV file to S3
     */
    private fun uploadCsvFileToS3(
        s3Client: AmazonS3Client,
        records: List<CheckoutRecord>,
        date: LocalDate,
        locationId: String
    ): String? {
        return try {
            val filename = generateFilename(date, locationId, "csv")
            val csvContent = generateCsvContent(records)
            val contentBytes = csvContent.toByteArray(Charsets.UTF_8)
            
            val metadata = ObjectMetadata().apply {
                contentLength = contentBytes.size.toLong()
                contentType = "text/csv"
                addUserMetadata("location-id", locationId)
                addUserMetadata("date", date.toString())
                addUserMetadata("record-count", records.size.toString())
            }
            
            val s3Key = s3Configuration.generateS3Key(filename, locationId)
            val putRequest = PutObjectRequest(
                s3Configuration.getBucketName(),
                s3Key,
                ByteArrayInputStream(contentBytes),
                metadata
            )
            
            s3Client.putObject(putRequest)
            s3Key
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Generate filename following the established naming convention
     */
    private fun generateFilename(date: LocalDate, locationId: String, format: String): String {
        val dateStr = date.format(dateFormatter)
        return "qr_checkouts_${dateStr}_$locationId.$format"
    }
    
    /**
     * Generate CSV content from records
     */
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
    
    /**
     * Get location ID from preferences
     */
    private fun getLocationId(): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString("location_id", "") ?: ""
    }
    
    /**
     * Test S3 connection and permissions
     */
    suspend fun testS3Connection(): S3TestResult {
        return withContext(Dispatchers.IO) {
            try {
                val validationResult = s3Configuration.validateConfiguration()
                if (!validationResult.isValid) {
                    return@withContext S3TestResult.Error("Configuration invalid: ${validationResult.missingFields.joinToString(", ")}")
                }
                
                val s3Client = createS3Client() 
                    ?: return@withContext S3TestResult.Error("Failed to create S3 client")
                
                val bucketName = s3Configuration.getBucketName()!!
                
                // Test bucket access by trying to list objects (which will fail if bucket doesn't exist)
                try {
                    s3Client.listObjects(bucketName).objectSummaries
                } catch (e: Exception) {
                    return@withContext S3TestResult.Error("Bucket '$bucketName' does not exist or is not accessible: ${e.message}")
                }
                
                // Test write permissions with a small test file
                val testKey = "${s3Configuration.getFolderPrefix()}/test-connection.txt"
                val testContent = "QR Scanner connection test"
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
                
                S3TestResult.Success("S3 connection successful!")
                
            } catch (e: Exception) {
                S3TestResult.Error("Connection failed: ${e.message}")
            }
        }
    }
}

/**
 * S3 test result sealed class
 */
sealed class S3TestResult {
    data class Success(val message: String) : S3TestResult()
    data class Error(val message: String) : S3TestResult()
}
