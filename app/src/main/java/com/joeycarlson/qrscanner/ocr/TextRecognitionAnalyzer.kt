package com.joeycarlson.qrscanner.ocr

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.concurrent.TimeUnit

/**
 * Analyzer for ML Kit Text Recognition
 * Processes camera frames to detect and extract text
 */
class TextRecognitionAnalyzer(
    private val onTextDetected: (TextRecognitionResult) -> Unit,
    private val onError: (Exception) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
    
    private var lastAnalyzedTimestamp = 0L
    private val analysisInterval = 500L // Analyze every 500ms for performance
    
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        if (currentTimestamp - lastAnalyzedTimestamp < analysisInterval) {
            imageProxy.close()
            return
        }
        
        lastAnalyzedTimestamp = currentTimestamp
        
        @androidx.camera.core.ExperimentalGetImage
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedTexts = mutableListOf<RecognizedText>()
                
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val text = line.text
                        val confidence = line.confidence ?: 0.8f
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
                
                if (recognizedTexts.isNotEmpty()) {
                    onTextDetected(
                        TextRecognitionResult(
                            texts = recognizedTexts,
                            timestamp = currentTimestamp
                        )
                    )
                }
                imageProxy.close()
            }
            .addOnFailureListener { exception ->
                onError(exception)
                imageProxy.close()
            }
    }
    
    fun close() {
        textRecognizer.close()
    }
}

/**
 * Data class representing a single recognized text element
 */
data class RecognizedText(
    val text: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect?
)

/**
 * Data class representing the complete result of text recognition
 */
data class TextRecognitionResult(
    val texts: List<RecognizedText>,
    val timestamp: Long
)
