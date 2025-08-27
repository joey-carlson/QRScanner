package com.joeycarlson.qrscanner.export

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.joeycarlson.qrscanner.databinding.ItemExportMethodBinding

class ExportMethodAdapter(
    private val methods: List<ExportMethod>,
    private val onMethodClick: (ExportMethod) -> Unit
) : RecyclerView.Adapter<ExportMethodAdapter.ExportMethodViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExportMethodViewHolder {
        val binding = ItemExportMethodBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExportMethodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExportMethodViewHolder, position: Int) {
        holder.bind(methods[position])
    }

    override fun getItemCount() = methods.size

    inner class ExportMethodViewHolder(
        private val binding: ItemExportMethodBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(method: ExportMethod) {
            binding.methodIcon.text = method.icon
            binding.methodName.text = method.name
            binding.methodDescription.text = method.description
            
            // Set visual state based on availability
            binding.root.isEnabled = method.isAvailable
            binding.root.alpha = if (method.isAvailable) 1.0f else 0.5f
            
            // Set click listener only for available methods
            if (method.isAvailable) {
                binding.root.setOnClickListener {
                    onMethodClick(method)
                }
            } else {
                binding.root.setOnClickListener(null)
            }
            
            // Show/hide "Coming Soon" badge
            binding.comingSoonBadge.visibility = if (method.isAvailable) {
                android.view.View.GONE
            } else {
                android.view.View.VISIBLE
            }
        }
    }
}
