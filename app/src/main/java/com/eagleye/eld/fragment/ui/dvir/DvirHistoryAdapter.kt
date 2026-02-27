package com.eagleye.eld.fragment.ui.dvir

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eagleye.eld.R
import com.eagleye.eld.databinding.ItemDvirReportBinding
import com.eagleye.eld.models.DvirReport
import java.util.Locale

class DvirHistoryAdapter : RecyclerView.Adapter<DvirHistoryAdapter.DvirViewHolder>() {

    private val reports = mutableListOf<DvirReport>()

    fun submitList(items: List<DvirReport>) {
        reports.clear()
        reports.addAll(items)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DvirViewHolder {
        val binding = ItemDvirReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DvirViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DvirViewHolder, position: Int) {
        holder.bind(reports[position])
    }

    override fun getItemCount(): Int = reports.size

    class DvirViewHolder(private val binding: ItemDvirReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DvirReport) {
            val trip = if (item.report_type == "post_trip") "Post Trip" else "Pre Trip"
            binding.tvDvirHeader.text = "$trip • ${item.report_date ?: "-"}"
            binding.tvDvirVehicle.text = "Vehicle: ${item.vehicle?.truck_no ?: item.vin_no ?: "-"}"
            val condition = when (item.vehicle_condition?.lowercase()) {
                "has_defects" -> "Has defects"
                "satisfactory" -> "Satisfactory"
                else -> normalizeReadableText(item.vehicle_condition)
            }
            binding.tvDvirCondition.text = "Condition: $condition"
            val status = normalizeReadableText(item.status)
            binding.tvDvirStatus.text = "Status: $status"
            binding.tvDvirDefects.text = "Defects: ${item.defects_description ?: "-"}"

            val statusColor = if ((item.status ?: "").equals("submitted", ignoreCase = true)) {
                ContextCompat.getColor(itemView.context, R.color.dvir_success)
            } else {
                ContextCompat.getColor(itemView.context, R.color.dvir_warning)
            }
            binding.tvDvirStatus.setTextColor(statusColor)
        }

        private fun normalizeReadableText(value: String?): String {
            if (value.isNullOrBlank()) return "-"
            return value
                .replace("_", " ")
                .trim()
                .lowercase(Locale.US)
                .split(Regex("\\s+"))
                .joinToString(" ") { token ->
                    token.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString() }
                }
        }
    }
}
