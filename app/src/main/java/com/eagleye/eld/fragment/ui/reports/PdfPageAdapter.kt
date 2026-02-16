package com.eagleye.eld.fragment.ui.reports

import com.eagleye.eld.R

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView

class PdfPageAdapter(private val renderer: PdfRenderer) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_pdf_page, parent, false)
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int {
        return renderer.pageCount
    }

    override fun onViewRecycled(holder: PdfPageViewHolder) {
        // Recycle the bitmap to free up memory when the view is scrolled off-screen
        holder.recycleBitmap()
    }

    inner class PdfPageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.pdf_page_image)
        private var currentBitmap: Bitmap? = null

        fun bind(pageIndex: Int) {
            // Recycle any previous bitmap
            recycleBitmap()

            try {
                // Use the 'use' block to ensure the page is always closed
                renderer.openPage(pageIndex).use { page ->
                    // Create a bitmap for the page
                    currentBitmap = Bitmap.createBitmap(page.width, page.height, Bitmap.Config.ARGB_8888)
                    // Render the page onto the bitmap
                    page.render(currentBitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    // Set the bitmap to the ImageView
                    imageView.setImageBitmap(currentBitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Handle errors, maybe show a placeholder
                imageView.setImageBitmap(null)
            }
        }

        fun recycleBitmap() {
            currentBitmap?.let {
                if (!it.isRecycled) {
                    it.recycle()
                }
                currentBitmap = null
            }
            imageView.setImageBitmap(null)
        }
    }
}