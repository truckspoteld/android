package com.truckspot.fragment.ui.reports

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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.Toast.makeText
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.truckspot.R
import com.truckspot.databinding.FragmentReportsBinding
import com.truckspot.databinding.LayoutEldGraphBinding
import com.truckspot.databinding.LayoutPrintBinding
import com.truckspot.fragment.ui.home.HomeViewModel
import com.truckspot.models.UserLogsItem
import com.truckspot.utils.NetworkResult
import com.truckspot.utils.PrefRepository
import com.truckspot.utils.PdfReportGenerator
import com.truckspot.PdfViewerActivity
import com.truckspot.models.GetLogsByDateResponse
import com.truckspot.models.GetCompanyById
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


@AndroidEntryPoint
class ReportsFragment : Fragment() {
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
    private var driverName: String = "SUKHDEEP SINGH"
    private var driverLicense: String = "CA / Y46O8156"
    private var vehicleVin: String = "-"

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportsBinding.inflate(inflater, container, false)
//        _bindingPrint = LayoutPrintBinding.bind(binding.layoutPrint.root)
        prefRepository = PrefRepository(requireContext())
        pdfGenerator = PdfReportGenerator(requireContext())

        // Observe company data for PDF generation
        homeViewModel.company.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    companyInfo = result.data?.results
                    Log.d("ReportsFragment", "Company info loaded: ${companyInfo?.company_name}")
                }
                is NetworkResult.Error -> {
                    Log.e("ReportsFragment", "Error loading company info: ${result.message}")
                }
                is NetworkResult.Loading -> {
                    Log.d("ReportsFragment", "Loading company info...")
                }
            }
        }
        
        // Get company data
        val driverId = prefRepository.getDriverId()
        if (driverId != 0) {
//            homeViewModel.dashboardRespository.getCompanyById(driverId)
        }
        
        // Add test PDF button for debugging
        binding.btnTestPdf.setOnClickListener {
            //openPdfFromAssets("report_user.pdf")
            startActivity(Intent(context, ViewPDE_Activity::class.java))
        }
//        binding.btnTestPdf.setOnClickListener {
//            generateTestPdf()
//        }

        val rootView = inflater.inflate(R.layout.layout_eld_graph, container, false)
        val root: View = binding.root
        setSelectedButton(isEmail = false)

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

// Get current date
        val calendar = Calendar.getInstance()
        val currentDate = dateFormat.format(calendar.time)

// Get date 7 days before
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val sevenDaysBefore = dateFormat.format(calendar.time)
        binding.btnEndDate.text = currentDate
        binding.btnStartDate.text = sevenDaysBefore
        binding.btnSend.setOnClickListener {
            lifecycleScope.launch {
                if(binding.btnEndDate.text == "End Date" || binding.btnStartDate.text == "Start Date"){
                    makeText(requireContext(), "Please select start and end date", Toast.LENGTH_SHORT).show()
                    return@launch
                }else if (binding.etEmailid.text.toString().isEmpty()) {
                    makeText(requireContext(), "Please enter email", Toast.LENGTH_SHORT).show()
                    return@launch
                }
                homeViewModel.dashboardRespository.downloadAndEmailCsvViaGmail(requireContext(), binding.etEmailid.text.toString() , binding.btnStartDate.text.toString(), binding.btnEndDate.text.toString() )
            }
        }

        binding.btnWeb.setOnClickListener {
            setSelectedButton(isEmail = false)
        }

        binding.btnEmail.setOnClickListener {
            setSelectedButton(isEmail = true)
        }

        // PDF Download Button
        binding.btnDownloadPdf.setOnClickListener {
            downloadPdfReport()
        }
        
        // Add a test button for debugging (you can remove this later)
        binding.btnDownloadPdf.setOnLongClickListener {
            testPdfGeneration()
            true
        }
        
        // Add a simple test button (you can remove this later)
        binding.btnSend.setOnLongClickListener {
            testSimplePdfGeneration()
            true
        }
        
        // Add a debug button to test API call (you can remove this later)
        binding.etEmailid.setOnLongClickListener {
            testApiCall()
            true
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    101
                )
            // wait for permission result
            }
        }
        return root

    }

    fun setSelectedButton(isEmail: Boolean) {
        val blackColor = ContextCompat.getColor(requireContext(), android.R.color.black)
        val whiteColor = ContextCompat.getColor(requireContext(), android.R.color.white)

        if (isEmail) {
            // Email selected
            isEmailSelected = true
            binding.btnEmail.setBackgroundColor(whiteColor)
            binding.btnEmail.setTextColor(blackColor)
            binding.btnEmail.strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)

            binding.btnWeb.setBackgroundColor(Color.TRANSPARENT)
            binding.btnWeb.setTextColor(whiteColor)
            binding.btnWeb.strokeColor = ColorStateList.valueOf(whiteColor)
            binding.etEmailid.hint = ("Enter email to share report")

        } else {
            // Web selected
            isEmailSelected = false
            binding.btnWeb.setBackgroundColor(whiteColor)
            binding.btnWeb.setTextColor(blackColor)
            binding.btnWeb.strokeColor = ColorStateList.valueOf(Color.TRANSPARENT)

            binding.btnEmail.setBackgroundColor(Color.TRANSPARENT)
            binding.btnEmail.setTextColor(whiteColor)
            binding.btnEmail.strokeColor = ColorStateList.valueOf(whiteColor)
            binding.etEmailid.hint = ("Submission Code (eRODS code)")
        }
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
        val request = com.truckspot.models.GetLogsByDateRequest(
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
                        
                        // Extract driver information from logs
                        val firstLog = reportData.results?.userLogs?.firstOrNull()
                        val extractedDriverName = firstLog?.let { log ->
                            // Try to get driver name from log data or use default
                            "Driver ${log.driverid ?: "Unknown"}"
                        } ?: driverName
                        
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
        val request = com.truckspot.models.GetLogsByDateRequest(
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}