package com.example.miniproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.miniproject.model.*
import com.example.miniproject.service.DatabaseService
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderDetailsScreen(
    navController: NavController,
    orderId: String
) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val decimalFormat = DecimalFormat("#,##0.00")
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    // State variables
    var order by remember { mutableStateOf<Order?>(null) }
    var orderDetails by remember { mutableStateOf<List<OrderDetail>>(emptyList()) }
    var customer by remember { mutableStateOf<Customer?>(null) }
    var vendor by remember { mutableStateOf<Vendor?>(null) }
    var payment by remember { mutableStateOf<Payment?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Load order data
    LaunchedEffect(orderId) {
        coroutineScope.launch {
            try {
                // Load order
                val loadedOrder = databaseService.getOrderById(orderId)
                if (loadedOrder != null) {
                    order = loadedOrder

                    // Load order details
                    val details = databaseService.getOrderDetails(orderId)
                    orderDetails = details

                    // Load customer
                    val cust = databaseService.getCustomerById(loadedOrder.customerId)
                    customer = cust

                    // Try to find vendor from order details
                    val vendors = mutableSetOf<String>()
                    details.forEach { detail ->
                        val product = databaseService.getProductById(detail.productId)
                        product?.vendorId?.let { vendorId ->
                            vendors.add(vendorId)
                        }
                    }

                    // Load the first vendor (most orders have one vendor)
                    if (vendors.isNotEmpty()) {
                        val vendorId = vendors.first()
                        vendor = databaseService.getVendorById(vendorId)
                    }

                    // Load payment
                    payment = databaseService.getPaymentByOrder(orderId)
                }
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
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
                            "Order Details",
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        actionIconContentColor = Color.White
                    ),
                    modifier = Modifier.background(Color.Transparent)
                )
            }
        },
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
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Order not found",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Order Not Found",
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Order ID: $orderId",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    // Order Summary Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
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
                                Column {
                                    Text(
                                        "Order #${order!!.orderId}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        dateFormat.format(order!!.orderDate.toDate()),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                OrderStatusBadgeLarge(status = order!!.status)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Order Stats Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Total Amount
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Total Amount",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "RM${decimalFormat.format(order!!.totalPrice)}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Platform Fee
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        "Platform Fee (10%)",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "RM${decimalFormat.format(order!!.totalPrice * 0.10)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFF9800)
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    // Customer Information Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Person,
                                    contentDescription = "Customer",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Customer Information",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (customer != null) {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InfoRow(label = "Name", value = customer!!.name)
                                    InfoRow(label = "Customer ID", value = customer!!.customerId)
                                    InfoRow(label = "Email", value = customer!!.email)
                                    InfoRow(label = "Phone", value = customer!!.phoneNumber)

                                    // Customer Stats
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                customer!!.orderCount.toString(),
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Orders",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "RM${decimalFormat.format(customer!!.totalSpent)}",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Total Spent",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                if (customer!!.isFrozen) "Frozen" else "Active",
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (customer!!.isFrozen) Color.Red else Color.Green
                                            )
                                            Text(
                                                "Status",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "Customer information not found",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // Vendor Information (if available)
                if (vendor != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Store,
                                        contentDescription = "Vendor",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Vendor Information",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InfoRow(label = "Vendor Name", value = vendor!!.vendorName)
                                    InfoRow(label = "Vendor ID", value = vendor!!.vendorId)
                                    InfoRow(label = "Category",
                                        value = VendorCategory.getDisplayName(vendor!!.category))
                                    InfoRow(label = "Email", value = vendor!!.email)
                                    InfoRow(label = "Contact", value = vendor!!.vendorContact)
                                    InfoRow(label = "Address", value = vendor!!.address)

                                    // Vendor Stats
                                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                vendor!!.orderCount.toString(),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Orders",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                "RM${decimalFormat.format(vendor!!.totalRevenue)}",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Revenue",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                String.format("%.1f", vendor!!.rating),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Rating",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Text(
                                                if (vendor!!.isFrozen) "Frozen" else "Active",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (vendor!!.isFrozen) Color.Red else Color.Green
                                            )
                                            Text(
                                                "Status",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Order Items Section
                if (orderDetails.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.ShoppingCart,
                                        contentDescription = "Order Items",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Order Items (${orderDetails.size})",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                orderDetails.forEachIndexed { index, detail ->
                                    OrderItemRow(
                                        detail = detail,
                                        decimalFormat = decimalFormat,
                                        isLast = index == orderDetails.size - 1
                                    )
                                }

                                Divider(modifier = Modifier.padding(vertical = 12.dp))

                                // Order Summary
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Subtotal",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "RM${decimalFormat.format(orderDetails.sumOf { it.subtotal })}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Service Fee",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "RM${decimalFormat.format(order!!.totalPrice * 0.06)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Platform Fee (10%)",
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            "RM${decimalFormat.format(order!!.totalPrice * 0.10)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFF9800)
                                        )
                                    }
                                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            "Total Amount",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            "RM${decimalFormat.format(order!!.totalPrice)}",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Payment Information
                if (payment != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Payment,
                                        contentDescription = "Payment",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        "Payment Information",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    InfoRow(label = "Payment Method", value = payment!!.paymentMethod)
                                    InfoRow(label = "Amount",
                                        value = "RM${decimalFormat.format(payment!!.amount)}")
                                    InfoRow(label = "Status", value = payment!!.paymentStatus)
                                    InfoRow(
                                        label = "Transaction Date",
                                        value = dateFormat.format(payment!!.transactionDate.toDate())
                                    )
                                }
                            }
                        }
                    }
                }

                // Order Timeline
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Timeline,
                                    contentDescription = "Order Timeline",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "Order Timeline",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Timeline items - UPDATED FOR "completed" STATUS
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                TimelineItem(
                                    title = "Order Placed",
                                    date = order!!.orderDate,
                                    isCompleted = true,
                                    isCurrent = order!!.status == "pending"
                                )

                                TimelineItem(
                                    title = "Order Confirmed",
                                    date = if (order!!.status in listOf("confirmed", "preparing", "completed"))
                                        order!!.updatedAt else null,
                                    isCompleted = order!!.status in listOf("confirmed", "preparing", "completed"),
                                    isCurrent = order!!.status == "confirmed"
                                )

                                TimelineItem(
                                    title = "Preparing",
                                    date = if (order!!.status in listOf("preparing", "completed"))
                                        order!!.updatedAt else null,
                                    isCompleted = order!!.status in listOf("preparing", "completed"),
                                    isCurrent = order!!.status == "preparing"
                                )

                                TimelineItem(
                                    title = "Completed", // Changed from "Delivered"
                                    date = if (order!!.status == "completed")
                                        order!!.updatedAt else null,
                                    isCompleted = order!!.status == "completed",
                                    isCurrent = order!!.status == "completed"
                                )

                                if (order!!.status == "cancelled") {
                                    TimelineItem(
                                        title = "Cancelled",
                                        date = order!!.updatedAt,
                                        isCompleted = true,
                                        isCurrent = true,
                                        isCancelled = true
                                    )
                                }
                            }
                        }
                    }
                }

                // Action Buttons
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                "Order Actions",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Update Status Button
                                var expanded by remember { mutableStateOf(false) }
                                Box(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedButton(
                                        onClick = { expanded = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Update Status",
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Update Status")
                                    }
                                    DropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        listOf("pending", "confirmed", "preparing", "completed", "cancelled")
                                            .forEach { status ->
                                                DropdownMenuItem(
                                                    text = {
                                                        Text(status.replaceFirstChar {
                                                            it.titlecase(Locale.getDefault())
                                                        })
                                                    },
                                                    onClick = {
                                                        coroutineScope.launch {
                                                            databaseService.updateOrderStatus(orderId, status)
                                                            // Refresh order
                                                            order = databaseService.getOrderById(orderId)
                                                            expanded = false
                                                        }
                                                    }
                                                )
                                            }
                                    }
                                }

                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Composable
fun OrderStatusBadgeLarge(status: String) {
    val (backgroundColor, textColor) = when (status) {
        "pending" -> Color(0xFFFF9800) to Color.White
        "confirmed" -> Color(0xFF2196F3) to Color.White
        "preparing" -> Color(0xFF9C27B0) to Color.White
        "completed" -> Color(0xFF4CAF50) to Color.White // Changed from "delivered" to "completed"
        "cancelled" -> Color(0xFFF44336) to Color.White
        else -> Color(0xFF607D8B) to Color.White
    }

    Box(
        modifier = Modifier
            .background(backgroundColor, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            when (status) {
                "completed" -> "Completed" // Show "Completed" instead of "Delivered"
                else -> status.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            },
            fontSize = 12.sp,
            color = textColor,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f, fill = false),
            maxLines = 2
        )
    }
}

@Composable
fun OrderItemRow(
    detail: OrderDetail,
    decimalFormat: DecimalFormat,
    isLast: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    detail.productName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2
                )
                Text(
                    "Product ID: ${detail.productId}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "RM${decimalFormat.format(detail.productPrice)} × ${detail.quantity}",
                    fontSize = 14.sp
                )
                Text(
                    "RM${decimalFormat.format(detail.subtotal)}",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!isLast) {
            Divider(modifier = Modifier.padding(vertical = 8.dp))
        }
    }
}

@Composable
fun TimelineItem(
    title: String,
    date: Timestamp?,
    isCompleted: Boolean,
    isCurrent: Boolean = false,
    isCancelled: Boolean = false
) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Timeline dot
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = when {
                        isCancelled -> Color.Red
                        isCurrent -> Color(0xFFFF9800)
                        isCompleted -> Color(0xFF4CAF50)
                        else -> Color(0xFF9E9E9E)
                    },
                    shape = RoundedCornerShape(50)
                )
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                title,
                fontSize = 14.sp,
                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    isCancelled -> Color.Red
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )

            if (date != null) {
                Text(
                    dateFormat.format(date.toDate()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (isCurrent) {
                Text(
                    "In progress",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Text(
                    "Pending",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


