package com.example.miniproject.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Order
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminOrderListScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val decimalFormat = DecimalFormat("#,##0.00")
    val dateFormat = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())

    // State variables
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterStatus by remember { mutableStateOf("all") } // all, pending, confirmed, completed, cancelled

    // --- NEW: Debounce state to prevent double-click crashes ---
    var lastClickTime by remember { mutableLongStateOf(0L) }

    // Load orders function
    val loadOrders = {
        coroutineScope.launch {
            isLoading = true
            try {
                orders = databaseService.getAllOrders().sortedByDescending { it.orderDate.seconds }
                isLoading = false
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    // Initial Load
    LaunchedEffect(Unit) {
        loadOrders()
    }

    // Filter Logic
    val filteredOrders = orders.filter { order ->
        val matchesSearch = searchQuery.isBlank() ||
                order.orderId.contains(searchQuery, ignoreCase = true) ||
                order.customerId.contains(searchQuery, ignoreCase = true)

        val matchesStatus = filterStatus == "all" || order.status.equals(filterStatus, ignoreCase = true)
        matchesSearch && matchesStatus
    }

    // Stats
    val totalRevenue = orders.sumOf { it.totalPrice }
    val platformRevenue = totalRevenue * 0.10

    Scaffold(
        bottomBar = { AdminBottomNavigation(navController) },
        containerColor = Color(0xFFF5F6F9) // Light Grey Background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // --- Header Section ---
            Column(
                modifier = Modifier
                    .background(Color.White)
                    .padding(bottom = 16.dp)
            ) {
                // Title Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Order Management",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = "${filteredOrders.size} orders found",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { loadOrders() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF5F6F9), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.Black
                        )
                    }
                }

                // Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModernStatChip(
                        label = "Total Rev",
                        value = "RM${decimalFormat.format(totalRevenue)}",
                        color = Color(0xFF4CAF50),
                        modifier = Modifier.weight(1f)
                    )
                    ModernStatChip(
                        label = "Platform",
                        value = "RM${decimalFormat.format(platformRevenue)}",
                        color = Color(0xFFFF9800),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Search Bar
                ModernOrderSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Filter Tabs
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val statuses = listOf("all", "pending", "confirmed", "preparing", "completed", "cancelled")
                    items(statuses) { status ->
                        ModernFilterChip(
                            label = status.replaceFirstChar { it.uppercase() },
                            selected = filterStatus == status,
                            onClick = { filterStatus = status }
                        )
                    }
                }
            }

            // --- Order List ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredOrders.isEmpty()) {
                EmptyOrderState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredOrders) { order ->
                        ModernOrderItem(
                            order = order,
                            dateFormat = dateFormat,
                            decimalFormat = decimalFormat,
                            onViewClick = {
                                // --- UPDATED: Safe Navigation Logic ---
                                val currentTime = System.currentTimeMillis()
                                if (currentTime - lastClickTime > 500) { // 500ms debounce
                                    lastClickTime = currentTime
                                    navController.navigate("adminOrderDetails/${order.orderId}")
                                }
                            },
                            onUpdateStatus = { newStatus ->
                                coroutineScope.launch {
                                    databaseService.updateOrderStatus(order.orderId, newStatus)
                                    // Local update to avoid full reload flicker
                                    orders = orders.map {
                                        if (it.orderId == order.orderId) it.copy(status = newStatus) else it
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

// --- Modern UI Components ---

@Composable
fun ModernStatChip(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        }
    }
}

@Composable
fun ModernOrderSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        placeholder = { Text("Search Order ID...", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, null, tint = Color.Gray)
                }
            }
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFFAFAFA),
            unfocusedContainerColor = Color(0xFFFAFAFA),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        singleLine = true
    )
}

@Composable
fun ModernFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (selected) Color(0xFF2196F3) else Color.White)
            .border(1.dp, if (selected) Color(0xFF2196F3) else Color(0xFFE0E0E0), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else Color.Gray,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun ModernOrderItem(
    order: Order,
    dateFormat: SimpleDateFormat,
    decimalFormat: DecimalFormat,
    onViewClick: () -> Unit,
    onUpdateStatus: (String) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Row 1: ID and Date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, null, tint = Color(0xFF2196F3), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Order #${order.orderId.takeLast(6).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
                Text(
                    text = dateFormat.format(order.orderDate.toDate()),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Customer and Amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Customer", fontSize = 10.sp, color = Color.LightGray)
                    Text(
                        text = order.customerId.take(12) + "...", // Truncate ID
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                }

                // Price Tag
                Box(
                    modifier = Modifier
                        .background(Color(0xFFF5F6F9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "RM${decimalFormat.format(order.totalPrice)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Divider
            HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)

            Spacer(modifier = Modifier.height(12.dp))

            // Row 3: Status Badge and Menu Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status Badge
                ModernStatusBadge(status = order.status)

                // Actions Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(Icons.Default.MoreHoriz, null, tint = Color.Gray)
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Details") },
                            onClick = {
                                showMenu = false
                                onViewClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Visibility, null) }
                        )
                        HorizontalDivider()
                        // Status Update Sub-items
                        Text("Update Status", fontSize = 10.sp, color = Color.Gray, modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp))

                        val statusOptions = listOf("confirmed", "preparing", "completed", "cancelled")
                        statusOptions.forEach { status ->
                            if (status != order.status) {
                                DropdownMenuItem(
                                    text = { Text(status.replaceFirstChar { it.uppercase() }) },
                                    onClick = {
                                        showMenu = false
                                        onUpdateStatus(status)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = if (status == "cancelled") Icons.Default.Close else Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (status == "cancelled") Color.Red else Color.Gray
                                        )
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernStatusBadge(status: String) {
    val (color, label) = when (status.lowercase()) {
        "pending" -> Color(0xFFFF9800) to "Pending"
        "confirmed" -> Color(0xFF2196F3) to "Confirmed"
        "preparing" -> Color(0xFF9C27B0) to "Preparing"
        "completed" -> Color(0xFF4CAF50) to "Completed"
        "cancelled" -> Color(0xFFF44336) to "Cancelled"
        else -> Color.Gray to status
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun EmptyOrderState() {
    Column(
        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.ReceiptLong, null, tint = Color.LightGray, modifier = Modifier.size(80.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text("No orders found", fontSize = 16.sp, color = Color.Gray)
    }
}