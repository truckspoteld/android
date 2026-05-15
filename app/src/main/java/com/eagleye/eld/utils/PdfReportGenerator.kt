package com.eagleye.eld.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Build
import android.view.View
import com.itextpdf.text.*
import com.itextpdf.text.pdf.*
import android.util.Log
import com.itextpdf.text.pdf.draw.LineSeparator
import com.eagleye.eld.models.GetLogsByDateResponse
import com.eagleye.eld.models.UserLog
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import com.eagleye.eld.models.GetCompanyById
import android.view.LayoutInflater
import com.eagleye.eld.R
import com.eagleye.eld.databinding.LayoutEldGraphBinding
import com.eagleye.eld.utils.AlertCalculationUtils

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

            // 1. Header: EaglEye | Title | Date
            addHeader(document, selectedDate)
            
            // 2. Merged company+driver info table (4 columns)
            addCompanyDriverInfo(document, reportData, companyInfo, driverName, driverLicense, vehicleVin)
            
            val timeZone = companyInfo?.company_timezone

            // 3. ELD Graph (before event table, matching reference layout)
            addEldGraph(document, reportData, timeZone)
            
            // 4. Event log table (6 columns with Comment)
            addEventLogData(document, reportData)
            
            // 5. Certification
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

    fun generateDriverDailyReportByDateRange(
        reportData: GetLogsByDateResponse,
        startDate: String,
        endDate: String,
        outputFile: File,
        companyInfo: GetCompanyById.Results? = null,
        driverName: String = "SUKHDEEP SINGH",
        driverLicense: String = "CA / Y46O8156",
        vehicleVin: String = "-"
    ): Boolean {
        return try {
            val allLogs = reportData.results.userLogs.orEmpty()
            if (allLogs.isEmpty()) {
                Log.w("PdfReportGenerator", "No logs found for date range report")
                return false
            }

            val start = parseYmd(startDate)
            val end = parseYmd(endDate)
            if (start == null || end == null) {
                Log.w("PdfReportGenerator", "Invalid date range for PDF: $startDate to $endDate")
                return false
            }

            // Build stable day key from date first, then created_on fallback.
            val logsWithDay = allLogs.mapNotNull { log ->
                val day = resolveLogDayKey(log)
                if (day == null) null else day to log
            }
            val logsByDate = logsWithDay.groupBy({ it.first }, { it.second })

            val availableDatesDesc = logsByDate.keys
                .mapNotNull { d -> parseYmd(d)?.let { d to it } }
                .filter { (_, day) -> !day.before(start) && !day.after(end) }
                .sortedByDescending { (_, day) -> day.time }
                .map { (d, _) -> d }

            if (availableDatesDesc.isEmpty()) {
                Log.w("PdfReportGenerator", "No available day logs in selected range, using single-page fallback")
                val fallbackDate = formatYmdToDisplay(endDate)
                return generateDriverDailyReport(
                    reportData = reportData,
                    selectedDate = fallbackDate,
                    outputFile = outputFile,
                    companyInfo = companyInfo,
                    driverName = driverName,
                    driverLicense = driverLicense,
                    vehicleVin = vehicleVin
                )
            }

            val document = Document(PageSize.A4, MARGIN, MARGIN, MARGIN, MARGIN)
            PdfWriter.getInstance(document, FileOutputStream(outputFile))
            document.open()

            // Build chronological order to compute previous-day ending log per date.
            val availableDatesChron = availableDatesDesc.reversed()
            val dutyModes = setOf("on", "off", "d", "sb")
            val previousDayLogByDate = mutableMapOf<String, GetLogsByDateResponse.Results.UserLog?>()
            var prevLastDutyLog: GetLogsByDateResponse.Results.UserLog? = null
            for (dateKey in availableDatesChron) {
                previousDayLogByDate[dateKey] = prevLastDutyLog
                val dayLogs = logsByDate[dateKey].orEmpty().sortedBy { parseTimeToSortable(it.time) }
                prevLastDutyLog = dayLogs.lastOrNull { it.modename?.lowercase() in dutyModes }
            }

            availableDatesDesc.forEachIndexed { index, dateKey ->
                val dayLogs = logsByDate[dateKey].orEmpty()
                    .sortedBy { parseTimeToSortable(it.time) }
                val prevDayLog = previousDayLogByDate[dateKey]
                val dayReportData = sliceReportDataForLogs(reportData, dayLogs, prevDayLog)
                val headerDate = formatYmdToDisplay(dateKey)

                if (index > 0) document.newPage()

                val timeZone = companyInfo?.company_timezone

                addHeader(document, headerDate)
                addCompanyDriverInfo(document, dayReportData, companyInfo, driverName, driverLicense, vehicleVin)
                addEldGraph(document, dayReportData, timeZone)
                addEventLogData(document, dayReportData)
                addCertification(document, dayReportData, driverName)
            }

            document.close()
            true
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error generating date range PDF: ${e.message}", e)
            // Final fallback to old single-page generator so paper-log email still works.
            try {
                generateDriverDailyReport(
                    reportData = reportData,
                    selectedDate = formatYmdToDisplay(endDate),
                    outputFile = outputFile,
                    companyInfo = companyInfo,
                    driverName = driverName,
                    driverLicense = driverLicense,
                    vehicleVin = vehicleVin
                )
            } catch (_: Exception) {
                false
            }
        }
    }

    private fun sliceReportDataForLogs(
        source: GetLogsByDateResponse,
        dayLogs: List<GetLogsByDateResponse.Results.UserLog>,
        previousDayLog: GetLogsByDateResponse.Results.UserLog? = null
    ): GetLogsByDateResponse {
        val last = dayLogs.lastOrNull()
        return source.copy(
            results = source.results.copy(
                totalCount = dayLogs.size,
                userLogs = dayLogs,
                previousDayLog = previousDayLog,
                nextDayLog = last,
                latestUpdatedLog = last
            )
        )
    }

    private fun parseYmd(value: String): Date? {
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            fmt.parse(value)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatYmdToDisplay(value: String): String {
        return try {
            val inFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply { isLenient = false }
            val outFmt = SimpleDateFormat("MMM dd, yyyy", Locale.US)
            val d = inFmt.parse(value)
            if (d != null) outFmt.format(d) else value
        } catch (_: Exception) {
            value
        }
    }

    private fun parseTimeToSortable(value: String?): Int {
        if (value.isNullOrBlank()) return Int.MAX_VALUE
        val parts = value.split(":")
        if (parts.size < 2) return Int.MAX_VALUE
        val h = parts[0].toIntOrNull() ?: return Int.MAX_VALUE
        val m = parts[1].toIntOrNull() ?: 0
        val s = if (parts.size > 2) (parts[2].toIntOrNull() ?: 0) else 0
        return (h * 3600) + (m * 60) + s
    }

    private fun resolveLogDayKey(log: GetLogsByDateResponse.Results.UserLog): String? {
        val fromDate = log.date.trim()
        if (parseYmd(fromDate) != null) return fromDate

        val createdOn = log.created_on.trim()
        if (createdOn.isNotBlank()) {
            // Handle values like "2026-03-27T..." and "2026-03-27 ..."
            val ymd = createdOn.take(10)
            if (parseYmd(ymd) != null) return ymd
        }
        return null
    }

    private fun addHeader(document: Document, selectedDate: String) {
        val headerTable = PdfPTable(3)
        headerTable.widthPercentage = 100f
        headerTable.setWidths(floatArrayOf(20f, 60f, 20f))
        
        // Left: EaglEye brand name
        val brandFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14f)
        val logoCell = PdfPCell(Phrase("TruckSpot", brandFont))
        logoCell.border = Rectangle.NO_BORDER
        logoCell.horizontalAlignment = Element.ALIGN_LEFT
        logoCell.verticalAlignment = Element.ALIGN_MIDDLE
        logoCell.setPadding(10f)
        headerTable.addCell(logoCell)
        
        // Center: empty (title will go below)
        val emptyCell = PdfPCell(Phrase(""))
        emptyCell.border = Rectangle.NO_BORDER
        headerTable.addCell(emptyCell)
        
        // Right: Date
        val dateFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
        val dateCell = PdfPCell(Phrase(selectedDate, dateFont))
        dateCell.border = Rectangle.NO_BORDER
        dateCell.horizontalAlignment = Element.ALIGN_RIGHT
        dateCell.verticalAlignment = Element.ALIGN_MIDDLE
        dateCell.setPadding(10f)
        headerTable.addCell(dateCell)
        
        document.add(headerTable)
        
        // Title centered below the header row
        val titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16f)
        val title = Paragraph("Driver's Daily Report", titleFont)
        title.alignment = Element.ALIGN_CENTER
        title.spacingAfter = 2f
        document.add(title)
        
        val subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11f)
        val subtitle = Paragraph("US 8 days 70 Hours Ruleset", subtitleFont)
        subtitle.alignment = Element.ALIGN_CENTER
        subtitle.spacingAfter = 6f
        document.add(subtitle)
    }

    private fun addCompanyDriverInfo(document: Document, reportData: GetLogsByDateResponse, companyInfo: GetCompanyById.Results?, driverName: String, driverLicense: String, vehicleVin: String) {
        val labelFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_NORMAL)
        val valueFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)
        val borderColor = BaseColor(220, 220, 220)
        
        // 4-column table: Label | Value | Label | Value
        val table = PdfPTable(4)
        table.widthPercentage = 100f
        table.setWidths(floatArrayOf(20f, 30f, 20f, 30f))
        
        fun addRow(label1: String, value1: String, label2: String, value2: String) {
            val lc1 = PdfPCell(Phrase(label1, labelFont))
            lc1.setPadding(6f); lc1.border = Rectangle.BOX; lc1.borderColor = borderColor
            table.addCell(lc1)
            
            val vc1 = PdfPCell(Phrase(value1, valueFont))
            vc1.setPadding(6f); vc1.border = Rectangle.BOX; vc1.borderColor = borderColor
            table.addCell(vc1)
            
            val lc2 = PdfPCell(Phrase(label2, labelFont))
            lc2.setPadding(6f); lc2.border = Rectangle.BOX; lc2.borderColor = borderColor
            table.addCell(lc2)
            
            val vc2 = PdfPCell(Phrase(value2, valueFont))
            vc2.setPadding(6f); vc2.border = Rectangle.BOX; vc2.borderColor = borderColor
            table.addCell(vc2)
        }
        
        val firstLog = reportData.results?.userLogs?.firstOrNull { !it.vin.isNullOrBlank() || !it.powerunitnumber.isNullOrBlank() }
            ?: reportData.results?.userLogs?.firstOrNull()
        val driverId = firstLog?.driverid?.toString() ?: "-"
        val coDriverId = firstLog?.codriverid?.takeIf { it > 0 }?.toString() ?: "-"
        val vin = firstLog?.vin?.takeIf { it.isNotBlank() } ?: vehicleVin
        val unitNumber = firstLog?.powerunitnumber?.trim()?.takeIf { it.isNotBlank() } ?: "-"
        val vinAndUnit = "$vin / $unitNumber"

        addRow("Carrier", companyInfo?.company_name ?: "-",
               "Driver Name / ID", "$driverName / $driverId")
        addRow("DOT Number", companyInfo?.dot_no ?: "-",
               "Co-Driver Name / ID", coDriverId)
        addRow("Office Address", companyInfo?.address ?: "-",
               "License State / #", driverLicense)
        addRow("Terminal Timezone", companyInfo?.company_timezone ?: "-",
               "Vehicle VIN / #", vinAndUnit)
        
        document.add(table)
        document.add(Paragraph("\n", FontFactory.getFont(FontFactory.HELVETICA, 4f)))
    }

    private fun addEventLogData(document: Document, reportData: GetLogsByDateResponse) {
        val headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_SMALL)
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_SMALL)
        
        val logs = (reportData.results?.userLogs ?: emptyList())
            .sortedBy { it.datetime.ifBlank { "${it.date} ${it.time}" } }
        if (logs.isNotEmpty()) {
            val originMap = mapOf("1" to "AUTO", "2" to "DRIVER", "3" to "ASSUMED", "4" to "ADMIN")
            // 8-column table matching portal/iOS layout
            val table = PdfPTable(8)
            table.widthPercentage = 100f
            table.setWidths(floatArrayOf(14f, 8f, 11f, 12f, 11f, 22f, 14f, 8f))

            val headers = arrayOf("Event Status", "Origin", "Time", "Engine Hours", "Odometer", "Location", "Annotation", "M/D")
            headers.forEach { header ->
                val cell = PdfPCell(Phrase(header, headerFont))
                cell.backgroundColor = BaseColor(242, 242, 242)
                cell.horizontalAlignment = Element.ALIGN_CENTER
                cell.verticalAlignment = Element.ALIGN_MIDDLE
                cell.setPaddingTop(5f)
                cell.setPaddingBottom(5f)
                cell.border = Rectangle.NO_BORDER
                table.addCell(cell)
            }

            logs.forEachIndexed { index, log ->
                val isAlternate = index % 2 != 0
                val bgColor = if (isAlternate) BaseColor(250, 250, 250) else BaseColor.WHITE

                fun makeCell(text: String): PdfPCell {
                    val c = PdfPCell(Phrase(text, normalFont))
                    c.backgroundColor = bgColor
                    c.horizontalAlignment = Element.ALIGN_CENTER
                    c.verticalAlignment = Element.ALIGN_MIDDLE
                    c.setPaddingTop(4f)
                    c.setPaddingBottom(4f)
                    c.border = Rectangle.NO_BORDER
                    return c
                }

                val normalizedMode = log.modename.trim().lowercase()
                val statusText = when {
                    normalizedMode == "personal" || log.discreption?.toString()?.trim()?.lowercase() == "personal" -> "Off Duty (PC)"
                    normalizedMode == "yard" || log.discreption?.toString()?.trim()?.lowercase() == "yard" -> "On Duty (YM)"
                    else -> log.modename.uppercase()
                }
                val originKey = log.eventrecordorigin?.toString()?.trim() ?: ""
                val originText = originMap[originKey] ?: ""
                val annotation = log.discreption?.toString()?.trim()
                    ?.takeIf { it.isNotEmpty() && !it.equals("Intermediate log", ignoreCase = true) } ?: ""
                val hasMalfunction = (log.malfunctioneld ?: 0) != 0
                val hasDiagnostic = (log.datadiagnostic ?: 0) != 0
                val mdText = when {
                    hasMalfunction && hasDiagnostic -> "M/D"
                    hasMalfunction -> "M"
                    hasDiagnostic -> "D"
                    else -> ""
                }

                table.addCell(makeCell(statusText))
                table.addCell(makeCell(originText))
                table.addCell(makeCell(log.time ?: ""))
                table.addCell(makeCell(log.eng_hours ?: ""))
                table.addCell(makeCell(log.odometerreading ?: ""))
                table.addCell(makeCell(log.location ?: ""))
                table.addCell(makeCell(annotation))
                table.addCell(makeCell(mdText))
            }
            
            document.add(table)
        } else {
            document.add(Paragraph("No event logs available for the selected date.", normalFont))
        }
        
        document.add(Paragraph("\n", FontFactory.getFont(FontFactory.HELVETICA, 4f)))
    }
    private fun addEldGraph(document: Document, reportData: GetLogsByDateResponse, timeZone: String?) {
        try {
            // Generate ELD graph bitmap using the app's own graph widget
            val graphBitmap = generateEldGraphBitmap(reportData, timeZone)
            
            if (graphBitmap != null) {
                val stream = java.io.ByteArrayOutputStream()
                graphBitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val image = Image.getInstance(stream.toByteArray())
                
                // Scale to fit page width
                val pageWidth = PageSize.A4.width - (MARGIN * 2)
                val scale = pageWidth / graphBitmap.width.toFloat()
                image.scaleAbsolute(graphBitmap.width * scale, graphBitmap.height * scale)
                image.alignment = Element.ALIGN_CENTER
                
                document.add(image)
                document.add(Paragraph("\n", FontFactory.getFont(FontFactory.HELVETICA, 4f)))
                
                Log.d("PdfReportGenerator", "ELD graph added successfully")
            } else {
                Log.w("PdfReportGenerator", "Failed to generate ELD graph bitmap")
                document.add(Paragraph("ELD graph could not be generated.", FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)))
            }
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error adding ELD graph: ${e.message}", e)
        }
    }

    private fun addCertification(document: Document, reportData: GetLogsByDateResponse, driverName: String) {
        val normalFont = FontFactory.getFont(FontFactory.HELVETICA, FONT_SIZE_NORMAL)
        val boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, FONT_SIZE_NORMAL)
        
        document.add(Paragraph("\n", FontFactory.getFont(FontFactory.HELVETICA, 4f)))
        
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
    fun generateEldGraphBitmap(reportData: GetLogsByDateResponse, timeZone: String?): Bitmap? {
        return try {
            val logs = (reportData.results?.userLogs ?: emptyList())
                .sortedBy { it.datetime.ifBlank { "${it.date} ${it.time}" } }
            Log.d("PdfReportGenerator", "Starting ELD graph bitmap generation with ${logs.size} logs")
            
            // Create ELD graph view
            val inflater = LayoutInflater.from(context)
            val graphBinding = LayoutEldGraphBinding.inflate(inflater)
            val graphView = graphBinding.root
            
            // Show legend pills
            graphBinding.graphTitle.visibility = View.GONE
            graphBinding.graphSubtitle.visibility = View.GONE
            graphBinding.legendContainer.visibility = View.VISIBLE
            
            // Set white background for PDF rendering
            graphView.setBackgroundColor(Color.WHITE)
            
            // Set up the graph data using robust logic from LogsFragment
            val logList = mutableListOf<ELDGraphData>()
            
            fun addGraphPoint(time: Float, modename: String?) {
                if (modename.isNullOrBlank()) return
                val status = modename.trim().lowercase()
                val normalizedStatus = when (status) {
                    "dr" -> "d"
                    "personal" -> "off"
                    "yard" -> "on"
                    "e_on", "power_on" -> "eng_on"
                    "e_off", "power_off" -> "eng_off"
                    else -> status
                }
                if (normalizedStatus in setOf("on", "off", "d", "sb")) {
                    logList.add(ELDGraphData(time, normalizedStatus, time.toLong()))
                }
            }

            if (reportData.results?.previousDayLog?.modename != null) {
                addGraphPoint(0f, reportData.results?.previousDayLog?.modename)
            }
            
            logs.forEach { log ->
                val time = log.time?.let { t -> AlertCalculationUtils.refinedTimeStringToFloat(t) } ?: 0f
                addGraphPoint(time, log.modename)
            }
            
            var isViewingToday = false
            var nowFloat = 24f
            if (!timeZone.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val todayStr = AlertCalculationUtils.getCurrentDateInTimezone(timeZone)
                val reportDate = logs.firstOrNull()?.date ?: ""
                isViewingToday = (reportDate == todayStr)
                if (isViewingToday) {
                    nowFloat = AlertCalculationUtils.getCurrentTimeAsFloatInTimezone(timeZone)
                }
            }
            
            val nextModename = reportData.results?.nextDayLog?.modename
            val nextModeIsDuty = nextModename?.let { m ->
                val normalized = when (m.trim().lowercase()) {
                    "dr" -> "d"
                    "personal" -> "off"
                    "yard" -> "on"
                    else -> m.trim().lowercase()
                }
                normalized in setOf("on", "off", "d", "sb")
            } == true

            if (nextModeIsDuty) {
                addGraphPoint(24f, nextModename)
            } else {
                val dutyStatuses = setOf("on", "off", "d", "sb")
                val lastDutyStatus = logList.lastOrNull { it.status?.lowercase() in dutyStatuses }?.status?.lowercase()
                if (logList.isNotEmpty() && lastDutyStatus != null) {
                    val endFloat = if (isViewingToday) nowFloat else 24f
                    val lastPoint = logList.last()
                    if (lastPoint.time < endFloat) {
                        logList.add(ELDGraphData(endFloat, lastDutyStatus, endFloat.toLong()))
                    }
                }
            }
            
            // If no logs available, create sample data
            if (logList.isEmpty()) {
                logList.add(ELDGraphData(0f, "off", 0L))
                logList.add(ELDGraphData(24f, "off", 24L))
            }
            
            // Filter out events that ELDGraph doesn't render lines for
            val filteredList = logList.filter { 
                it.status != "login" && it.status != "logout" &&
                it.status != "certification" && it.status != "INT" && 
                it.status != "eng_off" && it.status != "eng_on" && it.status != "power_on" && 
                it.status != "power_off"
            }
            
            Log.d("PdfReportGenerator", "Filtered list has ${filteredList.size} items")
            
            // Plot the graph data
            val meta = reportData.results?.meta
            val off = meta?.off ?: 0
            val sb = meta?.sb ?: 0
            val d = meta?.d ?: 0
            val on = meta?.on ?: 0
            
            fun formatTimeFromSeconds(totalSeconds: Int): String {
                if (totalSeconds < 0) return "00:00"
                val hours = totalSeconds / 3600
                val remainingSeconds = totalSeconds % 3600
                val minutes = remainingSeconds / 60
                return String.format("%02d:%02d", hours, minutes)
            }
            
            graphBinding.offDutyHours.text = formatTimeFromSeconds(off)
            graphBinding.sbHours.text = formatTimeFromSeconds(sb)
            graphBinding.drivingHours.text = formatTimeFromSeconds(d)
            graphBinding.onDutyHours.text = formatTimeFromSeconds(on)
            val totalSec = off + sb + d + on
            graphBinding.totalHours.text = formatTimeFromSeconds(totalSec)
            
            graphBinding.graph.plotGraph(filteredList, off, sb, d, on)
            
            // Use exact width, but let the view determine its required height
            val bitmapWidth = 1200
            
            val widthSpec = View.MeasureSpec.makeMeasureSpec(bitmapWidth, View.MeasureSpec.EXACTLY)
            val wrapHeightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            
            // First measure pass to get required height
            graphView.measure(widthSpec, wrapHeightSpec)
            val bitmapHeight = Math.max(graphView.measuredHeight, 200) // Ensure a minimum height
            
            val exactHeightSpec = View.MeasureSpec.makeMeasureSpec(bitmapHeight, View.MeasureSpec.EXACTLY)
            
            graphView.measure(widthSpec, exactHeightSpec)
            graphView.layout(0, 0, graphView.measuredWidth, graphView.measuredHeight)
            
            // Force the graph custom view to redraw after layout
            graphBinding.graph.invalidate()
            graphBinding.graph.requestLayout()
            
            // Re-measure and re-layout to pick up redrawn state
            graphView.measure(widthSpec, exactHeightSpec)
            graphView.layout(0, 0, graphView.measuredWidth, graphView.measuredHeight)
            
            // Create bitmap and draw
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE) // Ensure white background
            graphView.draw(canvas)
            
            Log.d("PdfReportGenerator", "ELD graph bitmap generated successfully: ${bitmap.width}x${bitmap.height}")
            bitmap
        } catch (e: Exception) {
            Log.e("PdfReportGenerator", "Error generating ELD graph bitmap: ${e.message}", e)
            null
        }
    }
}
