package com.joeycarlson.qrscanner.export

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.joeycarlson.qrscanner.config.AppConfig
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter

/**
 * Manages temporary file operations for exports.
 * Handles creation, URI generation, and cleanup of temporary export files.
 */
class TempFileManager(private val context: Context) {
    
    private val tempExportDir: File by lazy {
        File(context.cacheDir, "exports").apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }
    
    /**
     * Creates a temporary file with the given filename and content
     */
    fun createTempFile(filename: String, content: String): File {
        val file = File(tempExportDir, filename)
        
        FileOutputStream(file).use { fos ->
            OutputStreamWriter(fos).use { writer ->
                writer.write(content)
            }
        }
        
        return file
    }
    
    /**
     * Creates a temporary file with the given filename and byte content
     */
    fun createTempFile(filename: String, content: ByteArray): File {
        val file = File(tempExportDir, filename)
        
        FileOutputStream(file).use { fos ->
            fos.write(content)
        }
        
        return file
    }
    
    /**
     * Gets a content URI for the given file using FileProvider
     */
    fun getUriForFile(file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }
    
    /**
     * Cleans up specific temporary files
     */
    fun cleanupFiles(files: List<File>) {
        files.forEach { file ->
            try {
                if (file.exists()) {
                    file.delete()
                }
            } catch (e: Exception) {
                // Log but don't throw - cleanup is best effort
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Cleans up all temporary export files
     */
    fun cleanupAllExportFiles() {
        try {
            tempExportDir.listFiles()?.forEach { file ->
                file.delete()
            }
        } catch (e: Exception) {
            // Log but don't throw - cleanup is best effort
            e.printStackTrace()
        }
    }
    
    /**
     * Cleans up old temporary files (older than specified hours)
     */
    fun cleanupOldFiles(hoursOld: Int = 24) {
        val cutoffTime = System.currentTimeMillis() - (hoursOld * 60 * 60 * 1000)
        
        try {
            tempExportDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Log but don't throw - cleanup is best effort
            e.printStackTrace()
        }
    }
    
    /**
     * Gets the total size of all temporary export files
     */
    fun getTempFilesSizeInBytes(): Long {
        return tempExportDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
    
    /**
     * Gets the total size of temporary export files in MB
     */
    fun getTempFilesSizeInMB(): Double {
        return getTempFilesSizeInBytes() / (1024.0 * 1024.0)
    }
    
    /**
     * Checks if a temporary file exists
     */
    fun tempFileExists(filename: String): Boolean {
        return File(tempExportDir, filename).exists()
    }
    
    /**
     * Gets a list of all temporary export files
     */
    fun getAllTempFiles(): List<File> {
        return tempExportDir.listFiles()?.toList() ?: emptyList()
    }
    
    /**
     * Creates a unique temporary filename if the original already exists
     */
    fun ensureUniqueFilename(filename: String): String {
        if (!tempFileExists(filename)) {
            return filename
        }
        
        val nameWithoutExtension = filename.substringBeforeLast(".")
        val extension = filename.substringAfterLast(".", "")
        var counter = 1
        var uniqueFilename: String
        
        do {
            uniqueFilename = if (extension.isEmpty()) {
                "${nameWithoutExtension}_$counter"
            } else {
                "${nameWithoutExtension}_$counter.$extension"
            }
            counter++
        } while (tempFileExists(uniqueFilename))
        
        return uniqueFilename
    }
}
