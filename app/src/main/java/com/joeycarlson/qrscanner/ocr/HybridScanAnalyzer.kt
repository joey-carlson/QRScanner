package com.joeycarlson.qrscanner.ocr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.Executors

/**
 * Hybrid analyzer that can perform barcode scanning, OCR, or both
 * based on the selected scan mode
 */
class HybridScanAnalyzer(
    private var scanMode: ScanMode = ScanMode.BARCODE_ONLY,
    private val onScanResult: (ScanResult) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val barcodeScanner = BarcodeScanning.getClient()
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val dsnValidator = DsnValidator()
    
    private var lastAnalyzedTimestamp = 0L
    private val analysisInterval = 300L // Analyze every 300ms
    
    private val executor = Executors.newSingleThreadExecutor()
    
    // Track if we've found a result in the current frame to avoid duplicates
    private var resultFoundInFrame = false
    
    fun setScanMode(mode: ScanMode) {
        scanMode = mode
    }
    
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < analysisInterval) {
            imageProxy.close()
            return
        }
        
        lastAnalyzedTimestamp = currentTimestamp
        resultFoundInFrame = false
        
        @androidx.camera.core.ExperimentalGetImage
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        when (scanMode) {
            ScanMode.BARCODE_ONLY -> analyzeBarcodeOnly(image, imageProxy)
            ScanMode.OCR_ONLY -> analyzeOcrOnly(image, imageProxy)
            ScanMode.HYBRID -> analyzeHybrid(image, imageProxy)
        }
    }
    
    private fun analyzeBarcodeOnly(image: InputImage, imageProxy: ImageProxy) {
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty() && !resultFoundInFrame) {
                    val barcode = barcodes.first()
                    barcode.rawValue?.let { value ->
                        resultFoundInFrame = true
                        onScanResult(
                            ScanResult.BarcodeResult(
                                rawValue = value,
                                format = getBarcodeFormatName(barcode.format)
                            )
                        )
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener { exception ->
                onError(exception)
                imageProxy.close()
            }
    }
    
    private fun analyzeOcrOnly(image: InputImage, imageProxy: ImageProxy) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (!resultFoundInFrame) {
                    val recognizedTexts = extractRecognizedTexts(visionText)
                    val dsnCandidates = dsnValidator.findDsns(recognizedTexts)
                    
                    if (dsnCandidates.isNotEmpty()) {
                        val bestCandidate = dsnCandidates.first()
                        resultFoundInFrame = true
                        onScanResult(
                            ScanResult.OcrResult(
                                text = bestCandidate.dsn,
                                confidence = bestCandidate.confidence,
                                inferredComponentType = bestCandidate.componentType,
                                requiresManualVerification = bestCandidate.confidence < 0.9f
                            )
                        )
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener { exception ->
                onError(exception)
                imageProxy.close()
            }
    }
    
    private fun analyzeHybrid(image: InputImage, imageProxy: ImageProxy) {
        // First try barcode scanning (usually faster and more reliable)
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty() && !resultFoundInFrame) {
                    val barcode = barcodes.first()
                    barcode.rawValue?.let { value ->
                        resultFoundInFrame = true
                        onScanResult(
                            ScanResult.BarcodeResult(
                                rawValue = value,
                                format = getBarcodeFormatName(barcode.format)
                            )
                        )
                    }
                    imageProxy.close()
                } else {
                    // No barcode found, try OCR
                    analyzeWithOcrFallback(image, imageProxy)
                }
            }
            .addOnFailureListener {
                // Barcode scanning failed, try OCR
                analyzeWithOcrFallback(image, imageProxy)
            }
    }
    
    private fun analyzeWithOcrFallback(image: InputImage, imageProxy: ImageProxy) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                if (!resultFoundInFrame) {
                    val recognizedTexts = extractRecognizedTexts(visionText)
                    val dsnCandidates = dsnValidator.findDsns(recognizedTexts)
                    
                    if (dsnCandidates.isNotEmpty()) {
                        val bestCandidate = dsnCandidates.first()
                        resultFoundInFrame = true
                        onScanResult(
                            ScanResult.OcrResult(
                                text = bestCandidate.dsn,
                                confidence = bestCandidate.confidence,
                                inferredComponentType = bestCandidate.componentType,
                                requiresManualVerification = bestCandidate.confidence < 0.9f
                            )
                        )
                    } else if (recognizedTexts.isNotEmpty()) {
                        // No valid DSN found, but we have text - might need manual input
                        onScanResult(
                            ScanResult.ManualInputRequired(
                                reason = "No valid serial number detected. Please enter manually."
                            )
                        )
                    }
                }
                imageProxy.close()
            }
            .addOnFailureListener { exception ->
                onError(exception)
                imageProxy.close()
            }
    }
    
    private fun extractRecognizedTexts(visionText: com.google.mlkit.vision.text.Text): List<RecognizedText> {
        val recognizedTexts = mutableListOf<RecognizedText>()
        
        for (block in visionText.textBlocks) {
            for (line in block.lines) {
                val text = line.text
                val confidence = line.confidence ?: 0f
                val boundingBox = line.boundingBox
                
                recognizedTexts.add(
                    RecognizedText(
                        text = text,
                        confidence = confidence,
                        boundingBox = boundingBox
                    )
                )
            }
        }
        
        return recognizedTexts
    }
    
    private fun getBarcodeFormatName(format: Int): String {
        return when (format) {
            Barcode.FORMAT_QR_CODE -> "QR_CODE"
            Barcode.FORMAT_CODE_128 -> "CODE_128"
            Barcode.FORMAT_CODE_39 -> "CODE_39"
            Barcode.FORMAT_CODE_93 -> "CODE_93"
            Barcode.FORMAT_CODABAR -> "CODABAR"
            Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
            Barcode.FORMAT_EAN_13 -> "EAN_13"
            Barcode.FORMAT_EAN_8 -> "EAN_8"
            Barcode.FORMAT_ITF -> "ITF"
            Barcode.FORMAT_UPC_A -> "UPC_A"
            Barcode.FORMAT_UPC_E -> "UPC_E"
            Barcode.FORMAT_PDF417 -> "PDF417"
            Barcode.FORMAT_AZTEC -> "AZTEC"
            else -> "UNKNOWN"
        }
    }
    
    fun close() {
        barcodeScanner.close()
        textRecognizer.close()
        executor.shutdown()
    }
}
