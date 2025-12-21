package com.example.miniproject.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.miniproject.model.Order
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.utils.PdfGenerator
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAnalyticsScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val decimalFormat = DecimalFormat("#,##0.00")
    val context = LocalContext.current

    // State variables
    var vendors by remember { mutableStateOf<List<Vendor>>(emptyList()) }
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTimeRange by remember { mutableStateOf("month") } // day, week, month, year
    var platformRevenueData by remember { mutableStateOf<List<Double>>(emptyList()) }
    var isGeneratingReport by remember { mutableStateOf(false) }

    // --- NEW: Debounce state for back button safety ---
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    // Permission Launcher
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted -> hasNotificationPermission = isGranted }
    )

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasNotificationPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Load data
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val rawVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                vendors = databaseService.calculateAllVendorsStatsBatch(rawVendors)

                orders = databaseService.getAllOrders()
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
    val ordersByStatus = orders.groupBy { order -> order.status }.mapValues { entry -> entry.value.size }

    // 2. Fix Top Vendors Calculation: Use the actual calculated revenue from the Vendor object
    val topVendors = vendors
        .sortedByDescending { it.totalRevenue }
        .take(5)
        .map { it to it.totalRevenue }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Platform Analytics", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = {

                        val currentTime = System.currentTimeMillis()
                        // 500ms delay: Prevents clicks closer than half a second
                        if (currentTime - lastBackClickTime > 500) {
                            lastBackClickTime = currentTime
                            navController.popBackStack()

                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Download Report Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isGeneratingReport = true
                                try {
                                    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                                    val fileName = "Platform_Analytics_Report_$timestamp.pdf"
                                    val downloadsDir = File(context.getExternalFilesDir(null), "Downloads")
                                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                                    val file = File(downloadsDir, fileName)

                                    FileOutputStream(file).use { outputStream ->
                                        PdfGenerator.generatePlatformAnalyticsReport(
                                            context = context,
                                            outputStream = outputStream,
                                            vendors = vendors,
                                            orders = orders,
                                            platformRevenueData = platformRevenueData,
                                            selectedTimeRange = selectedTimeRange,
                                            totalRevenue = totalRevenue,
                                            platformRevenue = platformRevenue,
                                            vendorRevenue = vendorRevenue,
                                            totalOrders = totalOrders,
                                            activeVendors = activeVendors,
                                            ordersByStatus = ordersByStatus,
                                            topVendors = topVendors
                                        )
                                    }
                                    android.widget.Toast.makeText(context, "Report saved!", android.widget.Toast.LENGTH_SHORT).show()
                                    if (hasNotificationPermission) showDownloadNotification(context, file)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    android.widget.Toast.makeText(context, "Failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                } finally {
                                    isGeneratingReport = false
                                }
                            }
                        },
                        enabled = !isGeneratingReport && !isLoading && vendors.isNotEmpty()
                    ) {
                        if (isGeneratingReport) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Download, contentDescription = "Download Report")
                        }
                    }

                    // Refresh Button
                    IconButton(onClick = {
                        coroutineScope.launch {
                            isLoading = true
                            try {
                                // Reload with actual stats calculation
                                val rawVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                                vendors = rawVendors.map { vendor ->
                                    val stats = databaseService.calculateVendorActualStats(vendor.vendorId)
                                    vendor.copy(
                                        orderCount = stats.first,
                                        totalRevenue = stats.second,
                                        rating = stats.third
                                    )
                                }
                                orders = databaseService.getAllOrders()
                                platformRevenueData = generatePlatformRevenueData(orders, selectedTimeRange)
                            } catch (_: Exception) {}
                            isLoading = false
                        }
                    }) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { AdminTimeRangeSelector(selectedTimeRange, { selectedTimeRange = it }) }
                item { PlatformRevenueChart(platformRevenueData, selectedTimeRange, decimalFormat) }
                item { Text("Key Platform Metrics", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminMetricCard("Total Revenue", "RM${decimalFormat.format(totalRevenue)}", Icons.Default.AttachMoney, Color(0xFF4CAF50), Modifier.weight(1f))
                            AdminMetricCard("Platform Revenue", "RM${decimalFormat.format(platformRevenue)}", Icons.Default.AccountBalance, Color(0xFFFF9800), Modifier.weight(1f))
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AdminMetricCard("Total Orders", totalOrders.toString(), Icons.Default.Receipt, Color(0xFF2196F3), Modifier.weight(1f))
                            AdminMetricCard("Active Vendors", activeVendors.toString(), Icons.Default.Store, Color(0xFF9C27B0), Modifier.weight(1f))
                        }
                    }
                }
                item { RevenueBreakdownCard(platformRevenue, vendorRevenue, decimalFormat) }
                item { OrderStatusDistributionCard(ordersByStatus, totalOrders) }
                item { TopVendorsCard(vendors, decimalFormat) } // Updated to not need 'orders'
                item { RevenueTrendsCard(orders, selectedTimeRange, decimalFormat) }
            }
        }
    }
}

// REMOVED: calculateTopVendors and calculateVendorRevenue helper functions are no longer needed
// because we calculate it directly in the LaunchedEffect.

@Composable
fun TopVendorsCard(
    vendors: List<Vendor>,
    decimalFormat: DecimalFormat
) {
    // Sort by the actual revenue loaded in the Vendor objects
    val topVendors = vendors
        .sortedByDescending { it.totalRevenue }
        .take(5)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Top Vendors", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("${vendors.size} total", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            topVendors.forEachIndexed { index, vendor ->
                TopVendorItem(
                    vendor = vendor,
                    rank = index + 1,
                    revenue = vendor.totalRevenue, // Use the correct revenue here
                    decimalFormat = decimalFormat
                )
            }
        }
    }
}

// --- Rest of your UI Composable functions (AdminTimeRangeSelector, PlatformRevenueChart, etc.) remain the same ---
// ... (Include AdminTimeRangeSelector, PlatformRevenueChart, BeautifulBarChart, AdminMetricCard, etc. here) ...

@Composable
private fun AdminTimeRangeSelector(
    selectedTimeRange: String,
    onTimeRangeSelected: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Time Range", fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("day", "week", "month", "year").forEach { range ->
                    FilterChip(
                        selected = selectedTimeRange == range,
                        onClick = { onTimeRangeSelected(range) },
                        label = { Text(range.replaceFirstChar { char -> char.uppercaseChar() }) }
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
    var animationPlayed by remember { mutableStateOf(false) }
    LaunchedEffect(key1 = true) { animationPlayed = true }

    Card(
        modifier = Modifier.fillMaxWidth().height(300.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Platform Revenue", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
                    Text("RM ${decimalFormat.format(revenueData.sum())}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(text = "10% Cut", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (revenueData.isNotEmpty()) {
                    BeautifulBarChart(data = revenueData, animationPlayed = animationPlayed)
                } else {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available", color = Color.LightGray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            val labels = when (timeRange) {
                "day" -> listOf("12am", "6am", "12pm", "6pm")
                "week" -> listOf("Mon", "Wed", "Fri", "Sun")
                "month" -> listOf("Week 1", "Week 2", "Week 3", "Week 4")
                "year" -> listOf("Q1", "Q2", "Q3", "Q4")
                else -> emptyList()
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { label -> Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray) }
            }
        }
    }
}

@Composable
fun BeautifulBarChart(data: List<Double>, animationPlayed: Boolean) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val heightFactor by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 1000, delayMillis = 100),
        label = "barHeight"
    )
    val maxValue = data.maxOrNull() ?: 1.0

    Canvas(modifier = Modifier.fillMaxSize()) {
        val barWidth = size.width / (data.size * 1.5f)
        val space = (size.width - (barWidth * data.size)) / (data.size - 1)
        val maxBarHeight = size.height
        val brush = Brush.verticalGradient(colors = listOf(primaryColor, primaryColor.copy(alpha = 0.6f)))

        data.forEachIndexed { index, value ->
            val finalHeight = (value / maxValue).toFloat() * maxBarHeight
            val currentHeight = finalHeight * heightFactor
            val x = index * (barWidth + space)
            val y = size.height - currentHeight
            drawRoundRect(brush = brush, topLeft = Offset(x, y), size = Size(barWidth, currentHeight), cornerRadius = CornerRadius(8f, 8f))
        }
    }
}

@Composable
fun AdminMetricCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(40.dp).background(color.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp), tint = color)
            }
            Column {
                Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun RevenueBreakdownCard(platformRevenue: Double, vendorRevenue: Double, decimalFormat: DecimalFormat) {
    val total = platformRevenue + vendorRevenue
    val platformPercentage = if (total > 0) (platformRevenue / total) * 100 else 0.0
    val vendorPercentage = if (total > 0) (vendorRevenue / total) * 100 else 0.0

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Revenue Distribution", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(120.dp)) {
                    PieChartCanvas(platformPercentage = platformPercentage.toFloat(), vendorPercentage = vendorPercentage.toFloat())
                }
                Column(modifier = Modifier.weight(1f).padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    RevenueLegendItem(label = "Platform (10%)", value = "RM${decimalFormat.format(platformRevenue)}", percentage = platformPercentage, color = Color(0xFFFF9800))
                    RevenueLegendItem(label = "Vendors (90%)", value = "RM${decimalFormat.format(vendorRevenue)}", percentage = vendorPercentage, color = Color(0xFF4CAF50))
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Revenue:", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("RM${decimalFormat.format(total)}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun PieChartCanvas(platformPercentage: Float, vendorPercentage: Float) {
    val platformColor = Color(0xFFFF9800)
    val vendorColor = Color(0xFF4CAF50)
    Canvas(modifier = Modifier.size(220.dp)) {
        val strokeWidth = 40f
        val total = platformPercentage + vendorPercentage
        val effectiveTotal = if (total == 0f) 1f else total
        val vendorSweepAngle = (vendorPercentage / effectiveTotal) * 360f
        val platformSweepAngle = (platformPercentage / effectiveTotal) * 360f

        drawArc(color = vendorColor, startAngle = -90f, sweepAngle = vendorSweepAngle, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt))
        drawArc(color = platformColor, startAngle = -90f + vendorSweepAngle, sweepAngle = platformSweepAngle, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Butt))
    }
}

@Composable
fun RevenueLegendItem(label: String, value: String, percentage: Double, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp)
            Text(value, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${String.format(Locale.getDefault(), "%.1f", percentage)}%", fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun OrderStatusDistributionCard(ordersByStatus: Map<String, Int>, totalOrders: Int) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Order Status Distribution", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            ordersByStatus.entries.sortedByDescending { entry -> entry.value }.forEach { (status, count) ->
                val percentage = if (totalOrders > 0) (count.toDouble() / totalOrders) * 100 else 0.0
                OrderStatusDistributionItem(status = status, count = count, percentage = percentage)
            }
        }
    }
}

@Composable
fun OrderStatusDistributionItem(status: String, count: Int, percentage: Double) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(modifier = Modifier.size(12.dp).background(color = when (status) {
                "pending" -> Color(0xFFFF9800); "confirmed" -> Color(0xFF2196F3); "preparing" -> Color(0xFF9C27B0); "delivered" -> Color(0xFF4CAF50); "cancelled" -> Color(0xFFF44336); else -> Color(0xFF607D8B)
            }, shape = RoundedCornerShape(4.dp)))
            Column {
                Text(text = status.replaceFirstChar { char -> char.uppercaseChar() }, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(text = "$count orders", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(text = String.format(Locale.getDefault(), "%.1f%%", percentage), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun TopVendorItem(vendor: Vendor, rank: Int, revenue: Double, decimalFormat: DecimalFormat) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).background(Color.White, RoundedCornerShape(12.dp)).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(32.dp).background(color = when (rank) { 1 -> Color(0xFFFFD700); 2 -> Color(0xFFE0E0E0); 3 -> Color(0xFFCD7F32); else -> Color(0xFFF5F5F5) }, shape = androidx.compose.foundation.shape.CircleShape), contentAlignment = Alignment.Center) {
            Text(rank.toString(), fontWeight = FontWeight.Bold, color = if (rank <= 3) Color.White else Color.Black)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = vendor.vendorName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            Text(text = vendor.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = Color.Gray, letterSpacing = 1.sp)
        }
        Text(text = "RM${decimalFormat.format(revenue)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun RevenueTrendsCard(orders: List<Order>, selectedTimeRange: String, decimalFormat: DecimalFormat) {
    val currentMillis = System.currentTimeMillis() / 1000
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Revenue Trends", fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
            val recentOrders = when (selectedTimeRange) {
                "day" -> orders.filter { (currentMillis - it.orderDate.seconds) < 86400 }
                "week" -> orders.filter { (currentMillis - it.orderDate.seconds) < 604800 }
                "month" -> orders.filter { (currentMillis - it.orderDate.seconds) < 2592000 }
                "year" -> orders.filter { (currentMillis - it.orderDate.seconds) < 31536000 }
                else -> orders
            }
            val previousPeriodSeconds = when (selectedTimeRange) { "day" -> 86400; "week" -> 604800; "month" -> 2592000; "year" -> 31536000; else -> 0 }
            val previousPeriodOrders = if (previousPeriodSeconds > 0) orders.filter { (currentMillis - it.orderDate.seconds) in previousPeriodSeconds..(previousPeriodSeconds * 2) } else emptyList()
            val currentRevenue = recentOrders.sumOf { it.totalPrice }; val previousRevenue = previousPeriodOrders.sumOf { it.totalPrice }
            val revenueChange = if (previousRevenue > 0) ((currentRevenue - previousRevenue) / previousRevenue) * 100 else 0.0

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column { Text("Current Period", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("RM${decimalFormat.format(currentRevenue)}", fontSize = 18.sp, fontWeight = FontWeight.Bold) }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("vs Previous", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = if (revenueChange >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = "Trend", modifier = Modifier.size(16.dp), tint = if (revenueChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336))
                            Text("${String.format(Locale.getDefault(), "%.1f", revenueChange)}%", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = if (revenueChange >= 0) Color(0xFF4CAF50) else Color(0xFFF44336))
                        }
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TrendMetric(label = "Orders", current = recentOrders.size, previous = previousPeriodOrders.size)
                    TrendMetric(label = "Avg Order", current = if (recentOrders.isNotEmpty()) currentRevenue / recentOrders.size else 0.0, previous = if (previousPeriodOrders.isNotEmpty()) previousRevenue / previousPeriodOrders.size else 0.0, isCurrency = true)
                    TrendMetric(label = "Platform Rev", current = currentRevenue * 0.10, previous = previousRevenue * 0.10, isCurrency = true)
                }
            }
        }
    }
}

@Composable
fun TrendMetric(label: String, current: Number, previous: Number, isCurrency: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(if (isCurrency) "RM${String.format(Locale.getDefault(), "%.0f", current.toDouble())}" else current.toString(), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        val change = if (previous.toDouble() > 0) ((current.toDouble() - previous.toDouble()) / previous.toDouble()) * 100 else 0.0
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (change >= 0) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward, contentDescription = "Change", modifier = Modifier.size(12.dp), tint = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336))
            Text("${String.format(Locale.getDefault(), "%.1f", change)}%", fontSize = 10.sp, color = if (change >= 0) Color(0xFF4CAF50) else Color(0xFFF44336))
        }
    }
}

// ... (Rest of existing code: generatePlatformRevenueData, showDownloadNotification, etc. - Keep unchanged)
private fun generatePlatformRevenueData(orders: List<Order>, timeRange: String): List<Double> {
    val calendar = Calendar.getInstance()
    return when (timeRange) {
        "day" -> List(24) { hour -> orders.filter { (it.orderDate.seconds % 86400) / 3600 == hour.toLong() }.sumOf { it.totalPrice * 0.10 } }
        "week" -> {
            val weekData = MutableList(7) { 0.0 }
            orders.forEach { order ->
                calendar.timeInMillis = order.orderDate.seconds * 1000
                val day = calendar.get(Calendar.DAY_OF_WEEK)
                val index = if (day == Calendar.SUNDAY) 6 else day - 2
                if (index in 0..6) weekData[index] += (order.totalPrice * 0.10)
            }
            weekData
        }
        "month" -> {
            val monthData = MutableList(4) { 0.0 }
            orders.forEach { order ->
                calendar.timeInMillis = order.orderDate.seconds * 1000
                val week = calendar.get(Calendar.WEEK_OF_MONTH)
                val index = (week - 1).coerceIn(0, 3)
                monthData[index] += (order.totalPrice * 0.10)
            }
            monthData
        }
        "year" -> {
            val yearData = MutableList(12) { 0.0 }
            orders.forEach { order ->
                calendar.timeInMillis = order.orderDate.seconds * 1000
                yearData[calendar.get(Calendar.MONTH)] += (order.totalPrice * 0.10)
            }
            yearData
        }
        else -> emptyList()
    }
}

fun showDownloadNotification(context: Context, file: File) {
    val channelId = "download_channel"
    val notificationId = 1001
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(channelId, "Download Notifications", NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
    val intent = Intent(android.app.DownloadManager.ACTION_VIEW_DOWNLOADS)
    val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    val builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("Report Downloaded Successfully")
        .setContentText("Saved to: ${file.name}")
        .setStyle(NotificationCompat.BigTextStyle().bigText("Saved to: ${file.absolutePath}"))
        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
    try {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
        }
    } catch (e: SecurityException) { e.printStackTrace() }
}