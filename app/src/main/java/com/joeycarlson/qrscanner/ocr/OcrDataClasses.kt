package com.joeycarlson.qrscanner.ocr

import android.graphics.Rect

/**
 * Shared data classes for OCR functionality
 */

/**
 * Data class for multi-frame averaging
 * Used by both TextRecognitionAnalyzer and HybridScanAnalyzer
 */
data class FrameResult(
    val text: String,
    val confidence: Float,
    val timestamp: Long,
    val boundingBox: Rect?
)

/**
 * Data class representing a single recognized text element
 */
data class RecognizedText(
    val text: String,
    val confidence: Float,
    val boundingBox: Rect?,
    val requiresManualVerification: Boolean = false
)

/**
 * Data class representing the complete result of text recognition
 */
data class TextRecognitionResult(
    val texts: List<RecognizedText>,
    val timestamp: Long
)
