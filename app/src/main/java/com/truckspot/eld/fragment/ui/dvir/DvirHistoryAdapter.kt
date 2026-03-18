package com.truckspot.eld.fragment.ui.dvir

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.daimajia.androidanimations.library.YoYo
import com.daimajia.androidanimations.library.Techniques
import com.truckspot.eld.R
import com.truckspot.eld.databinding.ItemDvirReportBinding
import com.truckspot.eld.models.DvirReport
import java.util.Locale

class DvirHistoryAdapter : RecyclerView.Adapter<DvirHistoryAdapter.DvirViewHolder>() {

    private val reports = mutableListOf<DvirReport>()
    private var lastAnimatedPosition = -1

    fun submitList(items: List<DvirReport>) {
        reports.clear()
        reports.addAll(items)
        lastAnimatedPosition = -1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DvirViewHolder {
        val binding = ItemDvirReportBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DvirViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DvirViewHolder, position: Int) {
        holder.bind(reports[position])
        if (position > lastAnimatedPosition) {
            YoYo.with(Techniques.FadeInDown)
                .duration(500)
                .interpolate(AccelerateDecelerateInterpolator())
                .playOn(holder.itemView)
            lastAnimatedPosition = position
        }
    }

    override fun getItemCount(): Int = reports.size

    class DvirViewHolder(private val binding: ItemDvirReportBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: DvirReport) {
            val trip = if (item.report_type == "post_trip") "Post Trip" else "Pre Trip"
            binding.tvDvirHeader.text = "$trip • ${item.report_date ?: "-"}"
            binding.tvDvirVehicle.text = item.vehicle?.truck_no ?: item.vin_no ?: "-"
            
            val isSatisfactory = item.vehicle_condition?.lowercase() == "satisfactory"
            val condition = if (isSatisfactory) "Satisfactory" else normalizeReadableText(item.vehicle_condition)
            binding.tvDvirCondition.text = condition
            
            val status = normalizeReadableText(item.status)
            binding.tvDvirStatus.text = status
            binding.tvDvirDefects.text = "Defects: ${item.defects_description ?: "None"}"

            val isSubmitted = (item.status ?: "").equals("submitted", ignoreCase = true)
            val statusColor = if (isSubmitted) {
                ContextCompat.getColor(itemView.context, R.color.status_on_text)
            } else {
                ContextCompat.getColor(itemView.context, R.color.home_text_sub)
            }
            val statusBg = if (isSubmitted) {
                ContextCompat.getColor(itemView.context, R.color.status_on_bg)
            } else {
                ContextCompat.getColor(itemView.context, R.color.home_bg_blue_light) // Fallback to a light blue for other statuses
            }
            
            binding.tvDvirStatus.setTextColor(statusColor)
            binding.tvDvirStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(statusBg)
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
