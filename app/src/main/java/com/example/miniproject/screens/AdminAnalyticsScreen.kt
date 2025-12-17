package com.example.miniproject.screens

import android.R.attr.label
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Order
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTextApi::class)
@Composable
fun AdminAnalyticsScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val decimalFormat = DecimalFormat("#,##0.00")

    var vendors by remember { mutableStateOf<List<Vendor>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTimeRange by remember { mutableStateOf("month") }
    var platformRevenueData by remember { mutableStateOf<List<Double>>(emptyList()) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                vendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                orders = databaseService.getAllOrders()
                platformRevenueData = generatePlatformRevenueData(orders, selectedTimeRange)
                isLoading = false
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTimeRange, orders) {
        coroutineScope.launch {
            platformRevenueData = generatePlatformRevenueData(orders, selectedTimeRange)
        }
    }

    val totalRevenue = orders.sumOf { order -> order.totalPrice }
    val platformRevenue = totalRevenue * 0.10
    val vendorRevenue = totalRevenue * 0.90
    val totalOrders = orders.size
    val activeVendors = vendors.size
    val ordersByStatus = orders.groupBy { order -> order.status }.mapValues { entry -> entry.value.size }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Platform Analytics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                vendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                                orders = databaseService.getAllOrders()
                                platformRevenueData = generatePlatformRevenueData(orders, selectedTimeRange)
                            } catch (_: Exception) {
                            }
                            isLoading = false
                        }
                    }) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    AdminTimeRangeSelector(
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = { newRange -> selectedTimeRange = newRange }
                    )
                }

                item {
                    PlatformRevenueChart(
                        revenueData = platformRevenueData,
                        timeRange = selectedTimeRange,
                        decimalFormat = decimalFormat,
                        orders = orders
                    )
                }

                item {
                    Text(
                        "Key Platform Metrics",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminMetricCard(
                                title = "Total Revenue",
                                value = "RM${decimalFormat.format(totalRevenue)}",
                                icon = Icons.Default.AttachMoney,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                title = "Platform Revenue",
                                value = "RM${decimalFormat.format(platformRevenue)}",
                                icon = Icons.Default.AccountBalance,
                                color = Color(0xFFFF9800),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminMetricCard(
                                title = "Total Orders",
                                value = totalOrders.toString(),
                                icon = Icons.Default.Receipt,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                            AdminMetricCard(
                                title = "Active Vendors",
                                value = activeVendors.toString(),
                                icon = Icons.Default.Store,
                                color = Color(0xFF9C27B0),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                item {
                    RevenueBreakdownCard(
                        platformRevenue = platformRevenue,
                        vendorRevenue = vendorRevenue,
                        decimalFormat = decimalFormat
                    )
                }

                item {
                    OrderStatusDistributionCard(
                        ordersByStatus = ordersByStatus,
                        totalOrders = totalOrders
                    )
                }

                item {
                    TopVendorsCard(
                        vendors = vendors,
                        orders = orders,
                        decimalFormat = decimalFormat
                    )
                }

                item {
                    RevenueTrendsCard(
                        orders = orders,
                        selectedTimeRange = selectedTimeRange,
                        decimalFormat = decimalFormat
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminTimeRangeSelector(
    selectedTimeRange: String,
    onTimeRangeSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Time Range",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("day", "week", "month", "year").forEach { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { onTimeRangeSelected(range) },
                        label = {
                            Text(range.replaceFirstChar { char ->
                                char.uppercaseChar()
                            })
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun PlatformRevenueChart(
    revenueData: List<Double>,
    timeRange: String,
    decimalFormat: DecimalFormat,
    orders: List<Order>
) {
    val actualPlatformRevenue = orders.sumOf { it.totalPrice } * 0.10
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp), // Increased height for labels above bars
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Platform Revenue (10%)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    val timeLabel = when (timeRange) {
                        "day" -> "Daily Performance"
                        "week" -> "Weekly Performance"
                        "month" -> "Monthly Performance (Current Year)"
                        "year" -> "Yearly Performance (Last 5 Years)"
                        else -> "${timeRange.replaceFirstChar { it.uppercaseChar() }}ly Performance"
                    }
                    Text(
                        timeLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFF9800).copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "RM${decimalFormat.format(actualPlatformRevenue)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFFF9800)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(8.dp))
            ) {
                if (revenueData.isNotEmpty() && revenueData.any { it > 0 }) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        BarChartCanvas(
                            data = revenueData,
                            textMeasurer = textMeasurer,
                            timeRange = timeRange
                        )

                        // Labels for each time range
                        val labels = when (timeRange) {
                            "day" -> {
                                listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            }
                            "week" -> {
                                listOf("Week 1", "Week 2", "Week 3", "Week 4")
                            }
                            "month" -> {
                                listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
                            }
                            "year" -> {
                                val currentYear = Calendar.getInstance().get(Calendar.YEAR)
                                val startYear = currentYear - 4
                                (startYear..currentYear).map { year ->
                                    "'${year.toString().takeLast(2)}"
                                }
                            }
                            else -> emptyList()
                        }

                        // Show labels below the chart
                        if (labels.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                labels.forEach { label ->
                                    Text(
                                        text = label,
                                        fontSize = if (timeRange == "month") 9.sp else 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.BarChart,
                                contentDescription = "No data",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No revenue data available for this period",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}


@Composable
fun BarChartCanvas(
    data: List<Double>,
    textMeasurer: TextMeasurer,
    timeRange: String,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val maxValue = if (data.isNotEmpty()) data.maxOrNull() ?: 1.0 else 1.0

    Canvas(modifier = Modifier
        .fillMaxWidth()
        .height(220.dp)) {  // Increased height for labels above bars
        val totalBars = data.size

        // Dynamic spacing based on number of bars
        val spacing = when (totalBars) {
            12 -> 4.dp.toPx()  // Monthly view
            5 -> 8.dp.toPx()   // Yearly view
            4 -> 12.dp.toPx()  // Weekly view
            7 -> 8.dp.toPx()   // Daily view
            else -> 8.dp.toPx()
        }

        val availableWidth = size.width - (spacing * (totalBars + 1))
        val barWidth = (availableWidth / totalBars).coerceAtLeast(8.dp.toPx())

        // Leave space at top for labels and at bottom for axis labels
        val maxBarHeight = size.height * 0.65f
        val chartTop = 20.dp.toPx()  // Space for value labels above bars
        val chartBottom = size.height * 0.85f  // Leave space for month labels

        // Draw horizontal grid lines
        val gridLineCount = 4
        repeat(gridLineCount) { i ->
            val y = chartBottom - (maxBarHeight * i / (gridLineCount - 1))
            drawLine(
                color = Color.LightGray.copy(alpha = 0.2f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 0.5.dp.toPx()
            )
        }

        data.forEachIndexed { index, value ->
            val x = spacing + index * (barWidth + spacing)
            val barHeight = if (maxValue > 0) (value / maxValue).toFloat() * maxBarHeight else 0f
            val y = chartBottom - barHeight

            val barColor = Color(0xFFFF9800)

            // Draw bar with solid color
            drawRoundRect(
                color = barColor,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )

            // Draw outline
            drawRoundRect(
                color = barColor.copy(alpha = 0.3f),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx()),
                style = Stroke(width = 0.5.dp.toPx())
            )

            // Draw value ABOVE the bar
            if (value > 0) {
                // Smart formatting based on value size and time range
                val text = formatCompactValue(value, timeRange)

                val textStyle = TextStyle(
                    color = textColor,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )

                val textLayoutResult = textMeasurer.measure(text, textStyle)
                val textX = x + barWidth / 2 - textLayoutResult.size.width / 2
                val textY = y - 12.dp.toPx()  // Position above bar

                // Only draw if text fits within available space
                if (textX >= 0 && textX + textLayoutResult.size.width <= size.width) {
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(textX, textY)
                    )
                }
            }
        }

        // Draw baseline
        drawLine(
            color = Color.Gray.copy(alpha = 0.5f),
            start = Offset(0f, chartBottom),
            end = Offset(size.width, chartBottom),
            strokeWidth = 1.dp.toPx()
        )
    }
}

// Compact formatting for bar chart values
private fun formatCompactValue(value: Double, timeRange: String): String {
    return when (timeRange) {
        "day" -> {
            // Daily: show appropriate decimals
            when {
                value < 1 -> "RM${String.format(Locale.getDefault(), "%.2f", value)}"
                value < 10 -> "RM${String.format(Locale.getDefault(), "%.1f", value)}"
                else -> "RM${String.format(Locale.getDefault(), "%.0f", value)}"
            }
        }
        "week" -> {
            // Weekly: show appropriate decimals
            when {
                value < 10 -> "RM${String.format(Locale.getDefault(), "%.1f", value)}"
                else -> "RM${String.format(Locale.getDefault(), "%.0f", value)}"
            }
        }
        "month", "year" -> {
            // Monthly & Yearly: show appropriate decimals (no K/M suffixes)
            when {
                value < 1 -> "RM${String.format(Locale.getDefault(), "%.2f", value)}"
                value < 10 -> "RM${String.format(Locale.getDefault(), "%.1f", value)}"
                value < 1000 -> "RM${String.format(Locale.getDefault(), "%.0f", value)}"
                value < 10000 -> "RM${String.format(Locale.getDefault(), "%.1f", value)}"
                else -> "RM${String.format(Locale.getDefault(), "%.0f", value)}"
            }
        }
        else -> {
            // Default: simple formatting with decimals
            when {
                value < 1 -> "RM${String.format(Locale.getDefault(), "%.2f", value)}"
                value < 10 -> "RM${String.format(Locale.getDefault(), "%.1f", value)}"
                else -> "RM${String.format(Locale.getDefault(), "%.0f", value)}"
            }
        }
    }
}
@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                tint = color
            )
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RevenueBreakdownCard(
    platformRevenue: Double,
    vendorRevenue: Double,
    decimalFormat: DecimalFormat
) {
    val total = platformRevenue + vendorRevenue
    val platformPercentage = if (total > 0) (platformRevenue / total) * 100 else 0.0
    val vendorPercentage = if (total > 0) (vendorRevenue / total) * 100 else 0.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Revenue Distribution",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .weight(1f)
                ) {
                    PieChartCanvas(
                        platformPercentage = platformPercentage.toFloat(),
                        vendorPercentage = vendorPercentage.toFloat()
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    RevenueLegendItem(
                        label = "Platform (10%)",
                        value = "RM${decimalFormat.format(platformRevenue)}",
                        percentage = platformPercentage,
                        color = Color(0xFFFF9800)
                    )
                    RevenueLegendItem(
                        label = "Vendors (90%)",
                        value = "RM${decimalFormat.format(vendorRevenue)}",
                        percentage = vendorPercentage,
                        color = Color(0xFF4CAF50)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Revenue:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(
                    "RM${decimalFormat.format(total)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

}

@Composable
fun PieChartCanvas(
    platformPercentage: Float,
    vendorPercentage: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val diameter = size.minDimension

        // Define colors
        val platformColor = Color(0xFFFF9800)  // Orange
        val vendorColor = Color(0xFF4CAF50)    // Green

        // Calculate angles
        val vendorSweepAngle = vendorPercentage * 3.6f
        val platformSweepAngle = platformPercentage * 3.6f

        // Draw vendor segment first (90%) - the large green part
        drawArc(
            color = vendorColor,
            startAngle = -90f,
            sweepAngle = vendorSweepAngle,
            useCenter = true,
            size = Size(diameter, diameter)
        )

        // Draw platform segment second (10%) - the small orange part
        drawArc(
            color = platformColor,
            startAngle = -90f + vendorSweepAngle,
            sweepAngle = platformSweepAngle,
            useCenter = true,
            size = Size(diameter, diameter)
        )


    }
}

@Composable
fun RevenueLegendItem(
    label: String,
    value: String,
    percentage: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp)
            Text(value, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(
            "${String.format(Locale.getDefault(), "%.1f", percentage)}%",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun OrderStatusDistributionCard(
    ordersByStatus: Map<String, Int>,
    totalOrders: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Order Status Distribution",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ordersByStatus.entries.sortedByDescending { entry -> entry.value }.forEach { (status, count) ->
                val percentage = if (totalOrders > 0) (count.toDouble() / totalOrders) * 100 else 0.0
                OrderStatusDistributionItem(
                    status = status,
                    count = count,
                    percentage = percentage
                )
            }
        }
    }
}

@Composable
fun OrderStatusDistributionItem(
    status: String,
    count: Int,
    percentage: Double
) {
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
                    .background(
                        color = when (status) {
                            "pending" -> Color(0xFFFF9800)
                            "confirmed" -> Color(0xFF2196F3)
                            "preparing" -> Color(0xFF9C27B0)
                            "delivered" -> Color(0xFF4CAF50)
                            "cancelled" -> Color(0xFFF44336)
                            else -> Color(0xFF607D8B)
                        },
                        shape = RoundedCornerShape(4.dp)
                    )
            )

            Column {
                Text(
                    text = status.replaceFirstChar { char -> char.uppercaseChar() },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "$count orders",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = String.format(Locale.getDefault(), "%.1f%%", percentage),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TopVendorsCard(
    vendors: List<Vendor>,
    orders: List<Order>,
    decimalFormat: DecimalFormat
) {
    val vendorRevenueMap = calculateVendorRevenue(orders, vendors)

    val topVendors = vendors.map { vendor ->
        vendor to (vendorRevenueMap[vendor.vendorId] ?: 0.0)
    }.sortedByDescending { pair -> pair.second }.take(5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Top Vendors",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${vendors.size} total",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            topVendors.forEachIndexed { index, (vendor, revenue) ->
                TopVendorItem(
                    vendor = vendor,
                    rank = index + 1,
                    revenue = revenue,
                    decimalFormat = decimalFormat
                )
            }
        }
    }
}

private fun calculateVendorRevenue(orders: List<Order>, vendors: List<Vendor>): Map<String, Double> {
    val revenueMap = mutableMapOf<String, Double>()

    val averageRevenuePerVendor = if (vendors.isNotEmpty()) {
        (orders.sumOf { order -> order.totalPrice } * 0.9) / vendors.size
    } else {
        0.0
    }

    vendors.forEach { vendor ->
        revenueMap[vendor.vendorId] = averageRevenuePerVendor
    }

    return revenueMap
}

@Composable
fun TopVendorItem(
    vendor: Vendor,
    rank: Int,
    revenue: Double,
    decimalFormat: DecimalFormat
) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = when (rank) {
                                1 -> Color(0xFFFFD700)
                                2 -> Color(0xFFC0C0C0)
                                3 -> Color(0xFFCD7F32)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        rank.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (rank) {
                            1, 2, 3 -> Color.White
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                Column {
                    Text(
                        text = vendor.vendorName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = vendor.category.replaceFirstChar { char -> char.uppercaseChar() },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Revenue",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "RM${decimalFormat.format(revenue)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RevenueTrendsCard(
    orders: List<Order>,
    selectedTimeRange: String,
    decimalFormat: DecimalFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                "Revenue Trends",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val (currentOrders, previousOrders) = calculateOrdersForTimeRange(orders, selectedTimeRange)

            val currentRevenue = currentOrders.sumOf { order -> order.totalPrice }
            val previousRevenue = previousOrders.sumOf { order -> order.totalPrice }
            val revenueChange = if (previousRevenue > 0) {
                ((currentRevenue - previousRevenue) / previousRevenue) * 100
            } else {
                0.0
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            "Current Period",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "RM${decimalFormat.format(currentRevenue)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "vs Previous",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (revenueChange >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "Trend",
                                modifier = Modifier.size(16.dp),
                                tint = if (revenueChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            Text(
                                "${String.format(Locale.getDefault(), "%.1f", revenueChange)}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (revenueChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TrendMetric(
                        label = "Orders",
                        current = currentOrders.size,
                        previous = previousOrders.size,
                        decimalFormat = decimalFormat)
                    TrendMetric(
                        label = "Avg Order",
                        current = if (currentOrders.isNotEmpty()) currentRevenue / currentOrders.size else 0.0,
                        previous = if (previousOrders.isNotEmpty()) previousRevenue / previousOrders.size else 0.0,
                        isCurrency = true,
                        decimalFormat = decimalFormat
                    )
                    TrendMetric(
                        label = "Platform Rev",
                        current = currentRevenue * 0.10,
                        previous = previousRevenue * 0.10,
                        isCurrency = true,
                        decimalFormat = decimalFormat
                    )
                }
            }
        }
    }
}

private fun calculateOrdersForTimeRange(orders: List<Order>, timeRange: String): Pair<List<Order>, List<Order>> {
    val currentCalendar = Calendar.getInstance()
    val currentOrders: List<Order>
    val previousOrders: List<Order>

    when (timeRange) {
        "day" -> {
            val today = currentCalendar.clone() as Calendar
            val yesterday = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }

            currentOrders = orders.filter { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val orderCalendar = Calendar.getInstance().apply { time = date }

                orderCalendar.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                        orderCalendar.get(Calendar.MONTH) == today.get(Calendar.MONTH) &&
                        orderCalendar.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
            }

            previousOrders = orders.filter { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val orderCalendar = Calendar.getInstance().apply { time = date }

                orderCalendar.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) &&
                        orderCalendar.get(Calendar.MONTH) == yesterday.get(Calendar.MONTH) &&
                        orderCalendar.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)
            }
        }
        "week" -> {
            val currentWeek = currentCalendar.get(Calendar.WEEK_OF_YEAR)
            val currentYear = currentCalendar.get(Calendar.YEAR)

            val previousWeekCalendar = Calendar.getInstance().apply {
                add(Calendar.WEEK_OF_YEAR, -1)
            }
            val previousWeek = previousWeekCalendar.get(Calendar.WEEK_OF_YEAR)
            val previousWeekYear = previousWeekCalendar.get(Calendar.YEAR)

            currentOrders = orders.filter { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val orderCalendar = Calendar.getInstance().apply { time = date }

                orderCalendar.get(Calendar.YEAR) == currentYear &&
                        orderCalendar.get(Calendar.WEEK_OF_YEAR) == currentWeek
            }

            previousOrders = orders.filter { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val orderCalendar = Calendar.getInstance().apply { time = date }

                orderCalendar.get(Calendar.YEAR) == previousWeekYear &&
                        orderCalendar.get(Calendar.WEEK_OF_YEAR) == previousWeek
            }
        }
        "month" -> {
            val currentMonth = currentCalendar.get(Calendar.MONTH)
            val currentYear = currentCalendar.get(Calendar.YEAR)

            currentOrders = orders.filter { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val orderCalendar = Calendar.getInstance().apply { time = date }

                orderCalendar.get(Calendar.YEAR) == currentYear &&
                        orderCalendar.get(Calendar.MONTH) == currentMonth
            }

            val previousMonthCalendar = Calendar.getInstance().apply {
                add(Calendar.MONTH, -1)
            }
            val previousMonth = previousMonthCalendar.get(Calendar.MONTH)
            val previousMonthYear = previousMonthCalendar.get(Calendar.YEAR)

            previousOrders = orders.filter { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val orderCalendar = Calendar.getInstance().apply { time = date }

                orderCalendar.get(Calendar.YEAR) == previousMonthYear &&
                        orderCalendar.get(Calendar.MONTH) == previousMonth
            }
        }
        "year" -> {
            val currentYear = currentCalendar.get(Calendar.YEAR)

            currentOrders = orders.filter { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val orderCalendar = Calendar.getInstance().apply { time = date }

                orderCalendar.get(Calendar.YEAR) == currentYear
            }

            previousOrders = orders.filter { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val orderCalendar = Calendar.getInstance().apply { time = date }

                orderCalendar.get(Calendar.YEAR) == currentYear - 1
            }
        }
        else -> {
            currentOrders = emptyList()
            previousOrders = emptyList()
        }
    }

    return Pair(currentOrders, previousOrders)
}

@Composable
fun TrendMetric(
    label: String,
    current: Number,
    previous: Number,
    isCurrency: Boolean = false,
    decimalFormat: DecimalFormat
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (isCurrency) "RM${decimalFormat.format(current.toDouble())}" else current.toString(),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        val change = if (previous.toDouble() > 0) {
            ((current.toDouble() - previous.toDouble()) / previous.toDouble()) * 100
        } else {
            0.0
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (change >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = "Change",
                modifier = Modifier.size(12.dp),
                tint = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
            Text(
                "${String.format(Locale.getDefault(), "%.1f", change)}%",
                fontSize = 10.sp,
                color = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

private fun generatePlatformRevenueData(orders: List<Order>, timeRange: String): List<Double> {
    if (orders.isEmpty()) return emptyList()

    return when (timeRange) {
        "day" -> {
            val revenueByDay = MutableList(7) { 0.0 }

            orders.forEach { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val calendar = Calendar.getInstance()
                calendar.time = date

                var dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 2
                if (dayOfWeek < 0) dayOfWeek = 6

                revenueByDay[dayOfWeek] += order.totalPrice * 0.10
            }

            revenueByDay
        }
        "week" -> {
            val revenueByWeek = MutableList(4) { 0.0 }
            val currentCalendar = Calendar.getInstance()

            orders.forEach { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val calendar = Calendar.getInstance()
                calendar.time = date

                if (calendar.get(Calendar.YEAR) == currentCalendar.get(Calendar.YEAR) &&
                    calendar.get(Calendar.MONTH) == currentCalendar.get(Calendar.MONTH)) {

                    val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH) - 1
                    if (weekOfMonth in 0..3) {
                        revenueByWeek[weekOfMonth] += order.totalPrice * 0.10
                    }
                }
            }

            revenueByWeek
        }
        "month" -> {
            val revenueByMonth = MutableList(12) { 0.0 }
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)

            orders.forEach { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val calendar = Calendar.getInstance()
                calendar.time = date

                val orderYear = calendar.get(Calendar.YEAR)
                if (orderYear == currentYear) {
                    val month = calendar.get(Calendar.MONTH)
                    revenueByMonth[month] += order.totalPrice * 0.10
                }
            }

            revenueByMonth
        }
        "year" -> {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            val startYear = currentYear - 4

            val yearlyRevenue = MutableList(5) { 0.0 }

            orders.forEach { order ->
                val timestampMillis = order.orderDate.seconds * 1000L
                val date = Date(timestampMillis)
                val calendar = Calendar.getInstance()
                calendar.time = date

                val orderYear = calendar.get(Calendar.YEAR)
                if (orderYear in startYear..currentYear) {
                    val yearIndex = orderYear - startYear
                    yearlyRevenue[yearIndex] += order.totalPrice * 0.10
                }
            }

            yearlyRevenue
        }
        else -> emptyList()
    }
}