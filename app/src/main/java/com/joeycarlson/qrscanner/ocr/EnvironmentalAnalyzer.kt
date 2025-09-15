package com.joeycarlson.qrscanner.ocr

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.camera.core.ImageProxy
import com.joeycarlson.qrscanner.util.LogManager
import kotlin.math.sqrt

/**
 * Analyzes environmental conditions that affect OCR accuracy
 * Provides environmental factor scores for confidence adjustment
 */
class EnvironmentalAnalyzer(private val context: Context) : SensorEventListener {
    
    private val logManager = LogManager.getInstance(context)
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    
    // Sensors
    private val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    // Environmental measurements
    private var currentLightLevel = 100f // Default moderate light
    private var deviceStability = 1.0f // 1.0 = stable, 0.0 = very unstable
    
    // Tracking for stability calculation
    private val accelerometerReadings = mutableListOf<AccelerometerReading>()
    private val lightReadings = mutableListOf<Float>()
    
    // Analysis parameters
    private val stabilityWindowMs = 500L
    private val lightWindowSize = 10
    
    init {
        startMonitoring()
    }
    
    /**
     * Start monitoring environmental sensors
     */
    fun startMonitoring() {
        lightSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
            logManager.log("EnvironmentalAnalyzer", "Light sensor monitoring started")
        }
        
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            logManager.log("EnvironmentalAnalyzer", "Accelerometer monitoring started")
        }
    }
    
    /**
     * Stop monitoring environmental sensors
     */
    fun stopMonitoring() {
        sensorManager.unregisterListener(this)
        logManager.log("EnvironmentalAnalyzer", "Environmental monitoring stopped")
    }
    
    /**
     * Analyze overall environmental conditions
     */
    fun analyzeEnvironment(): EnvironmentalFactors {
        val lightLevel = getLightLevel()
        val stability = getDeviceStability()
        
        return EnvironmentalFactors(
            lightLevel = lightLevel,
            isStable = stability
        )
    }
    
    /**
     * Get current light level
     */
    fun getLightLevel(): Float {
        return if (lightReadings.isNotEmpty()) {
            lightReadings.average().toFloat()
        } else {
            currentLightLevel
        }
    }
    
    /**
     * Get device stability
     */
    fun getDeviceStability(): Boolean {
        return deviceStability >= 0.8f
    }
    
    /**
     * Get overall environmental factor
     */
    fun getEnvironmentalFactor(): Float {
        return getEnvironmentalScore()
    }
    
    /**
     * Get environmental score as a single float value (0.0 to 1.0)
     */
    fun getEnvironmentalScore(): Float {
        val factors = analyzeEnvironment()
        
        // Calculate light score (optimal range: 100-1000 lux)
        val lightScore = when {
            factors.lightLevel < 50 -> 0.3f
            factors.lightLevel < 100 -> 0.6f
            factors.lightLevel in 100f..1000f -> 1.0f
            factors.lightLevel > 1000 -> 0.8f
            factors.lightLevel > 5000 -> 0.5f
            else -> 0.7f
        }
        
        // Calculate stability score
        val stabilityScore = if (factors.isStable) 1.0f else 0.5f
        
        // Combine scores (70% weight on light, 30% on stability)
        return (lightScore * 0.7f + stabilityScore * 0.3f).coerceIn(0f, 1f)
    }
    
    /**
     * Analyze image characteristics for additional environmental factors
     */
    fun analyzeImageEnvironment(imageProxy: ImageProxy): ImageEnvironmentFactors {
        // Basic image quality analysis
        val brightness = estimateImageBrightness(imageProxy)
        val contrast = estimateImageContrast(imageProxy)
        val sharpness = estimateImageSharpness(imageProxy)
        
        return ImageEnvironmentFactors(
            brightness = brightness,
            contrast = contrast,
            sharpness = sharpness,
            overallQuality = (brightness * 0.3f + contrast * 0.4f + sharpness * 0.3f)
        )
    }
    
    /**
     * Calculate light factor based on current lighting conditions
     */
    private fun calculateLightFactor(): Float {
        // Optimal light range for OCR is 100-1000 lux
        return when {
            currentLightLevel < 10 -> 0.3f      // Very dark
            currentLightLevel < 50 -> 0.5f      // Dark
            currentLightLevel < 100 -> 0.7f     // Low light
            currentLightLevel < 1000 -> 1.0f    // Optimal
            currentLightLevel < 5000 -> 0.9f    // Bright
            currentLightLevel < 10000 -> 0.8f   // Very bright
            else -> 0.7f                        // Too bright (glare risk)
        }
    }
    
    /**
     * Estimate image brightness (simplified)
     */
    private fun estimateImageBrightness(imageProxy: ImageProxy): Float {
        // In a full implementation, this would analyze pixel values
        // For now, use light sensor as proxy
        return calculateLightFactor()
    }
    
    /**
     * Estimate image contrast (simplified)
     */
    private fun estimateImageContrast(imageProxy: ImageProxy): Float {
        // In a full implementation, this would calculate histogram spread
        // For now, return a reasonable default based on light conditions
        return when {
            currentLightLevel < 50 -> 0.6f     // Low contrast in dark
            currentLightLevel < 100 -> 0.8f
            currentLightLevel < 1000 -> 0.9f   // Good contrast
            else -> 0.85f                      // Slightly reduced in bright light
        }
    }
    
    /**
     * Estimate image sharpness based on device stability
     */
    private fun estimateImageSharpness(imageProxy: ImageProxy): Float {
        // Motion blur is primarily caused by device movement
        return deviceStability
    }
    
    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            Sensor.TYPE_LIGHT -> {
                currentLightLevel = event.values[0]
                updateLightHistory(currentLightLevel)
            }
            Sensor.TYPE_ACCELEROMETER -> {
                val x = event.values[0]
                val y = event.values[1]
                val z = event.values[2]
                updateAccelerometerHistory(x, y, z)
                calculateDeviceStability()
            }
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        // Not needed for our use case
    }
    
    /**
     * Update light reading history
     */
    private fun updateLightHistory(lightLevel: Float) {
        lightReadings.add(lightLevel)
        if (lightReadings.size > lightWindowSize) {
            lightReadings.removeAt(0)
        }
    }
    
    /**
     * Update accelerometer reading history
     */
    private fun updateAccelerometerHistory(x: Float, y: Float, z: Float) {
        val timestamp = System.currentTimeMillis()
        accelerometerReadings.add(AccelerometerReading(x, y, z, timestamp))
        
        // Remove old readings
        val cutoffTime = timestamp - stabilityWindowMs
        accelerometerReadings.removeAll { it.timestamp < cutoffTime }
    }
    
    /**
     * Calculate device stability based on accelerometer data
     */
    private fun calculateDeviceStability() {
        if (accelerometerReadings.size < 2) {
            deviceStability = 1.0f
            return
        }
        
        // Calculate variance in acceleration
        var totalVariance = 0f
        for (i in 1 until accelerometerReadings.size) {
            val prev = accelerometerReadings[i - 1]
            val curr = accelerometerReadings[i]
            
            val dx = curr.x - prev.x
            val dy = curr.y - prev.y
            val dz = curr.z - prev.z
            
            val variance = sqrt(dx * dx + dy * dy + dz * dz)
            totalVariance += variance
        }
        
        val avgVariance = totalVariance / (accelerometerReadings.size - 1)
        
        // Convert variance to stability score
        deviceStability = when {
            avgVariance < 0.5f -> 1.0f    // Very stable
            avgVariance < 1.0f -> 0.9f    // Stable
            avgVariance < 2.0f -> 0.8f    // Slight movement
            avgVariance < 3.0f -> 0.6f    // Moderate movement
            avgVariance < 5.0f -> 0.4f    // High movement
            else -> 0.2f                  // Very unstable
        }
    }
    
    /**
     * Get detailed environmental conditions
     */
    fun getEnvironmentalConditions(): EnvironmentalConditions {
        return EnvironmentalConditions(
            lightLevel = currentLightLevel,
            lightFactor = calculateLightFactor(),
            deviceStability = deviceStability,
            overallFactor = getEnvironmentalFactor(),
            lightCondition = describeLightCondition(),
            stabilityCondition = describeStabilityCondition()
        )
    }
    
    /**
     * Describe current light conditions
     */
    private fun describeLightCondition(): String {
        return when {
            currentLightLevel < 10 -> "Very Dark"
            currentLightLevel < 50 -> "Dark"
            currentLightLevel < 100 -> "Low Light"
            currentLightLevel < 1000 -> "Good Light"
            currentLightLevel < 5000 -> "Bright"
            currentLightLevel < 10000 -> "Very Bright"
            else -> "Extreme Brightness"
        }
    }
    
    /**
     * Describe current stability conditions
     */
    private fun describeStabilityCondition(): String {
        return when {
            deviceStability >= 0.9f -> "Very Stable"
            deviceStability >= 0.8f -> "Stable"
            deviceStability >= 0.6f -> "Slight Movement"
            deviceStability >= 0.4f -> "Moderate Movement"
            else -> "Unstable"
        }
    }
    
    /**
     * Reset all measurements
     */
    fun reset() {
        accelerometerReadings.clear()
        lightReadings.clear()
        currentLightLevel = 100f
        deviceStability = 1.0f
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMonitoring()
        reset()
    }
    
    /**
     * Data class for accelerometer readings
     */
    private data class AccelerometerReading(
        val x: Float,
        val y: Float,
        val z: Float,
        val timestamp: Long
    )
    
    /**
     * Environmental conditions summary
     */
    data class EnvironmentalConditions(
        val lightLevel: Float,
        val lightFactor: Float,
        val deviceStability: Float,
        val overallFactor: Float,
        val lightCondition: String,
        val stabilityCondition: String
    )
    
    /**
     * Image-based environmental factors
     */
    data class ImageEnvironmentFactors(
        val brightness: Float,
        val contrast: Float,
        val sharpness: Float,
        val overallQuality: Float
    )
    
    /**
     * Environmental factors for confidence calculation
     */
    data class EnvironmentalFactors(
        val lightLevel: Float,
        val isStable: Boolean
    )
}
