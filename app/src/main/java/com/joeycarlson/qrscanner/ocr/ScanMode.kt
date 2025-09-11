package com.joeycarlson.qrscanner.ocr

/**
 * Enum representing the available scanning modes
 * as defined in FR-OCR-001
 */
enum class ScanMode {
    /**
     * Barcode/QR code scanning only (default mode)
     * Uses ML Kit Barcode Scanner
     */
    BARCODE_ONLY,
    
    /**
     * OCR text recognition only
     * Uses ML Kit Text Recognition
     */
    OCR_ONLY
}

/**
 * Data class representing a scan result from any mode
 */
sealed class ScanResult {
    /**
     * Result from barcode scanning
     */
    data class BarcodeResult(
        val rawValue: String,
        val format: String, // QR_CODE, CODE_128, etc.
        val timestamp: Long = System.currentTimeMillis()
    ) : ScanResult()
    
    /**
     * Result from OCR scanning
     */
    data class OcrResult(
        val text: String,
        val confidence: Float,
        val inferredComponentType: DsnValidator.ComponentType? = null,
        val requiresManualVerification: Boolean = false,
        val timestamp: Long = System.currentTimeMillis()
    ) : ScanResult()
    
    /**
     * Result requiring manual input (when both modes fail)
     */
    data class ManualInputRequired(
        val reason: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : ScanResult()
}

/**
 * Interface for scan mode change listeners
 */
interface ScanModeChangeListener {
    fun onScanModeChanged(newMode: ScanMode)
}
