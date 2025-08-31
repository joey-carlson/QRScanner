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
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for managing kit bundle data storage and retrieval.
 * 
 * This repository handles:
 * - Saving kit bundles to JSON files
 * - Loading kit bundles from storage
 * - File naming with location ID and date
 * - Android version compatibility (MediaStore for Android 10+)
 */
class KitRepository(private val context: Context) {
    
    private val gson = Gson()
    private val prefs = context.getSharedPreferences("com.joeycarlson.qrscanner_preferences", Context.MODE_PRIVATE)
    
    /**
     * Gets the filename for today's kit bundles file
     * Format: qr_kits_MM-dd-yy_LocationID.json
     */
    private fun getTodaysFileName(): String {
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("MM-dd-yy")
        val locationId = prefs.getString("location_id", "unknown")
        
        // Include location ID in filename if configured
        return if (!locationId.isNullOrEmpty() && locationId != "unknown") {
            "qr_kits_${today.format(formatter)}_${locationId}.json"
        } else {
            "qr_kits_${today.format(formatter)}.json"
        }
    }
    
    /**
     * Gets the data file in Downloads directory
     */
    private fun getDataFile(): File {
        // Save to Downloads folder - always accessible via Files app
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return File(downloadsDir, getTodaysFileName())
    }
    
    /**
     * Saves a kit bundle to storage
     * @param kitBundle The kit bundle to save
     * @return true if successful, false otherwise
     */
    suspend fun saveKitBundle(kitBundle: KitBundle): Boolean = withContext(Dispatchers.IO) {
        try {
            // Validate kit bundle has at least one component
            if (!kitBundle.isValid()) {
                return@withContext false
            }
            
            val existingBundles = loadExistingBundles()
            existingBundles.add(kitBundle)
            
            val jsonContent = gson.toJson(existingBundles)
            
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
    
    /**
     * Saves content to MediaStore for Android 10+
     */
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
    
    /**
     * Loads existing kit bundles from storage
     */
    private fun loadExistingBundles(): MutableList<KitBundle> {
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
                    val type = object : TypeToken<MutableList<KitBundle>>() {}.type
                    gson.fromJson(reader, type) ?: mutableListOf()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            mutableListOf()
        }
    }
    
    /**
     * Loads bundles from MediaStore for Android 10+
     */
    private fun loadFromMediaStore(): MutableList<KitBundle> {
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
                        val type = object : TypeToken<MutableList<KitBundle>>() {}.type
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
    
    /**
     * Gets all kit bundles for today
     */
    suspend fun getAllKitBundles(): List<KitBundle> = withContext(Dispatchers.IO) {
        loadExistingBundles()
    }
    
    /**
     * Gets kit bundles for a specific date
     * @param date The date to retrieve bundles for
     * @return List of kit bundles for that date
     */
    suspend fun getBundlesForDate(date: LocalDate): List<KitBundle> = withContext(Dispatchers.IO) {
        try {
            val formatter = DateTimeFormatter.ofPattern("MM-dd-yy")
            val locationId = prefs.getString("location_id", "unknown")
            
            // Build filename for the specific date
            val fileName = if (!locationId.isNullOrEmpty() && locationId != "unknown") {
                "qr_kits_${date.format(formatter)}_${locationId}.json"
            } else {
                "qr_kits_${date.format(formatter)}.json"
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
                    val type = object : TypeToken<List<KitBundle>>() {}.type
                    gson.fromJson(reader, type) ?: emptyList()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Loads bundles from MediaStore by specific filename
     */
    private fun loadFromMediaStoreByFileName(fileName: String): List<KitBundle> {
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
                        val type = object : TypeToken<List<KitBundle>>() {}.type
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
    
    /**
     * Finds a kit bundle by kit ID
     * @param kitId The kit ID to search for
     * @return The kit bundle if found, null otherwise
     */
    suspend fun findKitBundleById(kitId: String): KitBundle? = withContext(Dispatchers.IO) {
        try {
            val bundles = loadExistingBundles()
            bundles.find { it.kitId == kitId }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Checks if a kit bundle with the given base kit code exists for today
     * @param baseKitCode The base kit code to check
     * @return true if a bundle exists, false otherwise
     */
    suspend fun bundleExistsForToday(baseKitCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val expectedKitId = KitBundle.generateKitId(baseKitCode)
            val bundles = loadExistingBundles()
            bundles.any { it.kitId == expectedKitId }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
