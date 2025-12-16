// Fixed package directive to match file location
package com.example.miniproject.screens.admin

import android.graphics.Paint
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Order
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val decimalFormat = DecimalFormat("#,##0.00")

    // State variables
    var vendors by remember { mutableStateOf<List<Vendor>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTimeRange by remember { mutableStateOf("month") } // day, week, month, year
    var platformRevenueData by remember { mutableStateOf<List<Double>>(emptyList()) }

    // Load data
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                vendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                orders = databaseService.getAllOrders()

                // Generate platform revenue data for chart
                platformRevenueData = generatePlatformRevenueData(orders, selectedTimeRange)

                isLoading = false
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    // Update revenue data when time range changes
    LaunchedEffect(selectedTimeRange, orders) {
        coroutineScope.launch {
            platformRevenueData = generatePlatformRevenueData(orders, selectedTimeRange)
        }
    }

    // Calculate stats
    val totalRevenue = orders.sumOf { order -> order.totalPrice }
    val platformRevenue = totalRevenue * 0.10
    val vendorRevenue = totalRevenue * 0.90
    val totalOrders = orders.size
    val activeVendors = vendors.size
    // avgOrderValue removed since it's unused
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
                        // Fixed deprecated icon
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
                                // Handle error
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
                // Time Range Selector
                item {
                    AdminTimeRangeSelector(
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = { newRange -> selectedTimeRange = newRange }
                    )
                }

                // Platform Revenue Chart
                item {
                    PlatformRevenueChart(
                        revenueData = platformRevenueData,
                        timeRange = selectedTimeRange,
                        decimalFormat = decimalFormat
                    )
                }

                // Key Metrics
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

                // Revenue Breakdown
                item {
                    RevenueBreakdownCard(
                        platformRevenue = platformRevenue,
                        vendorRevenue = vendorRevenue,
                        decimalFormat = decimalFormat
                    )
                }

                // Order Status Distribution
                item {
                    OrderStatusDistributionCard(
                        ordersByStatus = ordersByStatus,
                        totalOrders = totalOrders
                    )
                }

                // Top Performing Vendors
                item {
                    TopVendorsCard(
                        vendors = vendors,
                        orders = orders,
                        decimalFormat = decimalFormat
                    )
                }

                // Revenue Trends
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

@Composable
fun PlatformRevenueChart(
    revenueData: List<Double>,
    timeRange: String,
    decimalFormat: DecimalFormat
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        shape = RoundedCornerShape(16.dp)
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
                Text(
                    "Platform Revenue (10%)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "RM${decimalFormat.format(revenueData.sum())}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFFF9800)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Bar Chart
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.LightGray.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                if (revenueData.isNotEmpty()) {
                    BarChartCanvas(data = revenueData)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No revenue data available",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Time labels
            val labels = when (timeRange) {
                "day" -> listOf("12am", "6am", "12pm", "6pm")
                "week" -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                "month" -> listOf("W1", "W2", "W3", "W4")
                "year" -> listOf("Q1", "Q2", "Q3", "Q4")
                else -> emptyList()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                labels.forEach { label ->
                    Text(
                        label,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun BarChartCanvas(data: List<Double>) {
    val maxValue = data.maxOrNull() ?: 1.0
    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / (data.size * 2)
        val maxBarHeight = size.height * 0.7f

        data.forEachIndexed { index, value ->
            val barHeight = (value / maxValue).toFloat() * maxBarHeight
            val x = barWidth + index * (barWidth + barWidth)
            val y = size.height - barHeight

            // FIXED: Use drawRoundRect instead of drawRect for rounded corners
            drawRoundRect(
                color = Color(0xFFFF9800),
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(4.dp.toPx())
            )

            // Draw value on top of bar
            if (value > 0) {
                drawContext.canvas.nativeCanvas.drawText(
                    "RM${String.format(Locale.getDefault(), "%.0f", value)}",
                    x + barWidth / 2,
                    y - 8.dp.toPx(),
                    Paint().apply {
                        color = android.graphics.Color.BLACK
                        textSize = 10.sp.toPx()
                        textAlign = Paint.Align.CENTER
                        isAntiAlias = true
                    }
                )
            }
        }
    }
}

@Composable
fun AdminMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
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

            // Pie Chart
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pie Chart
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

                // Legend
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

            // Total
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
        val radius = diameter / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Draw platform revenue slice
        drawArc(
            color = Color(0xFFFF9800),
            startAngle = -90f,
            sweepAngle = platformPercentage * 3.6f,
            useCenter = true,
            size = Size(diameter, diameter)
        )

        // Draw vendor revenue slice
        drawArc(
            color = Color(0xFF4CAF50),
            startAngle = -90f + platformPercentage * 3.6f,
            sweepAngle = vendorPercentage * 3.6f,
            useCenter = true,
            size = Size(diameter, diameter)
        )

        // Draw center circle
        drawCircle(
            color = Color.White,
            center = center,
            radius = radius * 0.4f
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
        // Fixed: Added locale to String.format
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
            // Status color indicator
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

        // Fixed: Added locale to String.format
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
    // Calculate vendor revenue (simplified - in real app, you'd calculate from order details)
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

// Helper function to calculate vendor revenue
private fun calculateVendorRevenue(orders: List<Order>, vendors: List<Vendor>): Map<String, Double> {
    // This is a simplified calculation
    // In a real app, you would need to get order details to know which vendor products were ordered
    val revenueMap = mutableMapOf<String, Double>()

    // Distribute revenue evenly among vendors for demo purposes
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
                // Rank
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(
                            color = when (rank) {
                                1 -> Color(0xFFFFD700) // Gold
                                2 -> Color(0xFFC0C0C0) // Silver
                                3 -> Color(0xFFCD7F32) // Bronze
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
    val currentMillis = System.currentTimeMillis() / 1000 // Convert to seconds
    // Removed unused calendar variable

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

            // Simplified trend analysis
            val recentOrders = when (selectedTimeRange) {
                "day" -> orders.filter { order ->
                    val orderSeconds = order.orderDate.seconds
                    currentMillis - orderSeconds < 86400 // 24 hours in seconds
                }
                "week" -> orders.filter { order ->
                    val orderSeconds = order.orderDate.seconds
                    currentMillis - orderSeconds < 604800 // 7 days in seconds
                }
                "month" -> orders.filter { order ->
                    val orderSeconds = order.orderDate.seconds
                    currentMillis - orderSeconds < 2592000 // 30 days in seconds
                }
                "year" -> orders.filter { order ->
                    val orderSeconds = order.orderDate.seconds
                    currentMillis - orderSeconds < 31536000 // 365 days in seconds
                }
                else -> orders
            }

            val previousPeriodSeconds = when (selectedTimeRange) {
                "day" -> 86400
                "week" -> 604800
                "month" -> 2592000
                "year" -> 31536000
                else -> 0
            }

            val previousPeriodOrders = if (previousPeriodSeconds > 0) {
                orders.filter { order ->
                    val orderSeconds = order.orderDate.seconds
                    currentMillis - orderSeconds in previousPeriodSeconds..(previousPeriodSeconds * 2)
                }
            } else {
                emptyList()
            }

            val currentRevenue = recentOrders.sumOf { order -> order.totalPrice }
            val previousRevenue = previousPeriodOrders.sumOf { order -> order.totalPrice }
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
                                // Fixed: Added locale to String.format
                                "${String.format(Locale.getDefault(), "%.1f", revenueChange)}%",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (revenueChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }

                // Additional trend metrics
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TrendMetric(
                        label = "Orders",
                        current = recentOrders.size,
                        previous = previousPeriodOrders.size
                    )
                    TrendMetric(
                        label = "Avg Order",
                        current = if (recentOrders.isNotEmpty()) currentRevenue / recentOrders.size else 0.0,
                        previous = if (previousPeriodOrders.isNotEmpty()) previousRevenue / previousPeriodOrders.size else 0.0,
                        isCurrency = true
                    )
                    TrendMetric(
                        label = "Platform Rev",
                        current = currentRevenue * 0.10,
                        previous = previousRevenue * 0.10,
                        isCurrency = true
                    )
                }
            }
        }
    }
}

@Composable
fun TrendMetric(
    label: String,
    current: Number,
    previous: Number,
    isCurrency: Boolean = false
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        // Fixed: Added locale to String.format
        Text(
            if (isCurrency) "RM${String.format(Locale.getDefault(), "%.0f", current.toDouble())}" else current.toString(),
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
            // Fixed: Added locale to String.format
            Text(
                "${String.format(Locale.getDefault(), "%.1f", change)}%",
                fontSize = 10.sp,
                color = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

// Helper function to generate platform revenue data for charts
private fun generatePlatformRevenueData(orders: List<Order>, timeRange: String): List<Double> {
    return when (timeRange) {
        "day" -> List(24) { hour ->
            orders.filter { order ->
                val orderHour = (order.orderDate.seconds % 86400) / 3600
                orderHour.toInt() == hour
            }.sumOf { order -> order.totalPrice * 0.10 }
        }
        "week" -> List(7) { _ -> // Fixed: Changed unused parameter to underscore
            orders.filter { true }.sumOf { order -> order.totalPrice * 0.10 }
        }.map { value -> value / 7 }
        "month" -> List(4) { _ -> // Fixed: Changed unused parameter to underscore
            orders.filter { true }.sumOf { order -> order.totalPrice * 0.10 }
        }.map { value -> value / 4 }
        "year" -> List(12) { _ -> // Fixed: Changed unused parameter to underscore
            orders.filter { true }.sumOf { order -> order.totalPrice * 0.10 }
        }.map { value -> value / 12 }
        else -> emptyList()
    }
}