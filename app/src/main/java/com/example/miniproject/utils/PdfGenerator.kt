package com.example.miniproject.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.example.miniproject.R
import com.example.miniproject.model.Order
import com.example.miniproject.model.Vendor
import com.example.miniproject.screens.vendor.SalesData
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateSalesReport(
        context: Context,
        outputStream: OutputStream,
        vendor: Vendor,
        salesData: SalesData
    ) {
        val document = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        val headerPaint = Paint()

        //  SETUP STYLES
        titlePaint.color = Color.BLACK
        titlePaint.textSize = 24f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textAlign = Paint.Align.CENTER

        headerPaint.textSize = 14f
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        headerPaint.color = Color.DKGRAY

        paint.color = Color.BLACK
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT

        // A4 Dimensions
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f

        //  PAGE MANAGEMENT VARIABLES
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = 40f // Start slightly higher to fit logo

        //  DRAW LOGO
        try {

            val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)

            if (originalBitmap != null) {
                // Scale logo to 80x80 pixels (adjust as needed)
                val logoWidth = 80
                val logoHeight = 80
                val scaledLogo = Bitmap.createScaledBitmap(originalBitmap, logoWidth, logoHeight, true)

                // Draw centered
                canvas.drawBitmap(scaledLogo, (pageWidth / 2f) - (logoWidth / 2f), yPosition, paint)

                // Move Y position down after logo + padding
                yPosition += logoHeight + 20f
            }
        } catch (e: Exception) {
            e.printStackTrace()

        }



        // App Title & Report Name
        canvas.drawText("Tap-N-Chow", pageWidth / 2f, yPosition, titlePaint)
        yPosition += 30f

        val subTitlePaint = Paint(titlePaint)
        subTitlePaint.textSize = 18f
        canvas.drawText("Vendor Sales Report", pageWidth / 2f, yPosition, subTitlePaint)
        yPosition += 40f

        // Generation Date
        paint.textAlign = Paint.Align.RIGHT
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        canvas.drawText("Generated: ${dateFormat.format(Date())}", pageWidth - margin, yPosition, paint)
        paint.textAlign = Paint.Align.LEFT

        // Vendor Details
        canvas.drawLine(margin, yPosition + 10f, pageWidth - margin, yPosition + 10f, paint)
        yPosition += 40f

        canvas.drawText("Vendor Information:", margin, yPosition, headerPaint)
        yPosition += 20f
        canvas.drawText("Name: ${vendor.vendorName}", margin, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Category: ${vendor.category}", margin, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Contact: ${vendor.vendorContact}", margin, yPosition, paint)
        yPosition += 15f
        canvas.drawText("Email: ${vendor.email}", margin, yPosition, paint)

        yPosition += 40f

        // Financial Summary Box
        val boxTop = yPosition
        val boxHeight = 100f

        val boxPaint = Paint()
        boxPaint.color = Color.parseColor("#F0F0F0")
        canvas.drawRect(margin, boxTop, pageWidth - margin, boxTop + boxHeight, boxPaint)

        yPosition += 25f
        canvas.drawText("Financial Summary", margin + 10f, yPosition, headerPaint)

        yPosition += 30f
        canvas.drawText("Total Orders:", margin + 20f, yPosition, paint)
        canvas.drawText("${salesData.totalOrders}", margin + 120f, yPosition, headerPaint)

        canvas.drawText("Total Revenue (w/ Tax):", pageWidth / 2f, yPosition, paint)
        val revenueText = String.format(Locale.getDefault(), "RM %.2f", salesData.totalRevenueWithTax)
        canvas.drawText(revenueText, (pageWidth / 2f) + 140f, yPosition, headerPaint)

        yPosition += 25f
        canvas.drawText("Avg Order Value:", margin + 20f, yPosition, paint)
        val avgOrderText = String.format(Locale.getDefault(), "RM %.2f", salesData.averageOrderValueWithTax)
        canvas.drawText(avgOrderText, margin + 120f, yPosition, paint)

        canvas.drawText("Tax Collected:", pageWidth / 2f, yPosition, paint)
        val taxText = String.format(Locale.getDefault(), "RM %.2f", salesData.totalTax)
        canvas.drawText(taxText, (pageWidth / 2f) + 140f, yPosition, paint)

        yPosition = boxTop + boxHeight + 40f

        //  ORDER LIST HEADER
        canvas.drawText("Order History (All Orders)", margin, yPosition, headerPaint)
        yPosition += 20f

        // Define Column positions
        val col1 = margin
        val col2 = margin + 100f
        val col3 = margin + 250f
        val col4 = margin + 350f

        // Function to draw table header
        fun drawTableHeader(c: android.graphics.Canvas, y: Float) {
            val hPaint = Paint(headerPaint)
            hPaint.textSize = 11f
            c.drawText("Date", col1, y, hPaint)
            c.drawText("Order ID", col2, y, hPaint)
            c.drawText("Status", col3, y, hPaint)
            c.drawText("Amount", col4, y, hPaint)
            c.drawLine(margin, y + 5f, pageWidth - margin, y + 5f, paint)
        }

        drawTableHeader(canvas, yPosition)
        yPosition += 20f

        //  ITERATE THROUGH ALL ORDERS
        val allOrders = salesData.recentOrders // This contains ALL orders sorted by date
        paint.textSize = 10f

        for (order in allOrders) {
            // CHECK FOR PAGE BREAK
            if (yPosition > pageHeight - 50) {
                document.finishPage(page)

                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas

                yPosition = 50f
                drawTableHeader(canvas, yPosition)
                yPosition += 20f
            }

            // Draw Order Row
            val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(order.orderDate.toDate())

            canvas.drawText(dateStr, col1, yPosition, paint)
            canvas.drawText(order.getDisplayOrderId(), col2, yPosition, paint)

            // Status Coloring
            if (order.status.equals("completed", ignoreCase = true) ||
                order.status.equals("confirmed", ignoreCase = true)) {
                paint.color = Color.parseColor("#006400") // Dark Green
            } else if (order.status.equals("cancelled", ignoreCase = true)) {
                paint.color = Color.RED
            } else {
                paint.color = Color.BLACK
            }

            canvas.drawText(order.status.uppercase(), col3, yPosition, paint)
            paint.color = Color.BLACK // Reset color

            val orderTotalText = String.format(Locale.getDefault(), "RM %.2f", order.totalPrice)
            canvas.drawText(orderTotalText, col4, yPosition, paint)
            yPosition += 15f
        }

        document.finishPage(page)

        try {
            document.writeTo(outputStream)
            Toast.makeText(context, "Full Sales Report Saved", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
            outputStream.close()
        }
    }

    fun generatePlatformAnalyticsReport(
        context: Context,
        outputStream: OutputStream,
        vendors: List<Vendor>,
        orders: List<Order>,
        platformRevenueData: List<Double>,
        selectedTimeRange: String,
        totalRevenue: Double,
        platformRevenue: Double,
        vendorRevenue: Double,
        totalOrders: Int,
        activeVendors: Int,
        ordersByStatus: Map<String, Int>,
        topVendors: List<Pair<Vendor, Double>>
    ) {
        val document = PdfDocument()
        val paint = Paint()
        val titlePaint = Paint()
        val headerPaint = Paint()
        val subHeaderPaint = Paint()
        val moneyPaint = Paint()



        titlePaint.color = Color.BLACK
        titlePaint.textSize = 24f
        titlePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        titlePaint.textAlign = Paint.Align.CENTER


        headerPaint.textSize = 12f
        headerPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        headerPaint.color = Color.DKGRAY
        headerPaint.textAlign = Paint.Align.LEFT


        subHeaderPaint.textSize = 16f
        subHeaderPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        subHeaderPaint.color = Color.parseColor("#2196F3")
        subHeaderPaint.textAlign = Paint.Align.LEFT


        paint.color = Color.BLACK
        paint.textSize = 11f
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.LEFT


        moneyPaint.color = Color.BLACK
        moneyPaint.textSize = 11f
        moneyPaint.typeface = Typeface.DEFAULT
        moneyPaint.textAlign = Paint.Align.RIGHT


        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f


        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = 40f


        fun drawRowBackground(top: Float, height: Float) {
            val bgPaint = Paint()
            bgPaint.color = Color.parseColor("#F5F5F5")
            canvas.drawRect(margin, top - 10f, pageWidth - margin, top + height - 10f, bgPaint)
        }


        try {
            val originalBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.logo)
            if (originalBitmap != null) {
                val logoWidth = 60
                val logoHeight = 60
                val scaledLogo = Bitmap.createScaledBitmap(originalBitmap, logoWidth, logoHeight, true)

                canvas.drawBitmap(scaledLogo, (pageWidth / 2f) - (logoWidth / 2f), yPosition, paint)
                yPosition += logoHeight + 15f
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }


        canvas.drawText("Tap-N-Chow", pageWidth / 2f, yPosition, titlePaint)
        yPosition += 25f

        val reportTitlePaint = Paint(titlePaint)
        reportTitlePaint.textSize = 16f
        reportTitlePaint.typeface = Typeface.DEFAULT
        reportTitlePaint.color = Color.GRAY
        canvas.drawText("Platform Analytics Report", pageWidth / 2f, yPosition, reportTitlePaint)
        yPosition += 35f


        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        val dateStr = "Generated: ${dateFormat.format(Date())}"
        val rangeStr = "Time Range: ${selectedTimeRange.uppercase()}"


        val linePaint = Paint()
        linePaint.color = Color.LTGRAY
        linePaint.strokeWidth = 1f
        canvas.drawLine(margin, yPosition, pageWidth - margin, yPosition, linePaint)
        yPosition += 15f

        canvas.drawText(rangeStr, margin, yPosition, paint)

        val rightAlignPaint = Paint(paint)
        rightAlignPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(dateStr, pageWidth - margin, yPosition, rightAlignPaint)
        yPosition += 30f


        canvas.drawText("Key Platform Metrics", margin, yPosition, subHeaderPaint)
        yPosition += 20f


        val col1LabelX = margin + 10f
        val col1ValueX = (pageWidth / 2f) - 20f
        val col2LabelX = (pageWidth / 2f) + 20f
        val col2ValueX = pageWidth - margin - 10f


        val boxTop = yPosition - 10f
        val boxHeight = 70f
        val boxPaint = Paint()
        boxPaint.color = Color.parseColor("#F9F9F9")
        boxPaint.style = Paint.Style.FILL
        canvas.drawRect(margin, boxTop, pageWidth - margin, boxTop + boxHeight, boxPaint)


        boxPaint.style = Paint.Style.STROKE
        boxPaint.color = Color.LTGRAY
        canvas.drawRect(margin, boxTop, pageWidth - margin, boxTop + boxHeight, boxPaint)

        yPosition += 10f


        canvas.drawText("Total Revenue:", col1LabelX, yPosition, headerPaint)
        canvas.drawText(String.format(Locale.getDefault(), "RM %.2f", totalRevenue), col1ValueX, yPosition, moneyPaint)

        canvas.drawText("Platform Rev (10%):", col2LabelX, yPosition, headerPaint)
        canvas.drawText(String.format(Locale.getDefault(), "RM %.2f", platformRevenue), col2ValueX, yPosition, moneyPaint)

        yPosition += 25f


        canvas.drawText("Total Orders:", col1LabelX, yPosition, headerPaint)
        canvas.drawText(totalOrders.toString(), col1ValueX, yPosition, moneyPaint)

        canvas.drawText("Active Vendors:", col2LabelX, yPosition, headerPaint)
        canvas.drawText(activeVendors.toString(), col2ValueX, yPosition, moneyPaint)

        yPosition = boxTop + boxHeight + 35f

        // Order Status Distribution
        canvas.drawText("Order Status Distribution", margin, yPosition, subHeaderPaint)
        yPosition += 20f

        val tableCol1 = margin + 10f // Status
        val tableCol2 = margin + 250f // Count (Right Align target)
        val tableCol3 = pageWidth - margin - 10f // Percentage (Right Align target)


        val headerBgPaint = Paint()
        headerBgPaint.color = Color.parseColor("#E0E0E0")
        canvas.drawRect(margin, yPosition - 12f, pageWidth - margin, yPosition + 8f, headerBgPaint)


        canvas.drawText("Status", tableCol1, yPosition, headerPaint)

        val centerHeaderPaint = Paint(headerPaint)
        centerHeaderPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Count", tableCol2, yPosition, centerHeaderPaint)
        canvas.drawText("Percentage", tableCol3, yPosition, centerHeaderPaint)

        yPosition += 20f


        var rowIndex = 0
        ordersByStatus.entries.sortedByDescending { entry -> entry.value }.forEach { (status, count) ->

            if (rowIndex % 2 == 0) {
                drawRowBackground(yPosition, 20f)
            }

            val percentage = if (totalOrders > 0) (count.toDouble() / totalOrders) * 100 else 0.0

            canvas.drawText(status.replaceFirstChar { it.uppercase() }, tableCol1, yPosition, paint)
            canvas.drawText("$count", tableCol2, yPosition, moneyPaint)
            canvas.drawText(String.format(Locale.getDefault(), "%.1f%%", percentage), tableCol3, yPosition, moneyPaint)

            yPosition += 20f
            rowIndex++


            if (yPosition > pageHeight - 50) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }
        }


        canvas.drawLine(margin, yPosition - 5f, pageWidth - margin, yPosition - 5f, linePaint)
        yPosition += 30f

        // Top Performing Vendors


        if (yPosition > pageHeight - 150) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 50f
        }

        canvas.drawText("Top Performing Vendors", margin, yPosition, subHeaderPaint)
        yPosition += 20f


        val vCol1 = margin + 10f // Name
        val vCol2 = margin + 250f // Category
        val vCol3 = pageWidth - margin - 10f // Revenue (Right Align)

        canvas.drawRect(margin, yPosition - 12f, pageWidth - margin, yPosition + 8f, headerBgPaint)

        canvas.drawText("Vendor Name", vCol1, yPosition, headerPaint)
        canvas.drawText("Category", vCol2, yPosition, headerPaint)
        canvas.drawText("Revenue", vCol3, yPosition, centerHeaderPaint) // 使用右对齐的 Header Paint

        yPosition += 20f

        rowIndex = 0
        topVendors.forEachIndexed { index, (vendor, revenue) ->
            if (rowIndex % 2 == 0) {
                drawRowBackground(yPosition, 20f)
            }

            val rankPrefix = "${index + 1}. "
            canvas.drawText(rankPrefix + vendor.vendorName, vCol1, yPosition, paint)
            canvas.drawText(vendor.category.uppercase(), vCol2, yPosition, paint)
            canvas.drawText(String.format(Locale.getDefault(), "RM %.2f", revenue), vCol3, yPosition, moneyPaint)




            yPosition += 20f
            rowIndex++

            if (yPosition > pageHeight - 50) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPosition = 50f
            }
        }

        canvas.drawLine(margin, yPosition - 5f, pageWidth - margin, yPosition - 5f, linePaint)

        yPosition += 30f

        // Revenue Breakdown

        if (yPosition > pageHeight - 100) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPosition = 50f
        }


        val totalRev = platformRevenue + vendorRevenue
        val summaryBoxTop = yPosition

        canvas.drawText("Summary", margin, yPosition, subHeaderPaint)
        yPosition += 25f

        canvas.drawText("Total Revenue:", margin + 10f, yPosition, headerPaint)
        canvas.drawText(String.format(Locale.getDefault(), "RM %.2f", totalRev), pageWidth - margin - 10f, yPosition, moneyPaint)
        yPosition += 20f

        paint.color = Color.GRAY
        canvas.drawText(" * Includes both Platform commission and Vendor earnings.", margin + 10f, yPosition, paint)


        document.finishPage(page)

        try {
            document.writeTo(outputStream)
            Toast.makeText(context, "Analytics Report Saved Successfully", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_LONG).show()
        } finally {
            document.close()
            outputStream.close()
        }
    }
}