package com.example.miniproject.screens.vendor

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.miniproject.model.Customer
import com.example.miniproject.model.Order
import com.example.miniproject.model.OrderDetail
import com.example.miniproject.model.Product
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.util.OrderStatusHelper
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import com.example.miniproject.service.PayPalService
import kotlinx.coroutines.tasks.await


object VendorOrdersScreen {
    @OptIn(ExperimentalMaterial3Api::class) // Opt-in for PullToRefreshBox
    @Composable
    fun VendorOrdersContent() {
        val context = LocalContext.current
        val authService = AuthService()
        val databaseService = DatabaseService()
        val payPalService = remember { PayPalService() }
        val coroutineScope = rememberCoroutineScope()

        var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var isRefreshing by remember { mutableStateOf(false) } // State for pull-to-refresh
        var selectedOrder by remember { mutableStateOf<Order?>(null) }
        var orderDetails by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
        var showOrderDetails by remember { mutableStateOf(false) }
        var selectedTab by remember { mutableStateOf(0) } // 0: Active, 1: Completed

        val showMigrationButton = remember { false }


        suspend fun fetchOrdersData() {
            isLoading = true
            val currentVendor = authService.getCurrentVendor()

            currentVendor?.let { vendor ->

                val fastOrders = databaseService.getOrdersByVendor(vendor.vendorId)

                orders = fastOrders.sortedWith(compareBy(
                    { when (it.status) {
                        "pending" -> 0
                        "confirmed" -> 1
                        "preparing" -> 2
                        "ready" -> 3
                        "completed" -> 4
                        "cancelled" -> 5
                        else -> 6
                    } },
                    { -it.orderDate.seconds }
                ))
            }
            isLoading = false
        }
        // Initial Load
        LaunchedEffect(Unit) {
            fetchOrdersData()
            isLoading = false
        }

        // Load Order Details
        LaunchedEffect(selectedOrder) {
            selectedOrder?.let { order ->
                orderDetails = databaseService.getOrderDetails(order.orderId)
                showOrderDetails = true
            }
        }

        // Helper to handle status update
        fun handleStatusUpdate(newStatus: String) {
            coroutineScope.launch {
                selectedOrder?.let { order ->
                    var shouldUpdateStatus = true

                    if (newStatus == "cancelled" && order.paymentMethod == "paypal") {
                        isLoading = true
                        try {
                            val orderSnapshot = databaseService.db.collection("orders")
                                .document(order.orderId)
                                .get()
                                .await()

                            val captureId = orderSnapshot.getString("paypalOrderId")

                            if (!captureId.isNullOrEmpty()) {
                                Toast.makeText(context, "Processing Refund...", Toast.LENGTH_SHORT).show()

                                val refundResult = payPalService.refundPayment(captureId)

                                if (refundResult.isSuccess) {
                                    Toast.makeText(context, "✅ Refund Successful!", Toast.LENGTH_LONG).show()

                                    databaseService.updatePaymentStatus(order.orderId, "refunded")
                                } else {
                                    val error = refundResult.exceptionOrNull()?.message ?: "Unknown error"
                                    Toast.makeText(context, "Refund Failed: $error", Toast.LENGTH_LONG).show()
                                    shouldUpdateStatus = false
                                }
                            } else {
                                Toast.makeText(context, "Error: PayPal ID missing!", Toast.LENGTH_LONG).show()
                                shouldUpdateStatus = false
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            shouldUpdateStatus = false
                        }
                    }

                    if (shouldUpdateStatus) {
                        databaseService.updateOrderStatus(order.orderId, newStatus)

                        if (newStatus == "cancelled") {
                            orderDetails.forEach { detail ->
                                databaseService.updateProductStock(detail.productId, -detail.quantity)
                            }
                        }
                        isLoading = true
                        fetchOrdersData()
                        isLoading = false
                        showOrderDetails = false
                        selectedOrder = null
                    } else {
                        isLoading = false
                    }
                }
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (showOrderDetails && selectedOrder != null) {
            OrderDetailsDialog(
                order = selectedOrder!!,
                orderDetails = orderDetails,
                onDismiss = {
                    showOrderDetails = false
                    selectedOrder = null
                },
                onUpdateStatus = { newStatus -> handleStatusUpdate(newStatus) }
            )
        } else {
            //  MAIN CONTENT WITH PULL TO REFRESH
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    isRefreshing = true
                    coroutineScope.launch {
                        fetchOrdersData()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        "Customer Orders",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Status Tabs
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            text = { Text("Active Orders") },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 }
                        )
                        Tab(
                            text = { Text("Order History") },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Filter orders based on tab
                    val filteredOrders = when (selectedTab) {
                        0 -> orders.filter { it.status in listOf("pending", "confirmed", "preparing", "ready") }
                        1 -> orders.filter { it.status in listOf("completed", "cancelled") }
                        else -> emptyList()
                    }

                    if (filteredOrders.isEmpty()) {
                        when (selectedTab) {
                            0 -> EmptyActiveOrdersState()
                            1 -> EmptyOrderHistoryState()
                        }
                    } else {
                        // Pass scrollable content properly inside PullToRefreshBox
                        OrdersList(
                            orders = filteredOrders,
                            onOrderClick = { order -> selectedOrder = order }
                        )
                    }
                }
            }
        }
    }
    @Composable
    private fun EmptyActiveOrdersState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = "No active orders",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No Active Orders",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "New orders will appear here when customers place orders",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun EmptyOrderHistoryState() {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = "No order history",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No Order History",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Completed and cancelled orders will appear here",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }

    @Composable
    private fun OrdersList(
        orders: List<Order>,
        onOrderClick: (Order) -> Unit
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(orders) { order ->
                OrderItemCard(
                    order = order,
                    onClick = { onOrderClick(order) }
                )
            }
        }
    }

    @Composable
    private fun OrderItemCard(order: Order, onClick: () -> Unit) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() },
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
                    Column {
                        Text(
                            text = "Order #${order.orderId}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Placed on ${order.orderDate.toDate().toString().substring(0, 16)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    OrderStatusBadge(status = order.status)
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Order Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total: RM${"%.2f".format(order.totalPrice)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Next Action Indicator
                    NextActionIndicator(status = order.status)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Tap to manage order",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    @Composable
    private fun OrderStatusBadge(status: String) {
        val statusColor = OrderStatusHelper.getStatusColor(status)

        Box(
            modifier = Modifier
                .background(
                    color = statusColor.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = OrderStatusHelper.getStatusDisplayText(status),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = statusColor
            )
        }
    }

    @Composable
    private fun NextActionIndicator(status: String) {
        val (actionText, actionColor) = when (status) {
            "pending" -> "Confirm Order" to MaterialTheme.colorScheme.primary
            "confirmed" -> "Start Preparing" to MaterialTheme.colorScheme.primary
            "preparing" -> "Mark Ready" to MaterialTheme.colorScheme.primary
            "ready" -> "Complete Order" to MaterialTheme.colorScheme.primary
            else -> "View Details" to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Text(
            text = actionText,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = actionColor
        )
    }

    @Composable
    private fun OrderDetailsDialog(
        order: Order,
        orderDetails: List<OrderDetail>,
        onDismiss: () -> Unit,
        onUpdateStatus: (String) -> Unit
    ) {
        val authService = AuthService()
        val databaseService = DatabaseService()
        var vendorProducts by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
        var customer by remember { mutableStateOf<Customer?>(null) }
        var allProducts by remember { mutableStateOf<Map<String, Product>>(emptyMap()) }

        LaunchedEffect(order, orderDetails) {
            val currentVendor = authService.getCurrentVendor()
            // Filter order details to show only products from this vendor
            vendorProducts = orderDetails.filter { detail ->
                val product = databaseService.getProductById(detail.productId)
                product?.vendorId == currentVendor?.vendorId
            }

            // Get customer info
            customer = databaseService.getCustomerById(order.customerId)

            // Get product details for all items
            val productsMap = mutableMapOf<String, Product>()
            orderDetails.forEach { detail ->
                val product = databaseService.getProductById(detail.productId)
                product?.let { productsMap[detail.productId] = it }
            }
            allProducts = productsMap
        }

        Dialog(
            onDismissRequest = onDismiss
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Order Management",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Current Status
                    CurrentStatusSection(order)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Order Information
                    OrderInfoSection(order, customer)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Products List
                    ProductsListSection(vendorProducts, allProducts)

                    Spacer(modifier = Modifier.height(16.dp))

                    // Order Actions
                    OrderActionsSection(order, onUpdateStatus, onDismiss)
                }
            }
        }
    }

    @Composable
    private fun CurrentStatusSection(order: Order) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = OrderStatusHelper.getStatusColor(order.status).copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Current Status",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = OrderStatusHelper.getStatusDisplayText(order.status),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = OrderStatusHelper.getStatusColor(order.status)
                )
            }
        }
    }

    @Composable
    private fun OrderInfoSection(order: Order, customer: Customer?) {
        Column {
            Text(
                "Order Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            InfoRow("Order ID:", order.orderId)
            InfoRow("Order Date:", order.orderDate.toDate().toString().substring(0, 16))
            InfoRow("Customer:", customer?.name ?: "Unknown Customer")
            InfoRow("Contact:", customer?.phoneNumber ?: "N/A")
            InfoRow("Shipping Address:", order.shippingAddress)
            InfoRow("Payment Method:", order.paymentMethod.replaceFirstChar { it.uppercase() })
            InfoRow("Total Amount:", "RM${"%.2f".format(order.totalPrice)}")
        }
    }

    @Composable
    private fun ProductsListSection(
        vendorProducts: List<OrderDetail>,
        allProducts: Map<String, Product>
    ) {
        Column {
            Text(
                "Your Products in this Order",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (vendorProducts.isEmpty()) {
                Text(
                    "No products from your store in this order",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                vendorProducts.forEach { detail ->
                    val product = allProducts[detail.productId]
                    ProductOrderItem(detail, product)
                }

                // Total for vendor products
                val vendorTotal = vendorProducts.sumOf { it.subtotal }
                Spacer(modifier = Modifier.height(8.dp))
                Divider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Your Total:",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "RM${"%.2f".format(vendorTotal)}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    @Composable
    private fun ProductOrderItem(detail: OrderDetail, product: Product?) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = product?.productName ?: "Unknown Product",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Quantity: ${detail.quantity} × RM${"%.2f".format(detail.productPrice)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = "RM${"%.2f".format(detail.subtotal)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }

    @Composable
    private fun OrderActionsSection(
        order: Order,
        onUpdateStatus: (String) -> Unit,
        onDismiss: () -> Unit
    ) {
        val nextStatus = when (order.status) {
            "pending" -> "confirmed"
            "confirmed" -> "preparing"
            "preparing" -> "ready"
            "ready" -> "completed"
            else -> null
        }

        Column {
            Text(
                "Order Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (nextStatus != null) {
                Button(
                    onClick = {
                        onUpdateStatus(nextStatus)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        when (nextStatus) {
                            "confirmed" -> Icons.Default.Check
                            "preparing" -> Icons.Default.Restaurant
                            "ready" -> Icons.Default.DoneAll
                            "completed" -> Icons.Default.Star
                            else -> Icons.Default.Edit
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        when (nextStatus) {
                            "confirmed" -> "Confirm Order"
                            "preparing" -> "Start Preparing"
                            "ready" -> "Mark as Ready for Pickup"
                            "completed" -> "Complete Order"
                            else -> "Update Status"
                        }
                    )
                }
            }

            if (order.status != "cancelled" && order.status != "completed") {
                OutlinedButton(
                    onClick = {
                        onUpdateStatus("cancelled")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.Cancel, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cancel Order")
                }
            }

            if (order.status == "completed" || order.status == "cancelled") {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Text("Close")
                }
            }
        }
    }

    @Composable
    private fun InfoRow(label: String, value: String) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(120.dp)
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    @Composable
    private fun rememberScrollState(): androidx.compose.foundation.ScrollState {
        return androidx.compose.foundation.rememberScrollState()
    }
}