package com.joeycarlson.qrscanner.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.joeycarlson.qrscanner.config.AppConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.OutputStreamWriter

/**
 * Centralized file operations manager for the QR Scanner app.
 * Handles Android version compatibility, MediaStore vs direct file access,
 * and provides consistent error handling across all file operations.
 */
class FileManager(private val context: Context) {
    
    /**
     * File operation result wrapper for consistent error handling
     */
    sealed class FileResult<T> {
        data class Success<T>(val data: T) : FileResult<T>()
        data class Error<T>(val message: String, val exception: Throwable? = null) : FileResult<T>()
    }
    
    /**
     * File location strategies for different Android versions
     */
    private enum class FileStrategy {
        MEDIA_STORE,    // Android 10+ (API 29+)
        DIRECT_ACCESS   // Android 9 and below
    }
    
    private fun getFileStrategy(): FileStrategy {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            FileStrategy.MEDIA_STORE
        } else {
            FileStrategy.DIRECT_ACCESS
        }
    }
    
    /**
     * Save content to Downloads folder with Android version compatibility
     */
    suspend fun saveToDownloads(
        filename: String,
        content: String,
        mimeType: String = AppConfig.MIME_TYPE_JSON
    ): FileResult<Uri> = withContext(Dispatchers.IO) {
        try {
            when (getFileStrategy()) {
                FileStrategy.MEDIA_STORE -> saveToDownloadsMediaStore(filename, content, mimeType)
                FileStrategy.DIRECT_ACCESS -> saveToDownloadsDirect(filename, content)
            }
        } catch (e: Exception) {
            FileResult.Error("Failed to save to Downloads: ${e.message}", e)
        }
    }
    
    private fun saveToDownloadsMediaStore(
        filename: String,
        content: String,
        mimeType: String
    ): FileResult<Uri> {
        return try {
            val contentValues = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, filename)
                put(MediaStore.Downloads.MIME_TYPE, mimeType)
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            
            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            
            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    OutputStreamWriter(outputStream).use { writer ->
                        writer.write(content)
                    }
                }
                
                // Mark as not pending
                contentValues.clear()
                contentValues.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
                
                FileResult.Success(uri)
            } ?: FileResult.Error("Failed to create MediaStore entry")
        } catch (e: Exception) {
            FileResult.Error("MediaStore save failed: ${e.message}", e)
        }
    }
    
    private fun saveToDownloadsDirect(filename: String, content: String): FileResult<Uri> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            ensureDirectoryExists(downloadsDir)
            
            val file = File(downloadsDir, filename)
            FileWriter(file).use { writer ->
                writer.write(content)
            }
            
            FileResult.Success(Uri.fromFile(file))
        } catch (e: Exception) {
            FileResult.Error("Direct file save failed: ${e.message}", e)
        }
    }
    
    /**
     * Save content to temporary cache directory for sharing
     */
    suspend fun saveToTempFile(
        filename: String,
        content: String,
        subdirectory: String = AppConfig.TEMP_EXPORT_DIR
    ): FileResult<File> = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.cacheDir, subdirectory)
            ensureDirectoryExists(tempDir)
            
            val file = File(tempDir, filename)
            FileWriter(file).use { writer ->
                writer.write(content)
            }
            
            FileResult.Success(file)
        } catch (e: Exception) {
            FileResult.Error("Failed to save temp file: ${e.message}", e)
        }
    }
    
    /**
     * Load content from Downloads folder with Android version compatibility
     */
    suspend fun loadFromDownloads(filename: String): FileResult<String> = withContext(Dispatchers.IO) {
        try {
            when (getFileStrategy()) {
                FileStrategy.MEDIA_STORE -> loadFromDownloadsMediaStore(filename)
                FileStrategy.DIRECT_ACCESS -> loadFromDownloadsDirect(filename)
            }
        } catch (e: Exception) {
            FileResult.Error("Failed to load from Downloads: ${e.message}", e)
        }
    }
    
    private fun loadFromDownloadsMediaStore(filename: String): FileResult<String> {
        return try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(filename)
            
            val resolver = context.contentResolver
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                    val id = cursor.getLong(idColumn)
                    val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon()
                        .appendPath(id.toString()).build()
                    
                    resolver.openInputStream(uri)?.use { inputStream ->
                        val content = inputStream.bufferedReader().use { it.readText() }
                        FileResult.Success(content)
                    } ?: FileResult.Error("Failed to open input stream")
                } else {
                    FileResult.Error("File not found: $filename")
                }
            } ?: FileResult.Error("Query failed for file: $filename")
        } catch (e: Exception) {
            FileResult.Error("MediaStore load failed: ${e.message}", e)
        }
    }
    
    private fun loadFromDownloadsDirect(filename: String): FileResult<String> {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            
            if (!file.exists()) {
                return FileResult.Error("File not found: $filename")
            }
            
            val content = FileReader(file).use { it.readText() }
            FileResult.Success(content)
        } catch (e: Exception) {
            FileResult.Error("Direct file load failed: ${e.message}", e)
        }
    }
    
    /**
     * Check if file exists in Downloads folder
     */
    suspend fun fileExistsInDownloads(filename: String): Boolean = withContext(Dispatchers.IO) {
        try {
            when (getFileStrategy()) {
                FileStrategy.MEDIA_STORE -> fileExistsInDownloadsMediaStore(filename)
                FileStrategy.DIRECT_ACCESS -> fileExistsInDownloadsDirect(filename)
            }
        } catch (e: Exception) {
            false
        }
    }
    
    private fun fileExistsInDownloadsMediaStore(filename: String): Boolean {
        return try {
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(filename)
            
            val resolver = context.contentResolver
            resolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
            )?.use { cursor ->
                cursor.count > 0
            } ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    private fun fileExistsInDownloadsDirect(filename: String): Boolean {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadsDir, filename)
            file.exists()
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Create FileProvider URI for sharing files
     */
    fun createFileProviderUri(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            AppConfig.FILE_PROVIDER_AUTHORITY,
            file
        )
    }
    
    /**
     * Ensure directory exists, create if necessary
     */
    private fun ensureDirectoryExists(directory: File): Boolean {
        return if (!directory.exists()) {
            directory.mkdirs()
        } else {
            true
        }
    }
    
    /**
     * Clean up temporary files
     */
    fun cleanupTempFiles(files: List<File>) {
        files.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Log but don't throw - cleanup is best effort
                if (AppConfig.DEBUG) {
                    e.printStackTrace()
                }
            }
        }
    }
    
    /**
     * Clean up entire temporary directory
     */
    fun cleanupTempDirectory(subdirectory: String = AppConfig.TEMP_EXPORT_DIR) {
        try {
            val tempDir = File(context.cacheDir, subdirectory)
            if (tempDir.exists()) {
                tempDir.listFiles()?.forEach { file ->
                    file.delete()
                }
            }
        } catch (e: Exception) {
            if (AppConfig.DEBUG) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Get file size in bytes
     */
    fun getFileSize(file: File): Long {
        return try {
            if (file.exists()) file.length() else 0L
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Validate filename for security and compatibility
     */
    fun validateFilename(filename: String): Boolean {
        if (filename.isBlank()) return false
        if (filename.length > AppConfig.MAX_FILENAME_LENGTH) return false
        
        // Check for invalid characters
        val invalidChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        return !filename.any { it in invalidChars }
    }
    
    /**
     * Sanitize filename for safe file operations
     */
    fun sanitizeFilename(filename: String): String {
        return filename
            .replace(Regex("[/\\\\:*?\"<>|]"), "_")
            .take(AppConfig.MAX_FILENAME_LENGTH)
            .trim()
    }
    
    /**
     * Generate unique filename if file already exists
     */
    suspend fun generateUniqueFilename(
        baseFilename: String,
        checkLocation: FileLocation = FileLocation.DOWNLOADS
    ): String = withContext(Dispatchers.IO) {
        val baseName = baseFilename.substringBeforeLast(".")
        val extension = baseFilename.substringAfterLast(".", "")
        val fullExtension = if (extension.isNotEmpty()) ".$extension" else ""
        
        var counter = 1
        var candidateFilename = baseFilename
        
        while (fileExists(candidateFilename, checkLocation)) {
            candidateFilename = "${baseName}_$counter$fullExtension"
            counter++
            
            // Prevent infinite loop
            if (counter > AppConfig.MAX_FILE_VARIANTS) {
                break
            }
        }
        
        candidateFilename
    }
    
    /**
     * File location enumeration for different storage areas
     */
    enum class FileLocation {
        DOWNLOADS,
        TEMP_CACHE
    }
    
    /**
     * Check if file exists in specified location
     */
    private suspend fun fileExists(filename: String, location: FileLocation): Boolean {
        return when (location) {
            FileLocation.DOWNLOADS -> fileExistsInDownloads(filename)
            FileLocation.TEMP_CACHE -> {
                val tempDir = File(context.cacheDir, AppConfig.TEMP_EXPORT_DIR)
                File(tempDir, filename).exists()
            }
        }
    }
    
    /**
     * Batch file operations for multiple files
     */
    suspend fun saveMultipleToDownloads(
        files: Map<String, String>,
        mimeType: String = AppConfig.MIME_TYPE_JSON
    ): FileResult<List<Uri>> = withContext(Dispatchers.IO) {
        try {
            val results = mutableListOf<Uri>()
            val errors = mutableListOf<String>()
            
            files.forEach { (filename, content) ->
                when (val result = saveToDownloads(filename, content, mimeType)) {
                    is FileResult.Success -> results.add(result.data)
                    is FileResult.Error -> errors.add("$filename: ${result.message}")
                }
            }
            
            if (errors.isNotEmpty()) {
                FileResult.Error("Some files failed to save: ${errors.joinToString("; ")}")
            } else {
                FileResult.Success(results)
            }
        } catch (e: Exception) {
            FileResult.Error("Batch save failed: ${e.message}", e)
        }
    }
    
    /**
     * Get available storage space in Downloads directory
     */
    fun getAvailableStorageSpace(): Long {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            downloadsDir.freeSpace
        } catch (e: Exception) {
            0L
        }
    }
    
    /**
     * Check if external storage is available and writable
     */
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
}
