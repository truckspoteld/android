package com.eagleye.eld.fragment.ui.reports

import androidx.navigation.fragment.findNavController

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.text.InputType
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Observer
import com.eagleye.eld.R
import com.eagleye.eld.databinding.FragmentReportsBinding
import com.eagleye.eld.databinding.LayoutEldGraphBinding
import com.eagleye.eld.databinding.LayoutPrintBinding
import com.eagleye.eld.fragment.ui.home.HomeViewModel
import com.eagleye.eld.models.UserLogsItem
import com.eagleye.eld.utils.NetworkResult
import com.eagleye.eld.utils.PrefRepository
import com.eagleye.eld.utils.PdfReportGenerator
import com.eagleye.eld.PdfViewerActivity
import com.eagleye.eld.models.GetLogsByDateResponse
import com.eagleye.eld.models.GetCompanyById
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import com.daimajia.androidanimations.library.YoYo
import com.daimajia.androidanimations.library.Techniques


@AndroidEntryPoint
class ReportsFragment : Fragment() {
    private enum class TransferMethod {
        EMAIL,
        WEBSERVICE
    }
    private var _binding: FragmentReportsBinding? = null
    private var _bindingPrint: LayoutPrintBinding? = null
    private var _bindingEldGraph: LayoutEldGraphBinding? = null
    lateinit var prefRepository: PrefRepository

    private val binding get() = _binding!!
    private val bindingPrint get() = _bindingPrint!!
    private val bindingEldGraph get() = _bindingEldGraph!!
    private lateinit var userLogs: List<UserLogsItem>

    private var pd: ProgressDialog? = null;
    private var isEmailSelected: Boolean = false;

    private var reportIndex = 0
    private val homeViewModel by viewModels<HomeViewModel>()
    private lateinit var pdfGenerator: PdfReportGenerator
    
    // Company and driver information
    private var companyInfo: GetCompanyById.Results? = null
    private var driverName: String = "-"
    private var driverLicense: String = "-"
    private var vehicleVin: String = "-"

    private fun showFmcsaResultDialog(title: String, body: String) {
        val tv = TextView(requireContext()).apply {
            text = body
            setTextColor(Color.parseColor("#111827"))
            textSize = 14f
            setPadding(48, 32, 48, 16)
            movementMethod = ScrollingMovementMethod()
        }
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setView(tv)
            .setPositiveButton("OK", null)
            .setNeutralButton("Copy") { _, _ ->
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("FMCSA Result", body))
                Toast.makeText(requireContext(), "Copied", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun buildWebServiceResultText(response: com.eagleye.eld.models.FmcsaWebServiceTransferResponse?): String {
        if (response == null) return "No response."
        val sb = StringBuilder()
        sb.appendLine(response.message.ifBlank { "FMCSA response received." })

        val d = response.data
        if (d != null) {
            if (!d.status.isNullOrBlank()) sb.appendLine("\nStatus: ${d.status}")
            if (!d.submissionId.isNullOrBlank()) sb.appendLine("SubmissionId: ${d.submissionId}")
            if (d.errorCount != null) sb.appendLine("ErrorCount: ${d.errorCount}")
            if (!d.broadcast.isNullOrBlank()) sb.appendLine("\nBroadcast:\n${d.broadcast}")

            val errs = d.errors.orEmpty().filter { !it.message.isNullOrBlank() || !it.detail.isNullOrBlank() || !it.errorType.isNullOrBlank() }
            if (errs.isNotEmpty()) {
                sb.appendLine("\nMessages:")
                errs.take(50).forEachIndexed { idx, e ->
                    sb.appendLine("${idx + 1}. ${listOfNotNull(e.errorType, e.message).joinToString(" - ")}")
                    if (!e.detail.isNullOrBlank()) sb.appendLine("   ${e.detail}")
                }
                if (errs.size > 50) sb.appendLine("... (${errs.size - 50} more)")
            }
        }

        return sb.toString().trim()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
        prefRepository = PrefRepository(requireContext())
        pdfGenerator = PdfReportGenerator(requireContext())

        setupObservers()

        binding.btnStartReview.setOnClickListener {
            playClickAnimation(it)
            val bundle = Bundle().apply {
                putBoolean("isReviewMode", true)
            }
            findNavController().navigate(R.id.nav_gallery, bundle)
        }
        
        binding.btnBack.setOnClickListener {
            playClickAnimation(it)
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnEmail.setOnClickListener { 
            playClickAnimation(it)
            showFmcsaTransferDialog(TransferMethod.EMAIL)
        }
        binding.btnTransfer.setOnClickListener {
            playClickAnimation(it)
            showFmcsaTransferDialog(TransferMethod.WEBSERVICE)
        }
        binding.btnUsb.setOnClickListener { playClickAnimation(it) }
        binding.btnPaperLogs.setOnClickListener {
            playClickAnimation(it)
            showPaperLogsEmailDialog()
        }

        // Load real driver data from shared preferences
        driverName = prefRepository.getName().ifBlank { "-" }
        val driverId = prefRepository.getDriverId()
        Log.d("ReportsFragment", "Loaded driver: name=$driverName, id=$driverId")

        // Fetch company data from API
        homeViewModel.getCompanyName(requireContext())

        entranceAnimations()

        return binding.root
    }

    private fun setupObservers() {
        homeViewModel.company.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    companyInfo = result.data?.results
                }
                is NetworkResult.Error -> {}
                is NetworkResult.Loading -> {}
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleSendAction() {
        downloadPdfReport()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        android.app.DatePickerDialog(requireContext(), { _, y, m, d ->
            val selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", y, m + 1, d)
            onDateSelected(selectedDate)
        }, year, month, day).show()
    }

    private fun entranceAnimations() {
        if (_binding == null) return
        
        val duration = 700L
        val stagger = 80L

        val views = listOf(
            binding.cardRegulation,
            binding.cardFmcsa,
            binding.cardPaperLogs
        )

        views.forEachIndexed { index, view ->
            view.visibility = View.INVISIBLE
            lifecycleScope.launch {
                delay((index + 1) * stagger)
                if (_binding != null) {
                    view.visibility = View.VISIBLE
                    val technique = Techniques.FadeInUp
                    YoYo.with(technique).duration(duration).playOn(view)
                }
            }
        }
    }

    fun setSelectedButton(isEmail: Boolean) {
        // Logic removed for new UI
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun downloadPdfReport() {
        Log.d("ReportsFragment", "downloadPdfReport called")
        Log.d("ReportsFragment", "Skipping permission check, going directly to PDF generation")
        startPdfGeneration()
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startPdfGeneration() {
        // Show progress dialog
        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Generating PDF Report...")
            setCancelable(false)
            show()
        }

        // Get current date for the report
        val dateFormat = SimpleDateFormat("MMM dd yyyy", Locale.getDefault())
        val currentDate = dateFormat.format(Calendar.getInstance().time)

        // Create request for current date logs
        val request = com.eagleye.eld.models.GetLogsByDateRequest(
            prefRepository.getDriverId(),
            "2025-08-17",
            "2025-08-29"

        )

        // Get logs data
        Log.d("ReportsFragment", "Starting PDF generation for date: ${request.fromdate}")
        lifecycleScope.launch {
            homeViewModel.dashboardRespository.getLogsByDate(request)
        }
        
        // Observe the response
        homeViewModel.dashboardRespository.logByDate.observe(viewLifecycleOwner) { result ->
            Log.d("ReportsFragment", "Received result: $result")
            when (result) {
                is NetworkResult.Success -> {
                    try {
                        Log.d("ReportsFragment", "Success case - data received")
                        val reportData = result.data!!
                        Log.d("ReportsFragment", "Report data: ${reportData.results?.userLogs?.size} logs found")
                    
                        // Create PDF file in internal storage (no permissions needed)
                        val fileName = "Driver_Daily_Report_${currentDate.replace(" ", "_")}.pdf"
                        val internalDir = requireContext().getExternalFilesDir(null)
                        var pdfFile = File(internalDir, fileName)
                        Log.d("ReportsFragment", "PDF will be saved to: ${pdfFile.absolutePath}")
                        
                        // Ensure internal directory exists
                        if (!internalDir!!.exists()) {
                            val created = internalDir.mkdirs()
                            Log.d("ReportsFragment", "Internal directory created: $created")
                        }
                        
                        // Generate PDF
                        Log.d("ReportsFragment", "Starting PDF generation...")
                        
                        val firstLog = reportData.results?.userLogs?.firstOrNull()
                        val extractedDriverName = driverName.ifBlank {
                            firstLog?.let { "Driver ${it.driverid ?: "Unknown"}" } ?: "-"
                        }
                        
                        val extractedVehicleVin = firstLog?.vin ?: vehicleVin
                        
                        val success = pdfGenerator.generateDriverDailyReport(
                            reportData,
                            currentDate,
                            pdfFile,
                            companyInfo,
                            extractedDriverName,
                            driverLicense,
                            extractedVehicleVin
                        )
                        Log.d("ReportsFragment", "PDF generation result: $success")
                        
                        progressDialog.dismiss()
                        
                        if (success) {
                            Log.d("ReportsFragment", "PDF generated successfully at: ${pdfFile.absolutePath}")
                            // Show success message
                            makeText(requireContext(), "PDF Report generated successfully!", Toast.LENGTH_LONG).show()
                            
                            // Show options dialog
                            showPdfOptionsDialog(pdfFile, currentDate)
                        } else {
                            Log.e("ReportsFragment", "PDF generation failed")
                            makeText(requireContext(), "Failed to generate PDF report", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        progressDialog.dismiss()
                        Log.e("ReportsFragment", "Error generating PDF: ${e.message}", e)
                        makeText(requireContext(), "Error generating PDF: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                is NetworkResult.Error -> {
                    Log.e("ReportsFragment", "Network error: ${result.message}")
                    progressDialog.dismiss()
                    makeText(requireContext(), "Error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Loading -> {
                    Log.d("ReportsFragment", "Loading data...")
                    // Keep showing progress dialog
                }
            }
        }
    }

    private fun openPdfFromAssets(fileName: String) {
        // PDF file ko internal storage me copy karte hain
        val file = File(requireContext().filesDir, fileName)
        if (!file.exists()) {
            requireContext().assets.open(fileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }

        // FileProvider ke zariye URI banayein
        val uri: Uri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.provider",
            file
        )

        // Intent launch karein
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NO_HISTORY
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Agar PDF viewer install nahi hai to user ko alert karein
        }
    }
    private fun generateTestPdf() {
        try {
            val fileName = "Test_PDF_${System.currentTimeMillis()}.pdf"
            val internalDir = requireContext().getExternalFilesDir(null)
            val pdfFile = File(internalDir, fileName)
            
            Log.d("ReportsFragment", "Generating test PDF to: ${pdfFile.absolutePath}")
            
            val success = pdfGenerator.generateTestPdf(pdfFile)
            
            if (success) {
                Log.d("ReportsFragment", "Test PDF generated successfully")
                makeText(requireContext(), "Test PDF generated successfully!", Toast.LENGTH_LONG).show()
                
                // Show options dialog
                showPdfOptionsDialog(pdfFile, "Test PDF")
            } else {
                Log.e("ReportsFragment", "Test PDF generation failed")
                makeText(requireContext(), "Test PDF generation failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error generating test PDF: ${e.message}", e)
            makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showPdfOptionsDialog(pdfFile: File, currentDate: String) {
        val options = arrayOf("Preview PDF", "Share PDF", "Open in Downloads")
        
        AlertDialog.Builder(requireContext())
            .setTitle("PDF Report Generated")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> previewPdf(pdfFile, currentDate)
                    1 -> sharePdfFile(pdfFile)
                    2 -> openInDownloads(pdfFile)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun previewPdf(pdfFile: File, currentDate: String) {
        try {
            val intent = Intent(requireContext(), PdfViewerActivity::class.java).apply {
                putExtra(PdfViewerActivity.EXTRA_PDF_PATH, pdfFile.absolutePath)
                putExtra(PdfViewerActivity.EXTRA_PDF_TITLE, "Driver Daily Report - $currentDate")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error previewing PDF: ${e.message}", e)
            makeText(requireContext(), "Error previewing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openInDownloads(pdfFile: File) {
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.provider",
                    pdfFile
                ), "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open PDF with"))
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error opening PDF: ${e.message}", e)
            makeText(requireContext(), "Error opening PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sharePdfFile(pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.provider",
                pdfFile
            )
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Driver Daily Report")
                putExtra(Intent.EXTRA_TEXT, "Please find attached the driver daily report.")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(intent, "Share PDF Report"))
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Error sharing PDF: ${e.message}", e)
            makeText(requireContext(), "Error sharing PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.d("ReportsFragment", "Permission result - requestCode: $requestCode, permissions: ${permissions.joinToString()}, grantResults: ${grantResults.joinToString()}")
        
        when (requestCode) {
            102 -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    Log.d("ReportsFragment", "All permissions granted, retrying PDF generation")
                    // Permission granted, retry PDF generation
                    downloadPdfReport()
                } else {
                    Log.e("ReportsFragment", "Permission denied")
                    makeText(requireContext(), "Storage permission required to save PDF", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun testPdfGeneration() {
        Log.d("ReportsFragment", "Testing PDF generation...")
        
        // Create a simple test PDF
        val testFile = File(requireContext().getExternalFilesDir(null), "test_report.pdf")
        Log.d("ReportsFragment", "Test file path: ${testFile.absolutePath}")
        
        try {
            val success = pdfGenerator.generateDriverDailyReport(
                GetLogsByDateResponse(
                    code = 200,
                    message = "Test",
                    status = true,
                    results = GetLogsByDateResponse.Results(
                        conditions = GetLogsByDateResponse.Results.Conditions(
                            createdat = "",
                            cycle = 0,
                            cycleviolation = false,
                            drive = 0,
                            drivebreak = 0,
                            drivebreakviolation = false,
                            driveviolation = false,
                            shift = 0,
                            shiftviolation = false,
                            updatedat = "",
                            userid = 0
                        ),
                        totalCount = 0,
                        meta = GetLogsByDateResponse.Results.Meta(),
                        userLogs = listOf(
                            GetLogsByDateResponse.Results.UserLog(
                                authorization_status = "Test",
                                certification_date = "",
                                certification_status = "",
                                codriverid = 0,
                                company_timezone = "PST",
                                created_on = "2023-11-08",
                                datadiagnostic = 0,
                                date = "2023-11-08",
                                datetime = "2023-11-08 10:00:00",
                                discreption = "Test log",
                                distance = "0",
                                driverid = 1,
                                duration = 0,
                                end_datetime = "",
                                eng_hours = "100.0",
                                enginemiles = "0",
                                event_status = 0,
                                eventcode = 0,
                                eventrecordorigin = "",
                                eventrecordstatus = 0,
                                eventsequenceid = "test",
                                eventtype = 0,
                                exempt_driver = 0,
                                hours = 0,
                                id = 1,
                                is_active = 1,
                                is_autoinsert = 1,
                                lat = "0",
                                linedata_checkvalue = "",
                                location = "Test Location",
                                long = "0",
                                malfunctioneld = 0,
                                modename = "ON",
                                odometerreading = "1000",
                                order_number = "",
                                powerunitnumber = "",
                                shipping_number = "",
                                time = "10:00",
                                trailer_number = "",
                                vin = "TEST123"
                            )
                        ),
                        nextDayLog = null,
                        previousDayLog = null,
                        latestUpdatedLog = null
                    )
                ),
                "Nov 08 2023",
                testFile
            )
            
            if (success) {
                makeText(requireContext(), "Test PDF generated successfully!", Toast.LENGTH_LONG).show()
                Log.d("ReportsFragment", "Test PDF generated at: ${testFile.absolutePath}")
            } else {
                makeText(requireContext(), "Test PDF generation failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Test PDF generation error: ${e.message}", e)
            makeText(requireContext(), "Test error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun testSimplePdfGeneration() {
        Log.d("ReportsFragment", "Testing simple PDF generation...")
        
        // Try to create a simple PDF file
        val testFile = File(requireContext().getExternalFilesDir(null), "simple_test.pdf")
        Log.d("ReportsFragment", "Simple test file path: ${testFile.absolutePath}")
        
        try {
            // Create a simple text file first to test file writing
            val testTextFile = File(requireContext().getExternalFilesDir(null), "test.txt")
            testTextFile.writeText("Test file created successfully!")
            Log.d("ReportsFragment", "Text file created successfully")
            
            // Now try PDF generation
            val success = pdfGenerator.generateDriverDailyReport(
                GetLogsByDateResponse(
                    code = 200,
                    message = "Simple Test",
                    status = true,
                    results = GetLogsByDateResponse.Results(
                        conditions = GetLogsByDateResponse.Results.Conditions(
                            createdat = "",
                            cycle = 0,
                            cycleviolation = false,
                            drive = 0,
                            drivebreak = 0,
                            drivebreakviolation = false,
                            driveviolation = false,
                            shift = 0,
                            shiftviolation = false,
                            updatedat = "",
                            userid = 0
                        ),
                        totalCount = 0,
                        meta = GetLogsByDateResponse.Results.Meta(),
                        userLogs = emptyList(),
                        nextDayLog = null,
                        previousDayLog = null,
                        latestUpdatedLog = null
                    )
                ),
                "Test Date",
                testFile
            )
            
            if (success) {
                makeText(requireContext(), "Simple PDF test successful!", Toast.LENGTH_LONG).show()
                Log.d("ReportsFragment", "Simple PDF test successful at: ${testFile.absolutePath}")
            } else {
                makeText(requireContext(), "Simple PDF test failed", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Log.e("ReportsFragment", "Simple PDF test error: ${e.message}", e)
            makeText(requireContext(), "Simple test error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun testApiCall() {
        Log.d("ReportsFragment", "Testing API call...")
        
        // Create request for current date logs
        val request = com.eagleye.eld.models.GetLogsByDateRequest(
            prefRepository.getDriverId(),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time),
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
        )
        
        Log.d("ReportsFragment", "API request: driverId=${request.driverId}, date=${request.fromdate}")
        
        // Get logs data
        lifecycleScope.launch {
            homeViewModel.dashboardRespository.getLogsByDate(request)
        }
        
        // Observe the response
        homeViewModel.dashboardRespository.logByDate.observe(viewLifecycleOwner) { result ->
            Log.d("ReportsFragment", "API test result: $result")
            when (result) {
                is NetworkResult.Success -> {
                    val reportData = result.data!!
                    Log.d("ReportsFragment", "API test success - logs count: ${reportData.results?.userLogs?.size}")
                    makeText(requireContext(), "API test successful! Logs: ${reportData.results?.userLogs?.size}", Toast.LENGTH_LONG).show()
                }
                is NetworkResult.Error -> {
                    Log.e("ReportsFragment", "API test error: ${result.message}")
                    makeText(requireContext(), "API test error: ${result.message}", Toast.LENGTH_SHORT).show()
                }
                is NetworkResult.Loading -> {
                    Log.d("ReportsFragment", "API test loading...")
                    makeText(requireContext(), "API test loading...", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun playClickAnimation(view: View) {
        // High-end subtle scale feedback
        view.animate()
            .scaleX(0.96f)
            .scaleY(0.96f)
            .setDuration(120)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(120)
                    .start()
            }
            .start()
    }

    private fun showFmcsaTransferDialog(method: TransferMethod) {
        val input = EditText(requireContext()).apply {
            hint = "Enter transfer code"
            inputType = InputType.TYPE_CLASS_TEXT
            setPadding(48, 32, 48, 32)
        }
        val title = if (method == TransferMethod.EMAIL) "FMCSA Email Transfer" else "FMCSA Web Service Transfer (API)"
        val message = "Enter the inspection transfer code provided by officer."
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val transferCode = input.text?.toString()?.trim().orEmpty()
                if (transferCode.isBlank()) {
                    Toast.makeText(requireContext(), "Transfer code is required", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                sendFmcsaTransfer(method, transferCode)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showPaperLogsEmailDialog() {
        val input = EditText(requireContext()).apply {
            hint = "Enter email address"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("Send Paper Logs PDF")
            .setMessage("Enter email address to send paper logs PDF.")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text?.toString()?.trim().orEmpty()
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                sendPaperLogsPdfToEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun sendPaperLogsPdfToEmail(email: String) {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage("Preparing paper logs PDF...")
            setCancelable(false)
            show()
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endCal = Calendar.getInstance()
        val endDate = dateFormat.format(endCal.time)
        val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val startDate = dateFormat.format(startCal.time)

        val observer = object : Observer<NetworkResult<GetLogsByDateResponse>> {
            override fun onChanged(result: NetworkResult<GetLogsByDateResponse>) {
                when (result) {
                    is NetworkResult.Success -> {
                        homeViewModel.dashboardRespository.logByDate.removeObserver(this)
                        try {
                            val reportData = result.data ?: throw IllegalStateException("No report data")
                            val fileName = "Paper_Logs_${startDate}_to_${endDate}.pdf"
                            val pdfFile = File(requireContext().getExternalFilesDir(null), fileName)
                            val firstLog = reportData.results?.userLogs?.firstOrNull()
                            val extractedDriverName = driverName.ifBlank {
                                firstLog?.let { "Driver ${it.driverid ?: "Unknown"}" } ?: "-"
                            }
                            val extractedVehicleVin = firstLog?.vin ?: vehicleVin

                            val pdfOk = pdfGenerator.generateDriverDailyReportByDateRange(
                                reportData,
                                startDate,
                                endDate,
                                pdfFile,
                                companyInfo,
                                extractedDriverName,
                                driverLicense,
                                extractedVehicleVin
                            )
                            if (!pdfOk || !pdfFile.exists()) throw IllegalStateException("Failed to generate PDF")

                            val pdfBase64 = Base64.encodeToString(pdfFile.readBytes(), Base64.NO_WRAP)
                            lifecycleScope.launch {
                                val sendResult = homeViewModel.sendPaperLogsByEmail(
                                    email = email,
                                    startDate = startDate,
                                    endDate = endDate,
                                    fileName = fileName,
                                    pdfBase64 = pdfBase64
                                )
                                progressDialog.dismiss()
                                when (sendResult) {
                                    is NetworkResult.Success -> {
                                        Toast.makeText(
                                            requireContext(),
                                            sendResult.data?.message ?: "Paper logs sent successfully.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    is NetworkResult.Error -> {
                                        Toast.makeText(
                                            requireContext(),
                                            sendResult.message ?: "Failed to send paper logs.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                    is NetworkResult.Loading -> Unit
                                }
                            }
                        } catch (e: Exception) {
                            progressDialog.dismiss()
                            Toast.makeText(requireContext(), "Error preparing PDF: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    is NetworkResult.Error -> {
                        homeViewModel.dashboardRespository.logByDate.removeObserver(this)
                        progressDialog.dismiss()
                        Toast.makeText(requireContext(), result.message ?: "Unable to fetch logs.", Toast.LENGTH_LONG).show()
                    }
                    is NetworkResult.Loading -> Unit
                }
            }
        }

        homeViewModel.dashboardRespository.logByDate.observe(viewLifecycleOwner, observer)
        lifecycleScope.launch {
            homeViewModel.dashboardRespository.getLogsByDate(
                com.eagleye.eld.models.GetLogsByDateRequest(
                    prefRepository.getDriverId(),
                    startDate,
                    endDate
                )
            )
        }
    }

    private fun sendFmcsaTransfer(method: TransferMethod, transferCode: String) {
        val progressDialog = ProgressDialog(requireContext()).apply {
            setMessage(if (method == TransferMethod.EMAIL) "Sending logs to FMCSA via Email..." else "Sending logs to FMCSA via Web Service (API)...")
            setCancelable(false)
            show()
        }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val endCal = Calendar.getInstance()
        val endDate = dateFormat.format(endCal.time)
        val startCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
        val startDate = dateFormat.format(startCal.time)

        lifecycleScope.launch {
            when (method) {
                TransferMethod.EMAIL -> {
                    val result = homeViewModel.sendFmcsaEmailTransfer(startDate, endDate, transferCode)
                    progressDialog.dismiss()
                    when (result) {
                        is NetworkResult.Success -> {
                            Toast.makeText(
                                requireContext(),
                                result.data?.message ?: "FMCSA email transfer sent successfully",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is NetworkResult.Error -> {
                            Toast.makeText(
                                requireContext(),
                                result.message ?: "Failed to send FMCSA email transfer",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is NetworkResult.Loading -> Unit
                    }
                }

                TransferMethod.WEBSERVICE -> {
                    val result = homeViewModel.sendFmcsaWebServiceTransfer(startDate, endDate, transferCode)
                    progressDialog.dismiss()
                    when (result) {
                        is NetworkResult.Success -> {
                            val body = buildWebServiceResultText(result.data)
                            showFmcsaResultDialog("FMCSA Web Service Result", body)
                        }
                        is NetworkResult.Error -> {
                            Toast.makeText(
                                requireContext(),
                                result.message ?: "Failed to send FMCSA web service transfer",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        is NetworkResult.Loading -> Unit
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}