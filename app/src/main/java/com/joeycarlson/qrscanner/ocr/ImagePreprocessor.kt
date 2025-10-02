package com.joeycarlson.qrscanner.ocr

import android.graphics.*
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import kotlin.math.max
import kotlin.math.min

/**
 * Image preprocessing pipeline for enhancing OCR performance
 * Specifically optimized for light gray text on white backgrounds
 */
class ImagePreprocessor {
    
    companion object {
        // Preprocessing parameters tuned for DSN codes
        private const val CONTRAST_FACTOR = 2.5f
        private const val BRIGHTNESS_ADJUSTMENT = -20
        private const val GAMMA_CORRECTION = 0.8f
        private const val SHARPENING_FACTOR = 1.5f
        
        // Region of interest parameters
        private const val ROI_PADDING = 50 // pixels
        private const val MIN_ROI_SIZE = 200 // minimum dimension
    }
    
    /**
     * Main preprocessing pipeline for ImageProxy
     */
    fun preprocessImage(imageProxy: ImageProxy): Bitmap? {
        @androidx.camera.core.ExperimentalGetImage
        val mediaImage = imageProxy.image ?: return null
        
        // Convert to bitmap for processing
        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        val bitmap = imageProxyToBitmap(imageProxy) ?: return null
        
        // Apply preprocessing pipeline
        return bitmap
            .let { adjustContrastAndBrightness(it) }
            .let { applyGammaCorrection(it) }
            .let { sharpenImage(it) }
            .let { reduceNoise(it) }
    }
    
    /**
     * Main preprocessing pipeline for Image
     */
    fun preprocessImage(image: android.media.Image): Bitmap {
        // Convert Image to Bitmap
        val bitmap = imageToBitmap(image) ?: throw IllegalArgumentException("Failed to convert Image to Bitmap")
        
        // Apply preprocessing pipeline
        return adjustContrastAndBrightness(bitmap)
            .let { applyGammaCorrection(it) }
            .let { sharpenImage(it) }
            .let { reduceNoise(it) }
    }
    
    /**
     * Preprocess a specific region of interest
     */
    fun preprocessRegion(
        imageProxy: ImageProxy, 
        boundingBox: Rect?
    ): Bitmap? {
        val fullBitmap = preprocessImage(imageProxy) ?: return null
        
        if (boundingBox == null) return fullBitmap
        
        // Expand bounding box slightly for better context
        val expandedBox = Rect(
            max(0, boundingBox.left - ROI_PADDING),
            max(0, boundingBox.top - ROI_PADDING),
            min(fullBitmap.width, boundingBox.right + ROI_PADDING),
            min(fullBitmap.height, boundingBox.bottom + ROI_PADDING)
        )
        
        // Ensure minimum size
        if (expandedBox.width() < MIN_ROI_SIZE || expandedBox.height() < MIN_ROI_SIZE) {
            return fullBitmap
        }
        
        return Bitmap.createBitmap(
            fullBitmap, 
            expandedBox.left, 
            expandedBox.top, 
            expandedBox.width(), 
            expandedBox.height()
        )
    }
    
    /**
     * Convert ImageProxy to Bitmap for processing
     */
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap? {
        @androidx.camera.core.ExperimentalGetImage
        val image = imageProxy.image ?: return null
        
        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        
        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        yPlane.buffer.get(nv21, 0, ySize)
        vPlane.buffer.get(nv21, ySize, vSize)
        uPlane.buffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    /**
     * Convert Image to Bitmap
     */
    private fun imageToBitmap(image: android.media.Image): Bitmap? {
        val planes = image.planes
        val yPlane = planes[0]
        val uPlane = planes[1]
        val vPlane = planes[2]
        
        val ySize = yPlane.buffer.remaining()
        val uSize = uPlane.buffer.remaining()
        val vSize = vPlane.buffer.remaining()
        
        val nv21 = ByteArray(ySize + uSize + vSize)
        yPlane.buffer.get(nv21, 0, ySize)
        vPlane.buffer.get(nv21, ySize, vSize)
        uPlane.buffer.get(nv21, ySize + vSize, uSize)
        
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }
    
    /**
     * Adjust contrast and brightness specifically for light gray text
     */
    private fun adjustContrastAndBrightness(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint()
        
        val colorMatrix = ColorMatrix().apply {
            // Increase contrast
            val scale = CONTRAST_FACTOR
            val translate = (-(scale - 1) * 128f) + BRIGHTNESS_ADJUSTMENT
            set(floatArrayOf(
                scale, 0f, 0f, 0f, translate,
                0f, scale, 0f, 0f, translate,
                0f, 0f, scale, 0f, translate,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        
        paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Apply gamma correction to enhance mid-tones
     */
    private fun applyGammaCorrection(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Create gamma lookup table
        val gammaLUT = IntArray(256) { i ->
            val normalized = i / 255.0
            val corrected = Math.pow(normalized, GAMMA_CORRECTION.toDouble())
            (corrected * 255).toInt().coerceIn(0, 255)
        }
        
        // Apply gamma correction
        val pixels = IntArray(result.width * result.height)
        result.getPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        
        for (i in pixels.indices) {
            val pixel = pixels[i]
            val a = pixel shr 24 and 0xff
            val r = gammaLUT[pixel shr 16 and 0xff]
            val g = gammaLUT[pixel shr 8 and 0xff]
            val b = gammaLUT[pixel and 0xff]
            pixels[i] = a shl 24 or (r shl 16) or (g shl 8) or b
        }
        
        result.setPixels(pixels, 0, result.width, 0, 0, result.width, result.height)
        return result
    }
    
    /**
     * Sharpen image to improve edge definition
     */
    private fun sharpenImage(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint()
        
        // Sharpening kernel
        val kernel = floatArrayOf(
            0f, -1f, 0f,
            -1f, 5f, -1f,
            0f, -1f, 0f
        )
        
        // Note: For production, you'd use RenderScript or similar for convolution
        // This is simplified for the example
        paint.isFilterBitmap = true
        canvas.drawBitmap(bitmap, 0f, 0f, paint)
        
        return result
    }
    
    /**
     * Reduce noise while preserving edges
     */
    private fun reduceNoise(bitmap: Bitmap): Bitmap {
        val result = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        
        // Simple median filter implementation
        // For production, consider using more sophisticated noise reduction
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        
        val resultPixels = IntArray(width * height)
        
        for (y in 1 until height - 1) {
            for (x in 1 until width - 1) {
                // Get 3x3 neighborhood
                val neighbors = mutableListOf<Int>()
                for (dy in -1..1) {
                    for (dx in -1..1) {
                        neighbors.add(pixels[(y + dy) * width + (x + dx)])
                    }
                }
                
                // Apply median filter
                neighbors.sort()
                resultPixels[y * width + x] = neighbors[4] // median
            }
        }
        
        result.setPixels(resultPixels, 0, width, 0, 0, width, height)
        return result
    }
    
    /**
     * Analyze image quality metrics
     */
    fun analyzeImageQuality(bitmap: Bitmap): ImageQualityMetrics {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        
        var totalBrightness = 0.0
        var minBrightness = 255
        var maxBrightness = 0
        
        for (pixel in pixels) {
            val r = pixel shr 16 and 0xff
            val g = pixel shr 8 and 0xff
            val b = pixel and 0xff
            val brightness = (0.299 * r + 0.587 * g + 0.114 * b).toInt()
            
            totalBrightness += brightness
            minBrightness = min(minBrightness, brightness)
            maxBrightness = max(maxBrightness, brightness)
        }
        
        val avgBrightness = totalBrightness / pixels.size
        val contrast = maxBrightness - minBrightness
        
        return ImageQualityMetrics(
            averageBrightness = avgBrightness.toFloat(),
            contrast = contrast.toFloat(),
            isTooDark = avgBrightness < 50,
            isTooLight = avgBrightness > 200,
            hasLowContrast = contrast < 50
        )
    }
    
    /**
     * Auto-adjust preprocessing parameters based on image quality
     */
    fun getAdaptiveParameters(metrics: ImageQualityMetrics): PreprocessingParameters {
        return PreprocessingParameters(
            contrastFactor = when {
                metrics.hasLowContrast -> 3.0f
                metrics.contrast > 150 -> 1.5f
                else -> CONTRAST_FACTOR
            },
            brightnessAdjustment = when {
                metrics.isTooDark -> 30
                metrics.isTooLight -> -40
                else -> BRIGHTNESS_ADJUSTMENT
            },
            gammaCorrection = when {
                metrics.isTooDark -> 1.2f
                metrics.isTooLight -> 0.6f
                else -> GAMMA_CORRECTION
            },
            sharpeningFactor = if (metrics.hasLowContrast) 2.0f else SHARPENING_FACTOR
        )
    }
}

/**
 * Image quality analysis results
 */
data class ImageQualityMetrics(
    val averageBrightness: Float,
    val contrast: Float,
    val isTooDark: Boolean,
    val isTooLight: Boolean,
    val hasLowContrast: Boolean
)

/**
 * Adaptive preprocessing parameters
 */
data class PreprocessingParameters(
    val contrastFactor: Float,
    val brightnessAdjustment: Int,
    val gammaCorrection: Float,
    val sharpeningFactor: Float
)
