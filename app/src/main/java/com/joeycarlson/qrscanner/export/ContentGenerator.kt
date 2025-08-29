package com.joeycarlson.qrscanner.export

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.joeycarlson.qrscanner.data.CheckoutRecord

/**
 * Generates export content in various formats (JSON, CSV, XML, TXT).
 * Handles the conversion of CheckoutRecord data into different file formats.
 */
class ContentGenerator {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    /**
     * Generates content in the specified format
     */
    fun generateContent(
        records: List<CheckoutRecord>, 
        format: ExportFormat, 
        locationId: String
    ): String {
        return when (format) {
            ExportFormat.JSON -> generateJsonContent(records)
            ExportFormat.CSV -> generateCsvContent(records, locationId)
            ExportFormat.XML -> generateXmlContent(records, locationId)
            ExportFormat.TXT -> generateTxtContent(records, locationId)
        }
    }
    
    /**
     * Generates JSON content from checkout records
     */
    private fun generateJsonContent(records: List<CheckoutRecord>): String {
        return gson.toJson(records)
    }
    
    /**
     * Generates CSV content from checkout records
     */
    private fun generateCsvContent(records: List<CheckoutRecord>, locationId: String): String {
        val stringBuilder = StringBuilder()
        
        // CSV header
        stringBuilder.appendLine("User,Kit,Timestamp,Location")
        
        // CSV data rows
        records.forEach { record ->
            stringBuilder.append("\"${record.userId ?: ""}\",")
            stringBuilder.append("\"${record.kitId ?: ""}\",")
            stringBuilder.append("\"${record.timestamp}\",")
            stringBuilder.appendLine("\"$locationId\"")
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * Generates XML content from checkout records
     */
    private fun generateXmlContent(records: List<CheckoutRecord>, locationId: String): String {
        val stringBuilder = StringBuilder()
        
        stringBuilder.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        stringBuilder.appendLine("<checkouts location=\"$locationId\">")
        
        records.forEach { record ->
            stringBuilder.appendLine("  <checkout>")
            stringBuilder.appendLine("    <user>${escapeXml(record.userId ?: "")}</user>")
            stringBuilder.appendLine("    <kit>${escapeXml(record.kitId ?: "")}</kit>")
            stringBuilder.appendLine("    <timestamp>${escapeXml(record.timestamp)}</timestamp>")
            stringBuilder.appendLine("    <location>${escapeXml(locationId)}</location>")
            stringBuilder.appendLine("  </checkout>")
        }
        
        stringBuilder.appendLine("</checkouts>")
        return stringBuilder.toString()
    }
    
    /**
     * Generates plain text content from checkout records
     */
    private fun generateTxtContent(records: List<CheckoutRecord>, locationId: String): String {
        val stringBuilder = StringBuilder()
        
        stringBuilder.appendLine("QR Checkout Records")
        stringBuilder.appendLine("Location: $locationId")
        stringBuilder.appendLine("Total Records: ${records.size}")
        stringBuilder.appendLine("=".repeat(50))
        stringBuilder.appendLine()
        
        records.forEachIndexed { index, record ->
            stringBuilder.appendLine("Record ${index + 1}:")
            stringBuilder.appendLine("  User: ${record.userId}")
            stringBuilder.appendLine("  Kit: ${record.kitId}")
            stringBuilder.appendLine("  Timestamp: ${record.timestamp}")
            stringBuilder.appendLine("  Location: $locationId")
            stringBuilder.appendLine()
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * Escapes XML special characters
     */
    private fun escapeXml(text: String): String {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
