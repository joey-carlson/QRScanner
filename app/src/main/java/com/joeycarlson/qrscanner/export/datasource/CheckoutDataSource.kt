package com.joeycarlson.qrscanner.export.datasource

import android.content.Context
import com.google.gson.Gson
import com.joeycarlson.qrscanner.config.AppConfig
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.data.CheckoutRepository
import com.joeycarlson.qrscanner.export.ExportFormat
import java.time.LocalDate

/**
 * Data source adapter for kit checkout records.
 * Bridges the CheckoutRepository with the unified export system.
 */
class CheckoutDataSource(
    private val context: Context
) : ExportDataSource {
    
    private val repository = CheckoutRepository(context)
    private val gson = Gson()
    
    override fun getExportType(): String = AppConfig.EXPORT_TYPE_CHECKOUT
    
    override fun getDisplayName(): String = "Kit Checkouts"
    
    override fun supportsDateRange(): Boolean = true
    
    override suspend fun getDataForDateRange(
        startDate: LocalDate, 
        endDate: LocalDate
    ): Map<LocalDate, String> {
        val dataMap = mutableMapOf<LocalDate, String>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            val records = repository.getRecordsForDate(currentDate)
            if (records.isNotEmpty()) {
                dataMap[currentDate] = gson.toJson(records)
            }
            currentDate = currentDate.plusDays(1)
        }
        
        return dataMap
    }
    
    override suspend fun getAllData(): String {
        // For checkouts, we'll return all records from the past 30 days by default
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(30)
        val allRecords = mutableListOf<CheckoutRecord>()
        
        var currentDate = startDate
        while (!currentDate.isAfter(endDate)) {
            allRecords.addAll(repository.getRecordsForDate(currentDate))
            currentDate = currentDate.plusDays(1)
        }
        
        return gson.toJson(allRecords)
    }
    
    override fun getFilenamePrefix(date: LocalDate?): String {
        return "qr_checkouts"
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
                format = ExportFormat.XML,
                displayName = "XML",
                description = "Extensible Markup Language - for enterprise systems",
                isDefault = false
            ),
            com.joeycarlson.qrscanner.export.datasource.ExportFormat(
                format = ExportFormat.TXT,
                displayName = "Text",
                description = "Plain text format - for simple viewing",
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
            if (repository.getRecordsForDate(currentDate).isNotEmpty()) {
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
            count += repository.getRecordsForDate(currentDate).size
            currentDate = currentDate.plusDays(1)
        }
        
        return count
    }
}
