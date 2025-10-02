package com.joeycarlson.qrscanner.data

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.joeycarlson.qrscanner.config.PreferenceKeys
import com.joeycarlson.qrscanner.inventory.ComponentType
import com.joeycarlson.qrscanner.inventory.InventoryRecord
import com.joeycarlson.qrscanner.ocr.ScanMode
import com.joeycarlson.qrscanner.util.FileManager
import com.joeycarlson.qrscanner.util.LogManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repository for managing inventory data.
 * Handles in-memory storage and export functionality for device inventory records.
 */
class InventoryRepository(
    private val context: Context,
    private val fileManager: FileManager
) {
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    private val sharedPreferences: SharedPreferences = 
        PreferenceManager.getDefaultSharedPreferences(context)
    
    // In-memory storage for current inventory session
    private val inventoryRecords = mutableListOf<InventoryRecord>()
    
    // Track if there are unsaved changes
    private var hasUnsavedChanges = false
    
    /**
     * Add a new inventory record
     */
    suspend fun addInventoryRecord(
        deviceId: String,
        componentType: ComponentType,
        scanMode: ScanMode
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val record = InventoryRecord.create(
                deviceId = deviceId,
                componentType = componentType,
                scanMode = scanMode
            )
            
            inventoryRecords.add(record)
            hasUnsavedChanges = true
            
            LogManager.getInstance(context).log("InventoryRepository", "Added inventory record: $deviceId, Type: ${componentType.name}")
            true
        } catch (e: Exception) {
            LogManager.getInstance(context).logError("InventoryRepository", "Failed to add inventory record", e)
            false
        }
    }
    
    /**
     * Check if a device ID already exists in the current inventory
     */
    fun isDeviceAlreadyScanned(deviceId: String): Boolean {
        return inventoryRecords.any { it.deviceId == deviceId }
    }
    
    /**
     * Get all inventory records
     */
    fun getAllRecords(): List<InventoryRecord> = inventoryRecords.toList()
    
    /**
     * Get count of inventory records
     */
    fun getRecordCount(): Int = inventoryRecords.size
    
    /**
     * Get count by component type
     */
    fun getCountByType(componentType: ComponentType): Int {
        return inventoryRecords.count { it.componentType == componentType.name }
    }
    
    /**
     * Clear all inventory records
     */
    fun clearInventory() {
        inventoryRecords.clear()
        hasUnsavedChanges = false
        LogManager.getInstance(context).log("InventoryRepository", "Cleared all inventory records")
    }
    
    /**
     * Check if there are unsaved changes
     */
    fun hasUnsavedChanges(): Boolean = hasUnsavedChanges
    
    /**
     * Export inventory data as JSON for the export system
     * Returns the JSON string representation of inventory data
     */
    suspend fun exportInventoryData(): String? = withContext(Dispatchers.IO) {
        try {
            val locationId = sharedPreferences.getString(
                PreferenceKeys.LOCATION_ID, 
                "Unknown"
            ) ?: "Unknown"
            
            val exportData = InventoryExportData(
                locationId = locationId,
                exportDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                totalDevices = inventoryRecords.size,
                devicesByType = ComponentType.values().associate { type ->
                    type.displayName to getCountByType(type)
                },
                devices = inventoryRecords.sortedWith(
                    compareBy({ it.componentType }, { it.timestamp })
                )
            )
            
            val jsonData = gson.toJson(exportData)
            hasUnsavedChanges = false
            
            LogManager.getInstance(context).log("InventoryRepository", "Exported ${inventoryRecords.size} inventory records")
            jsonData
        } catch (e: Exception) {
            LogManager.getInstance(context).logError("InventoryRepository", "Failed to export inventory data", e)
            null
        }
    }
    
    /**
     * Remove the last scanned device (undo functionality)
     */
    fun removeLastScannedDevice(): Boolean {
        return if (inventoryRecords.isNotEmpty()) {
            val removed = inventoryRecords.removeAt(inventoryRecords.lastIndex)
            LogManager.getInstance(context).log("InventoryRepository", "Removed last scanned device: ${removed.deviceId}")
            true
        } else {
            false
        }
    }
    
    /**
     * Data class for inventory export format
     */
    private data class InventoryExportData(
        val locationId: String,
        val exportDate: String,
        val totalDevices: Int,
        val devicesByType: Map<String, Int>,
        val devices: List<InventoryRecord>
    )
}
