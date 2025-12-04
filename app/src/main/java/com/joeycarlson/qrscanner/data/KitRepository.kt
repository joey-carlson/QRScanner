package com.joeycarlson.qrscanner.data

import android.content.Context
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate

/**
 * Repository for managing kit bundle data storage and retrieval.
 * Extends BaseRepository to leverage common file I/O operations.
 * Saves records to JSON files with "qr_kits" prefix.
 */
class KitRepository(context: Context) : BaseRepository<KitBundle>(context) {
    
    override fun getFileNamePrefix(): String = "qr_kits"
    
    override fun getListType(): java.lang.reflect.Type = 
        object : TypeToken<MutableList<KitBundle>>() {}.type
    
    suspend fun saveKitBundle(kitBundle: KitBundle): Boolean = withContext(Dispatchers.IO) {
        try {
            // Validate kit bundle has at least one component
            if (!kitBundle.isValid()) {
                return@withContext false
            }
            
            val existingBundles = loadRecords()
            existingBundles.add(kitBundle)
            
            val jsonContent = gson.toJson(existingBundles)
            return@withContext saveJsonContent(jsonContent)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext false
        }
    }
    
    suspend fun getAllKitBundles(): List<KitBundle> = withContext(Dispatchers.IO) {
        loadRecords()
    }
    
    suspend fun getBundlesForDate(date: LocalDate): List<KitBundle> = withContext(Dispatchers.IO) {
        try {
            val fileName = getFileNameForDate(date)
            loadRecords(fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    suspend fun findKitBundleById(kitId: String): KitBundle? = withContext(Dispatchers.IO) {
        try {
            val bundles = loadRecords()
            bundles.find { it.kitId == kitId }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    suspend fun bundleExistsForToday(baseKitCode: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val expectedKitId = KitBundle.generateKitId(baseKitCode)
            val bundles = loadRecords()
            bundles.any { it.kitId == expectedKitId }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
