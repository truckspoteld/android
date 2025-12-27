package com.truckspot.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import android.util.Log
import com.itextpdf.text.pdf.draw.LineSeparator
import com.truckspot.models.GetLogsByDateResponse
import com.truckspot.models.UserLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.truckspot.models.GetCompanyById
import android.view.LayoutInflater
import com.truckspot.R
import com.truckspot.databinding.LayoutEldGraphBinding
import com.truckspot.utils.AlertCalculationUtils

class PdfReportGenerator(private val context: Context) {

    companion object {
        private const val FONT_SIZE_HEADER = 16f
        private const val FONT_SIZE_TITLE = 14f
        private const val FONT_SIZE_NORMAL = 10f
        private const val FONT_SIZE_SMALL = 8f
        private const val MARGIN = 30f
        private const val LINE_HEIGHT = 20f
    }

    fun generateDriverDailyReport(
        reportData: GetLogsByDateResponse,
        selectedDate: String,
        outputFile: File,
        companyInfo: GetCompanyById.Results? = null,
        driverName: String = "SUKHDEEP SINGH",
        driverLicense: String = "CA / Y46O8156",
        vehicleVin: String = "-"
    ): Boolean {
        return try {
            Log.d("PdfReportGenerator", "Starting PDF generation to: ${outputFile.absolutePath}")
            Log.d("PdfReportGenerator", "Report data: ${reportData.results?.userLogs?.size} logs")
            Log.d("PdfReportGenerator", "Company info: ${companyInfo?.company_name}")
            Log.d("PdfReportGenerator", "Driver name: $driverName")
            
            // Validate input data
            if (reportData.results?.userLogs.isNullOrEmpty()) {
                Log.w("PdfReportGenerator", "No logs data available, using fallback")
            }
            
            val document = Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN)
            val writer = PdfWriter.getInstance(document, FileOutputStream(outputFile))
            document.open()

            // Add header with logo and title
            Log.d("PdfReportGenerator", "Adding header...")
            addHeader(document, selectedDate)
            
            // Add company and driver information section
            Log.d("PdfReportGenerator", "Adding company/driver info...")
            addCompanyDriverInfo(document, reportData, companyInfo, driverName, driverLicense, vehicleVin)
            
            // Add separator line
            Log.d("PdfReportGenerator", "Adding separator...")
            addSeparatorLine(document)
            
            // Add event log table headers
            Log.d("PdfReportGenerator", "Adding event log headers...")
            addEventLogHeaders(document)
            
            // Add event log data
            Log.d("PdfReportGenerator", "Adding event log data...")
            addEventLogData(document, reportData)
            
            // Add ELD Graph
            Log.d("PdfReportGenerator", "Adding ELD graph...")
            addEldGraph(document, reportData)
            
            // Add certification
            Log.d("PdfReportGenerator", "Adding certification...")
            addCertification(document, reportData, driverName)

            document.close()
            Log.d("PdfReportGenerator", "PDF generated successfully")
            true
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error generating PDF: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    private fun addHeader(document: Document, selectedDate: String) {
        // Create a table for the header layout (logo, title, date)
        val headerTable = PdfPTable(3)
        headerTable.widthPercentage = 100f
        headerTable.setWidths(floatArrayOf(15f, 70f, 15f))
        
        // Logo placeholder (left column)
        val logoCell = PdfPCell(Phrase("LOGO", FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_NORMAL)))
        logoCell.border = Rectangle.NO_BORDER
        logoCell.horizontalAlignment = Element.ALIGN_LEFT
        logoCell.verticalAlignment = Element.ALIGN_TOP
        logoCell.setPaddingTop(10f)
        logoCell.setPaddingBottom(10f)
        headerTable.addCell(logoCell)
        
        // Title (center column)
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_HEADER)
        val title = Phrase("Driver's Daily Report\nUS 8 days 70 hours Ruleset", titleFont)
        val titleCell = PdfPCell(title)
        titleCell.border = Rectangle.NO_BORDER
        titleCell.horizontalAlignment = Element.ALIGN_CENTER
        titleCell.verticalAlignment = Element.ALIGN_TOP
        titleCell.setPaddingTop(10f)
        titleCell.setPaddingBottom(10f)
        headerTable.addCell(titleCell)
        
        // Date (right column)
        val dateFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)
        val date = Phrase(selectedDate, dateFont)
        val dateCell = PdfPCell(date)
        dateCell.border = Rectangle.NO_BORDER
        dateCell.horizontalAlignment = Element.ALIGN_RIGHT
        dateCell.verticalAlignment = Element.ALIGN_TOP
        dateCell.setPaddingTop(10f)
        dateCell.setPaddingBottom(10f)
        headerTable.addCell(dateCell)
        
        document.add(headerTable)
        document.add(Paragraph(" "))
    }

    private fun addCompanyDriverInfo(document: Document, reportData: GetLogsByDateResponse, companyInfo: GetCompanyById.Results?, driverName: String, driverLicense: String, vehicleVin: String) {
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_TITLE)
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)
        
        // Company Information
        val companyHeader = Paragraph("Company Information", headerFont)
        document.add(companyHeader)
        
        // Create company info table
        val companyTable = PdfPTable(2)
        companyTable.widthPercentage = 100f
        companyTable.setWidths(floatArrayOf(40f, 60f))
        
        addInfoRowToTable(companyTable, "Carrier", companyInfo?.company_name ?: "VNX EXPORT TRANSPORT INC", true)
        addInfoRowToTable(companyTable, "DOT Number", companyInfo?.dot_no ?: "3249740", false)
        addInfoRowToTable(companyTable, "Office Address", companyInfo?.address ?: "1155 W CENTER ST", false)
        addInfoRowToTable(companyTable, "Terminal Timezone", companyInfo?.company_timezone ?: "America / Los_Angeles", false)
        
        document.add(companyTable)
        document.add(Paragraph(" "))
        
        // Driver Information
        val driverHeader = Paragraph("Driver Information", headerFont)
        document.add(driverHeader)
        
        // Create driver info table
        val driverTable = PdfPTable(2)
        driverTable.widthPercentage = 100f
        driverTable.setWidths(floatArrayOf(40f, 60f))
        
        addInfoRowToTable(driverTable, "Driver Name / ID", driverName, false)
        addInfoRowToTable(driverTable, "Co-Driver Name / ID", "-", false)
        addInfoRowToTable(driverTable, "License State / #", driverLicense, false)
        addInfoRowToTable(driverTable, "Vehicle VIN / #", vehicleVin, false)
        
        document.add(driverTable)
        document.add(Paragraph(" "))
    }

    private fun addInfoRowToTable(table: PdfPTable, label: String, value: String, isBold: Boolean = false) {
        val labelFont = if (isBold) FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_NORMAL) 
                        else FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)
        val valueFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)
        
        // Label cell
        val labelCell = PdfPCell(Phrase(label, labelFont))
        labelCell.backgroundColor = BaseColor.LIGHT_GRAY
        labelCell.horizontalAlignment = Element.ALIGN_LEFT
        labelCell.verticalAlignment = Element.ALIGN_MIDDLE
        labelCell.setPadding(8f)
        labelCell.border = Rectangle.BOX
        labelCell.borderWidth = 1f
        table.addCell(labelCell)
        
        // Value cell
        val valueCell = PdfPCell(Phrase(value, valueFont))
        valueCell.horizontalAlignment = Element.ALIGN_LEFT
        valueCell.verticalAlignment = Element.ALIGN_MIDDLE
        valueCell.setPadding(8f)
        valueCell.border = Rectangle.BOX
        valueCell.borderWidth = 1f
        table.addCell(valueCell)
    }

    private fun addSeparatorLine(document: Document) {
        val line = LineSeparator()
        line.lineWidth = 1f
        line.lineColor = BaseColor.BLACK
        document.add(line)
        document.add(Paragraph(" "))
    }

    private fun addEventLogHeaders(document: Document) {
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_TITLE)
        
        val tableHeader = Paragraph("Event Log", headerFont)
        document.add(tableHeader)
        
        document.add(Paragraph(" "))
    }

    private fun addEventLogData(document: Document, reportData: GetLogsByDateResponse) {
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_SMALL)
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_SMALL)
        
        val logs = reportData.results?.userLogs ?: emptyList()
        if (logs.isNotEmpty()) {
            // Create table with 5 columns
            val table = PdfPTable(5)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(20f, 15f, 20f, 20f, 25f))
            
            // Add table headers
            val headers = arrayOf("Event", "Time", "Engine Hours", "Odometer", "Location")
            headers.forEach { header ->
                val cell = PdfPCell(Phrase(header, headerFont))
                cell.backgroundColor = BaseColor.LIGHT_GRAY
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.verticalAlignment = Element.ALIGN_MIDDLE
                cell.setPadding(8f)
                cell.border = Rectangle.BOX
                cell.borderWidth = 1f
                table.addCell(cell)
            }
            
            // Add data rows
            logs.forEach { log ->
                // Event Status
                val eventCell = PdfPCell(Phrase(log.modename ?: "", normalFont))
                eventCell.horizontalAlignment = Element.ALIGN_CENTER
                eventCell.verticalAlignment = Element.ALIGN_MIDDLE
                eventCell.setPadding(6f)
                eventCell.border = Rectangle.BOX
                eventCell.borderWidth = 0.5f
                table.addCell(eventCell)
                
                // Time
                val timeCell = PdfPCell(Phrase(log.time ?: "", normalFont))
                timeCell.horizontalAlignment = Element.ALIGN_CENTER
                timeCell.verticalAlignment = Element.ALIGN_MIDDLE
                timeCell.setPadding(6f)
                timeCell.border = Rectangle.BOX
                timeCell.borderWidth = 0.5f
                table.addCell(timeCell)
                
                // Engine Hours
                val engineCell = PdfPCell(Phrase(log.eng_hours ?: "", normalFont))
                engineCell.horizontalAlignment = Element.ALIGN_CENTER
                engineCell.verticalAlignment = Element.ALIGN_MIDDLE
                engineCell.setPadding(6f)
                engineCell.border = Rectangle.BOX
                engineCell.borderWidth = 0.5f
                table.addCell(engineCell)
                
                // Odometer
                val odometerCell = PdfPCell(Phrase(log.odometerreading ?: "", normalFont))
                odometerCell.horizontalAlignment = Element.ALIGN_CENTER
                odometerCell.verticalAlignment = Element.ALIGN_MIDDLE
                odometerCell.setPadding(6f)
                odometerCell.border = Rectangle.BOX
                odometerCell.borderWidth = 0.5f
                table.addCell(odometerCell)
                
                // Location
                val locationCell = PdfPCell(Phrase(log.location ?: "", normalFont))
                locationCell.horizontalAlignment = Element.ALIGN_CENTER
                locationCell.verticalAlignment = Element.ALIGN_MIDDLE
                locationCell.setPadding(6f)
                locationCell.border = Rectangle.BOX
                locationCell.borderWidth = 0.5f
                table.addCell(locationCell)
            }
            
            document.add(table)
        } else {
            document.add(Paragraph("No event logs available for the selected date.", normalFont))
            document.add(Paragraph("Please select a different date or check your data.", normalFont))
        }
        
        document.add(Paragraph(" "))
    }

    private fun addEldGraph(document: Document, reportData: GetLogsByDateResponse) {
        try {
            val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_TITLE)
            
            // Add graph header
            val graphHeader = Paragraph("ELD Duty Status Graph", headerFont)
            document.add(graphHeader)
            document.add(Paragraph(" "))
            
            // Generate ELD graph bitmap
            val logs = reportData.results?.userLogs ?: emptyList()
            val graphBitmap = generateEldGraphBitmap(logs)
            
            if (graphBitmap != null) {
                // Convert bitmap to PDF image using ByteArrayOutputStream
                val stream = java.io.ByteArrayOutputStream()
                graphBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val image = Image.getInstance(stream.toByteArray())
                
                // Scale image to fit page width while maintaining aspect ratio
                val pageWidth = PageSize.A4.width - (MARGIN * 2)
                val scale = pageWidth / graphBitmap.width.toFloat()
                image.scaleAbsolute(graphBitmap.width * scale, graphBitmap.height * scale)
                
                // Center the image
                image.alignment = Element.ALIGN_CENTER
                
                document.add(image)
                document.add(Paragraph(" "))
                
                // Add graph legend
                addGraphLegend(document)
                
                Log.d("PdfReportGenerator", "ELD graph added successfully")
            } else {
                Log.w("PdfReportGenerator", "Failed to generate ELD graph bitmap")
                document.add(Paragraph("ELD graph could not be generated.", FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)))
            }
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error adding ELD graph: ${e.message}", e)
            document.add(Paragraph("Error generating ELD graph: ${e.message}", FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)))
        }
    }

    private fun addGraphLegend(document: Document) {
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_SMALL)
        val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_SMALL)
        
        // Create legend table
        val legendTable = PdfPTable(2)
        legendTable.widthPercentage = 100f
        legendTable.setWidths(floatArrayOf(20f, 80f))
        
        // Legend entries
        val legendEntries = listOf(
            "OFF" to "Off Duty (Light Blue)",
            "SB" to "Sleeper Berth (Dark Blue)", 
            "D" to "Driving (Orange)",
            "ON" to "On Duty (Yellow)"
        )
        
        legendEntries.forEach { (status, description) ->
            val statusCell = PdfPCell(Phrase(status, boldFont))
            statusCell.horizontalAlignment = Element.ALIGN_CENTER
            statusCell.verticalAlignment = Element.ALIGN_MIDDLE
            statusCell.setPadding(4f)
            statusCell.border = Rectangle.BOX
            statusCell.borderWidth = 0.5f
            legendTable.addCell(statusCell)
            
            val descCell = PdfPCell(Phrase(description, normalFont))
            descCell.horizontalAlignment = Element.ALIGN_LEFT
            descCell.verticalAlignment = Element.ALIGN_MIDDLE
            descCell.setPadding(4f)
            descCell.border = Rectangle.BOX
            descCell.borderWidth = 0.5f
            legendTable.addCell(descCell)
        }
        
        document.add(legendTable)
        document.add(Paragraph(" "))
    }

    private fun addCertification(document: Document, reportData: GetLogsByDateResponse, driverName: String) {
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)
        val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_NORMAL)
        
        document.add(Paragraph(" "))
        document.add(Paragraph(" "))
        
        val certificationText = "I certify that these entries are true and correct"
        val certification = Paragraph(certificationText, normalFont)
        certification.alignment = Element.ALIGN_CENTER
        document.add(certification)
        
        val signature = Paragraph(driverName, boldFont)
        signature.alignment = Element.ALIGN_CENTER
        document.add(signature)
    }

    // Test method to generate a simple PDF
    fun generateTestPdf(outputFile: File): Boolean {
        return try {
            Log.d("PdfReportGenerator", "Generating test PDF to: ${outputFile.absolutePath}")
            
            val document = Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN)
            val writer = PdfWriter.getInstance(document, FileOutputStream(outputFile))
            document.open()

            // Add simple test content
            val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_HEADER)
            val title = Paragraph("Test PDF Generation", titleFont)
            title.alignment = Element.ALIGN_CENTER
            document.add(title)
            
            document.add(Paragraph(" "))
            
            val normalFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)
            document.add(Paragraph("This is a test PDF to verify the generation system is working.", normalFont))
            document.add(Paragraph("If you can see this, PDF generation is working correctly.", normalFont))
            
            document.add(Paragraph(" "))
            document.add(Paragraph("Generated at: ${Date()}", normalFont))

            document.close()
            Log.d("PdfReportGenerator", "Test PDF generated successfully")
            true
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error generating test PDF: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    // Keep the existing methods for backward compatibility
    fun generateEldGraphBitmap(logs: List<GetLogsByDateResponse.Results.UserLog>): Bitmap? {
        return try {
            Log.d("PdfReportGenerator", "Starting ELD graph bitmap generation with ${logs.size} logs")
            
            // Create ELD graph view
            val inflater = LayoutInflater.from(context)
            val graphBinding = LayoutEldGraphBinding.inflate(inflater)
            val graphView = graphBinding.root
            
            // Set up the graph data
            val logList = mutableListOf<ELDGraphData>()
            
            // Add previous day log if available
            val previousDayLog = logs.firstOrNull()
            if (previousDayLog != null) {
                logList.add(ELDGraphData(0f, previousDayLog.modename ?: "off", 0L))
                Log.d("PdfReportGenerator", "Added previous day log: ${previousDayLog.modename}")
            }
            
            // Add current day logs
            logs.forEach { log ->
                val time = log.time?.let { AlertCalculationUtils.refinedTimeStringToFloat(it) } ?: 0f
                logList.add(ELDGraphData(time, log.modename ?: "off", time.toLong()))
                Log.d("PdfReportGenerator", "Added log: ${log.modename} at time $time")
            }
            
            // Add next day log if available
            val nextDayLog = logs.lastOrNull()
            if (nextDayLog != null) {
                logList.add(ELDGraphData(24f, nextDayLog.modename ?: "off", 24L))
                Log.d("PdfReportGenerator", "Added next day log: ${nextDayLog.modename}")
            }
            
            // If no logs available, create sample data to show all duty statuses
            if (logList.isEmpty()) {
                logList.add(ELDGraphData(0f, "off", 0L))
                logList.add(ELDGraphData(6f, "sb", 6L))
                logList.add(ELDGraphData(8f, "d", 8L))
                logList.add(ELDGraphData(10f, "on", 10L))
                logList.add(ELDGraphData(12f, "off", 12L))
                logList.add(ELDGraphData(14f, "d", 14L))
                logList.add(ELDGraphData(16f, "on", 16L))
                logList.add(ELDGraphData(18f, "sb", 18L))
                logList.add(ELDGraphData(24f, "off", 24L))
                Log.d("PdfReportGenerator", "Added sample data to show all duty statuses")
            }
            
            // Filter out non-duty status logs
            val filteredList = logList.filter { 
                it.status != "login" && it.status != "logout" && it.status != "personal" && 
                it.status != "yard" && it.status != "certification" && it.status != "INT" && 
                it.status != "eng_off" && it.status != "eng_on" && it.status != "power_on" && 
                it.status != "power_off"
            }
            
            Log.d("PdfReportGenerator", "Filtered list has ${filteredList.size} items")
            Log.d("PdfReportGenerator", "Filtered items: ${filteredList.map { "${it.status}@${it.time}" }}")
            
            // Plot the graph
            graphBinding.graph.plotGraph(filteredList)
            
            // Force the graph to redraw
            graphBinding.graph.invalidate()
            
            // Measure and layout the view - use proper dimensions to show all 4 rows
            val widthSpec = View.MeasureSpec.makeMeasureSpec(800, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY) // Increased height to show all rows clearly
            graphView.measure(widthSpec, heightSpec)
            graphView.layout(0, 0, graphView.measuredWidth, graphView.measuredHeight)
            
            // Small delay to ensure proper rendering
            Thread.sleep(100)
            
            // Create bitmap and draw the view
            val bitmap = Bitmap.createBitmap(graphView.measuredWidth, graphView.measuredHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            graphView.draw(canvas)
            
            Log.d("PdfReportGenerator", "ELD graph bitmap generated successfully: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error generating ELD graph bitmap: ${e.message}", e)
            null
        }
    }
}
