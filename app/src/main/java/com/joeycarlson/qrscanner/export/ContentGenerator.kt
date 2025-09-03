package com.joeycarlson.qrscanner.export

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.joeycarlson.qrscanner.data.CheckoutRecord
import com.joeycarlson.qrscanner.data.KitBundle

/**
 * Generates export content in various formats (JSON, CSV, XML, TXT).
 * Handles the conversion of CheckoutRecord and KitBundle data into different file formats.
 */
class ContentGenerator {
    
    private val gson: Gson = GsonBuilder()
        .setPrettyPrinting()
        .create()
    
    /**
     * Generates content in the specified format for checkout records
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
            ExportFormat.KIT_LABELS_CSV -> throw IllegalArgumentException("KIT_LABELS_CSV format is only for kit bundles")
        }
    }
    
    /**
     * Generates content in the specified format for kit bundles
     */
    fun generateKitBundleContent(
        bundles: List<KitBundle>,
        format: ExportFormat,
        locationId: String
    ): String {
        return when (format) {
            ExportFormat.JSON -> generateKitBundleJsonContent(bundles)
            ExportFormat.CSV -> generateKitBundleCsvContent(bundles, locationId)
            ExportFormat.XML -> generateKitBundleXmlContent(bundles, locationId)
            ExportFormat.TXT -> generateKitBundleTxtContent(bundles, locationId)
            ExportFormat.KIT_LABELS_CSV -> generateKitLabelsCsvContent(bundles)
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
     * Generates JSON content from kit bundles
     */
    private fun generateKitBundleJsonContent(bundles: List<KitBundle>): String {
        return gson.toJson(bundles)
    }
    
    /**
     * Generates CSV content from kit bundles
     */
    private fun generateKitBundleCsvContent(bundles: List<KitBundle>, locationId: String): String {
        val stringBuilder = StringBuilder()
        
        // CSV header
        stringBuilder.appendLine("KitID,BaseKitCode,Glasses,Controller,Battery01,Battery02,Battery03,Pads,Unused01,Unused02,ComponentCount,Timestamp,Location")
        
        // CSV data rows
        bundles.forEach { bundle ->
            stringBuilder.append("\"${bundle.kitId}\",")
            stringBuilder.append("\"${bundle.baseKitCode}\",")
            stringBuilder.append("\"${bundle.glasses ?: ""}\",")
            stringBuilder.append("\"${bundle.controller ?: ""}\",")
            stringBuilder.append("\"${bundle.battery01 ?: ""}\",")
            stringBuilder.append("\"${bundle.battery02 ?: ""}\",")
            stringBuilder.append("\"${bundle.battery03 ?: ""}\",")
            stringBuilder.append("\"${bundle.pads ?: ""}\",")
            stringBuilder.append("\"${bundle.unused01 ?: ""}\",")
            stringBuilder.append("\"${bundle.unused02 ?: ""}\",")
            stringBuilder.append("\"${bundle.getFilledComponentCount()}\",")
            stringBuilder.append("\"${bundle.timestamp}\",")
            stringBuilder.appendLine("\"$locationId\"")
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * Generates XML content from kit bundles
     */
    private fun generateKitBundleXmlContent(bundles: List<KitBundle>, locationId: String): String {
        val stringBuilder = StringBuilder()
        
        stringBuilder.appendLine("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        stringBuilder.appendLine("<kitBundles location=\"$locationId\">")
        
        bundles.forEach { bundle ->
            stringBuilder.appendLine("  <kitBundle>")
            stringBuilder.appendLine("    <kitId>${escapeXml(bundle.kitId)}</kitId>")
            stringBuilder.appendLine("    <baseKitCode>${escapeXml(bundle.baseKitCode)}</baseKitCode>")
            stringBuilder.appendLine("    <components>")
            bundle.glasses?.let { stringBuilder.appendLine("      <glasses>${escapeXml(it)}</glasses>") }
            bundle.controller?.let { stringBuilder.appendLine("      <controller>${escapeXml(it)}</controller>") }
            bundle.battery01?.let { stringBuilder.appendLine("      <battery01>${escapeXml(it)}</battery01>") }
            bundle.battery02?.let { stringBuilder.appendLine("      <battery02>${escapeXml(it)}</battery02>") }
            bundle.battery03?.let { stringBuilder.appendLine("      <battery03>${escapeXml(it)}</battery03>") }
            bundle.pads?.let { stringBuilder.appendLine("      <pads>${escapeXml(it)}</pads>") }
            bundle.unused01?.let { stringBuilder.appendLine("      <unused01>${escapeXml(it)}</unused01>") }
            bundle.unused02?.let { stringBuilder.appendLine("      <unused02>${escapeXml(it)}</unused02>") }
            stringBuilder.appendLine("    </components>")
            stringBuilder.appendLine("    <componentCount>${bundle.getFilledComponentCount()}</componentCount>")
            stringBuilder.appendLine("    <timestamp>${escapeXml(bundle.timestamp.toString())}</timestamp>")
            stringBuilder.appendLine("    <location>${escapeXml(locationId)}</location>")
            stringBuilder.appendLine("  </kitBundle>")
        }
        
        stringBuilder.appendLine("</kitBundles>")
        return stringBuilder.toString()
    }
    
    /**
     * Generates plain text content from kit bundles
     */
    private fun generateKitBundleTxtContent(bundles: List<KitBundle>, locationId: String): String {
        val stringBuilder = StringBuilder()
        
        stringBuilder.appendLine("Kit Bundle Records")
        stringBuilder.appendLine("Location: $locationId")
        stringBuilder.appendLine("Total Bundles: ${bundles.size}")
        stringBuilder.appendLine("=".repeat(50))
        stringBuilder.appendLine()
        
        bundles.forEachIndexed { index, bundle ->
            stringBuilder.appendLine("Bundle ${index + 1}:")
            stringBuilder.appendLine("  Kit ID: ${bundle.kitId}")
            stringBuilder.appendLine("  Base Kit Code: ${bundle.baseKitCode}")
            stringBuilder.appendLine("  Components (${bundle.getFilledComponentCount()} total):")
            
            bundle.getComponentList().forEach { (type, code) ->
                stringBuilder.appendLine("    $type: $code")
            }
            
            stringBuilder.appendLine("  Timestamp: ${bundle.timestamp}")
            stringBuilder.appendLine("  Location: $locationId")
            stringBuilder.appendLine()
        }
        
        return stringBuilder.toString()
    }
    
    /**
     * Generates label-oriented CSV content from kit bundles for printing individual component labels
     * Each component gets its own row in the CSV for label printing
     */
    private fun generateKitLabelsCsvContent(bundles: List<KitBundle>): String {
        val stringBuilder = StringBuilder()
        
        bundles.forEach { bundle ->
            // Extract kit number from baseKitCode (e.g., "K123" → "123")
            val kitNumber = bundle.baseKitCode.replace(Regex("[^0-9]"), "")
            
            // Add kit label
            stringBuilder.appendLine("Kit $kitNumber")
            
            // Add controller label (mapped to "Puck")
            bundle.controller?.let {
                stringBuilder.appendLine("Puck $kitNumber")
            }
            
            // Add glasses label (mapped to "G")
            bundle.glasses?.let {
                stringBuilder.appendLine("G $kitNumber")
            }
            
            // Add battery labels with sequential numbering
            bundle.battery01?.let {
                stringBuilder.appendLine("Battery $kitNumber-1")
            }
            
            bundle.battery02?.let {
                stringBuilder.appendLine("Battery $kitNumber-2")
            }
            
            bundle.battery03?.let {
                stringBuilder.appendLine("Battery $kitNumber-3")
            }
            
            // Add pads label using component name
            bundle.pads?.let {
                stringBuilder.appendLine("Pads $kitNumber")
            }
            
            // Skip unused01 and unused02 as requested
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
