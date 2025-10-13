package com.joeycarlson.qrscanner.export.datasource

import android.content.Context
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.data.InventoryRepository
import com.joeycarlson.qrscanner.export.ExportFormat
import com.joeycarlson.qrscanner.inventory.InventoryRecord
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Data source adapter for inventory records.
 * Bridges the InventoryRepository with the unified export system.
 */
class InventoryDataSource(
    private val context: Context
) : ExportDataSource {
    
    private val repository = InventoryRepository(context)
    private val gson = Gson()
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    override fun getExportType(): String = AppConfig.EXPORT_TYPE_INVENTORY
    
    override fun getDisplayName(): String = "Device Inventory"
    
    override fun supportsDateRange(): Boolean = false  // Inventory is session-based, not date-range based
    
    override suspend fun getDataForDateRange(
        startDate: LocalDate, 
        endDate: LocalDate
    ): Map<LocalDate, String> {
        // For inventory, we export the current session as a single file
        // The date range is ignored, but we'll use today's date as the key
        val inventoryData = repository.getInventoryDataForExport()
        return if (inventoryData.isNotEmpty()) {
            mapOf(LocalDate.now() to inventoryData)
        } else {
            emptyMap()
        }
    }
    
    override suspend fun getAllData(): String {
        // Return the current inventory session data
        return repository.getInventoryDataForExport()
    }
    
    override fun getFilenamePrefix(date: LocalDate?): String {
        return "device_inventory"
    }
    
    override fun getSupportedFormats(): List<com.joeycarlson.qrscanner.export.datasource.ExportFormat> {
        // Inventory only supports JSON format as per requirements
        return listOf(
            com.joeycarlson.qrscanner.export.datasource.ExportFormat(
                format = ExportFormat.JSON,
                displayName = "JSON",
                description = "JavaScript Object Notation - structured inventory data",
                isDefault = true
            )
        )
    }
    
    override suspend fun hasData(): Boolean {
        return repository.getInventoryRecords().isNotEmpty()
    }
    
    override suspend fun getRecordCount(startDate: LocalDate?, endDate: LocalDate?): Int {
        return repository.getInventoryRecords().size
    }
    
    /**
     * Get a summary of the current inventory session
     */
    suspend fun getInventorySummary(): InventorySummary {
        val records = repository.getInventoryRecords()
        val devicesByType = records.groupBy { it.componentType }
        
        return InventorySummary(
            totalDevices = records.size,
            glassesCount = devicesByType["glasses"]?.size ?: 0,
            controllerCount = devicesByType["controller"]?.size ?: 0,
            batteryCount = devicesByType["battery"]?.size ?: 0,
            sessionDate = LocalDate.now()
        )
    }
    
    /**
     * Clear the current inventory session
     */
    suspend fun clearInventory() {
        repository.clearInventory()
    }
}

/**
 * Summary data for an inventory session
 */
data class InventorySummary(
    val totalDevices: Int,
    val glassesCount: Int,
    val controllerCount: Int,
    val batteryCount: Int,
    val sessionDate: LocalDate
)
