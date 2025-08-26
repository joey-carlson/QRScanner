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

class CheckoutRepository(private val context: Context) {
    
    private val gson = Gson()
    private val fileName = "qr_checkouts.json"
    
    private fun getDataFile(): File {
        // Save to Downloads folder - always accessible via Files app
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs()
        }
        return File(downloadsDir, fileName)
    }
    
    suspend fun saveCheckout(userId: String, kitId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = CheckoutRecord(userId, kitId)
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
    
    suspend fun getAllCheckouts(): List<CheckoutRecord> = withContext(Dispatchers.IO) {
        loadExistingRecords()
    }
}
