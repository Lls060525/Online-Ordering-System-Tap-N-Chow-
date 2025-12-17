package com.example.miniproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Person
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

    // Dialog States
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

                // Get ALL feedbacks
                val allFeedbacks = mutableListOf<Feedback>()

                try {
                    // Method 1: Query all feedbacks directly
                    val feedbackSnapshot = databaseService.db.collection("feedbacks")
                        .get()
                        .await()

                    feedbackSnapshot.documents.forEach { document ->
                        val data = document.data ?: return@forEach
                        // ... (Parsing logic remains the same)
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
                            feedbackDate = data["feedbackDate"] as? com.google.firebase.Timestamp
                                ?: data["createdAt"] as? com.google.firebase.Timestamp
                                ?: com.google.firebase.Timestamp.now(),
                            createdAt = data["createdAt"] as? com.google.firebase.Timestamp ?: com.google.firebase.Timestamp.now(),
                            isVisible = data["isVisible"] as? Boolean ?: true,
                            vendorReply = data["vendorReply"] as? String ?: "",
                            vendorReplyDate = data["vendorReplyDate"] as? com.google.firebase.Timestamp,
                            isReplied = data["isReplied"] as? Boolean ?: false
                        )
                        if (feedback.vendorId.isNotBlank()) {
                            allFeedbacks.add(feedback)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback
                }

                feedbacks = allFeedbacks.sortedByDescending { it.feedbackDate.seconds }
                isLoading = false

            } catch (e: Exception) {
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

    // Delete function
    fun deleteFeedback(feedback: Feedback) {
        coroutineScope.launch {
            try {
                databaseService.db.collection("feedbacks").document(feedback.feedbackId).delete().await()
                feedbacks = feedbacks.filter { it.feedbackId != feedback.feedbackId }
                showDeleteDialog = false
                feedbackToDelete = null
            } catch (e: Exception) {
                errorMessage = "Failed to delete: ${e.message}"
                showError = true
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFFF5F6F9),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Feedback Management",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF333333)
                    )
                },
                // Removed navigationIcon to remove "Exit" arrow
                actions = {
                    IconButton(onClick = { /* Refresh logic */ }) {
                        Icon(Icons.Default.Refresh, "Refresh", tint = Color(0xFF333333))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = { AdminBottomNavigation(navController) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Header Content
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(bottom = 16.dp)
            ) {
                // Stats Row (Clean & Consistent Sizing)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FeedbackStatCard(
                        title = "Total",
                        value = "$totalFeedbacks",
                        icon = Icons.Default.Reviews,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.weight(1f)
                    )
                    FeedbackStatCard(
                        title = "Avg Rating",
                        value = String.format(Locale.getDefault(), "%.1f", averageRating),
                        icon = Icons.Default.Star,
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                    FeedbackStatCard(
                        title = "Replied",
                        value = "$repliedFeedbacks",
                        icon = Icons.AutoMirrored.Filled.Reply,
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Search Bar (Matching other screens)
                FeedbackSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vendor Filter Chips (Matching Order List)
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FeedbackFilterChip(
                            label = "All Vendors",
                            selected = selectedVendor == "all",
                            onClick = { selectedVendor = "all" }
                        )
                    }
                    items(vendors) { vendor ->
                        FeedbackFilterChip(
                            label = vendor.vendorName,
                            selected = selectedVendor == vendor.vendorId,
                            onClick = { selectedVendor = vendor.vendorId }
                        )
                    }
                }
            }

            // List Content
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredFeedbacks.isEmpty()) {
                EmptyFeedbackListState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredFeedbacks) { feedback ->
                        FeedbackListItem(
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

    // Delete Dialog
    if (showDeleteDialog && feedbackToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Feedback", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete this feedback?") },
            confirmButton = {
                Button(
                    onClick = { feedbackToDelete?.let { deleteFeedback(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// --- PRIVATE COMPONENTS (Prevents conflicts) ---

@Composable
private fun FeedbackStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Text(
                title,
                fontSize = 11.sp,
                color = Color.Gray,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun FeedbackSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        placeholder = { Text("Search feedback...", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, null, tint = Color.Gray)
                }
            }
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color.Transparent,
            unfocusedBorderColor = Color.Transparent,
            focusedContainerColor = Color(0xFFFAFAFA),
            unfocusedContainerColor = Color(0xFFFAFAFA)
        ),
        singleLine = true
    )
}

@Composable
private fun FeedbackFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = Color(0xFF2196F3).copy(alpha = 0.1f),
            selectedLabelColor = Color(0xFF2196F3)
        ),
        border = FilterChipDefaults.filterChipBorder(
            borderColor = Color(0xFFE0E0E0),
            selectedBorderColor = Color(0xFF2196F3).copy(alpha = 0.5f),
            enabled = true,
            selected = selected
        )
    )
}

@Composable
private fun FeedbackListItem(
    feedback: Feedback,
    dateFormat: SimpleDateFormat,
    onDeleteClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Person, null, tint = Color(0xFF2196F3))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(feedback.customerName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("for ${feedback.vendorName}", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        dateFormat.format(feedback.feedbackDate.toDate()),
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                }

                // Rating & Menu
                Column(horizontalAlignment = Alignment.End) {
                    FeedbackStarRating(feedback.rating)
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(24.dp).padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text("Delete", color = Color.Red) },
                                onClick = { showMenu = false; onDeleteClick() },
                                leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                feedback.comment.ifEmpty { "No comment provided." },
                fontSize = 14.sp,
                color = Color(0xFF424242),
                lineHeight = 20.sp
            )

            if (feedback.isReplied && feedback.vendorReply.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F6F9), RoundedCornerShape(8.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Reply,
                        null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Vendor Reply", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text(feedback.vendorReply, fontSize = 13.sp, color = Color.DarkGray)
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedbackStarRating(rating: Double) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text(String.format(Locale.getDefault(), "%.1f", rating), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyFeedbackListState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.Reviews, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No feedback found", fontSize = 16.sp, color = Color.Gray)
    }
}