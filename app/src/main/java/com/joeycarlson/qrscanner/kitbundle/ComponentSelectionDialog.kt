package com.joeycarlson.qrscanner.kitbundle

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.joeycarlson.qrscanner.R

/**
 * Dialog for manual component type selection when detection confidence is low
 * or when user rejects the suggested component type
 */
class ComponentSelectionDialog : DialogFragment() {
    
    interface OnComponentSelectListener {
        fun onSelect(dsn: String, slot: String)
        fun onCancel()
    }
    
    private var listener: OnComponentSelectListener? = null
    private lateinit var dsn: String
    private var availableSlots: List<ComponentSlot> = emptyList()
    private lateinit var adapter: ComponentSlotAdapter
    
    companion object {
        private const val ARG_DSN = "arg_dsn"
        private const val ARG_AVAILABLE_SLOTS = "arg_available_slots"
        
        fun newInstance(
            dsn: String,
            availableSlots: List<ComponentSlot>
        ): ComponentSelectionDialog {
            return ComponentSelectionDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_DSN, dsn)
                    putParcelableArrayList(ARG_AVAILABLE_SLOTS, ArrayList(availableSlots))
                }
            }
        }
    }
    
    fun setOnComponentSelectListener(listener: OnComponentSelectListener) {
        this.listener = listener
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        arguments?.let { args ->
            dsn = args.getString(ARG_DSN) ?: ""
            @Suppress("DEPRECATION")
            availableSlots = args.getParcelableArrayList(ARG_AVAILABLE_SLOTS) ?: emptyList()
        }
    }
    
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val shortDsn = dsn.takeLast(12)
        
        // Create custom view with RecyclerView
        val view = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_component_selection, null)
        
        val recyclerView = view.findViewById<RecyclerView>(R.id.slotsRecyclerView)
        val emptyMessage = view.findViewById<TextView>(R.id.emptyMessage)
        
        if (availableSlots.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyMessage.visibility = View.VISIBLE
            emptyMessage.text = "All component slots are filled"
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyMessage.visibility = View.GONE
            
            adapter = ComponentSlotAdapter(availableSlots) { slot ->
                listener?.onSelect(dsn, slot.id)
                dismiss()
            }
            
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
        }
        
        return AlertDialog.Builder(requireContext())
            .setTitle("Select Component Type")
            .setMessage("DSN: ...$shortDsn\n\nWhat type of component is this?")
            .setView(view)
            .setNegativeButton("Cancel") { _, _ ->
                listener?.onCancel()
            }
            .setCancelable(false)
            .create()
    }
    
    /**
     * Adapter for displaying available component slots
     */
    private class ComponentSlotAdapter(
        private val slots: List<ComponentSlot>,
        private val onSlotClick: (ComponentSlot) -> Unit
    ) : RecyclerView.Adapter<ComponentSlotAdapter.SlotViewHolder>() {
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_component_slot, parent, false)
            return SlotViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
            holder.bind(slots[position])
        }
        
        override fun getItemCount() = slots.size
        
        inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val slotName: TextView = itemView.findViewById(R.id.slotName)
            
            fun bind(slot: ComponentSlot) {
                slotName.text = slot.displayName
                itemView.setOnClickListener {
                    onSlotClick(slot)
                }
            }
        }
    }
}
