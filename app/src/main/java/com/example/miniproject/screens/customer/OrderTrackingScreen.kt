package com.example.miniproject.screens.order

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.miniproject.model.Order
import com.example.miniproject.model.OrderDetail
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.service.PayPalService
import com.example.miniproject.util.OrderStatusHelper.formatOrderDate
import com.google.firebase.Timestamp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderTrackingScreen(
    navController: NavController,
    orderId: String
) {
    val context = LocalContext.current
    val databaseService = DatabaseService()
    val scope = rememberCoroutineScope()

    // State
    var order by remember { mutableStateOf<Order?>(null) }
    var orderDetails by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Cancellation State
    var showCancelDialog by remember { mutableStateOf(false) }
    var cancellationTimeRemaining by remember { mutableLongStateOf(0L) } // In Seconds
    var isCancelling by remember { mutableStateOf(false) }

    // State to prevent double clicks
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    val payPalService = remember { PayPalService() }

    // Initial Load & Real-time Listener
    DisposableEffect(orderId) {
        val detailsJob = kotlinx.coroutines.GlobalScope.launch {
            orderDetails = databaseService.getOrderDetails(orderId)
        }

        val listener = databaseService.listenToOrder(orderId) { updatedOrder ->
            order = updatedOrder
            isLoading = false

            // Calculate remaining time for cancellation (1 min = 60s)
            val orderTimeSeconds = updatedOrder.orderDate.seconds
            val nowSeconds = Timestamp.now().seconds
            val elapsed = nowSeconds - orderTimeSeconds
            cancellationTimeRemaining = maxOf(0L, 60L - elapsed)
        }

        onDispose {
            listener.remove()
        }
    }

    // Timer Countdown Effect
    LaunchedEffect(order) {
        while (cancellationTimeRemaining > 0 && order?.status != "cancelled") {
            delay(1000L) // Wait 1 second
            cancellationTimeRemaining--
        }
    }

    // Cancellation Handler
    fun performCancellation() {
        scope.launch {
            isCancelling = true

            val docSnapshot = try {
                databaseService.db.collection("orders").document(orderId).get().await()
            } catch (e: Exception) {
                null
            }

            val captureId = docSnapshot?.getString("paypalOrderId") ?: ""

            android.util.Log.d("RefundDebug", "Retrieved Capture ID from DB: $captureId")


            val cancelResult = databaseService.cancelOrderWithinTimeLimit(orderId)

            if (cancelResult.isSuccess) {
                if (order?.paymentMethod == "paypal") {

                    if (captureId.isNotEmpty()) {
                        Toast.makeText(context, "Processing Refund...", Toast.LENGTH_SHORT).show()

                        val refundResult = payPalService.refundPayment(captureId)

                        if (refundResult.isSuccess) {
                            Toast.makeText(context, "✅ Order Cancelled & Refunded!", Toast.LENGTH_LONG).show()
                            databaseService.updatePaymentStatus(orderId, "refunded")


                            android.util.Log.d("RefundDebug", "Refund Success: ${refundResult.getOrThrow().id}")
                        } else {
                            val error = refundResult.exceptionOrNull()?.message ?: "Unknown error"
                            Toast.makeText(context, "⚠️ Cancelled but Refund Failed: $error", Toast.LENGTH_LONG).show()
                            android.util.Log.e("RefundDebug", "Refund Failed: $error")
                        }
                    } else {
                        Toast.makeText(context, "⚠️ Cancelled but Refund ID missing. Contact Admin.", Toast.LENGTH_LONG).show()
                        android.util.Log.e("RefundDebug", "Capture ID is empty!")
                    }
                } else {
                    Toast.makeText(context, "Order cancelled successfully", Toast.LENGTH_SHORT).show()
                }

                showCancelDialog = false
                navController.navigate("home") { popUpTo(0) }

            } else {
                val errorMsg = cancelResult.exceptionOrNull()?.message
                Toast.makeText(context, "Failed: $errorMsg", Toast.LENGTH_LONG).show()
            }
            isCancelling = false
        }
    }

    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Order?") },
            text = { Text("Are you sure you want to cancel? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = { performCancellation() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (isCancelling) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Yes, Cancel")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No, Keep Order")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Order", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {

                        val currentTime = System.currentTimeMillis()
                        // 500ms delay: Prevents clicks closer than half a second
                        if (currentTime - lastBackClickTime > 500) {
                            lastBackClickTime = currentTime
                            navController.popBackStack()

                        }
                    }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        if (isLoading || order == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                OrderHeader(order!!)

                // Cancellation Button, only if time > 0 and status is valid
                val activeStatuses = listOf("pending", "confirmed", "preparing")

                AnimatedVisibility(
                    visible = cancellationTimeRemaining > 0 &&
                            order!!.status.lowercase() in activeStatuses
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        onClick = { showCancelDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Cancel Order (${formatTime(cancellationTimeRemaining)})",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // Timeline
                TrackingTimeline(currentStatus = order!!.status)

                Divider(thickness = 8.dp, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

                // Details
                OrderSummary(order!!, orderDetails)

                // Bottom padding
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

// Helper to format seconds into MM:SS
fun formatTime(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return "%02d:%02d".format(m, s)
}

@Composable
fun OrderHeader(order: Order) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Order #${order.getDisplayOrderId()}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = formatOrderDate(order.orderDate),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Text(
                text = "RM${"%.2f".format(order.totalPrice)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun TrackingTimeline(currentStatus: String) {
    // Add "Cancelled" as a possible step if the order is cancelled
    val isCancelled = currentStatus.equals("cancelled", ignoreCase = true)

    val steps = if (isCancelled) {
        listOf(
            TrackStep("pending", "Order Placed", "We have received your order", Icons.Default.Schedule),
            TrackStep("cancelled", "Cancelled", "This order was cancelled", Icons.Default.Close)
        )
    } else {
        listOf(
            TrackStep("pending", "Order Placed", "We have received your order", Icons.Default.Schedule),
            TrackStep("confirmed", "Confirmed", "Vendor has confirmed your order", Icons.Default.Check),
            TrackStep("preparing", "Preparing", "Your food is being prepared", Icons.Default.Restaurant),
            TrackStep("ready", "Ready for Pickup", "Order is ready at the counter", Icons.Default.ShoppingBag),
            TrackStep("completed", "Completed", "Enjoy your meal!", Icons.Default.CheckCircle)
        )
    }

    val currentStepIndex = if (isCancelled) 1 else getStatusIndex(currentStatus)

    Column(modifier = Modifier.padding(24.dp)) {
        Text(
            text = if(isCancelled) "Order Cancelled" else "Order Status",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if(isCancelled) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        steps.forEachIndexed { index, step ->
            val isCompleted = index <= currentStepIndex
            val isCurrent = index == currentStepIndex
            val isLast = index == steps.lastIndex

            TimelineRow(
                step = step,
                isCompleted = isCompleted,
                isCurrent = isCurrent,
                isLast = isLast,
                isError = isCancelled && isCurrent
            )
        }
    }
}

@Composable
fun TimelineRow(
    step: TrackStep,
    isCompleted: Boolean,
    isCurrent: Boolean,
    isLast: Boolean,
    isError: Boolean = false
) {
    // Determine colors
    val activeColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    val iconColor by animateColorAsState(
        targetValue = if (isCompleted) activeColor else Color.LightGray,
        animationSpec = tween(durationMillis = 500), label = "color"
    )

    val textColor = if (isCompleted) MaterialTheme.colorScheme.onSurface else Color.Gray

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) activeColor.copy(alpha = 0.1f) else Color.Transparent)
                    .then(if (!isCompleted) Modifier.border(2.dp, Color.LightGray, CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = iconColor
                )
            }

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .padding(vertical = 4.dp)
                        .background(if (isCompleted) activeColor else Color.LightGray.copy(alpha = 0.5f))
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 12.dp, bottom = 32.dp)
                .weight(1f)
        ) {
            Text(
                text = step.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                color = if (isError) MaterialTheme.colorScheme.error else textColor
            )
            Text(
                text = step.description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isCurrent) MaterialTheme.colorScheme.onSurfaceVariant else Color.Gray
            )
        }
    }
}

@Composable
fun OrderSummary(order: Order, details: List<OrderDetail>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Order Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Card(
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                details.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "${item.quantity}x",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(32.dp)
                        )

                        Text(
                            text = item.productName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 8.dp)
                        )

                        Text(
                            text = "RM${"%.2f".format(item.subtotal)}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "RM${"%.2f".format(order.totalPrice)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

data class TrackStep(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector
)

fun getStatusIndex(status: String): Int {
    return when (status.lowercase()) {
        "pending" -> 0
        "confirmed" -> 1
        "preparing" -> 2
        "ready" -> 3
        "completed", "delivered" -> 4
        else -> -1
    }
}