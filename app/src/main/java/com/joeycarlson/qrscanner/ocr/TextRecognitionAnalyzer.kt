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
    private val analysisInterval = 300L // Analyze every 300ms for better responsiveness
    
    // Image preprocessor for enhancing OCR performance
    private val imagePreprocessor = ImagePreprocessor()
    
    // Multi-frame averaging for stability
    private val frameResults = mutableListOf<com.joeycarlson.qrscanner.ocr.FrameResult>()
    private val maxFramesToAverage = 5
    private val frameResultTimeout = 1500L // 1.5 seconds
    
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
        
        // Clean up old frame results
        frameResults.removeAll { currentTimestamp - it.timestamp > frameResultTimeout }
        
        // Apply image preprocessing to enhance OCR performance
        val preprocessedImage = imagePreprocessor.preprocessImage(mediaImage)
        val image = InputImage.fromBitmap(preprocessedImage, imageProxy.imageInfo.rotationDegrees)
        
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val recognizedTexts = mutableListOf<RecognizedText>()
                
                for (block in visionText.textBlocks) {
                    for (line in block.lines) {
                        val text = line.text
                        val mlKitConfidence = line.confidence
                        val boundingBox = line.boundingBox
                        
                        // Add to frame results for averaging
                        frameResults.add(
                            FrameResult(
                                text = text,
                                confidence = mlKitConfidence ?: 0.8f,
                                timestamp = currentTimestamp,
                                boundingBox = boundingBox
                            )
                        )
                        
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
                
                // Apply multi-frame averaging for stability
                val averagedTexts = applyMultiFrameAveraging(recognizedTexts, currentTimestamp)
                
                if (averagedTexts.isNotEmpty()) {
                    onTextDetected(
                        TextRecognitionResult(
                            texts = averagedTexts,
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
        environmentalAnalyzer?.cleanup()
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
    
    /**
     * Apply multi-frame averaging to improve stability and reduce false positives
     */
    private fun applyMultiFrameAveraging(
        currentFrameTexts: List<RecognizedText>,
        currentTimestamp: Long
    ): List<RecognizedText> {
        if (frameResults.size < 2) {
            // Not enough frames for averaging
            return currentFrameTexts
        }
        
        val recentFrames = frameResults
            .filter { currentTimestamp - it.timestamp <= frameResultTimeout }
            .takeLast(maxFramesToAverage)
        
        // Group by similar text (considering OCR variations)
        val textGroups = mutableMapOf<String, MutableList<FrameResult>>()
        recentFrames.forEach { frame ->
            val normalizedText = DsnValidator.correctOcrMistakes(frame.text)
            var foundGroup = false
            
            for ((key, group) in textGroups) {
                if (DsnValidator.isSimilarText(normalizedText, key)) {
                    group.add(frame)
                    foundGroup = true
                    break
                }
            }
            
            if (!foundGroup) {
                textGroups[normalizedText] = mutableListOf(frame)
            }
        }
        
        // Calculate averaged results
        val averagedResults = mutableListOf<RecognizedText>()
        
        textGroups.forEach { (normalizedText, frames) ->
            if (frames.size >= 2) { // Only average if text appears in multiple frames
                val avgConfidence = frames.map { it.confidence }.average().toFloat()
                val mostRecentFrame = frames.maxByOrNull { it.timestamp }
                
                // Find corresponding current frame text
                val currentText = currentFrameTexts.find { 
                    DsnValidator.isSimilarText(
                        DsnValidator.correctOcrMistakes(it.text), 
                        normalizedText
                    )
                }
                
                if (currentText != null) {
                    averagedResults.add(
                        currentText.copy(
                            confidence = (currentText.confidence + avgConfidence) / 2f,
                            requiresManualVerification = currentText.requiresManualVerification 
                                && avgConfidence < confidenceConfig.manualVerificationThreshold
                        )
                    )
                }
            }
        }
        
        // Include high-confidence single-frame results
        currentFrameTexts.forEach { text ->
            if (text.confidence >= confidenceConfig.highConfidenceThreshold &&
                averagedResults.none { 
                    DsnValidator.isSimilarText(
                        DsnValidator.correctOcrMistakes(it.text),
                        DsnValidator.correctOcrMistakes(text.text)
                    )
                }
            ) {
                averagedResults.add(text)
            }
        }
        
        return averagedResults
    }
}
