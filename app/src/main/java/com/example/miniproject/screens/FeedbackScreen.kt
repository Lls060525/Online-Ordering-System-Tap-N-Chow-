package com.example.miniproject.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Feedback
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.miniproject.components.StarRatingDisplay

@Composable
fun FeedbackScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("To Rate", "My Reviews") // Removed "Rating Statistics"

    Column(modifier = Modifier.fillMaxSize()) {
        // Tab Row
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = { Text(title) },
                    selected = selectedTab == index,
                    onClick = { selectedTab = index }
                )
            }
        }

        // Tab Content
        when (selectedTab) {
            0 -> ToRateContent(navController)
            1 -> MyReviewsContent(navController)
        }
    }
}

@Composable
fun ToRateContent(navController: NavController) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    var ordersToRate by remember { mutableStateOf<List<com.example.miniproject.model.Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Function to refresh orders to rate
    fun refreshOrdersToRate() {
        coroutineScope.launch {
            val customer = authService.getCurrentCustomer()
            customer?.let {
                val allOrders = databaseService.getCustomerOrders(it.customerId)
                println("DEBUG: Found ${allOrders.size} total orders for customer ${it.customerId}")

                // Filter orders that are delivered/completed but not rated yet
                val filteredOrders = allOrders.filter { order ->
                    println("DEBUG: Order ${order.orderId} - Status: ${order.status}")
                    val isRated = databaseService.getFeedbackByOrder(order.orderId) != null
                    println("DEBUG: Order ${order.orderId} - Is Rated: $isRated")

                    // Show orders that are delivered/completed AND not rated
                    (order.status == "delivered" || order.status == "completed") && !isRated
                }

                println("DEBUG: Found ${filteredOrders.size} orders to rate")
                ordersToRate = filteredOrders.sortedByDescending { it.orderDate }
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        refreshOrdersToRate()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with refresh button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Orders to Rate",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = {
                    isLoading = true
                    refreshOrdersToRate()
                }
            ) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (ordersToRate.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Star,
                title = "No orders to rate",
                subtitle = "Completed orders will appear here for rating. Your feedback helps vendors improve!"
            )
        } else {
            LazyColumn {
                items(ordersToRate) { order ->
                    OrderToRateItem(
                        order = order,
                        onRateClick = {
                            navController.navigate("rateOrder/${order.orderId}")
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
fun MyReviewsContent(navController: NavController) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    var myReviews by remember { mutableStateOf<List<Feedback>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Function to load reviews
    fun loadReviews() {
        coroutineScope.launch {
            try {
                isLoading = true
                println("DEBUG: Loading reviews at ${System.currentTimeMillis()}")

                val customer = authService.getCurrentCustomer()
                println("DEBUG: Current customer: ${customer?.customerId}")

                customer?.let { cust ->
                    val reviews = databaseService.getFeedbackByCustomer(cust.customerId)
                    println("DEBUG: Retrieved ${reviews.size} reviews from database")

                    // Debug: Print each review to check for vendor replies
                    reviews.forEachIndexed { index, feedback ->
                        println("DEBUG: Review $index - Vendor: ${feedback.vendorName}, Has Reply: ${feedback.isReplied}, Reply: ${feedback.vendorReply}")
                    }

                    myReviews = reviews
                    lastRefreshTime = System.currentTimeMillis()
                } ?: run {
                    println("DEBUG: No customer found")
                    myReviews = emptyList()
                }
            } catch (e: Exception) {
                println("DEBUG: Error loading reviews: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Load reviews on initial load and when screen comes into focus
    LaunchedEffect(Unit) {
        loadReviews()
    }

    // Auto-refresh when coming back to this screen
    LaunchedEffect(navController) {
        // This will reload when navigating to this screen
        loadReviews()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header with refresh button and last updated time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "My Reviews",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Last updated: ${java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastRefreshTime))}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { loadReviews() },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                } else {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Refresh reviews"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator()
                    Text("Loading your reviews...")
                }
            }
        } else if (myReviews.isEmpty()) {
            EmptyState(
                icon = Icons.Default.Reviews,
                title = "No reviews yet",
                subtitle = "Your vendor reviews will appear here after you rate your completed orders"
            )
        } else {
            Text(
                "You have ${myReviews.size} review${if (myReviews.size > 1) "s" else ""}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(myReviews) { feedback ->
                    ReviewItem(feedback = feedback)
                }
            }
        }
    }
}

@Composable
fun OrderToRateItem(
    order: com.example.miniproject.model.Order,
    onRateClick: () -> Unit
) {
    val databaseService = DatabaseService()
    var orderDetails by remember { mutableStateOf<List<com.example.miniproject.model.OrderDetail>>(emptyList()) }

    LaunchedEffect(order.orderId) {
        orderDetails = databaseService.getOrderDetails(order.orderId)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Order #${order.getDisplayOrderId()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Completed on ${order.orderDate.toDate().toString().substring(0, 10)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Total: RM${"%.2f".format(order.totalPrice)}",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = onRateClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Rate Order")
                }
            }

            // Order items preview
            Spacer(modifier = Modifier.height(12.dp))
            if (orderDetails.isNotEmpty()) {
                Text(
                    "Items: ${orderDetails.size} product(s)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Show first 2 items as preview
                val previewItems = orderDetails.take(2)
                previewItems.forEach { detail ->
                    Text(
                        "• ${detail.productName} (Qty: ${detail.quantity})",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (orderDetails.size > 2) {
                    Text(
                        "... and ${orderDetails.size - 2} more items",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ReviewItem(feedback: Feedback) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with vendor name and rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        feedback.vendorName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Vendor Review",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    StarRatingDisplay(rating = feedback.rating, size = 20.dp)
                    Text(
                        "%.1f/5.0".format(feedback.rating),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Comment
            if (feedback.comment.isNotEmpty()) {
                Text(
                    feedback.comment,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            } else {
                Text(
                    "No comment provided",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            // Vendor Reply (if exists) - Enhanced display
            if (feedback.isReplied && feedback.vendorReply.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Vendor's Response",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            Text(
                                feedback.vendorReplyDate?.toDate()?.let { date ->
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                                } ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = feedback.vendorReply,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Order and date info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Order #${feedback.orderId.take(8)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    "Reviewed on ${feedback.feedbackDate?.toDate()?.toString()?.substring(0, 10) ?: "Unknown date"}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subtitle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}