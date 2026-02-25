package com.eagleye.eld.fragment.ui.dvir

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.eagleye.eld.databinding.ItemDvirReportBinding
import com.eagleye.eld.models.DvirReport

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
            binding.tvDvirCondition.text = "Condition: ${item.vehicle_condition ?: "-"}"
            binding.tvDvirStatus.text = "Status: ${item.status ?: "-"}"
            binding.tvDvirDefects.text = "Defects: ${item.defects_description ?: "-"}"
        }
    }
}
