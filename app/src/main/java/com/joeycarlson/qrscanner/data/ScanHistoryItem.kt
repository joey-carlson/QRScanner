package com.joeycarlson.qrscanner.data

import java.text.SimpleDateFormat
import java.util.*

/**
 * Represents a single item in the scan history
 */
data class ScanHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    var value: String,
    val timestamp: Long = System.currentTimeMillis(),
    val scanType: ScanType,
    val activityType: ActivityType
) {
    /**
     * Type of scan that produced this item
     */
    enum class ScanType {
        BARCODE,
        OCR,
        MANUAL
    }
    
    /**
     * Activity where this scan was performed
     */
    enum class ActivityType {
        CHECKOUT,
        CHECKIN,
        KIT_BUNDLE,
        INVENTORY
    }
    
    /**
     * Get formatted timestamp string
     */
    fun getFormattedTimestamp(): String {
        val sdf = SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Get short display value (truncate if too long)
     */
    fun getShortValue(maxLength: Int = 30): String {
        return if (value.length > maxLength) {
            "${value.substring(0, maxLength)}..."
        } else {
            value
        }
    }
}
