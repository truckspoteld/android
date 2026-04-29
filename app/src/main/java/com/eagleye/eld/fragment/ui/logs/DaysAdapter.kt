package com.eagleye.eld.fragment.ui.logs

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.eagleye.eld.R
import com.google.android.material.card.MaterialCardView

data class DayModel(
    val dayName: String,
    val dayNumber: String,
    val fullDate: String,
    val dayOffset: Int,
    var isSelected: Boolean = false
)

class DaysAdapter(
    private val days: List<DayModel>,
    private val onDaySelected: (DayModel) -> Unit
) : RecyclerView.Adapter<DaysAdapter.DayViewHolder>() {

    class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayCard: MaterialCardView = itemView.findViewById(R.id.day_card)
        val tvDayName: TextView = itemView.findViewById(R.id.tv_day_name)
        val tvDayNumber: TextView = itemView.findViewById(R.id.tv_day_number)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_day_box, parent, false)
        return DayViewHolder(view)
    }

    override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
        val day = days[position]
        holder.tvDayName.text = day.dayName
        holder.tvDayNumber.text = day.dayNumber

        if (day.isSelected) {
            holder.dayCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.nav_icon_active))
            holder.dayCard.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.nav_icon_active)
            holder.tvDayName.textColor = ContextCompat.getColor(holder.itemView.context, R.color.white)
            holder.tvDayNumber.textColor = ContextCompat.getColor(holder.itemView.context, R.color.white)
            
            // Bouncy animation for selection
            holder.itemView.animate().scaleX(1.1f).scaleY(1.1f).setDuration(200).withEndAction {
                holder.itemView.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
            }.start()
        } else {
            holder.dayCard.setCardBackgroundColor(ContextCompat.getColor(holder.itemView.context, R.color.white))
            holder.dayCard.strokeColor = ContextCompat.getColor(holder.itemView.context, R.color.strockColor)
            holder.tvDayName.textColor = ContextCompat.getColor(holder.itemView.context, R.color.home_text_sub)
            holder.tvDayNumber.textColor = ContextCompat.getColor(holder.itemView.context, R.color.home_text_main)
            holder.itemView.scaleX = 1.0f
            holder.itemView.scaleY = 1.0f
        }

        holder.itemView.setOnClickListener {
            if (!day.isSelected) {
                days.forEach { it.isSelected = false }
                day.isSelected = true
                notifyDataSetChanged()
                onDaySelected(day)
            }
        }
    }

    override fun getItemCount() = days.size
}

// Extension to help with setting colors easily
private var TextView.textColor: Int
    get() = currentTextColor
    set(value) = setTextColor(value)
