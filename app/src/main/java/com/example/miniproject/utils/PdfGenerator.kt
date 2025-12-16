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
import com.example.miniproject.model.Vendor
import com.example.miniproject.screens.SalesData
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

        // --- SETUP STYLES ---
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

        // --- PAGE MANAGEMENT VARIABLES ---
        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var yPosition = 40f // Start slightly higher to fit logo

        // --- DRAW LOGO ---
        try {
            // REPLACE 'R.drawable.logo' with your actual image filename
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
            // If logo fails, just skip it and continue
        }

        // --- DRAW FIRST PAGE HEADER (Vendor Info & Summary) ---

        // 1. App Title & Report Name
        canvas.drawText("Tap-N-Chow", pageWidth / 2f, yPosition, titlePaint)
        yPosition += 30f

        val subTitlePaint = Paint(titlePaint)
        subTitlePaint.textSize = 18f
        canvas.drawText("Vendor Sales Report", pageWidth / 2f, yPosition, subTitlePaint)
        yPosition += 40f

        // 2. Generation Date
        paint.textAlign = Paint.Align.RIGHT
        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        canvas.drawText("Generated: ${dateFormat.format(Date())}", pageWidth - margin, yPosition, paint)
        paint.textAlign = Paint.Align.LEFT

        // 3. Vendor Details
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

        // 4. Financial Summary Box
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
        canvas.drawText("RM %.2f".format(salesData.totalRevenueWithTax), (pageWidth / 2f) + 140f, yPosition, headerPaint)

        yPosition += 25f
        canvas.drawText("Avg Order Value:", margin + 20f, yPosition, paint)
        canvas.drawText("RM %.2f".format(salesData.averageOrderValueWithTax), margin + 120f, yPosition, paint)

        canvas.drawText("Tax Collected:", pageWidth / 2f, yPosition, paint)
        canvas.drawText("RM %.2f".format(salesData.totalTax), (pageWidth / 2f) + 140f, yPosition, paint)

        yPosition = boxTop + boxHeight + 40f

        // --- ORDER LIST HEADER ---
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

        // --- ITERATE THROUGH ALL ORDERS ---
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

            canvas.drawText("RM %.2f".format(order.totalPrice), col4, yPosition, paint)

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
}