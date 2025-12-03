package com.joeycarlson.qrscanner.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Base repository class providing common file I/O operations for all repositories.
 * Handles Android version-specific storage (MediaStore for API 29+, direct file access for older).
 * 
 * Follows SOLID principles:
 * - Single Responsibility: Manages file operations only
 * - Open/Closed: Extensible through inheritance
 * - Dependency Inversion: Subclasses depend on abstract methods
 */
abstract class BaseRepository<T>(protected val context: Context) {
    
    protected val gson = Gson()
    protected val prefs = context.getSharedPreferences(
        "com.joeycarlson.qrscanner_preferences", 
        Context.MODE_PRIVATE
    )
    
    /**
     * Subclasses must provide the base filename pattern (without date/location).
     * Example: "qr_checkouts" for checkout records
     */
    protected abstract fun getFileNamePrefix(): String
    
    /**
     * Subclasses must provide the TypeToken for Gson deserialization.
     * Example: object : TypeToken<MutableList<CheckoutRecord>>() {}.type
     */
    protected abstract fun getListType(): java.lang.reflect.Type
    
    /**
     * Generates filename with today's date and optional location ID.
     * Format: {prefix}_{MM-dd-yy}_{locationId}.json
     */
    protected fun getTodaysFileName(): String {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MM-dd-yy")
        val locationId = prefs.getString("location_id", "unknown")
        
        val prefix = getFileNamePrefix()
        
        return if (!locationId.isNullOrEmpty() && locationId != "unknown") {
            "${prefix}_${today.format(formatter)}_${locationId}.json"
        } else {
            "${prefix}_${today.format(formatter)}.json"
        }
    }
    
    /**
     * Generates filename for a specific date.
     */
    protected fun getFileNameForDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("MM-dd-yy")
        val locationId = prefs.getString("location_id", "unknown")
        
        val prefix = getFileNamePrefix()
        
        return if (!locationId.isNullOrEmpty() && locationId != "unknown") {
            "${prefix}_${date.format(formatter)}_${locationId}.json"
        } else {
            "${prefix}_${date.format(formatter)}.json"
        }
    }
    
    /**
     * Gets the file reference in the Downloads directory.
     * Used for Android versions below API 29.
     */
    protected fun getDataFile(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return File(downloadsDir, getTodaysFileName())
    }
    
    /**
     * Saves JSON content using the appropriate method for the Android version.
     */
    protected fun saveJsonContent(content: String, fileName: String = getTodaysFileName()): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveToMediaStore(content, fileName)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dataFile = File(downloadsDir, fileName)
                FileWriter(dataFile).use { writer ->
                    writer.write(content)
                }
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Saves content to MediaStore (Android 10+).
     */
    private fun saveToMediaStore(content: String, fileName: String): Boolean {
        return try {
            val resolver = context.contentResolver
            
            // Try to find existing file
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            
            var existingUri: android.net.Uri? = null
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
                    existingUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon()
                        .appendPath(id.toString()).build()
                }
            }
            
            val uri = existingUri ?: run {
                // Create new file if doesn't exist
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                }
                resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            }
            
            uri?.let {
                resolver.openOutputStream(it, "wt")?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Loads records from storage using the appropriate method for the Android version.
     */
    protected fun loadRecords(fileName: String = getTodaysFileName()): MutableList<T> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                loadFromMediaStore(fileName)
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dataFile = File(downloadsDir, fileName)
                
                if (!dataFile.exists()) {
                    return mutableListOf()
                }
                
                FileReader(dataFile).use { reader ->
                    gson.fromJson(reader, getListType()) ?: mutableListOf()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }
    
    /**
     * Loads records from MediaStore (Android 10+).
     */
    private fun loadFromMediaStore(fileName: String): MutableList<T> {
        return try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(fileName)
            
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
                        val jsonContent = inputStream.bufferedReader().use { it.readText() }
                        gson.fromJson(jsonContent, getListType()) ?: mutableListOf()
                    } ?: mutableListOf()
                } else {
                    mutableListOf()
                }
            } ?: mutableListOf()
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }
    
    /**
     * Sanitizes user input to prevent injection attacks and enforce limits.
     */
    protected fun sanitizeInput(input: String): String {
        return input
            .replace(Regex("[\"'`\\\\<>{}\\[\\];:,]"), "") // Remove potentially dangerous chars
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
            .take(200) // Enforce length limit
    }
}
