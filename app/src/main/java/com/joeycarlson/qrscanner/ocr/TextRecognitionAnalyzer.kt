package com.joeycarlson.qrscanner.ocr

import android.content.Context
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.joeycarlson.qrscanner.ocr.DsnValidator.ComponentType
import java.util.concurrent.TimeUnit

/**
 * Analyzer for ML Kit Text Recognition
 * Processes camera frames to detect and extract text with sophisticated confidence tuning
 */
class TextRecognitionAnalyzer(
    private val onTextDetected: (TextRecognitionResult) -> Unit,
    private val onError: (Exception) -> Unit,
    private val context: Context? = null,
    private val componentType: ComponentType? = null,
    private val confidenceConfig: OcrConfidenceConfig = OcrConfidenceConfig()
) : ImageAnalysis.Analyzer {
    
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
    
    private val confidenceManager: OcrConfidenceManager? = context?.let {
        OcrConfidenceManager(it, confidenceConfig)
    }
    private val environmentalAnalyzer: EnvironmentalAnalyzer? = context?.let { 
        EnvironmentalAnalyzer(it)
    }
    
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
                        val mlKitConfidence = line.confidence
                        val boundingBox = line.boundingBox
                        
                        // Calculate sophisticated confidence score
                        val enhancedResult = confidenceManager?.calculateConfidence(
                            mlKitConfidence = mlKitConfidence,
                            recognizedText = text,
                            boundingBox = boundingBox,
                            componentType = componentType,
                            timestamp = currentTimestamp
                        )
                        
                        // Update environmental factor if available
                        environmentalAnalyzer?.getEnvironmentalScore()?.let { score ->
                            confidenceManager?.updateEnvironmentalFactor(score)
                        }
                        
                        val finalConfidence = enhancedResult?.confidence ?: mlKitConfidence ?: 0.8f
                        val requiresVerification = enhancedResult?.requiresManualVerification ?: false
                        
                        recognizedTexts.add(
                            RecognizedText(
                                text = text,
                                confidence = finalConfidence,
                                boundingBox = boundingBox,
                                requiresManualVerification = requiresVerification
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
        environmentalAnalyzer?.close()
    }
    
    /**
     * Update confidence configuration
     */
    fun updateConfidenceConfig(config: OcrConfidenceConfig) {
        confidenceManager?.updateConfig(config)
    }
    
    /**
     * Get confidence history for analysis
     */
    fun getConfidenceHistory(): List<Float> = confidenceManager?.getConfidenceHistory() ?: emptyList()
}

/**
 * Data class representing a single recognized text element
 */
data class RecognizedText(
    val text: String,
    val confidence: Float,
    val boundingBox: android.graphics.Rect?,
    val requiresManualVerification: Boolean = false
)

/**
 * Data class representing the complete result of text recognition
 */
data class TextRecognitionResult(
    val texts: List<RecognizedText>,
    val timestamp: Long
)
