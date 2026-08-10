package com.joeycarlson.qrscanner.ocr

import android.graphics.Bitmap
import android.graphics.Color
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ImagePreprocessor].
 *
 * Scope is the two public methods with JVM-real logic:
 *  - [ImagePreprocessor.analyzeImageQuality] — per-pixel brightness/contrast
 *    math and the derived quality flags.
 *  - [ImagePreprocessor.getAdaptiveParameters] — the branch logic mapping
 *    quality metrics onto preprocessing parameters.
 *
 * The `Bitmap`-transforming methods (`adjustContrastAndBrightness`,
 * `sharpenImage`) route through `Canvas`/`ColorMatrixColorFilter`, which
 * Robolectric does not actually render, so their output pixels cannot be
 * asserted here. The YUV/ImageProxy conversion path is likewise out of scope.
 *
 * Robolectric is required only so that `Bitmap.createBitmap(...)` and
 * `getPixels` round-trip real pixel data on the JVM.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ImagePreprocessorTest {

    private val preprocessor = ImagePreprocessor()
    private val delta = 0.5f

    /** Build a bitmap by tiling [colors] across [width] x [height] row-major. */
    private fun bitmapOf(width: Int, height: Int, vararg colors: Int): Bitmap {
        val pixels = IntArray(width * height) { colors[it % colors.size] }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    // region analyzeImageQuality

    @Test
    fun `analyzeImageQuality reports white image as too light with zero contrast`() {
        val metrics = preprocessor.analyzeImageQuality(bitmapOf(4, 4, Color.WHITE))

        assertEquals(255f, metrics.averageBrightness, delta)
        assertEquals(0f, metrics.contrast, delta)
        assertTrue(metrics.isTooLight)
        assertFalse(metrics.isTooDark)
        assertTrue(metrics.hasLowContrast)
    }

    @Test
    fun `analyzeImageQuality reports black image as too dark with zero contrast`() {
        val metrics = preprocessor.analyzeImageQuality(bitmapOf(4, 4, Color.BLACK))

        assertEquals(0f, metrics.averageBrightness, delta)
        assertEquals(0f, metrics.contrast, delta)
        assertTrue(metrics.isTooDark)
        assertFalse(metrics.isTooLight)
        assertTrue(metrics.hasLowContrast)
    }

    @Test
    fun `analyzeImageQuality measures full contrast across black and white pixels`() {
        // Half black, half white: avg brightness ~127.5, contrast = 255.
        val metrics = preprocessor.analyzeImageQuality(bitmapOf(2, 1, Color.BLACK, Color.WHITE))

        assertEquals(127.5f, metrics.averageBrightness, delta)
        assertEquals(255f, metrics.contrast, delta)
        assertFalse(metrics.hasLowContrast)
        assertFalse(metrics.isTooDark)
        assertFalse(metrics.isTooLight)
    }

    @Test
    fun `analyzeImageQuality weights green most heavily in luminance`() {
        // Pure green -> 0.587*255 = 149.685 -> truncated to 149.
        val green = preprocessor.analyzeImageQuality(bitmapOf(1, 1, Color.GREEN))
        // Pure blue -> 0.114*255 = 29.07 -> truncated to 29.
        val blue = preprocessor.analyzeImageQuality(bitmapOf(1, 1, Color.BLUE))

        assertEquals(149f, green.averageBrightness, delta)
        assertEquals(29f, blue.averageBrightness, delta)
        assertTrue(green.averageBrightness > blue.averageBrightness)
    }

    // endregion

    // region getAdaptiveParameters

    private fun metrics(
        avg: Float = 128f,
        contrast: Float = 100f,
        tooDark: Boolean = false,
        tooLight: Boolean = false,
        lowContrast: Boolean = false
    ) = ImageQualityMetrics(avg, contrast, tooDark, tooLight, lowContrast)

    @Test
    fun `getAdaptiveParameters boosts contrast and sharpening for low-contrast images`() {
        val params = preprocessor.getAdaptiveParameters(metrics(lowContrast = true))

        assertEquals(3.0f, params.contrastFactor, delta)
        assertEquals(2.0f, params.sharpeningFactor, delta)
    }

    @Test
    fun `getAdaptiveParameters eases contrast for already high-contrast images`() {
        val params = preprocessor.getAdaptiveParameters(metrics(contrast = 200f))

        assertEquals(1.5f, params.contrastFactor, delta)
    }

    @Test
    fun `getAdaptiveParameters uses default contrast for normal images`() {
        val params = preprocessor.getAdaptiveParameters(metrics(contrast = 100f))

        assertEquals(2.5f, params.contrastFactor, delta)
        assertEquals(1.5f, params.sharpeningFactor, delta)
    }

    @Test
    fun `getAdaptiveParameters brightens and raises gamma for dark images`() {
        val params = preprocessor.getAdaptiveParameters(metrics(tooDark = true))

        assertEquals(30, params.brightnessAdjustment)
        assertEquals(1.2f, params.gammaCorrection, delta)
    }

    @Test
    fun `getAdaptiveParameters darkens and lowers gamma for light images`() {
        val params = preprocessor.getAdaptiveParameters(metrics(tooLight = true))

        assertEquals(-40, params.brightnessAdjustment)
        assertEquals(0.6f, params.gammaCorrection, delta)
    }

    @Test
    fun `getAdaptiveParameters uses default brightness and gamma for normal images`() {
        val params = preprocessor.getAdaptiveParameters(metrics())

        assertEquals(-20, params.brightnessAdjustment)
        assertEquals(0.8f, params.gammaCorrection, delta)
    }

    // endregion
}
