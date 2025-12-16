package com.example.miniproject.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Feedback
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.DatabaseService
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedbackListScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    // State variables
    var feedbacks by remember { mutableStateOf<List<Feedback>>(emptyList()) }
    var vendors by remember { mutableStateOf<List<Vendor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedVendor by remember { mutableStateOf("all") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var feedbackToDelete by remember { mutableStateOf<Feedback?>(null) }
    var showError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Load vendors and all feedbacks
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Get all vendors
                val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                vendors = allVendors

                // Get ALL feedbacks from the database using a different approach
                val allFeedbacks = mutableListOf<Feedback>()

                // Method 1: Try to get all feedbacks from feedbacks collection
                try {
                    // Query all feedbacks directly
                    val feedbackSnapshot = databaseService.db.collection("feedbacks")
                        .get()
                        .await()

                    feedbackSnapshot.documents.forEach { document ->
                        val data = document.data ?: return@forEach

                        val feedback = Feedback(
                            feedbackId = document.id,
                            customerId = data["customerId"] as? String ?: "",
                            customerName = data["customerName"] as? String ?: "",
                            vendorId = data["vendorId"] as? String ?: "",
                            vendorName = data["vendorName"] as? String ?: "",
                            orderId = data["orderId"] as? String ?: "",
                            productId = data["productId"] as? String ?: "",
                            productName = data["productName"] as? String ?: "",
                            rating = (data["rating"] as? Double) ?: (data["rating"] as? Long)?.toDouble() ?: 0.0,
                            comment = data["comment"] as? String ?: "",
                            feedbackDate = data["feedbackDate"] as? Timestamp
                                ?: data["createdAt"] as? Timestamp
                                ?: Timestamp.now(),
                            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                            isVisible = data["isVisible"] as? Boolean ?: true,
                            vendorReply = data["vendorReply"] as? String ?: "",
                            vendorReplyDate = data["vendorReplyDate"] as? Timestamp,
                            isReplied = data["isReplied"] as? Boolean ?: false
                        )

                        // Only add if feedback has a vendor ID
                        if (feedback.vendorId.isNotBlank()) {
                            allFeedbacks.add(feedback)
                        }
                    }

                    println("DEBUG: Successfully loaded ${allFeedbacks.size} feedbacks directly")
                } catch (e: Exception) {
                    println("DEBUG: Error loading all feedbacks: ${e.message}")

                    // Fallback: Get feedbacks by vendor
                    for (vendor in allVendors) {
                        try {
                            val vendorFeedbacks = databaseService.getFeedbackByVendor(vendor.vendorId)
                            println("DEBUG: Found ${vendorFeedbacks.size} feedbacks for vendor ${vendor.vendorName}")
                            allFeedbacks.addAll(vendorFeedbacks)
                        } catch (e: Exception) {
                            println("DEBUG: Error getting feedback for vendor ${vendor.vendorId}: ${e.message}")
                        }
                    }
                }

                // Sort by date (newest first)
                feedbacks = allFeedbacks.sortedByDescending { it.feedbackDate.seconds }
                isLoading = false

                println("DEBUG: Total feedbacks loaded: ${feedbacks.size}")
                println("DEBUG: Sample feedbacks:")
                feedbacks.take(3).forEach { fb ->
                    println("  - ${fb.customerName} for ${fb.vendorName}: ${fb.rating} stars")
                }

            } catch (e: Exception) {
                println("ERROR loading data: ${e.message}")
                e.printStackTrace()
                errorMessage = "Failed to load feedbacks: ${e.message}"
                showError = true
                isLoading = false
            }
        }
    }

    // Filter feedbacks
    val filteredFeedbacks = feedbacks.filter { feedback ->
        val matchesSearch = searchQuery.isBlank() ||
                feedback.customerName.contains(searchQuery, ignoreCase = true) ||
                feedback.comment.contains(searchQuery, ignoreCase = true) ||
                feedback.vendorName.contains(searchQuery, ignoreCase = true)

        val matchesVendor = selectedVendor == "all" || feedback.vendorId == selectedVendor

        matchesSearch && matchesVendor
    }

    // Calculate stats
    val totalFeedbacks = filteredFeedbacks.size
    val averageRating = if (filteredFeedbacks.isNotEmpty()) {
        filteredFeedbacks.map { it.rating }.average()
    } else {
        0.0
    }
    val repliedFeedbacks = filteredFeedbacks.count { it.isReplied }

    // Function to delete feedback
    suspend fun deleteFeedback(feedback: Feedback) {
        try {
            // Delete from Firestore
            databaseService.db.collection("feedbacks").document(feedback.feedbackId).delete().await()

            // Update local state
            feedbacks = feedbacks.filter { it.feedbackId != feedback.feedbackId }

            println("DEBUG: Feedback ${feedback.feedbackId} deleted successfully")
        } catch (e: Exception) {
            println("DEBUG: Error deleting feedback: ${e.message}")
            errorMessage = "Failed to delete feedback: ${e.message}"
            showError = true
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
                            "Tap N Chow - Feedback Management",
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
                            // Refresh feedbacks
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    // Re-fetch all feedbacks
                                    val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                                    vendors = allVendors

                                    val allFeedbacks = mutableListOf<Feedback>()
                                    for (vendor in allVendors) {
                                        val vendorFeedbacks = databaseService.getFeedbackByVendor(vendor.vendorId)
                                        allFeedbacks.addAll(vendorFeedbacks)
                                    }

                                    feedbacks = allFeedbacks.sortedByDescending { it.feedbackDate.seconds }
                                } catch (e: Exception) {
                                    errorMessage = "Refresh failed: ${e.message}"
                                    showError = true
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
        },
        bottomBar = {
            AdminBottomNavigation(navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Error Message
            if (showError) {
                AlertDialog(
                    onDismissRequest = { showError = false },
                    title = { Text("Error") },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(onClick = { showError = false }) {
                            Text("OK")
                        }
                    }
                )
            }

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminFeedbackStatCard(
                    title = "Showing",
                    value = "${filteredFeedbacks.size}/${feedbacks.size}",
                    icon = Icons.Default.Reviews,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )

                AdminFeedbackStatCard(
                    title = "Avg Rating",
                    value = String.format(Locale.getDefault(), "%.1f", averageRating),
                    icon = Icons.Default.Star,
                    color = Color(0xFFFF9800),
                    modifier = Modifier.weight(1f)
                )

                AdminFeedbackStatCard(
                    title = "Replied",
                    value = repliedFeedbacks.toString(),
                    icon = Icons.AutoMirrored.Filled.Reply,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
            }

            // Search and Filter Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = { Text("Search feedback by customer, vendor, or comment...") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search"
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vendor Filter
                    Text(
                        "Filter by Vendor:",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = selectedVendor == "all",
                            onClick = { selectedVendor = "all" },
                            label = { Text("All Vendors") },
                            leadingIcon = if (selectedVendor == "all") {
                                { Icon(Icons.Default.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                            } else null
                        )

                        // Create a horizontal scrollable row for vendor chips
                        LazyColumn(
                            modifier = Modifier.height(60.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(vendors.chunked(3)) { vendorRow ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    vendorRow.forEach { vendor ->
                                        FilterChip(
                                            selected = selectedVendor == vendor.vendorId,
                                            onClick = { selectedVendor = vendor.vendorId },
                                            label = {
                                                Text(
                                                    if (vendor.vendorName.length > 15)
                                                        "${vendor.vendorName.take(15)}..."
                                                    else
                                                        vendor.vendorName
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Loading State
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading feedbacks...")
                    }
                }
            }
            // Empty State
            else if (filteredFeedbacks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Reviews,
                            contentDescription = "No feedback",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            if (feedbacks.isEmpty()) "No Feedbacks Found"
                            else "No Feedbacks Match Your Search",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (feedbacks.isEmpty())
                                "There are no feedbacks in the system yet."
                            else
                                "Try adjusting your search or filter criteria.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            // Feedback List
            else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFeedbacks) { feedback ->
                        AdminFeedbackItem(
                            feedback = feedback,
                            dateFormat = dateFormat,
                            onDeleteClick = {
                                feedbackToDelete = feedback
                                showDeleteDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && feedbackToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                feedbackToDelete = null
            },
            title = {
                Text(
                    "Delete Feedback",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Column {
                    Text("Are you sure you want to delete this feedback?")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "From: ${feedbackToDelete?.customerName}",
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Vendor: ${feedbackToDelete?.vendorName}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Rating: ${feedbackToDelete?.rating} stars",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("This action cannot be undone.", color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            feedbackToDelete?.let { feedback ->
                                deleteFeedback(feedback)
                                feedbackToDelete = null
                                showDeleteDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        feedbackToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
@Composable
fun AdminFeedbackStatCard(
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = color
            )
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AdminFeedbackItem(
    feedback: Feedback,
    dateFormat: SimpleDateFormat,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        feedback.customerName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        feedback.vendorName,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StarRatingDisplay(rating = feedback.rating)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Comment
            Text(
                text = feedback.comment.ifEmpty { "No comment provided" },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Order and Date Info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Order #${feedback.orderId.take(8)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    dateFormat.format(feedback.feedbackDate.toDate()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Reply Section (if any)
            if (feedback.isReplied && feedback.vendorReply.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    // FIXED: Removed background modifier, use colors instead
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Vendor Reply",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                feedback.vendorReplyDate?.toDate().let { dateFormat.format(it) },
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            feedback.vendorReply,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // Delete Button
                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun StarRatingDisplay(rating: Double) {
    val totalStars = 5
    Row {
        for (i in 1..totalStars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star $i",
                modifier = Modifier.size(16.dp),
                tint = if (i <= rating) {
                    Color(0xFFFFD700) // Gold color for filled stars
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            String.format(Locale.getDefault(), "%.1f", rating),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
    }
}