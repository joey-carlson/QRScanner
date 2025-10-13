package com.joeycarlson.qrscanner.export.datasource

import android.content.Context
import com.google.gson.Gson
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.data.KitBundle
import com.joeycarlson.qrscanner.data.KitRepository
import com.joeycarlson.qrscanner.export.ExportFormat
import java.time.LocalDate

/**
 * Data source adapter for kit bundle records.
 * Bridges the KitRepository with the unified export system.
 */
class KitBundleDataSource(
    private val context: Context
) : ExportDataSource {
    
    private val repository = KitRepository(context)
    private val gson = Gson()
    
    override fun getExportType(): String = AppConfig.EXPORT_TYPE_KIT_BUNDLE
    
    override fun getDisplayName(): String = "Kit Bundles"
    
    override fun supportsDateRange(): Boolean = true
    
    override suspend fun getDataForDateRange(
        startDate: LocalDate, 
        endDate: LocalDate
    ): Map<LocalDate, String> {
        val dataMap = mutableMapOf<LocalDate, String>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            val records = repository.getKitsForDate(currentDate)
            if (records.isNotEmpty()) {
                dataMap[currentDate] = gson.toJson(records)
            }
            currentDate = currentDate.plusDays(1)
        }
        
        return dataMap
    }
    
    override suspend fun getAllData(): String {
        // For kit bundles, we'll return all records from the past 30 days by default
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(30)
        val allRecords = mutableListOf<KitBundle>()
        
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            allRecords.addAll(repository.getKitsForDate(currentDate))
            currentDate = currentDate.plusDays(1)
        }
        
        return gson.toJson(allRecords)
    }
    
    override fun getFilenamePrefix(date: LocalDate?): String {
        return "qr_kits"
    }
    
    override fun getSupportedFormats(): List<com.joeycarlson.qrscanner.export.datasource.ExportFormat> {
        return listOf(
            com.joeycarlson.qrscanner.export.datasource.ExportFormat(
                format = ExportFormat.JSON,
                displayName = "JSON",
                description = "JavaScript Object Notation - ideal for database import",
                isDefault = true
            ),
            com.joeycarlson.qrscanner.export.datasource.ExportFormat(
                format = ExportFormat.CSV,
                displayName = "CSV",
                description = "Comma-Separated Values - for spreadsheet applications",
                isDefault = false
            ),
            com.joeycarlson.qrscanner.export.datasource.ExportFormat(
                format = ExportFormat.KIT_LABELS_CSV,
                displayName = "Kit Labels CSV",
                description = "Single-column CSV format optimized for label printing",
                isDefault = false
            )
        )
    }
    
    override suspend fun hasData(): Boolean {
        // Check if there's any data in the last 90 days
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(90)
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            if (repository.getKitsForDate(currentDate).isNotEmpty()) {
                return true
            }
            currentDate = currentDate.plusDays(1)
        }
        
        return false
    }
    
    override suspend fun getRecordCount(startDate: LocalDate?, endDate: LocalDate?): Int {
        val actualStartDate = startDate ?: LocalDate.now().minusDays(30)
        val actualEndDate = endDate ?: LocalDate.now()
        
        var count = 0
        var currentDate = actualStartDate
        
        while (!currentDate.isAfter(actualEndDate)) {
            count += repository.getKitsForDate(currentDate).size
            currentDate = currentDate.plusDays(1)
        }
        
        return count
    }
    
    /**
     * Generate kit labels CSV content for all bundles in the date range
     */
    suspend fun generateKitLabelsContent(startDate: LocalDate, endDate: LocalDate): String {
        val allKits = mutableListOf<KitBundle>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            allKits.addAll(repository.getKitsForDate(currentDate))
            currentDate = currentDate.plusDays(1)
        }
        
        return generateKitLabelsCsvContent(allKits)
    }
    
    /**
     * Generate single-column CSV content for kit labels
     */
    private fun generateKitLabelsCsvContent(kits: List<KitBundle>): String {
        val labels = mutableListOf<String>()
        
        for (kit in kits) {
            // Extract the kit number from the kit ID (e.g., "K123-08/30" -> "123")
            val kitNumber = kit.baseKitCode.removePrefix("K")
            
            // Add the kit label
            labels.add("Kit $kitNumber")
            
            // Add component labels with naming based on type
            kit.controller?.let { 
                labels.add("Puck $kitNumber")  // Controllers are labeled as "Puck"
            }
            
            kit.glasses?.let { 
                labels.add("G $kitNumber")  // Glasses are labeled as "G"
            }
            
            // Add battery labels with sequential numbering
            var batteryCount = 1
            kit.battery01?.let { 
                labels.add("Battery $kitNumber-$batteryCount")
                batteryCount++
            }
            kit.battery02?.let { 
                labels.add("Battery $kitNumber-$batteryCount")
                batteryCount++
            }
            kit.battery03?.let { 
                labels.add("Battery $kitNumber-$batteryCount")
            }
            
            // Add pads label if present
            kit.pads?.let { 
                labels.add("Pads $kitNumber")
            }
            
            // Note: unused01 and unused02 are intentionally not included in labels
        }
        
        // Return as single-column CSV (each label on a new line)
        return labels.joinToString("\n")
    }
}
