package com.joeycarlson.qrscanner.data

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class CheckoutRepository(private val context: Context) {
    
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("com.joeycarlson.qrscanner_preferences", Context.MODE_PRIVATE)
    
    private fun getTodaysFileName(): String {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MM-dd-yy")
        val locationId = prefs.getString("location_id", "unknown")
        
        // Include location ID in filename if configured
        return if (!locationId.isNullOrEmpty() && locationId != "unknown") {
            "qr_checkouts_${today.format(formatter)}_${locationId}.json"
        } else {
            "qr_checkouts_${today.format(formatter)}.json"
        }
    }
    
    private fun getDataFile(): File {
        // Save to Downloads folder - always accessible via Files app
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return File(downloadsDir, getTodaysFileName())
    }
    
    suspend fun saveCheckout(userId: String, kitId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = CheckoutRecord(
                userId = userId,
                kitId = kitId,
                type = "CHECKOUT",
                value = "User $userId checked out Kit $kitId"
            )
            val existingRecords = loadExistingRecords()
            existingRecords.add(record)
            
            val jsonContent = gson.toJson(existingRecords)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+ (API 29+)
                saveToMediaStore(jsonContent)
            } else {
                // Use direct file access for older Android versions
                val dataFile = getDataFile()
                FileWriter(dataFile).use { writer ->
                    writer.write(jsonContent)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun saveToMediaStore(content: String): Boolean {
        return try {
            val resolver = context.contentResolver
            
            // First, try to find existing file
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(getTodaysFileName())
            
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
                    put(MediaStore.MediaColumns.DISPLAY_NAME, getTodaysFileName())
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
    
    private fun loadExistingRecords(): MutableList<CheckoutRecord> {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Try to load from MediaStore (Android 10+)
                loadFromMediaStore()
            } else {
                // Load from direct file access (older Android)
                val dataFile = getDataFile()
                if (!dataFile.exists()) {
                    return mutableListOf()
                }
                
                FileReader(dataFile).use { reader ->
                    val type = object : TypeToken<MutableList<CheckoutRecord>>() {}.type
                    gson.fromJson(reader, type) ?: mutableListOf()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }
    
    private fun loadFromMediaStore(): MutableList<CheckoutRecord> {
        return try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME
            )
            
            val selection = "${MediaStore.MediaColumns.DISPLAY_NAME} = ?"
            val selectionArgs = arrayOf(getTodaysFileName())
            
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
                        val type = object : TypeToken<MutableList<CheckoutRecord>>() {}.type
                        gson.fromJson(jsonContent, type) ?: mutableListOf()
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
    
    suspend fun saveOtherEntry(value: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // Additional security sanitization at repository level
            val sanitizedValue = sanitizeInput(value)
            
            val record = CheckoutRecord(
                userId = null,
                kitId = null,
                type = "OTHER",
                value = sanitizedValue
            )
            val existingRecords = loadExistingRecords()
            existingRecords.add(record)
            
            val jsonContent = gson.toJson(existingRecords)
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use MediaStore for Android 10+ (API 29+)
                saveToMediaStore(jsonContent)
            } else {
                // Use direct file access for older Android versions
                val dataFile = getDataFile()
                FileWriter(dataFile).use { writer ->
                    writer.write(jsonContent)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    private fun sanitizeInput(input: String): String {
        // Additional layer of security sanitization
        // Remove any remaining potentially dangerous characters
        return input
            .replace(Regex("[\"'`\\\\<>{}\\[\\];:,]"), "") // Remove quotes, brackets, special chars
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()
            .take(200) // Enforce length limit
    }
    
    suspend fun getAllCheckouts(): List<CheckoutRecord> = withContext(Dispatchers.IO) {
        loadExistingRecords()
    }
    
    suspend fun getRecordsForDate(date: LocalDate): List<CheckoutRecord> = withContext(Dispatchers.IO) {
        try {
            val formatter = DateTimeFormatter.ofPattern("MM-dd-yy")
            val locationId = prefs.getString("location_id", "unknown")
            
            // Build filename for the specific date
            val fileName = if (!locationId.isNullOrEmpty() && locationId != "unknown") {
                "qr_checkouts_${date.format(formatter)}_${locationId}.json"
            } else {
                "qr_checkouts_${date.format(formatter)}.json"
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Load from MediaStore for Android 10+
                loadFromMediaStoreByFileName(fileName)
            } else {
                // Load from direct file access for older Android
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val dataFile = File(downloadsDir, fileName)
                
                if (!dataFile.exists()) {
                    return@withContext emptyList()
                }
                
                FileReader(dataFile).use { reader ->
                    val type = object : TypeToken<List<CheckoutRecord>>() {}.type
                    gson.fromJson(reader, type) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    private fun loadFromMediaStoreByFileName(fileName: String): List<CheckoutRecord> {
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
                        val type = object : TypeToken<List<CheckoutRecord>>() {}.type
                        gson.fromJson(jsonContent, type) ?: emptyList()
                    } ?: emptyList()
                } else {
                    emptyList()
                }
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun getLastCheckout(): CheckoutRecord? = withContext(Dispatchers.IO) {
        try {
            val records = loadExistingRecords()
            records.filter { it.type == "CHECKOUT" }.maxByOrNull { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun deleteLastCheckout(): Boolean = withContext(Dispatchers.IO) {
        try {
            val records = loadExistingRecords()
            val lastCheckout = records.filter { it.type == "CHECKOUT" }.maxByOrNull { it.timestamp }
            
            if (lastCheckout != null) {
                records.remove(lastCheckout)
                val jsonContent = gson.toJson(records)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    saveToMediaStore(jsonContent)
                } else {
                    val dataFile = getDataFile()
                    FileWriter(dataFile).use { writer ->
                        writer.write(jsonContent)
                    }
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
