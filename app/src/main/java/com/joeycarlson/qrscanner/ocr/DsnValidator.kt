package com.joeycarlson.qrscanner.ocr

import android.graphics.Rect

/**
 * Validates and parses Device Serial Numbers (DSN) from recognized text
 * Supports common DSN patterns and component type inference with confidence scoring
 */
class DsnValidator {
    
    /**
     * Enum representing the different component types in a kit bundle
     */
    enum class ComponentType {
        GLASSES,
        CONTROLLER,
        BATTERY_01,
        BATTERY_02,
        BATTERY_03,
        PADS,
        UNUSED_01,
        UNUSED_02
    }
    
    /**
     * Confidence levels for component type detection
     */
    enum class ConfidenceLevel {
        HIGH,      // >90% - Auto-assign
        MEDIUM,    // 70-90% - Confirm with user
        LOW        // <70% - Manual selection required
    }
    
    companion object {
        // Real-world DSN patterns with high priority
        private val REAL_WORLD_DSN_PATTERNS = listOf(
            // Controllers: G0G46K025224xxxx
            Regex("^G0G46K\\d{9,}$"),
            
            // Batteries: G0G4NU015166xxxx  
            Regex("^G0G4NU\\d{9,}$"),
            
            // Glasses: G0G348025246xxxx and G0G348025263xxxx
            Regex("^G0G348\\d{9,}$")
        )
        
        // Common DSN patterns (fallback patterns)
        private val COMMON_DSN_PATTERNS = listOf(
            // Pattern 1: Component prefix followed by digits (e.g., GL-123456, CTRL-789012)
            Regex("^(GL|CTRL|BAT|PAD|UN)-\\d{6,}$"),
            
            // Pattern 2: Alphanumeric with specific length (e.g., ABC123DEF456)
            Regex("^[A-Z]{3}\\d{3}[A-Z]{3}\\d{3}$"),
            
            // Pattern 3: Serial number with dashes (e.g., 12-34-56-78)
            Regex("^\\d{2}-\\d{2}-\\d{2}-\\d{2}$"),
            
            // Pattern 4: Alphanumeric with underscores (e.g., SN_ABC_123456)
            Regex("^SN_[A-Z]+_\\d{6,}$"),
            
            // Pattern 5: Simple alphanumeric (minimum 8 characters)
            Regex("^[A-Z0-9]{8,}$")
        )
        
        // High confidence patterns based on real-world DSNs
        private val HIGH_CONFIDENCE_PATTERNS = mapOf(
            ComponentType.CONTROLLER to listOf(
                Regex("^G0G46K\\d{9,}$")  // Real-world controller pattern
            ),
            ComponentType.BATTERY_01 to listOf(
                Regex("^G0G4NU\\d{9,}$")  // Real-world battery pattern
            ),
            ComponentType.GLASSES to listOf(
                Regex("^G0G348\\d{9,}$")  // Real-world glasses pattern
            )
        )
        
        // Medium confidence patterns
        private val MEDIUM_CONFIDENCE_PATTERNS = mapOf(
            ComponentType.GLASSES to listOf(
                Regex("^GL[-_]?.*"),
                Regex(".*GLASS.*"),
                Regex(".*LENS.*")
            ),
            ComponentType.CONTROLLER to listOf(
                Regex("^CTRL[-_]?.*"),
                Regex(".*CONTROL.*"),
                Regex(".*REMOTE.*")
            ),
            ComponentType.BATTERY_01 to listOf(
                Regex("^BAT[-_]?.*"),
                Regex(".*BATTERY.*")
            ),
            ComponentType.PADS to listOf(
                Regex("^PAD[-_]?.*"),
                Regex(".*PADS?.*")
            )
        )
        
        // Low confidence patterns
        private val LOW_CONFIDENCE_PATTERNS = mapOf(
            ComponentType.UNUSED_01 to listOf(
                Regex("^UN[-_]?0?1.*"),
                Regex(".*UNUSED[-_]?0?1.*")
            ),
            ComponentType.UNUSED_02 to listOf(
                Regex("^UN[-_]?0?2.*"),
                Regex(".*UNUSED[-_]?0?2.*")
            )
        )
        
        private const val MIN_CONFIDENCE_THRESHOLD = 0.8f
        private const val HIGH_CONFIDENCE_THRESHOLD = 0.9f
        private const val MEDIUM_CONFIDENCE_THRESHOLD = 0.7f
    }
    
    /**
     * Validates if the recognized text matches DSN patterns
     */
    fun isValidDsn(text: String): Boolean {
        val normalizedText = text.trim().uppercase()
        
        // Check real-world patterns first (higher priority)
        if (REAL_WORLD_DSN_PATTERNS.any { pattern -> pattern.matches(normalizedText) }) {
            return true
        }
        
        // Fall back to common patterns
        return COMMON_DSN_PATTERNS.any { pattern -> pattern.matches(normalizedText) }
    }
    
    /**
     * Validates DSN with confidence threshold
     */
    fun isValidDsnWithConfidence(text: String, confidence: Float): Boolean {
        return confidence >= MIN_CONFIDENCE_THRESHOLD && isValidDsn(text)
    }
    
    /**
     * Attempts to infer component type from DSN with confidence level
     */
    fun inferComponentTypeWithConfidence(dsn: String): Pair<ComponentType?, ConfidenceLevel> {
        val normalizedDsn = dsn.trim().uppercase()
        
        // Check high confidence patterns first
        for ((componentType, patterns) in HIGH_CONFIDENCE_PATTERNS) {
            if (patterns.any { pattern -> pattern.matches(normalizedDsn) }) {
                return Pair(componentType, ConfidenceLevel.HIGH)
            }
        }
        
        // Check medium confidence patterns
        for ((componentType, patterns) in MEDIUM_CONFIDENCE_PATTERNS) {
            if (patterns.any { pattern -> pattern.matches(normalizedDsn) }) {
                return Pair(componentType, ConfidenceLevel.MEDIUM)
            }
        }
        
        // Check low confidence patterns
        for ((componentType, patterns) in LOW_CONFIDENCE_PATTERNS) {
            if (patterns.any { pattern -> pattern.matches(normalizedDsn) }) {
                return Pair(componentType, ConfidenceLevel.LOW)
            }
        }
        
        // No match found
        return Pair(null, ConfidenceLevel.LOW)
    }
    
    /**
     * Attempts to infer component type from DSN (backward compatibility)
     */
    fun inferComponentType(dsn: String): ComponentType? {
        return inferComponentTypeWithConfidence(dsn).first
    }
    
    /**
     * Gets the detection confidence level for a DSN
     */
    fun getDetectionConfidence(dsn: String, ocrConfidence: Float): ConfidenceLevel {
        val (componentType, patternConfidence) = inferComponentTypeWithConfidence(dsn)
        
        return when {
            componentType == null -> ConfidenceLevel.LOW
            ocrConfidence >= HIGH_CONFIDENCE_THRESHOLD && patternConfidence == ConfidenceLevel.HIGH -> ConfidenceLevel.HIGH
            ocrConfidence >= MEDIUM_CONFIDENCE_THRESHOLD && patternConfidence != ConfidenceLevel.LOW -> ConfidenceLevel.MEDIUM
            else -> ConfidenceLevel.LOW
        }
    }
    
    /**
     * For batteries, attempts to detect any battery type (not specific number)
     */
    fun isBatteryDsn(dsn: String): Boolean {
        val normalizedDsn = dsn.trim().uppercase()
        
        // Check real-world battery pattern
        if (Regex("^G0G4NU\\d{9,}$").matches(normalizedDsn)) {
            return true
        }
        
        // Check generic battery patterns
        return Regex("^BAT[-_]?.*").matches(normalizedDsn) || 
               normalizedDsn.contains("BATTERY")
    }
    
    /**
     * Normalizes DSN for consistent storage
     */
    fun normalizeDsn(text: String): String {
        return text.trim()
            .uppercase()
            .replace(Regex("\\s+"), "-") // Replace spaces with dashes
            .replace(Regex("[^A-Z0-9\\-_/.]"), "") // Remove invalid characters
    }
    
    /**
     * Finds all potential DSNs in a list of recognized texts
     */
    fun findDsns(recognizedTexts: List<RecognizedText>): List<DsnCandidate> {
        return recognizedTexts
            .filter { isValidDsnWithConfidence(it.text, it.confidence) }
            .map { recognizedText ->
                DsnCandidate(
                    dsn = normalizeDsn(recognizedText.text),
                    confidence = recognizedText.confidence,
                    componentType = inferComponentType(recognizedText.text),
                    originalText = recognizedText.text,
                    boundingBox = recognizedText.boundingBox
                )
            }
            .sortedByDescending { it.confidence }
    }
    
    /**
     * Validates if a manually entered text could be a valid DSN
     */
    fun validateManualEntry(text: String): ValidationResult {
        val normalized = normalizeDsn(text)
        
        return when {
            normalized.length < 6 -> ValidationResult(
                isValid = false,
                error = "DSN must be at least 6 characters"
            )
            normalized.isEmpty() -> ValidationResult(
                isValid = false,
                error = "DSN cannot be empty"
            )
            !Regex("^[A-Z0-9\\-_/.]+$").matches(normalized) -> ValidationResult(
                isValid = false,
                error = "DSN contains invalid characters"
            )
            else -> ValidationResult(
                isValid = true,
                normalizedDsn = normalized,
                inferredType = inferComponentType(normalized)
            )
        }
    }
}

/**
 * Represents a potential DSN candidate from OCR
 */
data class DsnCandidate(
    val dsn: String,
    val confidence: Float,
    val componentType: DsnValidator.ComponentType?,
    val originalText: String,
    val boundingBox: android.graphics.Rect?
)

/**
 * Result of manual DSN validation
 */
data class ValidationResult(
    val isValid: Boolean,
    val normalizedDsn: String? = null,
    val inferredType: DsnValidator.ComponentType? = null,
    val error: String? = null
)
