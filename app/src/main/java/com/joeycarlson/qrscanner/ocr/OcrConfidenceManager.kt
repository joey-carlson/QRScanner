package com.joeycarlson.qrscanner.ocr

import android.content.Context
import android.graphics.Rect
import androidx.preference.PreferenceManager
import com.joeycarlson.qrscanner.util.LogManager

/**
 * Central manager for sophisticated OCR confidence calculations
 * Implements multi-factor scoring and adaptive thresholds
 */
class OcrConfidenceManager(
    private val context: Context,
    private var config: OcrConfidenceConfig = OcrConfidenceConfig()
) {
    
    private val logManager = LogManager.getInstance(context)
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)
    
    // Tracking for stability analysis
    private val boundingBoxHistory = mutableListOf<BoundingBoxFrame>()
    private val confidenceHistory = mutableListOf<Float>()
    
    // Environmental factors
    private var currentEnvironmentalFactor = 1.0f
    
    /**
     * Calculate enhanced confidence score using multi-factor analysis
     */
    fun calculateConfidence(
        mlKitConfidence: Float?,
        recognizedText: String,
        boundingBox: Rect?,
        componentType: DsnValidator.ComponentType?,
        timestamp: Long
    ): EnhancedConfidenceResult {
        
        // Get base ML Kit confidence or use intelligent fallback
        val baseConfidence = mlKitConfidence ?: calculateFallbackConfidence(recognizedText, componentType)
        
        // Calculate individual factors
        val patternMatchScore = calculatePatternMatchScore(recognizedText, componentType)
        val stabilityScore = calculateStabilityScore(boundingBox, timestamp)
        val environmentalScore = calculateEnvironmentalScore()
        
        // Apply multi-factor scoring with configured weights
        val weightedScore = (
            baseConfidence * config.mlKitConfidenceWeight +
            patternMatchScore * config.patternMatchWeight +
            stabilityScore * config.stabilityWeight +
            environmentalScore * config.environmentalWeight
        )
        
        // Apply component-specific adjustments
        val componentAdjustedScore = applyComponentSpecificAdjustments(
            weightedScore,
            componentType
        )
        
        // Track confidence history
        if (config.enableConfidenceHistory) {
            addToConfidenceHistory(componentAdjustedScore)
        }
        
        // Determine if manual verification is needed
        val threshold = getThresholdForComponent(componentType)
        val requiresManualVerification = componentAdjustedScore < threshold.manualVerificationThreshold
        
        logManager.log(
            "OcrConfidenceManager",
            "Confidence calculation - Base: ${baseConfidence}, Pattern: $patternMatchScore, " +
            "Stability: $stabilityScore, Environmental: $environmentalScore, " +
            "Final: $componentAdjustedScore, Component: $componentType"
        )
        
        return EnhancedConfidenceResult(
            confidence = componentAdjustedScore,
            requiresManualVerification = requiresManualVerification,
            factors = ConfidenceFactors(
                mlKitConfidence = baseConfidence,
                patternMatchScore = patternMatchScore,
                stabilityScore = stabilityScore,
                environmentalScore = environmentalScore
            ),
            componentType = componentType,
            threshold = threshold
        )
    }
    
    /**
     * Calculate intelligent fallback confidence when ML Kit doesn't provide one
     */
    private fun calculateFallbackConfidence(
        text: String,
        componentType: DsnValidator.ComponentType?
    ): Float {
        // Start with a base confidence based on text characteristics
        var confidence = when {
            text.length < config.minimumTextLength -> 0.5f
            text.contains(Regex("[^A-Za-z0-9]")) -> 0.7f // Contains special characters
            else -> 0.8f
        }
        
        // Adjust based on component type expectations
        componentType?.let {
            val componentConfig = config.componentThresholds[it]
            if (componentConfig != null) {
                // If it's a critical component (like battery), be more conservative
                if (componentConfig.patternStrictness == OcrConfidenceConfig.PatternStrictness.STRICT) {
                    confidence *= 0.9f
                }
            }
        }
        
        return confidence.coerceIn(0f, 1f)
    }
    
    /**
     * Calculate pattern matching score based on DSN format compliance
     */
    private fun calculatePatternMatchScore(
        text: String,
        componentType: DsnValidator.ComponentType?
    ): Float {
        val dsnValidator = DsnValidator()
        
        // Check if text matches DSN pattern
        val matchesPattern = dsnValidator.isValidDsn(text)
        if (!matchesPattern) return 0.3f
        
        // Get pattern strictness for component
        val strictness = componentType?.let { type ->
            config.componentThresholds[type]?.patternStrictness
        } ?: OcrConfidenceConfig.PatternStrictness.MEDIUM
        
        return when (strictness) {
            OcrConfidenceConfig.PatternStrictness.LOOSE -> {
                // Basic pattern match is enough
                if (matchesPattern) 0.9f else 0.3f
            }
            OcrConfidenceConfig.PatternStrictness.MEDIUM -> {
                // Check additional constraints
                val hasCorrectLength = text.length in 10..12
                val hasExpectedPrefix = text.matches(Regex("^[0-9]{5,}.*"))
                
                when {
                    matchesPattern && hasCorrectLength && hasExpectedPrefix -> 0.95f
                    matchesPattern && hasCorrectLength -> 0.85f
                    matchesPattern -> 0.75f
                    else -> 0.3f
                }
            }
            OcrConfidenceConfig.PatternStrictness.STRICT -> {
                // Strict validation including checksum if applicable
                val componentMatch = componentType?.let { 
                    dsnValidator.inferComponentType(text) == it 
                } ?: false
                
                when {
                    matchesPattern && componentMatch -> 1.0f
                    matchesPattern -> 0.7f
                    else -> 0.2f
                }
            }
        }
    }
    
    /**
     * Calculate stability score based on bounding box consistency
     */
    private fun calculateStabilityScore(
        boundingBox: Rect?,
        timestamp: Long
    ): Float {
        if (boundingBox == null || !config.enableEnvironmentalAdaptation) {
            return 0.8f // Default stability score
        }
        
        // Add to history
        boundingBoxHistory.add(BoundingBoxFrame(boundingBox, timestamp))
        
        // Keep only recent frames
        val cutoffTime = timestamp - 1000 // Last 1 second
        boundingBoxHistory.removeAll { it.timestamp < cutoffTime }
        
        if (boundingBoxHistory.size < config.boundingBoxStabilityFrames) {
            return 0.7f // Not enough history yet
        }
        
        // Calculate stability based on bounding box movement
        val recentBoxes = boundingBoxHistory.takeLast(config.boundingBoxStabilityFrames)
        val avgCenterX = recentBoxes.map { it.boundingBox.centerX() }.average().toFloat()
        val avgCenterY = recentBoxes.map { it.boundingBox.centerY() }.average().toFloat()
        
        var totalDeviation = 0f
        recentBoxes.forEach { frame ->
            val deviationX = Math.abs(frame.boundingBox.centerX() - avgCenterX)
            val deviationY = Math.abs(frame.boundingBox.centerY() - avgCenterY)
            totalDeviation += deviationX + deviationY
        }
        
        // Convert deviation to stability score (less movement = higher stability)
        val avgDeviation = totalDeviation / recentBoxes.size
        return when {
            avgDeviation < 10 -> 1.0f   // Very stable
            avgDeviation < 25 -> 0.9f   // Stable
            avgDeviation < 50 -> 0.8f   // Moderate
            avgDeviation < 100 -> 0.6f  // Unstable
            else -> 0.4f                // Very unstable
        }
    }
    
    /**
     * Calculate environmental score (placeholder for future light sensor integration)
     */
    private fun calculateEnvironmentalScore(): Float {
        // This would integrate with device sensors in a full implementation
        // For now, return the current environmental factor which can be set externally
        return currentEnvironmentalFactor
    }
    
    /**
     * Apply component-specific confidence adjustments
     */
    private fun applyComponentSpecificAdjustments(
        baseScore: Float,
        componentType: DsnValidator.ComponentType?
    ): Float {
        if (componentType == null) return baseScore
        
        val componentConfig = config.componentThresholds[componentType] ?: return baseScore
        
        // Apply sensitivity mode adjustments
        val sensitivityMultiplier = when (config.sensitivityMode) {
            OcrConfidenceConfig.SensitivityMode.CONSERVATIVE -> 0.95f
            OcrConfidenceConfig.SensitivityMode.BALANCED -> 1.0f
            OcrConfidenceConfig.SensitivityMode.AGGRESSIVE -> 1.05f
        }
        
        return (baseScore * sensitivityMultiplier).coerceIn(0f, 1f)
    }
    
    /**
     * Get threshold configuration for a specific component type
     */
    private fun getThresholdForComponent(
        componentType: DsnValidator.ComponentType?
    ): OcrConfidenceConfig.ComponentConfidenceConfig {
        return componentType?.let { type ->
            config.componentThresholds[type]
        } ?: OcrConfidenceConfig.ComponentConfidenceConfig(
            baseThreshold = config.baseConfidenceThreshold,
            manualVerificationThreshold = config.manualVerificationThreshold
        )
    }
    
    /**
     * Add confidence value to history for tracking
     */
    private fun addToConfidenceHistory(confidence: Float) {
        confidenceHistory.add(confidence)
        if (confidenceHistory.size > config.confidenceHistorySize) {
            confidenceHistory.removeAt(0)
        }
    }
    
    /**
     * Update environmental factor (can be called based on sensor data)
     */
    fun updateEnvironmentalFactor(factor: Float) {
        currentEnvironmentalFactor = factor.coerceIn(0f, 1f)
    }
    
    /**
     * Update configuration (e.g., from settings)
     */
    fun updateConfig(newConfig: OcrConfidenceConfig) {
        config = newConfig
        logManager.log(
            "OcrConfidenceManager",
            "Configuration updated - Mode: ${newConfig.sensitivityMode}"
        )
    }
    
    /**
     * Load configuration from SharedPreferences
     */
    fun loadConfigFromPreferences() {
        val modeName = prefs.getString("ocr_sensitivity_mode", "BALANCED") ?: "BALANCED"
        val mode = try {
            OcrConfidenceConfig.SensitivityMode.valueOf(modeName)
        } catch (e: IllegalArgumentException) {
            OcrConfidenceConfig.SensitivityMode.BALANCED
        }
        
        config = OcrConfidenceConfig.fromSensitivityMode(mode).copy(
            enableRealTimeVisualization = prefs.getBoolean("ocr_show_confidence", false),
            enableConfidenceHistory = prefs.getBoolean("ocr_enable_history", true)
        )
    }
    
    /**
     * Get confidence history for analysis
     */
    fun getConfidenceHistory(): List<Float> = confidenceHistory.toList()
    
    /**
     * Get average confidence from history
     */
    fun getAverageConfidence(): Float {
        return if (confidenceHistory.isEmpty()) {
            config.baseConfidenceThreshold
        } else {
            confidenceHistory.average().toFloat()
        }
    }
    
    /**
     * Clear all tracking data
     */
    fun reset() {
        boundingBoxHistory.clear()
        confidenceHistory.clear()
        currentEnvironmentalFactor = 1.0f
    }
    
    /**
     * Data class for tracking bounding box frames
     */
    private data class BoundingBoxFrame(
        val boundingBox: Rect,
        val timestamp: Long
    )
    
    /**
     * Result of enhanced confidence calculation
     */
    data class EnhancedConfidenceResult(
        val confidence: Float,
        val requiresManualVerification: Boolean,
        val factors: ConfidenceFactors,
        val componentType: DsnValidator.ComponentType?,
        val threshold: OcrConfidenceConfig.ComponentConfidenceConfig
    )
    
    /**
     * Individual confidence factors for debugging and visualization
     */
    data class ConfidenceFactors(
        val mlKitConfidence: Float,
        val patternMatchScore: Float,
        val stabilityScore: Float,
        val environmentalScore: Float
    )
}
