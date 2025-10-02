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
        
        /**
         * Corrects common OCR mistakes in alphanumeric codes
         */
        @JvmStatic
        fun correctOcrMistakes(text: String): String {
            var corrected = text.trim().uppercase()
            
            // Common character confusions in OCR
            val charConfusions = mapOf(
                "O" to "0", // Letter O to zero
                "I" to "1", // Letter I to one
                "l" to "1", // Lowercase L to one
                "S" to "5", // Letter S to five
                "Z" to "2", // Letter Z to two
                "G" to "6", // Letter G to six (sometimes)
                "B" to "8", // Letter B to eight
                "Q" to "0", // Letter Q to zero
                "|" to "1", // Pipe to one
                "(" to "C", // Parenthesis to C
                ")" to "D", // Parenthesis to D
            )
            
            // For real-world patterns, apply specific corrections
            // Controller pattern: G0G46K... - the 0s should be zeros
            if (corrected.matches(Regex("^G[O0Q]G46K.*"))) {
                corrected = corrected.replace(Regex("^G[O0Q]G46K"), "G0G46K")
            }
            
            // Battery pattern: G0G4NU... - the 0 should be zero
            if (corrected.matches(Regex("^G[O0Q]G4NU.*"))) {
                corrected = corrected.replace(Regex("^G[O0Q]G4NU"), "G0G4NU")
            }
            
            // Glasses pattern: G0G348... - the 0 should be zero
            if (corrected.matches(Regex("^G[O0Q]G348.*"))) {
                corrected = corrected.replace(Regex("^G[O0Q]G348"), "G0G348")
            }
            
            // For patterns with known numeric positions, correct O to 0
            // Pattern: Letter-Zero-Letter at the beginning (common in serial numbers)
            corrected = corrected.replace(Regex("^([A-Z])[OQ]([A-Z])"), "$10$2")
            
            // For serial numbers that should be all numeric after a prefix
            // If we have a known prefix followed by what should be numbers
            if (corrected.matches(Regex("^G0G[A-Z0-9]{3}[O0Q-9ILSZGB]+$"))) {
                // Replace confusing characters with numbers in the numeric portion
                val prefix = corrected.substring(0, 6)
                var numericPart = corrected.substring(6)
                
                // Apply character corrections in numeric portion
                numericPart = numericPart
                    .replace("O", "0")
                    .replace("Q", "0")
                    .replace("I", "1")
                    .replace("l", "1")
                    .replace("S", "5")
                    .replace("Z", "2")
                
                corrected = prefix + numericPart
            }
            
            // Additional pattern-specific corrections
            // For patterns like H1B-POR2 (from the image), ensure consistent format
            if (corrected.matches(Regex("^H[I1l][B8]-POR[0-9]+$"))) {
                corrected = corrected
                    .replace(Regex("^H[I1l]"), "H1")
                    .replace("B8", "B")
            }
            
            return corrected
        }
        
        /**
         * Checks if two texts are similar (considering OCR variations)
         */
        @JvmStatic
        fun isSimilarText(text1: String, text2: String): Boolean {
            // If texts are exactly equal after normalization
            if (text1.trim().uppercase() == text2.trim().uppercase()) {
                return true
            }
            
            // Apply OCR corrections and compare
            val corrected1 = correctOcrMistakes(text1)
            val corrected2 = correctOcrMistakes(text2)
            
            if (corrected1 == corrected2) {
                return true
            }
            
            // Check if they differ by only one character (common OCR error)
            if (corrected1.length == corrected2.length && corrected1.length > 0) {
                var differences = 0
                for (i in corrected1.indices) {
                    if (corrected1[i] != corrected2[i]) {
                        differences++
                        if (differences > 1) break
                    }
                }
                if (differences == 1) {
                    return true
                }
            }
            
            // Check Levenshtein distance for small variations
            val maxLength = maxOf(corrected1.length, corrected2.length)
            if (maxLength > 0) {
                val distance = levenshteinDistance(corrected1, corrected2)
                val similarity = 1.0 - (distance.toDouble() / maxLength)
                return similarity >= 0.85 // 85% similarity threshold
            }
            
            return false
        }
        
        /**
         * Calculate Levenshtein distance between two strings
         */
        private fun levenshteinDistance(s1: String, s2: String): Int {
            val len1 = s1.length
            val len2 = s2.length
            
            val dp = Array(len1 + 1) { IntArray(len2 + 1) }
            
            for (i in 0..len1) {
                dp[i][0] = i
            }
            
            for (j in 0..len2) {
                dp[0][j] = j
            }
            
            for (i in 1..len1) {
                for (j in 1..len2) {
                    val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                    dp[i][j] = minOf(
                        dp[i - 1][j] + 1,      // deletion
                        dp[i][j - 1] + 1,      // insertion
                        dp[i - 1][j - 1] + cost // substitution
                    )
                }
            }
            
            return dp[len1][len2]
        }
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
        // First correct common OCR mistakes using the companion object function
        val corrected = correctOcrMistakes(text.trim().uppercase())
        
        return corrected
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
