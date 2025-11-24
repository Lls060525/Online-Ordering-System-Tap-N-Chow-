package com.example.miniproject.screens

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
import kotlinx.coroutines.launch

object VendorOrdersScreen {
    @Composable
    fun VendorOrdersContent() {
        val authService = AuthService()
        val databaseService = DatabaseService()
        val coroutineScope = rememberCoroutineScope()

        var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }
        var selectedOrder by remember { mutableStateOf<Order?>(null) }
        var orderDetails by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
        var showOrderDetails by remember { mutableStateOf(false) }

        // Function to refresh orders
        fun refreshOrders() {
            coroutineScope.launch {
                val currentVendor = authService.getCurrentVendor()
                currentVendor?.let { vendor ->
                    val allOrders = databaseService.getAllOrders()
                    val vendorOrders = mutableListOf<Order>()

                    for (order in allOrders) {
                        val details = databaseService.getOrderDetails(order.orderId)
                        val hasVendorProducts = details.any { detail ->
                            val product = databaseService.getProductById(detail.productId)
                            product?.vendorId == vendor.vendorId
                        }
                        if (hasVendorProducts) {
                            vendorOrders.add(order)
                        }
                    }
                    orders = vendorOrders.sortedByDescending { it.orderDate }
                }
            }
        }

        // Load orders on startup
        LaunchedEffect(Unit) {
            refreshOrders()
            isLoading = false
        }

        // Load order details when an order is selected
        LaunchedEffect(selectedOrder) {
            selectedOrder?.let { order ->
                orderDetails = databaseService.getOrderDetails(order.orderId)
                showOrderDetails = true
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
                onUpdateStatus = { newStatus ->
                    coroutineScope.launch {
                        selectedOrder?.let { order ->
                            databaseService.updateOrderStatus(order.orderId, newStatus)
                            refreshOrders()
                        }
                    }
                }
            )
        } else {
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

                Text(
                    "Orders containing your products",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (orders.isEmpty()) {
                    EmptyOrdersState()
                } else {
                    OrdersList(
                        orders = orders,
                        onOrderClick = { order ->
                            selectedOrder = order
                        }
                    )
                }
            }
        }
    }

    @Composable
    private fun EmptyOrdersState() {
        Box(
            modifier = Modifier
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    Icons.Default.Receipt,
                    contentDescription = "No orders",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No orders yet",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Orders from customers will appear here when they purchase your products",
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Order #${order.orderId}", // Show full order ID
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

                    Text(
                        text = "Tap to view details",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }

    @Composable
    private fun OrderStatusBadge(status: String) {
        val (backgroundColor, textColor) = when (status) {
            "pending" -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
            "confirmed" -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
            "preparing" -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
            "ready" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) to MaterialTheme.colorScheme.primary
            "completed" -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
            "cancelled" -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
            else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = Modifier
                .background(
                    color = backgroundColor,
                    shape = RoundedCornerShape(8.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = status.replaceFirstChar { it.uppercase() },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = textColor
            )
        }
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
                            "Order Details",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

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
    private fun OrderInfoSection(order: Order, customer: Customer?) {
        Column {
            Text(
                "Order Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            InfoRow("Order ID:", order.orderId) // Show full order ID
            InfoRow("Order Date:", order.orderDate.toDate().toString())
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
                        text = "Quantity: ${detail.quantity}",
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
                "Order Status: ${order.status.replaceFirstChar { it.uppercase() }}",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            if (nextStatus != null) {
                Button(
                    onClick = {
                        onUpdateStatus(nextStatus)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as ${nextStatus.replaceFirstChar { it.uppercase() }}")
                }
            }

            if (order.status != "cancelled") {
                OutlinedButton(
                    onClick = {
                        onUpdateStatus("cancelled")
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Order")
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

    // Add this function to support scrolling in the dialog
    @Composable
    private fun rememberScrollState(): androidx.compose.foundation.ScrollState {
        return androidx.compose.foundation.rememberScrollState()
    }
}