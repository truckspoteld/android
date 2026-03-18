package com.truckspot.eld

import android.os.Bundle
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.truckspot.eld.databinding.ActivityPdfViewerBinding
import java.io.File
import android.util.Log
import android.content.Intent
import android.app.AlertDialog

class PdfViewerActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfViewerBinding

    companion object {
        const val EXTRA_PDF_PATH = "pdf_path"
        const val EXTRA_PDF_TITLE = "pdf_title"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = intent.getStringExtra(EXTRA_PDF_TITLE) ?: "PDF Report"

        // Show initial message
        binding.webView.loadData(
            "<html><body style='text-align: center; padding: 20px; font-family: Arial, sans-serif;'>" +
            "<h1>PDF Viewer</h1>" +
            "<p>Your PDF has been generated successfully!</p>" +
            "<p>Please wait while we prepare the viewing options...</p>" +
            "</body></html>",
            "text/html",
            "UTF-8"
        )

        // Get PDF file path
        val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
        if (pdfPath != null) {
            loadPdf(pdfPath)
        } else {
            binding.webView.loadData("<html><body><h1>Error: PDF file not found</h1></body></html>", "text/html", "UTF-8")
        }
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.pdf_viewer_menu, menu)
        return true
    }

    private fun loadPdf(pdfPath: String) {
        val pdfFile = File(pdfPath)
        Log.d("PdfViewerActivity", "Loading PDF from path: $pdfPath")
        Log.d("PdfViewerActivity", "File exists: ${pdfFile.exists()}")
        Log.d("PdfViewerActivity", "File size: ${pdfFile.length()} bytes")

        if (pdfFile.exists()) {
            try {
                // First, try to open in external PDF viewer (most reliable)
                Log.d("PdfViewerActivity", "Attempting to open PDF in external viewer...")
                if (openPdfInExternalApp(pdfFile)) {
                    // If external viewer opens successfully, close this activity
                    finish()
                    return
                }

                // If external viewer fails, show options to user
                Log.d("PdfViewerActivity", "External viewer failed, showing options...")
                showPdfViewingOptions(pdfFile)

            } catch (e: Exception) {
                Log.e("PdfViewerActivity", "Error loading PDF: ${e.message}", e)
                showPdfViewingOptions(pdfFile)
            }
        } else {
            Log.e("PdfViewerActivity", "PDF file does not exist at path: $pdfPath")
            binding.webView.loadData("<html><body><h1>Error: PDF file not found at $pdfPath</h1></body></html>", "text/html", "UTF-8")
        }
    }

    /**
     * Opens the PDF in an external PDF viewer app
     */
    private fun openPdfInExternalApp(pdfFile: File): Boolean {
        try {
            val contentUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                return true // Indicate success
            } else {
                return false // Indicate failure
            }
        } catch (e: Exception) {
            Log.e("PdfViewerActivity", "Error opening PDF in external app: ${e.message}", e)
            return false // Indicate failure
        }
    }

    private fun showPdfViewingOptions(pdfFile: File) {
        val options = arrayOf(
            "Open with Google Docs Viewer",
            "Open with PDF Viewer App",
            "Try WebView Preview",
            "Cancel"
        )

        AlertDialog.Builder(this)
            .setTitle("Open PDF")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> { // Open with Google Docs Viewer
                        try {
                            val googleDocsUrl = "https://docs.google.com/viewer?url=file://$pdfFile.path&embedded=true"
                            binding.webView.loadUrl(googleDocsUrl)
                        } catch (e: Exception) {
                            Log.e("PdfViewerActivity", "Error opening with Google Docs: ${e.message}", e)
                            binding.webView.loadData(
                                "<html><body><h1>Error opening PDF</h1><p>Could not open with Google Docs: ${e.message}</p></body></html>",
                                "text/html",
                                "UTF-8"
                            )
                        }
                    }
                    1 -> { // Open with PDF Viewer App
                        try {
                            val contentUri = FileProvider.getUriForFile(
                                this,
                                "${packageName}.fileprovider",
                                pdfFile
                            )
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(contentUri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            if (intent.resolveActivity(packageManager) != null) {
                                startActivity(intent)
                            } else {
                                binding.webView.loadData(
                                    "<html><body><h1>No PDF Viewer App Found</h1><p>Please install a PDF viewer app from the Play Store.</p></body></html>",
                                    "text/html",
                                    "UTF-8"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("PdfViewerActivity", "Error opening with PDF Viewer App: ${e.message}", e)
                            binding.webView.loadData(
                                "<html><body><h1>Error</h1><p>Could not open PDF in external app: ${e.message}</p></body></html>",
                                "text/html",
                                "UTF-8"
                            )
                        }
                    }
                    2 -> { // Try WebView Preview
                        tryWebViewPreview(pdfFile)
                    }
                    3 -> { // Cancel
                        // No action needed, dialog will close
                    }
                }
            }
            .setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun tryWebViewPreview(pdfFile: File) {
        try {
            Log.d("PdfViewerActivity", "Trying WebView preview...")

            // Configure WebView for PDF viewing
            binding.webView.settings.apply {
                javaScriptEnabled = true
                allowFileAccess = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                setSupportZoom(true)
                allowContentAccess = true
                allowFileAccessFromFileURLs = true
                allowUniversalAccessFromFileURLs = true
            }

            binding.webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d("PdfViewerActivity", "WebView page finished loading: $url")
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    super.onReceivedError(view, errorCode, description, failingUrl)
                    Log.e("PdfViewerActivity", "WebView error: $errorCode - $description for URL: $failingUrl")
                    // Show error message
                    binding.webView.loadData(
                        "<html><body><h1>PDF Preview Failed</h1><p>Error: $description</p><p>Please use one of the other options to view the PDF.</p></body></html>",
                        "text/html",
                        "UTF-8"
                    )
                }
            }

            // Try to load PDF using content URI
            val contentUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                pdfFile
            )

            Log.d("PdfViewerActivity", "Loading PDF in WebView with content URI: $contentUri")
            binding.webView.loadUrl(contentUri.toString())

        } catch (e: Exception) {
            Log.e("PdfViewerActivity", "Error with WebView preview: ${e.message}", e)
            binding.webView.loadData(
                "<html><body><h1>WebView Preview Failed</h1><p>Error: ${e.message}</p><p>Please use one of the other options to view the PDF.</p></body></html>",
                "text/html",
                "UTF-8"
            )
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_open_external -> {
                val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
                if (pdfPath != null) {
                    loadPdf(pdfPath)
                }
                true
            }
            R.id.action_refresh -> {
                val pdfPath = intent.getStringExtra(EXTRA_PDF_PATH)
                if (pdfPath != null) {
                    loadPdf(pdfPath)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onBackPressed() {
        if (binding.webView.canGoBack()) {
            binding.webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}
