package com.example.miniproject.screens.vendor

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Order
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.util.OrderStatusHelper
import com.example.miniproject.utils.PdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesAnalyticsScreen(navController: NavController) {
    val context = LocalContext.current
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    // State for Data
    var salesData by remember { mutableStateOf<SalesData?>(null) }
    var currentVendor by remember { mutableStateOf<Vendor?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }


    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val outputStream = context.contentResolver.openOutputStream(uri)
                        if (outputStream != null && salesData != null && currentVendor != null) {
                            PdfGenerator.generateSalesReport(
                                context,
                                outputStream,
                                currentVendor!!,
                                salesData!!
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun initiatePdfDownload() {
        if (salesData == null || currentVendor == null) return

        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val fileName = "Sales_Report_$timeStamp.pdf"

        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, fileName)
        }
        createDocumentLauncher.launch(intent)
    }

    // Function to load sales data
    fun loadSalesData() {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                val vendor = authService.getCurrentVendor()
                if (vendor == null) {
                    errorMessage = "Vendor not found"
                    return@launch
                }
                currentVendor = vendor // Save vendor to state

                val allOrders = databaseService.getAllOrders()
                val vendorSalesData = calculateVendorSalesData(vendor.vendorId, allOrders, databaseService)
                salesData = vendorSalesData
            } catch (e: Exception) {
                errorMessage = "Failed to load sales data: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    // Load data on startup
    LaunchedEffect(Unit) {
        loadSalesData()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sales Analytics") },

                navigationIcon = {
                    IconButton(
                        onClick = {

                            val currentTime = System.currentTimeMillis()
                            // Only allow click if 500ms have passed since the last click
                            if (currentTime - lastBackClickTime > 500) {
                                lastBackClickTime = currentTime
                                navController.popBackStack()
                            }
                        }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Only show download button if data is loaded
                    if (!isLoading && salesData != null) {
                        IconButton(onClick = { initiatePdfDownload() }) {
                            Icon(
                                imageVector = Icons.Default.Download,
                                contentDescription = "Download Report",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFA500), // Orange Background
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Error Loading Data",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.error
                    )
                    Text(
                        text = errorMessage ?: "Unknown error",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(onClick = { loadSalesData() }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            SalesAnalyticsContent(
                salesData = salesData,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            )
        }
    }
}

@Composable
fun SalesAnalyticsContent(
    salesData: SalesData?,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                "Sales Overview",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
        }

        // Key Metrics Cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total Revenue Card
                MetricCard(
                    title = "Total Revenue",
                    value = "RM${"%.2f".format(salesData?.totalRevenueWithTax ?: 0.0)}",
                    subtitle = "RM${"%.2f".format(salesData?.totalRevenue ?: 0.0)} + RM${"%.2f".format(salesData?.totalTax ?: 0.0)} tax",
                    icon = Icons.Default.MonetizationOn,
                    color = colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                // Total Orders Card
                MetricCard(
                    title = "Total Orders",
                    value = "${salesData?.totalOrders ?: 0}",
                    icon = Icons.Default.Receipt,
                    color = colorScheme.secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Average Order Value
        item {
            MetricCard(
                title = "Average Order Value",
                value = "RM${"%.2f".format(salesData?.averageOrderValueWithTax ?: 0.0)}",
                subtitle = "RM${"%.2f".format(salesData?.averageOrderValue ?: 0.0)} + RM${"%.2f".format(salesData?.averageTax ?: 0.0)} tax",
                icon = Icons.Default.BarChart,
                color = colorScheme.tertiary,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Sales Trend Graph
        item {
            SalesTrendGraph(salesData = salesData)
        }

        // Order Status Breakdown
        item {
            OrderStatusBreakdownCard(salesData = salesData)
        }

        // Recent Orders
        item {
            RecentOrdersCard(salesData = salesData)
        }

        // Monthly Revenue (if available)
        item {
            MonthlyRevenueCard(salesData = salesData)
        }
    }
}

// Helper Composables & Data Logic

@Composable
fun MetricCard(
    title: String,
    value: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = title,
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SalesTrendGraph(salesData: SalesData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Sales Trend (Last 7 Days)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val dailySales = salesData?.dailyRevenueWithTax ?: emptyMap()

            if (dailySales.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No sales data available for the last 7 days",
                        fontSize = 14.sp,
                        color = colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                // Calculate trend
                val salesValues = dailySales.values.toList()
                val trend = calculateTrend(salesValues)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Trend:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = when {
                            trend > 0.1 -> "📈 Increasing"
                            trend < -0.1 -> "📉 Decreasing"
                            else -> "➡️ Stable"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            trend > 0.1 -> Color(0xFF4CAF50) // Green
                            trend < -0.1 -> Color(0xFFF44336) // Red
                            else -> colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Line Graph
                LineChart(
                    data = dailySales,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun LineChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val values = data.values.toList()
    val keys = data.keys.toList()

    if (values.isEmpty() || keys.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("No data available")
        }
        return
    }

    val maxValue = values.maxOrNull() ?: 0.0
    val minValue = values.minOrNull() ?: 0.0
    val valueRange = maxValue - minValue

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val padding = 32.dp.toPx()

            // Draw grid lines
            for (i in 0..4) {
                val y = padding + (height - 2 * padding) * i / 4
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = androidx.compose.ui.geometry.Offset(padding, y),
                    end = androidx.compose.ui.geometry.Offset(width - padding, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw data points and line
            val path = Path()
            val pointRadius = 4.dp.toPx()

            values.forEachIndexed { index, value ->
                val x = padding + (width - 2 * padding) * index / (values.size - 1)
                val y = if (valueRange > 0) {
                    height - padding - ((value - minValue) / valueRange * (height - 2 * padding))
                } else {
                    height / 2
                }

                // Draw data point
                drawCircle(
                    color = Color.Blue,
                    radius = pointRadius,
                    center = androidx.compose.ui.geometry.Offset(x, y.toFloat())
                )

                // Add to path
                if (index == 0) {
                    path.moveTo(x, y.toFloat())
                } else {
                    path.lineTo(x, y.toFloat())
                }
            }

            // Draw line
            drawPath(
                path = path,
                color = Color.Blue,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // Add labels separately using Compose Text
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top labels for values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                values.forEachIndexed { _, value ->
                    Text(
                        text = "RM${"%.0f".format(value)}",
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Bottom labels for dates
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                keys.forEach { date ->
                    Text(
                        text = date.substringAfter('/'), // Show only day part
                        fontSize = 10.sp,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun calculateTrend(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val firstHalf = values.take(values.size / 2).average()
    val secondHalf = values.takeLast(values.size / 2).average()
    return if (firstHalf != 0.0) (secondHalf - firstHalf) / firstHalf else 0.0
}

@Composable
fun OrderStatusBreakdownCard(salesData: SalesData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Order Status Breakdown",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val statusCounts = salesData?.orderStatusCounts ?: emptyMap()
            val totalOrders = salesData?.totalOrders ?: 1

            if (statusCounts.isEmpty()) {
                Text(
                    "No order data available",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                statusCounts.entries.sortedByDescending { it.value }.forEach { (status, count) ->
                    val percentage = if (totalOrders > 0) (count.toDouble() / totalOrders * 100) else 0.0

                    OrderStatusItem(
                        status = status,
                        count = count,
                        percentage = percentage,
                        color = OrderStatusHelper.getStatusColor(status)
                    )
                }
            }
        }
    }
}

@Composable
fun OrderStatusItem(status: String, count: Int, percentage: Double, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(color = color, shape = RoundedCornerShape(4.dp))
            )

            Column {
                Text(
                    text = OrderStatusHelper.getStatusDisplayText(status),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = "$count orders",
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "%.1f%%".format(percentage),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onSurface
        )
    }
}

@Composable
fun RecentOrdersCard(salesData: SalesData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Recent Orders",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val recentOrders = salesData?.recentOrders?.take(5) ?: emptyList()

            if (recentOrders.isEmpty()) {
                Text(
                    "No recent orders",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                recentOrders.forEach { order ->
                    RecentOrderItem(order = order)
                }
            }
        }
    }
}

@Composable
fun RecentOrderItem(order: Order) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Order #${order.orderId}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(order.orderDate.toDate()),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "RM${"%.2f".format(order.totalPrice)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary
                )
                OrderStatusBadge(status = order.status)
            }
        }
    }
}

@Composable
fun OrderStatusBadge(status: String) {
    val statusColor = OrderStatusHelper.getStatusColor(status)

    Box(
        modifier = Modifier
            .background(
                color = statusColor.copy(alpha = 0.2f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = OrderStatusHelper.getStatusDisplayText(status),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = statusColor
        )
    }
}

@Composable
fun MonthlyRevenueCard(salesData: SalesData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                "Monthly Revenue",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val monthlyRevenue = salesData?.monthlyRevenueWithTax ?: emptyMap()

            if (monthlyRevenue.isEmpty()) {
                Text(
                    "No monthly revenue data available",
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                monthlyRevenue.entries.sortedByDescending { it.key }.forEach { (month, revenue) ->
                    MonthlyRevenueItem(month = month, revenue = revenue)
                }
            }
        }
    }
}

@Composable
fun MonthlyRevenueItem(month: String, revenue: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = month,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = colorScheme.onSurface
        )
        Text(
            text = "RM${"%.2f".format(revenue)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.primary
        )
    }
}

// Data class for sales analytics
data class SalesData(
    val totalRevenue: Double,
    val totalTax: Double,
    val totalRevenueWithTax: Double,
    val totalOrders: Int,
    val averageOrderValue: Double,
    val averageTax: Double,
    val averageOrderValueWithTax: Double,
    val orderStatusCounts: Map<String, Int>,
    val recentOrders: List<Order>,
    val monthlyRevenue: Map<String, Double>,
    val monthlyRevenueWithTax: Map<String, Double>,
    val dailyRevenueWithTax: Map<String, Double>
)

// Helper function to calculate vendor sales data

private suspend fun calculateVendorSalesData(
    vendorId: String,
    vendorOrders: List<Order>,
    databaseService: DatabaseService
): SalesData {


    val myProducts = databaseService.getProductsByVendor(vendorId)
    val myProductIds = myProducts.map { it.productId }.toSet() // 轉成 Set 查詢速度最快

    val filteredOrders = mutableListOf<Order>()


    var totalRevenue = 0.0
    var totalTax = 0.0
    var validOrdersCount = 0

    val orderStatusCounts = mutableMapOf<String, Int>()
    val monthlyRevenue = mutableMapOf<String, Double>()
    val monthlyRevenueWithTax = mutableMapOf<String, Double>()
    val dailyRevenue = mutableMapOf<String, Double>()


    val calendar = Calendar.getInstance()
    val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val shortDateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())


    for (i in 6 downTo 0) {
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -i)
        val dateKey = shortDateFormat.format(calendar.time)
        dailyRevenue[dateKey] = 0.0
    }


    for (order in vendorOrders) {

        val orderDetails = databaseService.getOrderDetails(order.orderId)

        var vendorOrderTotal = 0.0
        var vendorOrderTax = 0.0
        var hasMyItems = false


        for (detail in orderDetails) {
            if (myProductIds.contains(detail.productId)) {
                val subtotal = detail.subtotal
                val tax = subtotal * 0.06 // 6% tax
                vendorOrderTotal += subtotal
                vendorOrderTax += tax
                hasMyItems = true
            }
        }

        if (hasMyItems && vendorOrderTotal >= 0) { // Safety check
            val vendorOrderWithTax = vendorOrderTotal + vendorOrderTax

            // Add to list for "Recent Orders"
            val vendorOrder = order.copy(totalPrice = vendorOrderWithTax)
            filteredOrders.add(vendorOrder)

            // Update status counts
            orderStatusCounts[order.status] = orderStatusCounts.getOrDefault(order.status, 0) + 1

            // FILTERING LOGIC
            if (!order.status.equals("cancelled", ignoreCase = true)) {
                totalRevenue += vendorOrderTotal
                totalTax += vendorOrderTax
                validOrdersCount++

                // Update monthly revenue
                val monthYear = monthFormat.format(order.orderDate.toDate())
                monthlyRevenue[monthYear] = monthlyRevenue.getOrDefault(monthYear, 0.0) + vendorOrderTotal
                monthlyRevenueWithTax[monthYear] = monthlyRevenueWithTax.getOrDefault(monthYear, 0.0) + vendorOrderWithTax

                // Update daily revenue
                val calendarToday = Calendar.getInstance().apply { time = Date() }
                calendarToday.add(Calendar.DAY_OF_YEAR, -6)
                // Reset hour/min/sec for correct date comparison
                calendarToday.set(Calendar.HOUR_OF_DAY, 0)
                calendarToday.set(Calendar.MINUTE, 0)

                if (order.orderDate.toDate().after(calendarToday.time)) {
                    val dayKey = shortDateFormat.format(order.orderDate.toDate())
                    // Only update if the key exists (meaning it's within the last 7 days we initialized)
                    if (dailyRevenue.containsKey(dayKey)) {
                        dailyRevenue[dayKey] = dailyRevenue.getOrDefault(dayKey, 0.0) + vendorOrderWithTax
                    }
                }
            }
        }
    }

    val totalRevenueWithTax = totalRevenue + totalTax

    val averageOrderValue = if (validOrdersCount > 0) totalRevenue / validOrdersCount else 0.0
    val averageTax = if (validOrdersCount > 0) totalTax / validOrdersCount else 0.0
    val averageOrderValueWithTax = if (validOrdersCount > 0) totalRevenueWithTax / validOrdersCount else 0.0

    val recentOrders = filteredOrders.sortedByDescending { it.orderDate.seconds }

    return SalesData(
        totalRevenue = totalRevenue,
        totalTax = totalTax,
        totalRevenueWithTax = totalRevenueWithTax,
        totalOrders = validOrdersCount,
        averageOrderValue = averageOrderValue,
        averageTax = averageTax,
        averageOrderValueWithTax = averageOrderValueWithTax,
        orderStatusCounts = orderStatusCounts,
        recentOrders = recentOrders,
        monthlyRevenue = monthlyRevenue,
        monthlyRevenueWithTax = monthlyRevenueWithTax,
        dailyRevenueWithTax = dailyRevenue
    )
}