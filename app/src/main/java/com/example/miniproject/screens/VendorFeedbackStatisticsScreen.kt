// [file name]: VendorFeedbackStatisticsScreen.kt
package com.example.miniproject.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Feedback
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Date


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorFeedbackStatisticsScreen(navController: NavController) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    var vendor by remember { mutableStateOf<com.example.miniproject.model.Vendor?>(null) }
    var allFeedbacks by remember { mutableStateOf<List<Feedback>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedTimeRange by remember { mutableStateOf("current") } // current, week, month, year

    // Function to load feedbacks
    fun loadFeedbacks() {
        coroutineScope.launch {
            try {
                isLoading = true
                val currentVendor = authService.getCurrentVendor()
                vendor = currentVendor

                if (currentVendor != null) {
                    val vendorFeedbacks = databaseService.getFeedbackWithReplies(currentVendor.vendorId)
                    allFeedbacks = vendorFeedbacks
                }
            } catch (e: Exception) {
                println("DEBUG: Error loading feedbacks: ${e.message}")
            } finally {
                isLoading = false
            }
        }
    }

    // Load feedbacks on initial load
    LaunchedEffect(Unit) {
        loadFeedbacks()
    }

    // Filter feedbacks based on selected time range
    val filteredFeedbacks = remember(allFeedbacks, selectedTimeRange) {
        filterFeedbacksByTimeRange(allFeedbacks, selectedTimeRange)
    }

    // Calculate statistics
    val statistics = remember(filteredFeedbacks) {
        calculateStatistics(filteredFeedbacks)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Feedback Statistics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // Navigate back to Analytics screen
                        navController.popBackStack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Analytics")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredFeedbacks.isEmpty()) {
                EmptyStatisticsState(selectedTimeRange)
            } else {
                // Use verticalScroll for the entire content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Time Range Selector
                    TimeRangeSelector(
                        selectedTimeRange = selectedTimeRange,
                        onTimeRangeSelected = { selectedTimeRange = it }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Statistics Overview
                    StatisticsOverview(statistics = statistics)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Rating Distribution Chart
                    RatingDistributionChart(statistics = statistics)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Bar Chart Visualization
                    RatingBarChart(statistics = statistics)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Pie Chart for Rating Distribution
                    RatingPieChart(statistics = statistics)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Detailed Stats
                    DetailedStatistics(statistics = statistics)

                    // Add some bottom padding for better scrolling
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun TimeRangeSelector(
    selectedTimeRange: String,
    onTimeRangeSelected: (String) -> Unit
) {
    val timeRanges = listOf(
        "current" to "Current",
        "week" to "Past Week",
        "month" to "Past Month",
        "year" to "Past Year"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Time Range",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                timeRanges.forEach { (key, displayName) ->
                    FilterChip(
                        selected = selectedTimeRange == key,
                        onClick = { onTimeRangeSelected(key) },
                        label = { Text(displayName) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun StatisticsOverview(statistics: FeedbackStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Overview",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatCard(
                    value = statistics.totalReviews.toString(),
                    label = "Total Reviews",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(
                    value = "%.1f".format(statistics.averageRating),
                    label = "Avg Rating",
                    icon = Icons.Default.Star,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StatCard(
                    value = "%.1f%%".format(statistics.replyRate),
                    label = "Reply Rate",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RatingDistributionChart(statistics: FeedbackStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Rating Distribution",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Get default color here to pass down
            val defaultColor = MaterialTheme.colorScheme.primary

            statistics.ratingDistribution.forEach { (rating, count) ->
                RatingBar(
                    rating = rating,
                    count = count,
                    total = statistics.totalReviews,
                    defaultColor = defaultColor, // Pass color
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun RatingBar(
    rating: Int,
    count: Int,
    total: Int,
    defaultColor: Color, // New parameter
    modifier: Modifier = Modifier
) {
    val percentage = if (total > 0) (count.toFloat() / total) * 100 else 0f

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "$rating ★",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.width(40.dp)
            )

            // Progress bar
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(20.dp)
                    .padding(horizontal = 8.dp)
            ) {
                // Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(10.dp)
                        )
                )
                // Progress
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage / 100f)
                        .fillMaxHeight()
                        .background(
                            color = getRatingColor(rating, defaultColor), // Pass color
                            shape = RoundedCornerShape(10.dp)
                        )
                )
            }

            Text(
                "$count (${"%.1f".format(percentage)}%)",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// Bar Chart Visualization
@Composable
fun RatingBarChart(statistics: FeedbackStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Rating Bar Chart",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Get default color here
            val defaultColor = MaterialTheme.colorScheme.primary

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                statistics.ratingDistribution.entries.sortedBy { it.key }.forEach { (rating, count) ->
                    BarChartItem(
                        rating = rating,
                        count = count,
                        maxCount = statistics.ratingDistribution.values.maxOrNull() ?: 1,
                        totalReviews = statistics.totalReviews,
                        defaultColor = defaultColor // Pass color
                    )
                }
            }
        }
    }
}

@Composable
fun BarChartItem(rating: Int, count: Int, maxCount: Int, totalReviews: Int, defaultColor: Color) {
    val percentage = if (maxCount > 0) (count.toFloat() / maxCount) else 0f
    val itemPercentage = if (totalReviews > 0) (count.toFloat() / totalReviews) * 100 else 0f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$rating ★",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(40.dp)
        )

        // Bar
        Box(
            modifier = Modifier
                .weight(1f)
                .height(20.dp)
                .padding(horizontal = 8.dp)
        ) {
            // Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    )
            )
            // Bar fill
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .fillMaxHeight()
                    .background(
                        color = getRatingColor(rating, defaultColor), // Pass color
                        shape = RoundedCornerShape(4.dp)
                    )
            )
        }

        Text(
            "$count (${"%.1f".format(itemPercentage)}%)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
    }
}

// Pie Chart Visualization with proper Canvas implementation
@Composable
fun RatingPieChart(statistics: FeedbackStatistics) {
    val textMeasurer = rememberTextMeasurer()
    val defaultColor = MaterialTheme.colorScheme.primary // Capture color here

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Rating Distribution Pie Chart",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Proper pie chart using Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .padding(16.dp)
                ) {
                    CanvasPieChart(statistics = statistics, textMeasurer = textMeasurer, defaultColor = defaultColor)
                }

                // Legend
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    statistics.ratingDistribution.forEach { (rating, count) ->
                        PieChartLegendItem(
                            rating = rating,
                            count = count,
                            total = statistics.totalReviews,
                            defaultColor = defaultColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasPieChart(
    statistics: FeedbackStatistics,
    textMeasurer: androidx.compose.ui.text.TextMeasurer,
    defaultColor: Color
) {
    val total = statistics.totalReviews
    if (total == 0) return

    val ratings = statistics.ratingDistribution.entries.sortedBy { it.key }
    var startAngle = -90f // Start from top (12 o'clock position)

    Canvas(modifier = Modifier.fillMaxSize()) {
        val diameter = size.minDimension
        val radius = diameter / 2
        val center = Offset(size.width / 2, size.height / 2)

        // Draw pie chart segments
        ratings.forEach { (rating, count) ->
            val sweepAngle = (count.toFloat() / total) * 360f
            if (sweepAngle > 0) {
                drawArc(
                    // FIX: Now calling a regular function, not a Composable
                    color = getRatingColor(rating, defaultColor),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true
                )
                startAngle += sweepAngle
            }
        }

        // Draw center circle for donut effect
        val innerRadius = radius * 0.4f
        drawCircle(
            color = Color.White, // Use a hardcoded color or pass background color if needed
            center = center,
            radius = innerRadius
        )

        // Draw average rating text in center
        val averageText = "%.1f".format(statistics.averageRating)
        val avgTextLayoutResult = textMeasurer.measure(
            text = averageText,
            style = TextStyle(
                color = Color.Black, // Ensure contrast
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        )

        val avgTextOffset = Offset(
            center.x - avgTextLayoutResult.size.width / 2,
            center.y - avgTextLayoutResult.size.height / 2 - 8f
        )

        drawText(
            textLayoutResult = avgTextLayoutResult,
            topLeft = avgTextOffset
        )

        // Draw "Avg" text below
        val labelTextLayoutResult = textMeasurer.measure(
            text = "Avg",
            style = TextStyle(
                color = Color.Gray,
                fontSize = 12.sp
            )
        )

        val labelTextOffset = Offset(
            center.x - labelTextLayoutResult.size.width / 2,
            center.y + labelTextLayoutResult.size.height / 2 + 4f
        )

        drawText(
            textLayoutResult = labelTextLayoutResult,
            topLeft = labelTextOffset
        )
    }
}

@Composable
fun PieChartLegendItem(rating: Int, count: Int, total: Int, defaultColor: Color) {
    val percentage = if (total > 0) (count.toFloat() / total) * 100 else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(getRatingColor(rating, defaultColor), RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            "$rating ★",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            "$count (${"%.1f".format(percentage)}%)",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun DetailedStatistics(statistics: FeedbackStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Detailed Statistics",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            StatRow("Total Reviews", statistics.totalReviews.toString())
            StatRow("Average Rating", "%.2f".format(statistics.averageRating))
            StatRow("Total Replies", statistics.totalReplies.toString())
            StatRow("Reply Rate", "%.2f%%".format(statistics.replyRate))
            StatRow("5-Star Reviews", statistics.ratingDistribution[5]?.toString() ?: "0")
            StatRow("4-Star Reviews", statistics.ratingDistribution[4]?.toString() ?: "0")
            StatRow("3-Star Reviews", statistics.ratingDistribution[3]?.toString() ?: "0")
            StatRow("2-Star Reviews", statistics.ratingDistribution[2]?.toString() ?: "0")
            StatRow("1-Star Reviews", statistics.ratingDistribution[1]?.toString() ?: "0")
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmptyStatisticsState(selectedTimeRange: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.Analytics,
                contentDescription = "No statistics",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No Data Available",
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "No feedback data available for the selected time range: ${
                    when (selectedTimeRange) {
                        "current" -> "Current"
                        "week" -> "Past Week"
                        "month" -> "Past Month"
                        "year" -> "Past Year"
                        else -> selectedTimeRange
                    }
                }",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// FIX: Removed @Composable annotation and added defaultColor parameter
private fun getRatingColor(rating: Int, defaultColor: Color): Color {
    return when (rating) {
        1 -> Color(0xFFF44336) // Red for 1-star
        2 -> Color(0xFFFF9800) // Orange for 2-star
        3 -> Color(0xFFFFC107) // Amber for 3-star
        4 -> Color(0xFF8BC34A) // Light Green for 4-star
        5 -> Color(0xFF4CAF50) // Green for 5-star
        else -> defaultColor
    }
}

// Data classes and helper functions
data class FeedbackStatistics(
    val totalReviews: Int,
    val averageRating: Double,
    val totalReplies: Int,
    val replyRate: Double,
    val ratingDistribution: Map<Int, Int>
)

private fun filterFeedbacksByTimeRange(feedbacks: List<Feedback>, timeRange: String): List<Feedback> {
    if (feedbacks.isEmpty()) return emptyList()

    val calendar = Calendar.getInstance()
    val now = Date()

    return when (timeRange) {
        "current" -> feedbacks // All current feedbacks
        "week" -> {
            calendar.time = now
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            val oneWeekAgo = calendar.time
            feedbacks.filter { feedback ->
                feedback.feedbackDate?.toDate()?.after(oneWeekAgo) == true
            }
        }
        "month" -> {
            calendar.time = now
            calendar.add(Calendar.MONTH, -1)
            val oneMonthAgo = calendar.time
            feedbacks.filter { feedback ->
                feedback.feedbackDate?.toDate()?.after(oneMonthAgo) == true
            }
        }
        "year" -> {
            calendar.time = now
            calendar.add(Calendar.YEAR, -1)
            val oneYearAgo = calendar.time
            feedbacks.filter { feedback ->
                feedback.feedbackDate?.toDate()?.after(oneYearAgo) == true
            }
        }
        else -> feedbacks
    }
}

private fun calculateStatistics(feedbacks: List<Feedback>): FeedbackStatistics {
    if (feedbacks.isEmpty()) {
        return FeedbackStatistics(
            totalReviews = 0,
            averageRating = 0.0,
            totalReplies = 0,
            replyRate = 0.0,
            ratingDistribution = mapOf(1 to 0, 2 to 0, 3 to 0, 4 to 0, 5 to 0)
        )
    }

    val totalReviews = feedbacks.size
    val averageRating = feedbacks.map { it.rating }.average()
    val totalReplies = feedbacks.count { it.isReplied }
    val replyRate = if (totalReviews > 0) (totalReplies.toDouble() / totalReviews) * 100 else 0.0

    val ratingDistribution = mutableMapOf<Int, Int>()
    for (i in 1..5) ratingDistribution[i] = 0

    feedbacks.forEach { feedback ->
        val starRating = feedback.rating.toInt().coerceIn(1, 5)
        ratingDistribution[starRating] = ratingDistribution[starRating]!! + 1
    }

    return FeedbackStatistics(
        totalReviews = totalReviews,
        averageRating = averageRating,
        totalReplies = totalReplies,
        replyRate = replyRate,
        ratingDistribution = ratingDistribution
    )
}