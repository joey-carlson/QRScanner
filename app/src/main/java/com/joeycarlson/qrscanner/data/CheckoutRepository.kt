package com.joeycarlson.qrscanner.data

import android.content.Context
import android.os.Environment
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
        // Use app's external files directory - accessible via Files app without special permissions
        val appExternalDir = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            ?: context.getExternalFilesDir(null) // Fallback to root external files dir
        
        if (appExternalDir?.exists() != true) {
            appExternalDir?.mkdirs()
        }
        return File(appExternalDir, fileName)
    }
    
    suspend fun saveCheckout(userId: String, kitId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = CheckoutRecord(userId, kitId)
            val existingRecords = loadExistingRecords()
            existingRecords.add(record)
            
            val dataFile = getDataFile()
            FileWriter(dataFile).use { writer ->
                gson.toJson(existingRecords, writer)
            }
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }
    
    private fun loadExistingRecords(): MutableList<CheckoutRecord> {
        return try {
            val dataFile = getDataFile()
            if (!dataFile.exists()) {
                return mutableListOf()
            }
            
            FileReader(dataFile).use { reader ->
                val type = object : TypeToken<MutableList<CheckoutRecord>>() {}.type
                gson.fromJson(reader, type) ?: mutableListOf()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            mutableListOf()
        }
    }
    
    suspend fun getAllCheckouts(): List<CheckoutRecord> = withContext(Dispatchers.IO) {
        loadExistingRecords()
    }
}
