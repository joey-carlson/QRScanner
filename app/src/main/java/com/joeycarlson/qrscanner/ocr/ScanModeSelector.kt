package com.joeycarlson.qrscanner.ocr

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.joeycarlson.qrscanner.R

/**
 * Custom view for selecting scan mode
 * Implements FR-OCR-003: Provide scan mode switching UI controls
 */
class ScanModeSelector @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    
    private val toggleGroup: MaterialButtonToggleGroup
    private val barcodeButton: MaterialButton
    private val ocrButton: MaterialButton
    private val hybridButton: MaterialButton
    
    private var onModeChangeListener: ((ScanMode) -> Unit)? = null
    
    init {
        orientation = VERTICAL
        LayoutInflater.from(context).inflate(R.layout.view_scan_mode_selector, this, true)
        
        toggleGroup = findViewById(R.id.scanModeToggleGroup)
        barcodeButton = findViewById(R.id.barcodeModeButton)
        ocrButton = findViewById(R.id.ocrModeButton)
        hybridButton = findViewById(R.id.hybridModeButton)
        
        // Set default selection
        toggleGroup.check(R.id.barcodeModeButton)
        
        // Set up click listeners
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val mode = when (checkedId) {
                    R.id.barcodeModeButton -> ScanMode.BARCODE_ONLY
                    R.id.ocrModeButton -> ScanMode.OCR_ONLY
                    R.id.hybridModeButton -> ScanMode.HYBRID
                    else -> ScanMode.BARCODE_ONLY
                }
                onModeChangeListener?.invoke(mode)
            }
        }
    }
    
    /**
     * Set the current scan mode
     */
    fun setMode(mode: ScanMode) {
        val buttonId = when (mode) {
            ScanMode.BARCODE_ONLY -> R.id.barcodeModeButton
            ScanMode.OCR_ONLY -> R.id.ocrModeButton
            ScanMode.HYBRID -> R.id.hybridModeButton
        }
        toggleGroup.check(buttonId)
    }
    
    /**
     * Get the current scan mode
     */
    fun getMode(): ScanMode {
        return when (toggleGroup.checkedButtonId) {
            R.id.barcodeModeButton -> ScanMode.BARCODE_ONLY
            R.id.ocrModeButton -> ScanMode.OCR_ONLY
            R.id.hybridModeButton -> ScanMode.HYBRID
            else -> ScanMode.BARCODE_ONLY
        }
    }
    
    /**
     * Set listener for mode changes
     */
    fun setOnModeChangeListener(listener: (ScanMode) -> Unit) {
        onModeChangeListener = listener
    }
    
    /**
     * Enable or disable the selector
     */
    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        toggleGroup.isEnabled = enabled
        barcodeButton.isEnabled = enabled
        ocrButton.isEnabled = enabled
        hybridButton.isEnabled = enabled
    }
}
