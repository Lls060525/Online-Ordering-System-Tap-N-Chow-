package com.example.miniproject.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Feedback
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminFeedbackListScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // State variables
    var feedbacks by remember { mutableStateOf<List<Feedback>>(emptyList()) }
    var vendors by remember { mutableStateOf<List<Vendor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedVendor by remember { mutableStateOf("all") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var feedbackToDelete by remember { mutableStateOf<Feedback?>(null) }

    // Load feedbacks and vendors
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Get all feedbacks from all vendors
                val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                vendors = allVendors

                // Get feedbacks for all vendors
                val allFeedbacks = mutableListOf<Feedback>()
                for (vendor in allVendors) {
                    val vendorFeedbacks = databaseService.getFeedbackByVendor(vendor.vendorId)
                    allFeedbacks.addAll(vendorFeedbacks)
                }

                feedbacks = allFeedbacks.sortedByDescending { it.feedbackDate.seconds }
                isLoading = false
            } catch (_: Exception) {
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
    val totalFeedbacks = feedbacks.size
    val averageRating = if (feedbacks.isNotEmpty()) {
        feedbacks.map { it.rating }.average()
    } else {
        0.0
    }
    val repliedFeedbacks = feedbacks.count { it.isReplied }

    Scaffold(
        topBar = {
            Box(
                modifier = Modifier
                    .background(Color(0xFF2196F3)) // Blue color
                    .statusBarsPadding()
            ) {
                TopAppBar(
                    title = {
                        Text(
                            "Tap N Chow - Admin",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White // White text for better contrast on blue
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
                        IconButton(onClick = { /* Refresh */ }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            // Logout
                            navController.navigate("adminLogin") {
                                popUpTo("adminDashboard") { inclusive = true }
                            }
                        }) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent, // Make the TopAppBar transparent
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
            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminFeedbackStatCard(
                    title = "Total Reviews",
                    value = totalFeedbacks.toString(),
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

            // Search and Filter
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Search Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("Search feedback...") },

                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Vendor Filter
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedVendor == "all",
                            onClick = { selectedVendor = "all" },
                            label = { Text("All Vendors") }
                        )
                        vendors.take(3).forEach { vendor ->
                            FilterChip(
                                selected = selectedVendor == vendor.vendorId,
                                onClick = { selectedVendor = vendor.vendorId },
                                label = {
                                    Text(
                                        if (vendor.vendorName.length > 10)
                                            "${vendor.vendorName.take(10)}..."
                                        else
                                            vendor.vendorName
                                    )
                                }
                            )
                        }
                        if (vendors.size > 3) {
                            var expanded by remember { mutableStateOf(false) }
                            Box {
                                FilterChip(
                                    selected = selectedVendor != "all" &&
                                            !vendors.take(3).any { it.vendorId == selectedVendor },
                                    onClick = { expanded = true },
                                    label = { Text("More...") }
                                )
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    vendors.drop(3).forEach { vendor ->
                                        DropdownMenuItem(
                                            text = { Text(vendor.vendorName) },
                                            onClick = {
                                                selectedVendor = vendor.vendorId
                                                expanded = false
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

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredFeedbacks.isEmpty()) {
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
                            "No Feedback Found",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isNotBlank() || selectedVendor != "all") {
                            Text(
                                "Try adjusting your search or filter",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
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
                Text("Are you sure you want to delete this feedback from ${feedbackToDelete?.customerName}? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            feedbackToDelete?.let { feedback ->
                                // Delete feedback from database
                                // Note: You need to add deleteFeedback function to DatabaseService
                                // databaseService.deleteFeedback(feedback.feedbackId)
                                feedbacks = feedbacks.filter { it.feedbackId != feedback.feedbackId }
                                feedbackToDelete = null
                                showDeleteDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
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