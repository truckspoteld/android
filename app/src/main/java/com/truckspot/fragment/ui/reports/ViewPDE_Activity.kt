//package com.truckspot.fragment.ui.reports
//
//import android.os.Bundle
//import androidx.appcompat.app.AppCompatActivity
//import com.github.barteksc.pdfviewer.PDFView
//import com.truckspot.R
//
//class ViewPDE_Activity : AppCompatActivity() {
//    private lateinit var pdfView: PDFView
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_view_pde)
//        pdfView=findViewById(R.id.pdfViews)
//       pdfView.fromAsset("report_user.pdf")
//            .enableSwipe(true) // swipe se page change
//            .swipeHorizontal(false)
//            .enableDoubletap(true)
//            .load()
//    }
//}

package com.truckspot.fragment.ui.reports

import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.truckspot.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class ViewPDE_Activity : AppCompatActivity() {

    private lateinit var pdfRecyclerView: RecyclerView
    private var pdfRenderer: PdfRenderer? = null
    private var tempPdfFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_pde)

        pdfRecyclerView = findViewById(R.id.pdf_recycler_view)

        try {
            tempPdfFile = copyAssetToFile("report_user.pdf")
            openPdfRenderer()
            setupRecyclerView()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "An error occurred", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    @Throws(IOException::class)
    private fun copyAssetToFile(fileName: String): File {
        val file = File(cacheDir, fileName)
        if (!file.exists()) {
            assets.open(fileName).use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
        }
        return file
    }

    @Throws(IOException::class)
    private fun openPdfRenderer() {
        tempPdfFile?.let { file ->
            val fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            pdfRenderer = PdfRenderer(fileDescriptor)
        }
    }

    private fun setupRecyclerView() {
        pdfRenderer?.let { renderer ->
            // Set up the RecyclerView with a vertical LinearLayoutManager
            pdfRecyclerView.layoutManager = LinearLayoutManager(this)
            // Create and set the adapter, passing the PdfRenderer instance
            pdfRecyclerView.adapter = PdfPageAdapter(renderer)
        }
    }

    override fun onDestroy() {
        // Clean up all resources
        pdfRenderer?.close()
        tempPdfFile?.delete()
        super.onDestroy()
    }
}