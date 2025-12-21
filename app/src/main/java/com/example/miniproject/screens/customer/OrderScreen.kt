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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderScreen(navController: NavController) {
    val context = LocalContext.current
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    val notificationHelper = remember { NotificationHelper(context) }

    // States
    var allOrders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }
    var orderDetails by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
    var showOrderDetails by remember { mutableStateOf(false) }

    // Filter State (Default to "Active")
    var selectedFilter by remember { mutableStateOf("Active") }

    // Alert Dialog States
    var showPickupDialog by remember { mutableStateOf(false) }
    var pickupOrderId by remember { mutableStateOf("") }

    // Define status categories
    val activeStatuses = listOf("pending", "confirmed", "preparing", "ready", "delivered")
    val completedStatuses = listOf("completed")
    val cancelledStatuses = listOf("cancelled")

    // Derived State: Filtered Orders
    val filteredOrders = remember(allOrders, selectedFilter) {
        when (selectedFilter) {
            "Active" -> allOrders.filter { it.status.lowercase() in activeStatuses }
            "Completed" -> allOrders.filter { it.status.lowercase() in completedStatuses }
            "Cancelled" -> allOrders.filter { it.status.lowercase() in cancelledStatuses }
            else -> allOrders
        }
    }

    // Permission Launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                Log.d("OrderScreen", "Notification permission granted")
            }
        }
    )

    LaunchedEffect(Unit) {
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

                // Get ALL orders
                val fetchedOrders = databaseService.getCustomerOrders(customer.customerId)
                allOrders = fetchedOrders.sortedByDescending { it.orderDate }
                isLoading = false

                // Check for ready orders (for notification)
                val alreadyReadyOrder = allOrders.find { it.status.equals("ready", ignoreCase = true) }
                if (alreadyReadyOrder != null) {
                    pickupOrderId = alreadyReadyOrder.getDisplayOrderId()
                    showPickupDialog = true
                    notificationHelper.showOrderReadyNotification(pickupOrderId)
                }

                // Real-time listener
                databaseService.listenToOrderUpdates(customer.customerId) { readyOrder ->
                    notificationHelper.showOrderReadyNotification(readyOrder.getDisplayOrderId())
                    pickupOrderId = readyOrder.getDisplayOrderId()
                    showPickupDialog = true

                    // Refresh list
                    coroutineScope.launch {
                        val updatedOrders = databaseService.getCustomerOrders(customer.customerId)
                        allOrders = updatedOrders.sortedByDescending { it.orderDate }
                    }
                }

            } catch (e: Exception) {
                isLoading = false
                Log.e("OrderScreen", "Error loading orders: ${e.message}")
            }
        }
    }

    // Pickup Dialog
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
            title = { Text("Order Ready for Pickup!", fontWeight = FontWeight.Bold) },
            text = { Text("Order #$pickupOrderId is ready! Please collect your food at the counter.") },
            confirmButton = {
                TextButton(onClick = { showPickupDialog = false }) { Text("OK, I'm Coming") }
            }
        )
    }

    // Details Dialog
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
        // Main Screen
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

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChipItem(
                    label = "Active",
                    selected = selectedFilter == "Active",
                    onClick = { selectedFilter = "Active" }
                )
                FilterChipItem(
                    label = "Completed",
                    selected = selectedFilter == "Completed",
                    onClick = { selectedFilter = "Completed" }
                )
                FilterChipItem(
                    label = "Cancelled",
                    selected = selectedFilter == "Cancelled",
                    onClick = { selectedFilter = "Cancelled" }
                )
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredOrders.isEmpty()) {
                EmptyOrderState(
                    title = "No ${selectedFilter.lowercase()} orders",
                    subtitle = "Orders will appear here"
                )
            } else {
                LazyColumn {
                    items(filteredOrders) { order ->
                        OrderCard(
                            order = order,
                            onClick = {
                                // Logic: If Active -> Go to Tracking. If History -> Show Dialog.
                                if (order.status.lowercase() in activeStatuses) {
                                    navController.navigate("tracking/${order.orderId}")
                                } else {
                                    coroutineScope.launch {
                                        selectedOrder = order
                                        orderDetails = databaseService.getOrderDetails(order.orderId)
                                        showOrderDetails = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterChipItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    ElevatedFilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        },
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

            Text(
                text = "Placed on ${formatOrderDate(order.orderDate)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )

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

            // Hint Text varies based on status
            val isHistory = order.status.lowercase() in listOf("completed", "cancelled")
            Text(
                text = if (isHistory) "Tap to view details" else "Tap to track order",
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
        } catch (e: Exception) { }
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
                Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column {
                    // INFO SECTION
                    Text("Order Information", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Order Date:")
                        Text(formatOrderDate(order.orderDate))
                    }

                    Spacer(Modifier.height(8.dp))

                    // ITEMS SECTION
                    Text("Order Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 12.dp))

                    if (orderDetails.isEmpty()) {
                        Text("No items found", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                            items(orderDetails) { detail ->
                                OrderItemRow(detail)
                                if (orderDetails.indexOf(detail) < orderDetails.size - 1) Divider(Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }

                    Divider(Modifier.padding(vertical = 8.dp))

                    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                        Text("Total Amount:", fontWeight = FontWeight.Bold)
                        Text("RM${"%.2f".format(order.totalPrice)}", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
@Composable
fun OrderItemRow(orderDetail: OrderDetail) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = orderDetail.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(text = "RM${"%.2f".format(orderDetail.productPrice)} × ${orderDetail.quantity}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(top = 2.dp))
        }
        Text(text = "RM${"%.2f".format(orderDetail.subtotal)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
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