package com.example.miniproject.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

    // --- NEW: State to prevent double clicks (Crash Prevention) ---
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

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
        containerColor = Color(0xFFF5F6F9), // Light Grey Background
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Order Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color(0xFF2196F3)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        // --- UPDATED: Safe Back Button Logic ---
                        val currentTime = System.currentTimeMillis()
                        // Only allow action if 500ms has passed since last click
                        if (currentTime - lastBackClickTime > 500) {
                            lastBackClickTime = currentTime
                            navController.popBackStack()
                        }
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF333333)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
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
                        Icons.Default.ErrorOutline,
                        contentDescription = "Not Found",
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Text(
                        "Order Not Found",
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
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
                // 1. Order ID & Status Header Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Order #${order!!.orderId.takeLast(8).uppercase()}",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF333333)
                                    )
                                    Text(
                                        text = dateFormat.format(order!!.orderDate.toDate()),
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }
                                ModernStatusBadgeLarge(status = order!!.status)
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            HorizontalDivider(color = Color(0xFFF5F5F5))
                            Spacer(modifier = Modifier.height(20.dp))

                            // Key Financials
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                FinancialColumn(
                                    label = "Total Amount",
                                    value = "RM${decimalFormat.format(order!!.totalPrice)}",
                                    isHighlight = true
                                )
                                FinancialColumn(
                                    label = "Platform Fee (10%)",
                                    value = "RM${decimalFormat.format(order!!.totalPrice * 0.10)}",
                                    isHighlight = false
                                )
                            }
                        }
                    }
                }

                // 2. Timeline Card
                item {
                    ModernSectionCard(title = "Order Timeline", icon = Icons.Default.Timeline, iconColor = Color(0xFF9C27B0)) {
                        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                            // Order Placed
                            ModernTimelineItem(
                                title = "Order Placed",
                                date = order!!.orderDate,
                                state = TimelineState.COMPLETED,
                                isFirst = true,
                                isLast = false
                            )

                            // Confirmed
                            val isConfirmed = order!!.status in listOf("confirmed", "preparing", "completed")
                            ModernTimelineItem(
                                title = "Order Confirmed",
                                date = if (isConfirmed) order!!.updatedAt else null,
                                state = if (isConfirmed) TimelineState.COMPLETED else TimelineState.PENDING,
                                isFirst = false,
                                isLast = false
                            )

                            // Preparing
                            val isPreparing = order!!.status in listOf("preparing", "completed")
                            ModernTimelineItem(
                                title = "Preparing",
                                date = if (isPreparing) order!!.updatedAt else null,
                                state = if (isPreparing) TimelineState.COMPLETED else TimelineState.PENDING,
                                isFirst = false,
                                isLast = false
                            )

                            // Completed/Cancelled
                            if (order!!.status == "cancelled") {
                                ModernTimelineItem(
                                    title = "Cancelled",
                                    date = order!!.updatedAt,
                                    state = TimelineState.CANCELLED,
                                    isFirst = false,
                                    isLast = true
                                )
                            } else {
                                val isCompleted = order!!.status == "completed"
                                ModernTimelineItem(
                                    title = "Completed",
                                    date = if (isCompleted) order!!.updatedAt else null,
                                    state = if (isCompleted) TimelineState.COMPLETED else TimelineState.PENDING,
                                    isFirst = false,
                                    isLast = true
                                )
                            }
                        }
                    }
                }

                // 3. Customer & Vendor Info
                item {
                    ModernSectionCard(title = "Customer Details", icon = Icons.Default.Person, iconColor = Color(0xFF2196F3)) {
                        if (customer != null) {
                            ModernInfoRow(icon = Icons.Default.Badge, label = "Name", value = customer!!.name)
                            ModernInfoRow(icon = Icons.Default.Email, label = "Email", value = customer!!.email)
                            ModernInfoRow(icon = Icons.Default.Phone, label = "Phone", value = customer!!.phoneNumber)
                        } else {
                            Text("Customer info unavailable", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                if (vendor != null) {
                    item {
                        ModernSectionCard(title = "Vendor Details", icon = Icons.Default.Store, iconColor = Color(0xFFFF9800)) {
                            ModernInfoRow(icon = Icons.Default.Storefront, label = "Name", value = vendor!!.vendorName)
                            // FIX: Replaced VendorCategory.getDisplayName with simple string formatting to prevent crashes
                            ModernInfoRow(icon = Icons.Default.Category, label = "Category", value = vendor!!.category.replaceFirstChar { it.uppercase() })
                            ModernInfoRow(icon = Icons.Default.Phone, label = "Contact", value = vendor!!.vendorContact)
                        }
                    }
                }

                // 4. Order Items
                if (orderDetails.isNotEmpty()) {
                    item {
                        ModernSectionCard(title = "Items Ordered", icon = Icons.Default.ShoppingBag, iconColor = Color(0xFFE91E63)) {
                            val mergedDetails = remember(orderDetails) {
                                orderDetails.groupBy { it.productId }.map { (_, items) ->
                                    items.first().copy(
                                        quantity = items.sumOf { it.quantity },
                                        subtotal = items.sumOf { it.subtotal }
                                    )
                                }
                            }

                            mergedDetails.forEachIndexed { index, detail ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(detail.productName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text("Qty: ${detail.quantity}", fontSize = 12.sp, color = Color.Gray)
                                    }
                                    Text(
                                        "RM${decimalFormat.format(detail.subtotal)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                if (index < mergedDetails.lastIndex) {
                                    HorizontalDivider(color = Color(0xFFF5F5F5))
                                }
                            }
                        }
                    }
                }

                // 5. Payment Info
                if (payment != null) {
                    item {
                        ModernSectionCard(title = "Payment", icon = Icons.Default.Payment, iconColor = Color(0xFF009688)) {
                            ModernInfoRow(icon = Icons.Default.CreditCard, label = "Method", value = payment!!.paymentMethod)
                            ModernInfoRow(icon = Icons.Default.CheckCircle, label = "Status", value = payment!!.paymentStatus)
                            ModernInfoRow(icon = Icons.Default.Schedule, label = "Date", value = dateFormat.format(payment!!.transactionDate.toDate()))
                        }
                    }
                }

                // 6. Action Button (Update Status)
                item {
                    var expanded by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expanded = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Order Status", fontSize = 16.sp)
                        }

                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.background(Color.White).width(250.dp)
                        ) {
                            val statuses = listOf("pending", "confirmed", "preparing", "completed", "cancelled")
                            statuses.forEach { status ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            status.replaceFirstChar { it.uppercase() },
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    onClick = {
                                        coroutineScope.launch {
                                            databaseService.updateOrderStatus(orderId, status)
                                            val updatedOrder = databaseService.getOrderById(orderId)
                                            if (updatedOrder != null) order = updatedOrder
                                            expanded = false
                                        }
                                    },
                                    leadingIcon = {
                                        // FIX: Replaced OrderStatusHelper with local logic
                                        val color = when (status.lowercase()) {
                                            "pending" -> Color(0xFFFF9800)
                                            "confirmed" -> Color(0xFF2196F3)
                                            "preparing" -> Color(0xFF9C27B0)
                                            "completed" -> Color(0xFF4CAF50)
                                            "cancelled" -> Color(0xFFF44336)
                                            else -> Color.Gray
                                        }
                                        Box(modifier = Modifier.size(12.dp).background(color, CircleShape))
                                    }
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// --- Local Helpers (No external files required) ---

@Composable
fun ModernSectionCard(
    title: String,
    icon: ImageVector,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iconColor.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun FinancialColumn(label: String, value: String, isHighlight: Boolean) {
    Column {
        Text(label, fontSize = 12.sp, color = Color.Gray)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else Color(0xFFFF9800)
        )
    }
}

@Composable
fun ModernInfoRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.DarkGray)
    }
}

@Composable
fun ModernStatusBadgeLarge(status: String) {
    val (color, label) = when (status.lowercase()) {
        "pending" -> Color(0xFFFF9800) to "Pending"
        "confirmed" -> Color(0xFF2196F3) to "Confirmed"
        "preparing" -> Color(0xFF9C27B0) to "Preparing"
        "completed" -> Color(0xFF4CAF50) to "Completed"
        "cancelled" -> Color(0xFFF44336) to "Cancelled"
        else -> Color.Gray to status
    }

    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class TimelineState { COMPLETED, PENDING, CANCELLED }

@Composable
fun ModernTimelineItem(
    title: String,
    date: Timestamp?,
    state: TimelineState,
    isFirst: Boolean,
    isLast: Boolean
) {
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    val color = when(state) {
        TimelineState.COMPLETED -> Color(0xFF4CAF50)
        TimelineState.CANCELLED -> Color(0xFFF44336)
        TimelineState.PENDING -> Color(0xFFE0E0E0)
    }

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(24.dp)) {
            if (!isFirst) {
                Box(modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (state == TimelineState.PENDING) Color(0xFFE0E0E0) else color))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(color, CircleShape)
                    .then(if (state == TimelineState.PENDING) Modifier else Modifier.padding(3.dp).background(Color.White, CircleShape))
            )

            if (!isLast) {
                Box(modifier = Modifier
                    .width(2.dp)
                    .weight(1f)
                    .background(if (state == TimelineState.PENDING) Color(0xFFE0E0E0) else color))
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            Text(
                title,
                fontWeight = if (state != TimelineState.PENDING) FontWeight.Bold else FontWeight.Normal,
                color = if (state != TimelineState.PENDING) Color.Black else Color.Gray,
                fontSize = 14.sp
            )
            if (date != null) {
                Text(dateFormat.format(date.toDate()), fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}