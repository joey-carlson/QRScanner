package com.joeycarlson.qrscanner.kitbundle

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.ocr.DsnValidator

/**
 * Dialog for confirming component assignment when detection confidence is medium
 */
class ComponentConfirmationDialog : DialogFragment() {
    
    interface OnComponentConfirmListener {
        fun onConfirm(dsn: String, slot: String)
        fun onCancel()
    }
    
    private var listener: OnComponentConfirmListener? = null
    private lateinit var dsn: String
    private var componentType: DsnValidator.ComponentType? = null
    private var suggestedSlot: String? = null
    
    companion object {
        private const val ARG_DSN = "arg_dsn"
        private const val ARG_COMPONENT_TYPE = "arg_component_type"
        private const val ARG_SUGGESTED_SLOT = "arg_suggested_slot"
        
        fun newInstance(
            dsn: String,
            componentType: DsnValidator.ComponentType?,
            suggestedSlot: String?
        ): ComponentConfirmationDialog {
            return ComponentConfirmationDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DSN, dsn)
                    componentType?.let { putString(ARG_COMPONENT_TYPE, it.name) }
                    putString(ARG_SUGGESTED_SLOT, suggestedSlot)
                }
            }
        }
    }
    
    fun setOnComponentConfirmListener(listener: OnComponentConfirmListener) {
        this.listener = listener
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        arguments?.let { args ->
            dsn = args.getString(ARG_DSN) ?: ""
            args.getString(ARG_COMPONENT_TYPE)?.let { typeName ->
                componentType = try {
                    DsnValidator.ComponentType.valueOf(typeName)
                } catch (e: IllegalArgumentException) {
                    null
                }
            }
            suggestedSlot = args.getString(ARG_SUGGESTED_SLOT)
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val componentName = getComponentDisplayName(componentType)
        val shortDsn = dsn.takeLast(12)
        
        val message = buildString {
            append("Component detected with medium confidence:\n\n")
            append("DSN: ...$shortDsn\n")
            append("Type: $componentName\n\n")
            append("Is this correct?")
        }
        
        return AlertDialog.Builder(requireContext())
            .setTitle("Confirm Component")
            .setMessage(message)
            .setPositiveButton("Yes, Assign") { _, _ ->
                suggestedSlot?.let { slot ->
                    listener?.onConfirm(dsn, slot)
                }
            }
            .setNegativeButton("No, Select Manually") { _, _ ->
                // This will trigger the manual selection dialog
                listener?.onCancel()
            }
            .setCancelable(false)
            .create()
    }
    
    private fun getComponentDisplayName(type: DsnValidator.ComponentType?): String {
        return when (type) {
            DsnValidator.ComponentType.GLASSES -> "Glasses"
            DsnValidator.ComponentType.CONTROLLER -> "Controller"
            DsnValidator.ComponentType.BATTERY_01,
            DsnValidator.ComponentType.BATTERY_02,
            DsnValidator.ComponentType.BATTERY_03 -> "Battery"
            DsnValidator.ComponentType.PADS -> "Pads"
            DsnValidator.ComponentType.UNUSED_01 -> "Unused 01"
            DsnValidator.ComponentType.UNUSED_02 -> "Unused 02"
            null -> "Unknown"
        }
    }
}
