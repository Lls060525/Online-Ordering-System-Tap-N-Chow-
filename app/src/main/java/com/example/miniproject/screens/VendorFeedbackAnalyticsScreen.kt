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
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorFeedbackAnalyticsScreen(navController: NavController) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    var vendor by remember { mutableStateOf<com.example.miniproject.model.Vendor?>(null) }
    var feedbacks by remember { mutableStateOf<List<com.example.miniproject.model.Feedback>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedFeedback by remember { mutableStateOf<com.example.miniproject.model.Feedback?>(null) }
    var showReplyDialog by remember { mutableStateOf(false) }
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Function to load feedbacks
    fun loadFeedbacks() {
        coroutineScope.launch {
            try {
                isLoading = true
                println("DEBUG: Starting to load feedbacks...")

                val currentVendor = authService.getCurrentVendor()
                vendor = currentVendor

                if (currentVendor == null) {
                    println("DEBUG: No current vendor found!")
                    return@launch
                }

                println("DEBUG: Vendor found: ${currentVendor.vendorId} - ${currentVendor.vendorName}")

                // Get feedbacks from database
                println("DEBUG: Querying Firestore for vendor: ${currentVendor.vendorId}")

                val vendorFeedbacks = databaseService.getFeedbackWithReplies(currentVendor.vendorId)

                println("DEBUG: Retrieved ${vendorFeedbacks.size} feedbacks from database")

                // Detailed feedback analysis
                vendorFeedbacks.forEachIndexed { index, feedback ->
                    println("DEBUG: Feedback $index Details:")
                    println("   - Feedback ID: ${feedback.feedbackId}")
                    println("   - Customer: ${feedback.customerName} (${feedback.customerId})")
                    println("   - Vendor: ${feedback.vendorName} (${feedback.vendorId})")
                    println("   - Order ID: ${feedback.orderId}")
                    println("   - Rating: ${feedback.rating}")
                    println("   - Comment: ${feedback.comment}")
                    println("   - Date: ${feedback.feedbackDate?.toDate()}")
                    println("   - Visible: ${feedback.isVisible}")
                    println("   - Has Reply: ${feedback.isReplied}")
                }

                feedbacks = vendorFeedbacks
                lastRefreshTime = System.currentTimeMillis()

                // Update analytics
                databaseService.updateVendorAnalyticsData(currentVendor.vendorId)

                println("DEBUG: Load completed successfully with ${feedbacks.size} feedbacks")

            } catch (e: Exception) {
                println("DEBUG: Error loading feedbacks: ${e.message}")
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    // Check vendor authentication
    fun checkVendorAuth() {
        coroutineScope.launch {
            val currentVendor = authService.getCurrentVendor()
            if (currentVendor == null) {
                println("DEBUG: No vendor logged in!")
                println("DEBUG: This might be the issue - user is not authenticated as vendor")
            } else {
                println("DEBUG: Vendor authenticated:")
                println("DEBUG: - Vendor ID: ${currentVendor.vendorId}")
                println("DEBUG: - Vendor Name: ${currentVendor.vendorName}")
                println("DEBUG: - Email: ${currentVendor.email}")
            }
        }
    }

    // Load feedbacks on initial load
    LaunchedEffect(Unit) {
        checkVendorAuth()
        loadFeedbacks()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Customer Feedback",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back to Analytics")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(
                        onClick = { loadFeedbacks() },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                }
            )
        }
        // REMOVED: bottomBar navigation
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // REMOVED: DEBUG INFO panel

            // Header with stats and refresh info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Customer Reviews",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Last updated: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(lastRefreshTime))}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    "${feedbacks.size} review${if (feedbacks.size != 1) "s" else ""}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator()
                        Text("Loading customer feedback...")
                    }
                }
            } else if (feedbacks.isEmpty()) {
                // Enhanced empty state with guidance
                EmptyFeedbackStateWithGuidance(loadFeedbacks = { loadFeedbacks() })
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(feedbacks) { feedback ->
                        FeedbackItemWithReply(
                            feedback = feedback,
                            onReplyClick = {
                                selectedFeedback = feedback
                                showReplyDialog = true
                            }
                        )
                    }
                }
            }
        }
    }

    // Reply Dialog
    if (showReplyDialog && selectedFeedback != null) {
        ReplyToFeedbackDialog(
            feedback = selectedFeedback!!,
            onDismiss = {
                showReplyDialog = false
                selectedFeedback = null
            },
            onReplySent = { updatedFeedback ->
                // Update local state and refresh
                feedbacks = feedbacks.map {
                    if (it.feedbackId == updatedFeedback.feedbackId) updatedFeedback else it
                }
                showReplyDialog = false
                selectedFeedback = null
                // Refresh to get latest data
                loadFeedbacks()
            }
        )
    }
}

@Composable
fun EmptyFeedbackStateWithGuidance(loadFeedbacks: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Icon(
                Icons.Default.Reviews,
                contentDescription = "No feedback",
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "No Customer Feedback Yet",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    "Customer reviews will appear here once they:\n" +
                            "1. Complete their orders\n" +
                            "2. Rate your service in the Feedback section\n" +
                            "3. Submit their reviews",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            // Action buttons for guidance
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = loadFeedbacks,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check for New Reviews")
                }

                OutlinedButton(
                    onClick = { /* You can add navigation to orders screen */ }
                ) {
                    Icon(Icons.Default.Receipt, contentDescription = "View Orders", modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Check Order Status")
                }
            }

            // Help card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Tips to Get Reviews",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        "• Provide excellent service\n" +
                                "• Ensure orders are marked as completed\n" +
                                "• Ask customers to rate their experience",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
fun FeedbackItemWithReply(
    feedback: com.example.miniproject.model.Feedback,
    onReplyClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with customer name and rating
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        feedback.customerName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Order #${feedback.orderId.take(8)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StarRatingDisplay(rating = feedback.rating, size = 20.dp)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer Comment
            Text(
                text = feedback.comment.ifEmpty { "No comment provided" },
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Vendor Reply Section
            if (feedback.isReplied && feedback.vendorReply.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Your Reply",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                feedback.vendorReplyDate?.toDate()?.let { date ->
                                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                                } ?: "",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = feedback.vendorReply,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (feedback.isReplied) {
                    OutlinedButton(
                        onClick = onReplyClick,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit reply", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit Reply")
                    }
                } else {
                    Button(
                        onClick = onReplyClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Reply, contentDescription = "Reply", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reply")
                    }
                }
            }

            // Date
            Text(
                text = "Reviewed on ${feedback.feedbackDate.toDate().toString().substring(0, 10)}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun ReplyToFeedbackDialog(
    feedback: com.example.miniproject.model.Feedback,
    onDismiss: () -> Unit,
    onReplySent: (com.example.miniproject.model.Feedback) -> Unit
) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    var replyText by remember { mutableStateOf(feedback.vendorReply) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (feedback.isReplied) "Edit Your Reply" else "Reply to Customer",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                // Customer's original feedback
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "${feedback.customerName}'s Review:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            feedback.comment.ifEmpty { "No comment provided" },
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        StarRatingDisplay(rating = feedback.rating, size = 16.dp)
                    }
                }

                // Reply text field
                OutlinedTextField(
                    value = replyText,
                    onValueChange = { replyText = it },
                    label = { Text("Your reply to the customer") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    placeholder = { Text("Thank the customer for their feedback...") },
                    singleLine = false,
                    maxLines = 5
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (replyText.isBlank()) {
                        errorMessage = "Please enter a reply"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        val result = if (feedback.isReplied) {
                            databaseService.updateVendorReply(feedback.feedbackId, replyText)
                        } else {
                            databaseService.addVendorReply(feedback.feedbackId, replyText)
                        }

                        if (result.isSuccess) {
                            val updatedFeedback = feedback.copy(
                                vendorReply = replyText,
                                vendorReplyDate = com.google.firebase.Timestamp.now(),
                                isReplied = true
                            )
                            onReplySent(updatedFeedback)
                        } else {
                            errorMessage = "Failed to send reply: ${result.exceptionOrNull()?.message}"
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading && replyText.isNotBlank()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(if (feedback.isReplied) "Update Reply" else "Send Reply")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun StarRatingDisplay(rating: Double, size: androidx.compose.ui.unit.Dp = 16.dp) {
    val totalStars = 5

    Row {
        for (i in 1..totalStars) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Star $i",
                modifier = Modifier.size(size),
                tint = if (i <= rating) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                }
            )
        }
    }
}