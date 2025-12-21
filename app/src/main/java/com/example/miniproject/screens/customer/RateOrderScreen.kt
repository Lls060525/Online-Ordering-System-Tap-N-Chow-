package com.example.miniproject.screens.customer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Feedback
import com.example.miniproject.model.Order
import com.example.miniproject.model.OrderDetail
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RateOrderScreen(navController: NavController, orderId: String?) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    var order by remember { mutableStateOf<Order?>(null) }
    var orderDetails by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var vendorRating by remember { mutableStateOf(0) }
    var overallComment by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var vendorId by remember { mutableStateOf("") }
    var vendorName by remember { mutableStateOf("Unknown Vendor") }

    // --- NEW: State to prevent double clicks (Crash Prevention) ---
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(orderId) {
        if (orderId != null) {
            order = databaseService.getOrderById(orderId)
            orderDetails = databaseService.getOrderDetails(orderId)

            // Get vendor ID from order details more reliably
            if (orderDetails.isNotEmpty()) {
                val firstProduct = databaseService.getProductById(orderDetails[0].productId)
                firstProduct?.let { product ->
                    // Store vendor info for use in feedback
                    vendorId = product.vendorId
                    val vendor = databaseService.getVendorById(product.vendorId)
                    vendorName = vendor?.vendorName ?: "Unknown Vendor"
                    println("DEBUG: Vendor info - ID: $vendorId, Name: $vendorName")
                }
            }
            isLoading = false
        }
    }

    // Success Dialog
    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't allow dismiss by clicking outside */ },
            title = { Text("Review Submitted!") },
            text = { Text("Thank you for your feedback! Your review has been submitted successfully.") },
            confirmButton = {
                Button(
                    onClick = {
                        showSuccessDialog = false
                        // Navigate back to Home
                        navController.navigate("home") {
                            popUpTo("home") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Rate Your Order",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // --- UPDATED: Safe Back Button Logic ---
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastBackClickTime > 500) {
                            lastBackClickTime = currentTime
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
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
        } else if (order == null) {
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
                    Text("Order not found")
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // Order Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Order #${order!!.getDisplayOrderId()}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Please rate your vendor experience",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Vendor Rating Section
                Text(
                    "Rate the Vendor*",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    "How was your experience with this vendor?",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                StarRating(
                    rating = vendorRating.toDouble(),
                    size = 48.dp,
                    onRatingChanged = { vendorRating = it }
                )

                if (vendorRating == 0) {
                    Text(
                        "Please select a rating to submit your review",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Order Items Preview (Read-only)
                Text(
                    "Your Order Items",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                if (orderDetails.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            orderDetails.forEach { detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            detail.productName,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            "Qty: ${detail.quantity} × RM${"%.2f".format(detail.productPrice)}",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "RM${"%.2f".format(detail.subtotal)}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                }
                                if (detail != orderDetails.last()) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Overall Comment
                Text(
                    "Overall Comment (Optional)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = overallComment,
                    onValueChange = { overallComment = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Share your overall experience with this vendor...") },
                    singleLine = false,
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cancel Button
                    OutlinedButton(
                        onClick = {
                            // --- UPDATED: Safe Back Navigation ---
                            val currentTime = System.currentTimeMillis()
                            if (currentTime - lastBackClickTime > 500) {
                                lastBackClickTime = currentTime
                                navController.popBackStack()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }

                    // Submit Button
                    Button(
                        onClick = {
                            if (vendorRating > 0) {
                                isSubmitting = true
                                coroutineScope.launch {
                                    try {
                                        val customer = authService.getCurrentCustomer()
                                        if (customer == null) return@launch

                                        // Get vendor information
                                        val vId = if (orderDetails.isNotEmpty()) {
                                            val firstProduct = databaseService.getProductById(orderDetails[0].productId)
                                            firstProduct?.vendorId ?: ""
                                        } else {
                                            ""
                                        }

                                        val vendor = if (vId.isNotEmpty()) databaseService.getVendorById(vId) else null
                                        val vName = vendor?.vendorName ?: "Unknown Vendor"

                                        // Create feedback object
                                        val vendorFeedback = Feedback(
                                            customerId = customer.customerId,
                                            customerName = customer.name,
                                            vendorId = vId,
                                            vendorName = vName,
                                            orderId = orderId ?: "",
                                            productId = "",
                                            productName = "",
                                            rating = vendorRating.toDouble(),
                                            comment = overallComment,
                                            feedbackDate = Timestamp.now(),
                                            createdAt = Timestamp.now(),
                                            isVisible = true
                                        )

                                        // Save to Firebase
                                        val result = databaseService.addFeedback(vendorFeedback)
                                        if (result.isSuccess) {
                                            showSuccessDialog = true
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isSubmitting = false
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = vendorRating > 0 && !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Submit Review")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StarRating(
    rating: Double,
    size: Dp = 24.dp,
    onRatingChanged: ((Int) -> Unit)? = null
) {
    val totalStars = 5

    Row {
        for (i in 1..totalStars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star $i",
                modifier = Modifier
                    .size(size)
                    .clickable { onRatingChanged?.invoke(i) },
                tint = if (i <= rating) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}