package com.example.miniproject.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    // UI State for Time Range Selection (Visual mainly for now, but can be hooked up to logic)
    var selectedTimeRange by remember { mutableStateOf("Week") }

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
        containerColor = Color(0xFFF5F6F9), // Light Grey Background
        topBar = {
            // Modern White Header
            Column(modifier = Modifier.background(Color.White)) {
                Box(
                    modifier = Modifier
                        .statusBarsPadding()
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    // Back Button
                    IconButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }

                    // Title
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = vendorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.Black
                        )
                        Text(
                            text = "Sales Report",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    val vendor = databaseService.getVendorById(vendorId)
                                    vendorName = vendor?.vendorName ?: "Unknown Vendor"
                                    val allOrders = databaseService.getAllOrders()
                                    salesData = calculateAdminVendorSalesData(vendorId, allOrders, databaseService)
                                } catch (e: Exception) { errorMessage = e.message }
                                isLoading = false
                            }
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Black)
                    }
                }
                Divider(color = Color(0xFFEEEEEE))
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                ErrorView(message = errorMessage ?: "Unknown Error") {
                    // Retry logic
                    isLoading = true
                    // ... (repeat fetch logic)
                }
            } else if (salesData == null || salesData?.totalOrders == 0) {
                EmptyDataView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 1. Key Metrics Row
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ModernMetricCard(
                                title = "Total Revenue",
                                value = "RM${"%.2f".format(salesData?.totalRevenueWithTax ?: 0.0)}",
                                icon = Icons.Default.AttachMoney,
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            ModernMetricCard(
                                title = "Total Orders",
                                value = "${salesData?.totalOrders ?: 0}",
                                icon = Icons.Default.ShoppingBag,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        ModernMetricCard(
                            title = "Avg. Order Value",
                            value = "RM${"%.2f".format(salesData?.averageOrderValueWithTax ?: 0.0)}",
                            icon = Icons.Default.TrendingUp,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // 2. Revenue Trend Graph (Modern Line Chart)
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Sales Trend",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    // Visual Time Range Selector
                                    Row(
                                        modifier = Modifier
                                            .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                            .padding(2.dp)
                                    ) {
                                        listOf("Day", "Week", "Month").forEach { range ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(if (selectedTimeRange == range) Color.White else Color.Transparent)
                                                    .clickable { selectedTimeRange = range }
                                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                            ) {
                                                Text(
                                                    text = range,
                                                    fontSize = 12.sp,
                                                    fontWeight = if (selectedTimeRange == range) FontWeight.Bold else FontWeight.Normal,
                                                    color = if (selectedTimeRange == range) Color.Black else Color.Gray
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                // 1. 获取对应当前选择的数据源
                                val currentChartData = when (selectedTimeRange) {
                                    "Day" -> salesData?.chartDataDay ?: emptyMap()
                                    "Week" -> salesData?.chartDataWeek ?: emptyMap()
                                    "Month" -> salesData?.chartDataMonth ?: emptyMap()
                                    else -> emptyMap()
                                }
                                // Graph Component
                                SmoothLineChart(
                                    data = currentChartData, // <--- 这里不再是写死的 dailyRevenueWithTax
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                )
                            }
                        }
                    }

                    // 3. Revenue Distribution (Donut Chart)
                    item {
                        RevenueDistributionCard(salesData = salesData)
                    }

                    // 4. Order Status Breakdown
                    item {
                        OrderStatusCard(salesData = salesData)
                    }

                    // 5. Recent Orders List
                    item {
                        Text(
                            "Recent Orders",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            salesData?.recentOrders?.take(5)?.forEach { order ->
                                RecentOrderRow(order = order)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Modern Components ---

@Composable
fun ModernMetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(title, fontSize = 12.sp, color = Color.Gray)
                Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
            }
        }
    }
}

@Composable
fun SmoothLineChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val values = data.values.toList()
    val labels = data.keys.toList().map { it.substringAfter('/') } // Shorten dates

    if (values.isEmpty()) return

    val maxVal = values.maxOrNull() ?: 1.0
    val pathColor = Color(0xFF2196F3)
    val gradientColors = listOf(pathColor.copy(alpha = 0.3f), pathColor.copy(alpha = 0.0f))

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val spacing = width / (values.size - 1)

        // Prepare Path
        val path = Path()
        val fillPath = Path() // For gradient under the line

        values.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - ((value / maxVal) * height).toFloat()

            if (index == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height) // Start from bottom-left
                fillPath.lineTo(x, y)
            } else {
                // Bezier Curve Logic for smoothness
                val prevX = (index - 1) * spacing
                val prevVal = values[index - 1]
                val prevY = height - ((prevVal / maxVal) * height).toFloat()

                val controlX1 = prevX + (x - prevX) / 2
                val controlY1 = prevY
                val controlX2 = prevX + (x - prevX) / 2
                val controlY2 = y

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
            }
        }

        // Close the fill path
        fillPath.lineTo(width, height)
        fillPath.close()

        // Draw Gradient Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(gradientColors)
        )

        // Draw Line
        drawPath(
            path = path,
            color = pathColor,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )

        // Draw Dots
        values.forEachIndexed { index, value ->
            val x = index * spacing
            val y = height - ((value / maxVal) * height).toFloat()

            drawCircle(
                color = Color.White,
                radius = 4.dp.toPx(),
                center = Offset(x, y)
            )
            drawCircle(
                color = pathColor,
                radius = 4.dp.toPx(),
                center = Offset(x, y),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
    }

    // Simple X-Axis Labels
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        labels.forEach { label ->
            Text(label, fontSize = 10.sp, color = Color.Gray)
        }
    }
}

@Composable
fun RevenueDistributionCard(salesData: VendorSalesData?) {
    val totalRevenue = salesData?.totalRevenueWithTax ?: 0.0
    val platformRevenue = totalRevenue * 0.10
    val vendorRevenue = totalRevenue * 0.90

    // Avoid division by zero
    val totalForCalc = if (totalRevenue == 0.0) 1.0 else totalRevenue
    val vendorSweep = ((vendorRevenue / totalForCalc) * 360f).toFloat()
    val platformSweep = ((platformRevenue / totalForCalc) * 360f).toFloat()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Revenue Distribution", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                // DONUT CHART
                Box(modifier = Modifier.size(120.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 30f
                        // Vendor Arc (Blue)
                        drawArc(
                            color = Color(0xFF3F51B5), // Dark Blue
                            startAngle = -90f,
                            sweepAngle = vendorSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                        // Platform Arc (Orange)
                        drawArc(
                            color = Color(0xFFFF9800), // Orange
                            startAngle = -90f + vendorSweep,
                            sweepAngle = platformSweep,
                            useCenter = false,
                            style = Stroke(width = strokeWidth)
                        )
                    }
                    // Inner Text
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Total", fontSize = 10.sp, color = Color.Gray)
                        Text("RM${"%.0f".format(totalRevenue)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Legend
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    LegendItem(color = Color(0xFF3F51B5), label = "Vendor (90%)", amount = vendorRevenue)
                    LegendItem(color = Color(0xFFFF9800), label = "Platform (10%)", amount = platformRevenue)
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, amount: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text("RM${"%.2f".format(amount)}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun OrderStatusCard(salesData: VendorSalesData?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Order Breakdown", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(16.dp))

            val counts = salesData?.orderStatusCounts ?: emptyMap()
            if (counts.isEmpty()) {
                Text("No orders yet", fontSize = 14.sp, color = Color.Gray)
            } else {
                // Sort by count descending
                counts.entries.sortedByDescending { it.value }.forEach { (status, count) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(OrderStatusHelper.getStatusColor(status), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                OrderStatusHelper.getStatusDisplayText(status),
                                fontSize = 14.sp,
                                color = Color(0xFF333333)
                            )
                        }
                        Text("$count orders", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Color(0xFFF5F5F5))
                }
            }
        }
    }
}

@Composable
fun RecentOrderRow(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Order #${order.orderId}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(order.orderDate.toDate()),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "RM${"%.2f".format(order.totalPrice)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    fontSize = 14.sp
                )
                Text(
                    order.status.uppercase(),
                    fontSize = 10.sp,
                    color = OrderStatusHelper.getStatusColor(order.status),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Error & Empty States
@Composable
fun ErrorView(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ErrorOutline, null, tint = Color.Red, modifier = Modifier.size(48.dp))
        Text("Error loading data", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(message, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
fun EmptyDataView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.BarChart, null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No sales data available", fontSize = 18.sp, color = Color.Gray)
    }
}

// Data class (Keep existing)
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
    val chartDataDay: Map<String, Double>,   // Day View: 00:00 - 23:00
    val chartDataWeek: Map<String, Double>,  // Week View: Mon - Sun (or last 7 days)
    val chartDataMonth: Map<String, Double>  // Month View: Week 1 - Week 4
)

// Logic function remains largely the same, just ensuring context correctness
private suspend fun calculateAdminVendorSalesData(
    vendorId: String,
    allOrders: List<Order>,
    databaseService: DatabaseService
): VendorSalesData {
    val vendorOrders = mutableListOf<Order>()
    var totalRevenue = 0.0
    var totalTax = 0.0
    val orderStatusCounts = mutableMapOf<String, Int>()

    // --- 初始化图表数据容器 ---
    // 1. Day View: 00:00 到 23:00
    val chartDataDay = LinkedHashMap<String, Double>()
    for (i in 0..23) {
        chartDataDay[String.format("%02d:00", i)] = 0.0
    }

    // 2. Week View: 过去7天
    val chartDataWeek = LinkedHashMap<String, Double>()
    val calendar = Calendar.getInstance()
    val shortDateFormat = SimpleDateFormat("MM/dd", Locale.getDefault())
    // 预先填充过去7天的日期作为 Key
    val weekKeys = mutableListOf<String>()
    for (i in 6 downTo 0) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -i)
        val dateKey = shortDateFormat.format(cal.time)
        chartDataWeek[dateKey] = 0.0
        weekKeys.add(dateKey)
    }

    // 3. Month View: Week 1 到 Week 4 (或 5)
    val chartDataMonth = LinkedHashMap<String, Double>()
    for (i in 1..4) {
        chartDataMonth["Week $i"] = 0.0
    }

    // --- 辅助变量 ---
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
    }
    val currentMonth = Calendar.getInstance().get(Calendar.MONTH)

    // --- 遍历订单 ---
    for (order in allOrders) {
        val orderDetails = databaseService.getOrderDetails(order.orderId)
        var vendorOrderTotal = 0.0
        var vendorOrderTax = 0.0

        // 计算该 Vendor 在此订单中的份额
        for (detail in orderDetails) {
            val product = databaseService.getProductById(detail.productId)
            if (product?.vendorId == vendorId) {
                val subtotal = detail.subtotal
                val tax = subtotal * 0.06
                vendorOrderTotal += subtotal
                vendorOrderTax += tax
            }
        }

        if (vendorOrderTotal > 0) {
            val vendorOrderWithTax = vendorOrderTotal + vendorOrderTax
            val vendorOrder = order.copy(totalPrice = vendorOrderWithTax)
            vendorOrders.add(vendorOrder)

            totalRevenue += vendorOrderTotal
            totalTax += vendorOrderTax

            // 更新订单状态统计
            orderStatusCounts[order.status] = orderStatusCounts.getOrDefault(order.status, 0) + 1

            // --- 填充图表数据 ---
            val orderDate = order.orderDate.toDate()
            val orderCal = Calendar.getInstance().apply { time = orderDate }

            // A. Day Data (仅限今天的订单)
            if (orderDate.after(todayStart.time)) {
                val hourKey = String.format("%02d:00", orderCal.get(Calendar.HOUR_OF_DAY))
                chartDataDay[hourKey] = chartDataDay.getOrDefault(hourKey, 0.0) + vendorOrderWithTax
            }

            // B. Week Data (过去7天)
            // 简单的检查方法：看这个日期生成的 Key 是否在我们的 keys 列表中
            val dayKey = shortDateFormat.format(orderDate)
            if (chartDataWeek.containsKey(dayKey)) {
                chartDataWeek[dayKey] = chartDataWeek.getOrDefault(dayKey, 0.0) + vendorOrderWithTax
            }

            // C. Month Data (仅限本月)
            if (orderCal.get(Calendar.MONTH) == currentMonth) {
                // 计算是第几周 (1-4, 超过4归入Week 4)
                val weekNum = orderCal.get(Calendar.WEEK_OF_MONTH).coerceAtMost(4)
                val weekKey = "Week $weekNum"
                chartDataMonth[weekKey] = chartDataMonth.getOrDefault(weekKey, 0.0) + vendorOrderWithTax
            }
        }
    }

    val totalOrders = vendorOrders.size
    val totalRevenueWithTax = totalRevenue + totalTax
    val averageOrderValue = if (totalOrders > 0) totalRevenue / totalOrders else 0.0
    val averageTax = if (totalOrders > 0) totalTax / totalOrders else 0.0
    val averageOrderValueWithTax = if (totalOrders > 0) totalRevenueWithTax / totalOrders else 0.0

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
        // 填入新计算的数据
        chartDataDay = chartDataDay,
        chartDataWeek = chartDataWeek,
        chartDataMonth = chartDataMonth
    )
}