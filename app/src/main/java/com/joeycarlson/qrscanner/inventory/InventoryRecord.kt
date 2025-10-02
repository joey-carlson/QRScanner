package com.joeycarlson.qrscanner.inventory

import com.google.gson.annotations.SerializedName
import com.joeycarlson.qrscanner.ocr.ScanMode
import java.time.Instant

/**
 * Data model representing an inventory record for a scanned device.
 * Used to create master inventory lists of all available devices.
 */
data class InventoryRecord(
    @SerializedName("device_id")
    val deviceId: String,
    
    @SerializedName("component_type")
    val componentType: String,
    
    @SerializedName("scan_mode")
    val scanMode: String,
    
    @SerializedName("timestamp")
    val timestamp: String = Instant.now().toString()
) {
    companion object {
        /**
         * Create an InventoryRecord from scan data
         */
        fun create(
            deviceId: String,
            componentType: ComponentType,
            scanMode: ScanMode
        ): InventoryRecord {
            return InventoryRecord(
                deviceId = deviceId,
                componentType = componentType.name,
                scanMode = when (scanMode) {
                    ScanMode.BARCODE_ONLY -> "BARCODE"
                    ScanMode.OCR_ONLY -> "OCR"
                    else -> "UNKNOWN"
                }
            )
        }
    }
}
