package com.eagleye.eld.fragment.ui.reports

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.eagleye.eld.R
import com.eagleye.eld.models.ReportLogItem

class ReportsAdapterAdaptor(private val dataSet: MutableList<ReportLogItem>) :
    RecyclerView.Adapter<ReportsAdapterAdaptor.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTime: TextView
        val tvMode: TextView
        val tvLocation: TextView
        val tvOdomerter: TextView
        val tvHours: TextView
        val tvComments: TextView

        init {
            tvTime = view.findViewById(R.id.preview_time)
            tvMode = view.findViewById(R.id.preview_mode)
            tvLocation = view.findViewById(R.id.preview_location)
            tvOdomerter = view.findViewById(R.id.preview_odometer)
            tvHours = view.findViewById(R.id.preview_hours)
            tvComments = view.findViewById(R.id.preview_comments)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.custom_reports_view, viewGroup, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Ensure position is within the bounds of the dataSet
        if (position in 0 until dataSet.size) {
            val currentItem = dataSet[position]
            viewHolder.tvTime.text = currentItem.date
            viewHolder.tvMode.text = currentItem.modename
            viewHolder.tvLocation.text = currentItem.location
            viewHolder.tvOdomerter.text = currentItem.odometerreading
            viewHolder.tvComments.text = currentItem.discreption
            viewHolder.tvHours.text = currentItem.eng_hours
        }
    }


    // Update the getItemCount method to return the size of the dataSet
    override fun getItemCount() = dataSet.size

}
