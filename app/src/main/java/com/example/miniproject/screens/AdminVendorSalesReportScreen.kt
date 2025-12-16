package com.example.miniproject.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Order
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.util.OrderStatusHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVendorSalesReportScreen(
    navController: NavController,
    vendorId: String
) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    // State variables
    var vendorName by remember { mutableStateOf("") }
    var salesData by remember { mutableStateOf<VendorSalesData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Load vendor and sales data
    LaunchedEffect(vendorId) {
        coroutineScope.launch {
            try {
                isLoading = true
                errorMessage = null

                // Get vendor info
                val vendor = databaseService.getVendorById(vendorId)
                vendorName = vendor?.vendorName ?: "Unknown Vendor"

                // Get all orders and calculate vendor sales
                val allOrders = databaseService.getAllOrders()
                salesData = calculateAdminVendorSalesData(vendorId, allOrders, databaseService)

                isLoading = false
            } catch (e: Exception) {
                errorMessage = "Failed to load sales data: ${e.message}"
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .background(Color(0xFF2196F3))
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Vendor Sales Report",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                try {
                                    isLoading = true
                                    val vendor = databaseService.getVendorById(vendorId)
                                    vendorName = vendor?.vendorName ?: "Unknown Vendor"
                                    val allOrders = databaseService.getAllOrders()
                                    salesData = calculateAdminVendorSalesData(vendorId, allOrders, databaseService)
                                } catch (e: Exception) {
                                    errorMessage = "Failed to refresh data: ${e.message}"
                                }
                                isLoading = false
                            }
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Vendor Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        vendorName,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Vendor ID: $vendorId",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Error",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Error Loading Data",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = errorMessage ?: "Unknown error",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val vendor = databaseService.getVendorById(vendorId)
                                    vendorName = vendor?.vendorName ?: "Unknown Vendor"
                                    val allOrders = databaseService.getAllOrders()
                                    salesData = calculateAdminVendorSalesData(vendorId, allOrders, databaseService)
                                } catch (e: Exception) {
                                    errorMessage = "Failed to load data: ${e.message}"
                                }
                                isLoading = false
                            }
                        }) {
                            Text("Retry")
                        }
                    }
                }
            } else if (salesData == null || salesData?.totalOrders == 0) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.BarChart,
                            contentDescription = "No data",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No Sales Data Available",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "This vendor hasn't made any sales yet",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            "Sales Overview",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Key Metrics Cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AdminVendorMetricCard(
                                title = "Total Revenue",
                                value = "RM${"%.2f".format(salesData?.totalRevenueWithTax ?: 0.0)}",
                                subtitle = "RM${"%.2f".format(salesData?.totalRevenue ?: 0.0)} + RM${"%.2f".format(salesData?.totalTax ?: 0.0)} tax",
                                icon = Icons.Default.AttachMoney,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            AdminVendorMetricCard(
                                title = "Total Orders",
                                value = "${salesData?.totalOrders ?: 0}",
                                icon = Icons.Default.Receipt,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        AdminVendorMetricCard(
                            title = "Average Order Value",
                            value = "RM${"%.2f".format(salesData?.averageOrderValueWithTax ?: 0.0)}",
                            subtitle = "RM${"%.2f".format(salesData?.averageOrderValue ?: 0.0)} + RM${"%.2f".format(salesData?.averageTax ?: 0.0)} tax",
                            icon = Icons.Default.TrendingUp,
                            color = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Sales Trend Graph
                    item {
                        AdminVendorSalesTrendGraph(salesData = salesData)
                    }

                    // Order Status Breakdown
                    item {
                        AdminVendorOrderStatusCard(salesData = salesData)
                    }

                    // Recent Orders
                    item {
                        AdminVendorRecentOrdersCard(salesData = salesData)
                    }

                    // Monthly Revenue
                    item {
                        AdminVendorMonthlyRevenueCard(salesData = salesData)
                    }

                    // Platform Revenue Breakdown
                    item {
                        AdminPlatformRevenueBreakdownCard(salesData = salesData)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminVendorMetricCard(
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdminVendorSalesTrendGraph(salesData: VendorSalesData?) {
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
                color = MaterialTheme.colorScheme.onSurface,
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        color = MaterialTheme.colorScheme.onSurface
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
                            trend > 0.1 -> Color(0xFF4CAF50)
                            trend < -0.1 -> Color(0xFFF44336)
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Line Graph
                AdminLineChart(
                    data = dailySales,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
fun AdminLineChart(
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

        // Add labels
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top labels for values
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                values.forEachIndexed { index, value ->
                    Text(
                        text = "RM${"%.0f".format(value)}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                        text = date.substringAfter('/'),
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun AdminVendorOrderStatusCard(salesData: VendorSalesData?) {
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val statusCounts = salesData?.orderStatusCounts ?: emptyMap()
            val totalOrders = salesData?.totalOrders ?: 1

            if (statusCounts.isEmpty()) {
                Text(
                    "No order data available",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                statusCounts.entries.sortedByDescending { it.value }.forEach { (status, count) ->
                    val percentage = if (totalOrders > 0) (count.toDouble() / totalOrders * 100) else 0.0

                    AdminOrderStatusItem(
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
fun AdminOrderStatusItem(status: String, count: Int, percentage: Double, color: Color) {
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
                    .background(color = color, shape = RoundedCornerShape(4.dp))
            )

            Column {
                Text(
                    text = OrderStatusHelper.getStatusDisplayText(status),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$count orders",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "%.1f%%".format(percentage),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AdminVendorRecentOrdersCard(salesData: VendorSalesData?) {
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val recentOrders = salesData?.recentOrders?.take(5) ?: emptyList()

            if (recentOrders.isEmpty()) {
                Text(
                    "No recent orders",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                recentOrders.forEach { order ->
                    AdminRecentOrderItem(order = order)
                }
            }
        }
    }
}

@Composable
fun AdminRecentOrderItem(order: Order) {
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
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                        .format(order.orderDate.toDate()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "RM${"%.2f".format(order.totalPrice)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                AdminOrderStatusBadge(status = order.status)
            }
        }
    }
}

@Composable
fun AdminOrderStatusBadge(status: String) {
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
fun AdminVendorMonthlyRevenueCard(salesData: VendorSalesData?) {
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
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val monthlyRevenue = salesData?.monthlyRevenueWithTax ?: emptyMap()

            if (monthlyRevenue.isEmpty()) {
                Text(
                    "No monthly revenue data available",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                monthlyRevenue.entries.sortedByDescending { it.key }.forEach { (month, revenue) ->
                    AdminMonthlyRevenueItem(month = month, revenue = revenue)
                }
            }
        }
    }
}

@Composable
fun AdminMonthlyRevenueItem(month: String, revenue: Double) {
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
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "RM${"%.2f".format(revenue)}",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun AdminPlatformRevenueBreakdownCard(salesData: VendorSalesData?) {
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
                "Revenue Distribution",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            val totalRevenue = salesData?.totalRevenueWithTax ?: 0.0
            val platformRevenue = totalRevenue * 0.10
            val vendorRevenue = totalRevenue * 0.90

            val platformPercentage = if (totalRevenue > 0) (platformRevenue / totalRevenue) * 100 else 0.0
            val vendorPercentage = if (totalRevenue > 0) (vendorRevenue / totalRevenue) * 100 else 0.0

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(20.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(platformPercentage.toFloat() / 100f)
                            .fillMaxHeight()
                            .background(Color(0xFFFF9800), RoundedCornerShape(10.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(vendorPercentage.toFloat() / 100f)
                            .fillMaxHeight()
                            .background(Color(0xFF4CAF50), RoundedCornerShape(10.dp))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFFFF9800), RoundedCornerShape(4.dp))
                        )
                        Text("Platform (10%)", fontSize = 14.sp)
                    }
                    Text(
                        "RM${"%.2f".format(platformRevenue)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        "${"%.1f".format(platformPercentage)}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .background(Color(0xFF4CAF50), RoundedCornerShape(4.dp))
                        )
                        Text("Vendor (90%)", fontSize = 14.sp)
                    }
                    Text(
                        "RM${"%.2f".format(vendorRevenue)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        "${"%.1f".format(vendorPercentage)}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Revenue:", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(
                    "RM${"%.2f".format(totalRevenue)}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

// Data class for vendor sales data
data class VendorSalesData(
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

// Function to calculate vendor sales data for admin
private suspend fun calculateAdminVendorSalesData(
    vendorId: String,
    allOrders: List<Order>,
    databaseService: DatabaseService
): VendorSalesData {
    val vendorOrders = mutableListOf<Order>()
    var totalRevenue = 0.0
    var totalTax = 0.0
    val orderStatusCounts = mutableMapOf<String, Int>()
    val monthlyRevenue = mutableMapOf<String, Double>()
    val monthlyRevenueWithTax = mutableMapOf<String, Double>()
    val dailyRevenue = mutableMapOf<String, Double>()

    // Get current date for last 7 days calculation
    val calendar = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val monthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val shortDateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())

    // Initialize last 7 days
    for (i in 6 downTo 0) {
        calendar.time = Date()
        calendar.add(Calendar.DAY_OF_YEAR, -i)
        val dateKey = shortDateFormat.format(calendar.time)
        dailyRevenue[dateKey] = 0.0
    }

    // Filter orders that contain products from this vendor
    for (order in allOrders) {
        val orderDetails = databaseService.getOrderDetails(order.orderId)
        var vendorOrderTotal = 0.0
        var vendorOrderTax = 0.0

        // Calculate vendor's portion of the order with tax
        for (detail in orderDetails) {
            val product = databaseService.getProductById(detail.productId)
            if (product?.vendorId == vendorId) {
                val subtotal = detail.subtotal
                // Assuming 6% tax rate
                val tax = subtotal * 0.06
                vendorOrderTotal += subtotal
                vendorOrderTax += tax
            }
        }

        // If vendor has products in this order, include it
        if (vendorOrderTotal > 0) {
            val vendorOrderWithTax = vendorOrderTotal + vendorOrderTax

            // Create a modified order with vendor-specific total including tax
            val vendorOrder = order.copy(totalPrice = vendorOrderWithTax)
            vendorOrders.add(vendorOrder)

            totalRevenue += vendorOrderTotal
            totalTax += vendorOrderTax

            // Update status counts
            orderStatusCounts[order.status] = orderStatusCounts.getOrDefault(order.status, 0) + 1

            // Update monthly revenue
            val monthYear = monthFormat.format(order.orderDate.toDate())
            monthlyRevenue[monthYear] = monthlyRevenue.getOrDefault(monthYear, 0.0) + vendorOrderTotal
            monthlyRevenueWithTax[monthYear] = monthlyRevenueWithTax.getOrDefault(monthYear, 0.0) + vendorOrderWithTax

            // Update daily revenue for last 7 days
            val orderDate = dateFormat.format(order.orderDate.toDate())
            val today = dateFormat.format(Date())
            val calendarOrder = Calendar.getInstance().apply {
                time = order.orderDate.toDate()
            }
            val calendarToday = Calendar.getInstance().apply {
                time = Date()
            }

            // Check if order is within last 7 days
            calendarToday.add(Calendar.DAY_OF_YEAR, -6) // 7 days ago
            if (!order.orderDate.toDate().before(calendarToday.time)) {
                val dayKey = shortDateFormat.format(order.orderDate.toDate())
                dailyRevenue[dayKey] = dailyRevenue.getOrDefault(dayKey, 0.0) + vendorOrderWithTax
            }
        }
    }

    val totalOrders = vendorOrders.size
    val totalRevenueWithTax = totalRevenue + totalTax
    val averageOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0.0
    val averageTax = if (totalOrders > 0) totalTax / totalOrders else 0.0
    val averageOrderValueWithTax = if (totalOrders > 0) totalRevenueWithTax / totalOrders else 0.0

    // Sort recent orders by date (newest first)
    val recentOrders = vendorOrders.sortedByDescending { it.orderDate.seconds }

    return VendorSalesData(
        totalRevenue = totalRevenue,
        totalTax = totalTax,
        totalRevenueWithTax = totalRevenueWithTax,
        totalOrders = totalOrders,
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

private fun calculateTrend(values: List<Double>): Double {
    if (values.size < 2) return 0.0
    val firstHalf = values.take(values.size / 2).average()
    val secondHalf = values.takeLast(values.size / 2).average()
    return if (firstHalf != 0.0) (secondHalf - firstHalf) / firstHalf else 0.0
}