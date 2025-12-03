package com.joeycarlson.qrscanner.ui

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.joeycarlson.qrscanner.R
import com.joeycarlson.qrscanner.data.ScanHistoryItem

/**
 * Adapter for displaying scan history with inline editing capability
 */
class ScanHistoryAdapter(
    private val onItemEdit: (String, String) -> Unit, // (itemId, newValue) -> Unit
    private val onItemDelete: (String) -> Unit // (itemId) -> Unit
) : ListAdapter<ScanHistoryItem, ScanHistoryAdapter.HistoryViewHolder>(HistoryDiffCallback()) {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_scan_history, parent, false)
        return HistoryViewHolder(view, onItemEdit, onItemDelete)
    }
    
    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
    
    class HistoryViewHolder(
        itemView: View,
        private val onItemEdit: (String, String) -> Unit,
        private val onItemDelete: (String) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        
        private val valueEditText: EditText = itemView.findViewById(R.id.valueEditText)
        private val timestampText: TextView = itemView.findViewById(R.id.timestampText)
        private val scanTypeText: TextView = itemView.findViewById(R.id.scanTypeText)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)
        
        private var currentItem: ScanHistoryItem? = null
        private var textWatcher: TextWatcher? = null
        
        fun bind(item: ScanHistoryItem) {
            currentItem = item
            
            // Remove previous text watcher to avoid triggering on setText
            textWatcher?.let { valueEditText.removeTextChangedListener(it) }
            
            // Set values
            valueEditText.setText(item.value)
            timestampText.text = item.getFormattedTimestamp()
            scanTypeText.text = when (item.scanType) {
                ScanHistoryItem.ScanType.BARCODE -> "Barcode"
                ScanHistoryItem.ScanType.OCR -> "OCR"
                ScanHistoryItem.ScanType.MANUAL -> "Manual"
            }
            
            // Add text watcher for edits
            textWatcher = object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newValue = s?.toString() ?: ""
                    if (newValue != item.value && newValue.isNotBlank()) {
                        onItemEdit(item.id, newValue)
                    }
                }
            }
            valueEditText.addTextChangedListener(textWatcher)
            
            // Delete button
            deleteButton.setOnClickListener {
                onItemDelete(item.id)
            }
        }
    }
    
    class HistoryDiffCallback : DiffUtil.ItemCallback<ScanHistoryItem>() {
        override fun areItemsTheSame(oldItem: ScanHistoryItem, newItem: ScanHistoryItem): Boolean {
            return oldItem.id == newItem.id
        }
        
        override fun areContentsTheSame(oldItem: ScanHistoryItem, newItem: ScanHistoryItem): Boolean {
            return oldItem == newItem
        }
    }
}
