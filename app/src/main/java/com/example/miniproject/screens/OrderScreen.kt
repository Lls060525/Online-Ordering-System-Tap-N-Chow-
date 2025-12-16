package com.example.miniproject.screens.order

import android.Manifest
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedFilterChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.miniproject.model.Order
import com.example.miniproject.model.OrderDetail
import com.example.miniproject.model.Payment
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.util.NotificationHelper
import com.example.miniproject.util.OrderStatusHelper.formatOrderDate
import com.example.miniproject.util.OrderStatusHelper.getStatusColor
import com.example.miniproject.util.OrderStatusHelper.getStatusDisplayText
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(navController: NavController) {
    val context = LocalContext.current
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    // initialize Notification Helper
    val notificationHelper = remember { NotificationHelper(context) }

    // Existing States
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var orderDetails by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
    var showOrderDetails by remember { mutableStateOf(false) }

    // NEW: Alert Dialog States
    var showPickupDialog by remember { mutableStateOf(false) }
    var pickupOrderId by remember { mutableStateOf("") }

    // --- NEW: Filter State and Logic ---
    var selectedFilter by remember { mutableStateOf("All") }

    // Define status categories
    // Note: Ensure these match the raw strings stored in your Firestore
    val activeStatuses = listOf("pending", "confirmed", "preparing", "ready", "delivered")
    val completedStatuses = listOf("completed")
    val cancelledStatuses = listOf("cancelled")

    // Calculate Counts dynamically based on the full orders list
    val allCount = orders.size
    val activeCount = orders.count { it.status.lowercase() in activeStatuses }
    val completedCount = orders.count { it.status.lowercase() in completedStatuses }
    val cancelledCount = orders.count { it.status.lowercase() in cancelledStatuses }

    // Create the filtered list to display
    val filteredOrders = remember(orders, selectedFilter) {
        when (selectedFilter) {
            "Active" -> orders.filter { it.status.lowercase() in activeStatuses }
            "Completed" -> orders.filter { it.status.lowercase() in completedStatuses }
            "Cancelled" -> orders.filter { it.status.lowercase() in cancelledStatuses }
            else -> orders // "All"
        }
    }

    // Permission Launcher (For Android 13+)
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("OrderScreen", "Notification permission granted")
            }
        }
    )

    LaunchedEffect(Unit) {
        // 1. Ask for permission immediately
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        coroutineScope.launch {
            try {
                val customer = authService.getCurrentCustomer()
                if (customer == null) {
                    isLoading = false
                    return@launch
                }

                // 2. Initial Load of Orders
                val customerOrders = databaseService.getCustomerOrders(customer.customerId)
                // Sort by date descending (newest first)
                orders = customerOrders.sortedByDescending { it.orderDate }
                isLoading = false

                // 3. IMMEDIATE CHECK: Check if any order is ALREADY "ready"
                val alreadyReadyOrder = customerOrders.find { it.status.equals("ready", ignoreCase = true) }
                if (alreadyReadyOrder != null) {
                    pickupOrderId = alreadyReadyOrder.getDisplayOrderId()
                    showPickupDialog = true
                    notificationHelper.showOrderReadyNotification(pickupOrderId)
                }

                // 4. REAL-TIME LISTENER
                databaseService.listenToOrderUpdates(customer.customerId) { readyOrder ->
                    notificationHelper.showOrderReadyNotification(readyOrder.getDisplayOrderId())
                    pickupOrderId = readyOrder.getDisplayOrderId()
                    showPickupDialog = true

                    // Refresh list
                    coroutineScope.launch {
                        val updatedOrders = databaseService.getCustomerOrders(customer.customerId)
                        orders = updatedOrders.sortedByDescending { it.orderDate }
                    }
                }

            } catch (e: Exception) {
                isLoading = false
                Log.e("OrderScreen", "Error loading orders: ${e.message}")
            }
        }
    }

    // --- ALERT DIALOG (Visible Pop-up) ---
    if (showPickupDialog) {
        AlertDialog(
            onDismissRequest = { showPickupDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Restaurant,
                    contentDescription = "Food Ready",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Order Ready for Pickup!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Order #$pickupOrderId is ready! Please collect your food at the counter.",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { showPickupDialog = false }
                ) {
                    Text("OK, I'm Coming")
                }
            }
        )
    }

    // --- ORDER DETAILS DIALOG ---
    if (showOrderDetails && selectedOrder != null) {
        OrderDetailDialog(
            order = selectedOrder!!,
            orderDetails = orderDetails,
            onDismiss = {
                showOrderDetails = false
                selectedOrder = null
                orderDetails = emptyList()
            }
        )
    } else {
        // --- MAIN SCREEN CONTENT ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "My Orders",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // --- NEW: Filter Chips Row ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = "All",
                    count = allCount,
                    selected = selectedFilter == "All",
                    onClick = { selectedFilter = "All" }
                )
                FilterChipItem(
                    label = "Active",
                    count = activeCount,
                    selected = selectedFilter == "Active",
                    onClick = { selectedFilter = "Active" }
                )
                FilterChipItem(
                    label = "Completed",
                    count = completedCount,
                    selected = selectedFilter == "Completed",
                    onClick = { selectedFilter = "Completed" }
                )
                FilterChipItem(
                    label = "Cancelled",
                    count = cancelledCount,
                    selected = selectedFilter == "Cancelled",
                    onClick = { selectedFilter = "Cancelled" }
                )
            }

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Loading orders...",
                            modifier = Modifier.padding(top = 16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (filteredOrders.isEmpty()) {
                // Modified empty state to reflect current filter
                val emptyMessage = if (orders.isNotEmpty()) "No $selectedFilter orders found" else "No orders yet"
                EmptyOrderState(subtitle = emptyMessage)
            } else {
                LazyColumn {
                    // Iterate over filteredOrders instead of all orders
                    items(filteredOrders) { order ->
                        OrderCard(
                            order = order,
                            onClick = {
                                coroutineScope.launch {
                                    selectedOrder = order
                                    orderDetails = databaseService.getOrderDetails(order.orderId)
                                    showOrderDetails = true
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- NEW: Helper Composable for Filter Chips ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipItem(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    ElevatedFilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = "$label ($count)",
                style = MaterialTheme.typography.labelLarge
            )
        },
        enabled = true, // Required
        colors = FilterChipDefaults.elevatedFilterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Order Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order #${order.getDisplayOrderId()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "RM${"%.2f".format(order.totalPrice)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Order Date
            Text(
                text = "Placed on ${formatOrderDate(order.orderDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Order Status
            Row(
                modifier = Modifier.padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(getStatusColor(order.status))
                )
                Text(
                    text = getStatusDisplayText(order.status),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = getStatusColor(order.status),
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            // Delivery Address (truncated)
            Text(
                text = "Delivery to: ${order.shippingAddress.take(50)}${if (order.shippingAddress.length > 50) "..." else ""}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Payment Method
            Text(
                text = "Payment: ${order.paymentMethod.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Tap to view details hint
            Text(
                text = "Tap to view details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .align(Alignment.End)
            )
        }
    }
}

@Composable
fun OrderDetailDialog(
    order: Order,
    orderDetails: List<OrderDetail>,
    onDismiss: () -> Unit
) {
    val databaseService = DatabaseService()
    var payment by remember { mutableStateOf<Payment?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(order.orderId) {
        try {
            payment = databaseService.getPaymentByOrder(order.orderId)
        } catch (e: Exception) {
            // Handle error silently
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Order #${order.getDisplayOrderId()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                // Show status with color
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(getStatusColor(order.status))
                    )
                    Text(
                        text = getStatusDisplayText(order.status),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = getStatusColor(order.status),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    // Order Information Section
                    Text(
                        text = "Order Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Order Date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Order Date:")
                        Text(formatOrderDate(order.orderDate))
                    }

                    // Payment Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Status:")
                        Text(
                            text = payment?.paymentStatus?.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                            } ?: "Unknown",
                            color = when (payment?.paymentStatus?.lowercase()) {
                                "completed" -> Color(0xFF4CAF50)
                                "pending" -> Color(0xFFFF9800)
                                "failed" -> Color(0xFFF44336)
                                else -> MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Payment Method
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Payment Method:")
                        Text(
                            order.paymentMethod.replaceFirstChar {
                                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
                            },
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Delivery Address
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            "Delivery Address:",
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                        Text(
                            text = order.shippingAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Divider
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))

                    // Order Items Section
                    Text(
                        text = "Order Items",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (orderDetails.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.ShoppingCart,
                                contentDescription = "No items",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No items found in this order",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            Text(
                                text = "Order ID: ${order.orderId}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 300.dp)
                        ) {
                            items(orderDetails) { detail ->
                                OrderItemRow(orderDetail = detail)
                                if (orderDetails.indexOf(detail) < orderDetails.size - 1) {
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }

                    // Total Amount Section
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Amount:",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RM${"%.2f".format(order.totalPrice)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Additional order information
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Text(
                            text = "Need help with this order?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Contact customer support with your order ID.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(8.dp)
            ) {
                Text("Close Order Details")
            }
        }
    )
}

@Composable
fun OrderItemRow(orderDetail: OrderDetail) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = orderDetail.productName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "RM${"%.2f".format(orderDetail.productPrice)} × ${orderDetail.quantity}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        Text(
            text = "RM${"%.2f".format(orderDetail.subtotal)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun EmptyOrderState(
    icon: ImageVector = Icons.Default.ShoppingCart,
    title: String = "No orders yet",
    subtitle: String = "Your orders will appear here"
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "No orders",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}