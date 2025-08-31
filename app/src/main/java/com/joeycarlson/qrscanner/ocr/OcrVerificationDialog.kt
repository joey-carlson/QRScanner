package com.joeycarlson.qrscanner.ocr

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.joeycarlson.qrscanner.R
import kotlin.math.roundToInt

/**
 * Dialog for manual verification and editing of OCR results
 * Implements FR-OCR-006: Manual verification/editing of OCR results
 */
class OcrVerificationDialog : DialogFragment() {
    
    private var ocrResult: String = ""
    private var confidence: Float = 0f
    private var componentType: DsnValidator.ComponentType? = null
    private var onConfirm: ((String) -> Unit)? = null
    private var onCancel: (() -> Unit)? = null
    
    companion object {
        private const val ARG_OCR_RESULT = "ocr_result"
        private const val ARG_CONFIDENCE = "confidence"
        private const val ARG_COMPONENT_TYPE = "component_type"
        
        fun newInstance(
            ocrResult: String,
            confidence: Float,
            componentType: DsnValidator.ComponentType? = null
        ): OcrVerificationDialog {
            return OcrVerificationDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_OCR_RESULT, ocrResult)
                    putFloat(ARG_CONFIDENCE, confidence)
                    componentType?.let { 
                        putString(ARG_COMPONENT_TYPE, it.name)
                    }
                }
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            ocrResult = it.getString(ARG_OCR_RESULT, "")
            confidence = it.getFloat(ARG_CONFIDENCE, 0f)
            it.getString(ARG_COMPONENT_TYPE)?.let { typeName ->
                componentType = try {
                    DsnValidator.ComponentType.valueOf(typeName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = LayoutInflater.from(requireContext())
        val view = inflater.inflate(R.layout.dialog_ocr_verification, null)
        
        // Set up views
        val titleText = view.findViewById<TextView>(R.id.dialogTitle)
        val confidenceText = view.findViewById<TextView>(R.id.confidenceText)
        val confidenceBar = view.findViewById<ProgressBar>(R.id.confidenceBar)
        val inferredTypeText = view.findViewById<TextView>(R.id.inferredTypeText)
        val editText = view.findViewById<EditText>(R.id.serialNumberEditText)
        val confirmButton = view.findViewById<Button>(R.id.confirmButton)
        val cancelButton = view.findViewById<Button>(R.id.cancelButton)
        
        // Set title based on confidence
        titleText.text = if (confidence >= 0.8f) {
            "Verify Serial Number"
        } else {
            "Low Confidence - Please Verify"
        }
        
        // Display confidence
        val confidencePercent = (confidence * 100).roundToInt()
        confidenceText.text = "Confidence: $confidencePercent%"
        confidenceBar.progress = confidencePercent
        
        // Color code confidence bar
        val progressDrawable = when {
            confidence >= 0.9f -> ContextCompat.getDrawable(requireContext(), R.drawable.progress_high_confidence)
            confidence >= 0.7f -> ContextCompat.getDrawable(requireContext(), R.drawable.progress_medium_confidence)
            else -> ContextCompat.getDrawable(requireContext(), R.drawable.progress_low_confidence)
        }
        confidenceBar.progressDrawable = progressDrawable
        
        // Show inferred component type if available
        componentType?.let { type ->
            inferredTypeText.visibility = View.VISIBLE
            inferredTypeText.text = "Detected Type: ${getComponentTypeDisplayName(type)}"
        } ?: run {
            inferredTypeText.visibility = View.GONE
        }
        
        // Set initial text
        editText.setText(ocrResult)
        editText.setSelection(ocrResult.length)
        
        // Show keyboard
        editText.postDelayed({
            editText.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }, 200)
        
        // Set up dialog
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(view)
            .setCancelable(false)
            .create()
        
        // Button listeners
        confirmButton.setOnClickListener {
            val editedText = editText.text.toString().trim()
            if (editedText.isNotEmpty()) {
                val validator = DsnValidator()
                val validationResult = validator.validateManualEntry(editedText)
                
                if (validationResult.isValid) {
                    onConfirm?.invoke(validationResult.normalizedDsn ?: editedText)
                    dismiss()
                } else {
                    editText.error = validationResult.error
                }
            } else {
                editText.error = "Serial number cannot be empty"
            }
        }
        
        cancelButton.setOnClickListener {
            onCancel?.invoke()
            dismiss()
        }
        
        return dialog
    }
    
    fun setOnConfirmListener(listener: (String) -> Unit) {
        onConfirm = listener
    }
    
    fun setOnCancelListener(listener: () -> Unit) {
        onCancel = listener
    }
    
    private fun getComponentTypeDisplayName(type: DsnValidator.ComponentType): String {
        return when (type) {
            DsnValidator.ComponentType.GLASSES -> "Glasses"
            DsnValidator.ComponentType.CONTROLLER -> "Controller"
            DsnValidator.ComponentType.BATTERY_01 -> "Battery 1"
            DsnValidator.ComponentType.BATTERY_02 -> "Battery 2"
            DsnValidator.ComponentType.BATTERY_03 -> "Battery 3"
            DsnValidator.ComponentType.PADS -> "Pads"
            DsnValidator.ComponentType.UNUSED_01 -> "Unused 1"
            DsnValidator.ComponentType.UNUSED_02 -> "Unused 2"
        }
    }
}
