package com.joeycarlson.qrscanner.kitbundle

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.joeycarlson.qrscanner.R

class DuplicateComponentDialog : DialogFragment() {
    
    private var onDuplicateActionListener: OnDuplicateActionListener? = null
    
    interface OnDuplicateActionListener {
        fun onIgnore()
        fun onReassign(newSlot: String)
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dsn = arguments?.getString(ARG_DSN) ?: ""
        val currentSlot = arguments?.getString(ARG_CURRENT_SLOT) ?: ""
        val currentSlotDisplayName = arguments?.getString(ARG_CURRENT_SLOT_DISPLAY) ?: ""
        val suggestedNewSlot = arguments?.getString(ARG_SUGGESTED_SLOT)
        val suggestedSlotDisplayName = arguments?.getString(ARG_SUGGESTED_SLOT_DISPLAY) ?: "another slot"
        
        val shortDsn = if (dsn.length > 8) "...${dsn.takeLast(8)}" else dsn
        
        val message = if (suggestedNewSlot != null) {
            "Component $shortDsn is already assigned to $currentSlotDisplayName.\n\n" +
            "Would you like to ignore this scan or reassign it to $suggestedSlotDisplayName?"
        } else {
            "Component $shortDsn is already assigned to $currentSlotDisplayName.\n\n" +
            "Would you like to ignore this scan?"
        }
        
        val builder = AlertDialog.Builder(requireContext())
            .setTitle("Duplicate Component Detected")
            .setMessage(message)
            .setCancelable(false)
            .setNegativeButton("Ignore") { _, _ ->
                onDuplicateActionListener?.onIgnore()
                dismiss()
            }
        
        // Only show reassign button if we have a suggested slot
        if (suggestedNewSlot != null) {
            builder.setPositiveButton("Reassign to $suggestedSlotDisplayName") { _, _ ->
                onDuplicateActionListener?.onReassign(suggestedNewSlot)
                dismiss()
            }
        }
        
        return builder.create()
    }
    
    fun setOnDuplicateActionListener(listener: OnDuplicateActionListener) {
        onDuplicateActionListener = listener
    }
    
    companion object {
        private const val ARG_DSN = "dsn"
        private const val ARG_CURRENT_SLOT = "current_slot"
        private const val ARG_CURRENT_SLOT_DISPLAY = "current_slot_display"
        private const val ARG_SUGGESTED_SLOT = "suggested_slot"
        private const val ARG_SUGGESTED_SLOT_DISPLAY = "suggested_slot_display"
        
        fun newInstance(
            dsn: String,
            currentSlot: String,
            currentSlotDisplayName: String,
            suggestedNewSlot: String? = null,
            suggestedSlotDisplayName: String? = null
        ): DuplicateComponentDialog {
            return DuplicateComponentDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DSN, dsn)
                    putString(ARG_CURRENT_SLOT, currentSlot)
                    putString(ARG_CURRENT_SLOT_DISPLAY, currentSlotDisplayName)
                    suggestedNewSlot?.let { putString(ARG_SUGGESTED_SLOT, it) }
                    suggestedSlotDisplayName?.let { putString(ARG_SUGGESTED_SLOT_DISPLAY, it) }
                }
            }
        }
    }
}
