package com.example.miniproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Customer
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUserManagementScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    // State variables
    var customers by remember { mutableStateOf<List<Customer>>(emptyList()) }
    var vendors by remember { mutableStateOf<List<Vendor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }

    // Dialog States
    var showFreezeDialog by remember { mutableStateOf(false) }
    var userToFreeze by remember { mutableStateOf<Any?>(null) }
    var freezeAction by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<Any?>(null) }

    // Snackbar
    val snackbarHostState = remember { SnackbarHostState() }

    // Define data loading function to reuse it
    val loadData = {
        coroutineScope.launch {
            isLoading = true
            try {
                // Get customers
                val allCustomers = databaseService.getAllCustomers()
                customers = allCustomers.map { customer ->
                    val stats = databaseService.calculateCustomerActualStats(customer.customerId)
                    customer.copy(orderCount = stats.first, totalSpent = stats.second)
                }

                // Get vendors
                val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                vendors = allVendors.map { vendor ->
                    val stats = databaseService.calculateVendorActualStats(vendor.vendorId)
                    vendor.copy(
                        orderCount = stats.first,
                        totalRevenue = stats.second,
                        rating = stats.third
                    )
                }
                isLoading = false
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    // Load Data Initially
    LaunchedEffect(Unit) {
        loadData()
    }

    // Filters
    val filteredCustomers = customers.filter {
        searchQuery.isBlank() || it.name.contains(searchQuery, true) || it.email.contains(searchQuery, true)
    }
    val filteredVendors = vendors.filter {
        searchQuery.isBlank() || it.vendorName.contains(searchQuery, true) || it.email.contains(searchQuery, true)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    Text(
                        text = "User Management",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )

                    // REFRESH BUTTON (Added here, Badges removed)
                    IconButton(
                        onClick = { loadData() }, // Call the load function
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

                // Search Bar
                ModernUserSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Custom Tab Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .height(48.dp)
                        .background(Color(0xFFF5F6F9), RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabButton(
                        text = "Customers",
                        count = customers.size,
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        text = "Vendors",
                        count = vendors.size,
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // --- User List ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (selectedTab == 0) {
                        if (filteredCustomers.isEmpty()) item { EmptyState("No customers found") }
                        else {
                            items(filteredCustomers) { customer ->
                                CustomerCard(
                                    customer = customer,
                                    dateFormat = dateFormat,
                                    onFreeze = {
                                        userToFreeze = customer
                                        freezeAction = !customer.isFrozen
                                        showFreezeDialog = true
                                    },
                                    onDelete = {
                                        userToDelete = customer
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    } else {
                        if (filteredVendors.isEmpty()) item { EmptyState("No vendors found") }
                        else {
                            items(filteredVendors) { vendor ->
                                VendorCard(
                                    vendor = vendor,
                                    dateFormat = dateFormat,
                                    onFreeze = {
                                        userToFreeze = vendor
                                        freezeAction = !vendor.isFrozen
                                        showFreezeDialog = true
                                    },
                                    onDelete = {
                                        userToDelete = vendor
                                        showDeleteDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Dialogs (Code unchanged from previous version, omitted for brevity but should be kept) ---
    // ... [Paste the dialog code blocks here if you are replacing the entire file, otherwise they stay the same] ...

    // Freeze Dialog
    if (showFreezeDialog && userToFreeze != null) {
        val name = if (userToFreeze is Customer) (userToFreeze as Customer).name else (userToFreeze as Vendor).vendorName
        AlertDialog(
            onDismissRequest = { showFreezeDialog = false },
            title = { Text(if (freezeAction) "Freeze Account" else "Unfreeze Account", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to ${if(freezeAction) "freeze" else "unfreeze"} $name?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val isCust = userToFreeze is Customer
                            if (isCust) {
                                val c = userToFreeze as Customer
                                val res = if (freezeAction) databaseService.freezeCustomerAccount(c.customerId) else databaseService.unfreezeCustomerAccount(c.customerId)
                                if (res.isSuccess) customers = customers.map { if(it.customerId == c.customerId) it.copy(isFrozen = freezeAction) else it }
                            } else {
                                val v = userToFreeze as Vendor
                                val res = if (freezeAction) databaseService.freezeVendorAccount(v.vendorId) else databaseService.unfreezeVendorAccount(v.vendorId)
                                if (res.isSuccess) vendors = vendors.map { if(it.vendorId == v.vendorId) it.copy(isFrozen = freezeAction) else it }
                            }
                            showFreezeDialog = false
                            snackbarHostState.showSnackbar("Status updated")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (freezeAction) Color.Red else Color(0xFF4CAF50))
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showFreezeDialog = false }) { Text("Cancel") } }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && userToDelete != null) {
        val name = if (userToDelete is Customer) (userToDelete as Customer).name else (userToDelete as Vendor).vendorName
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Account", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("Permanently delete $name? This cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (userToDelete is Customer) {
                                databaseService.deleteCustomer((userToDelete as Customer).customerId)
                                customers = customers.filter { it.customerId != (userToDelete as Customer).customerId }
                            } else {
                                databaseService.deleteVendor((userToDelete as Vendor).vendorId)
                                vendors = vendors.filter { it.vendorId != (userToDelete as Vendor).vendorId }
                            }
                            showDeleteDialog = false
                            snackbarHostState.showSnackbar("Account deleted")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ModernUserSearchBar(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        placeholder = { Text("Search by name or email...", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
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
fun TabButton(text: String, count: Int, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) Color.White else Color.Transparent)
            .clickable(onClick = onClick)
            .then(if (selected) Modifier.shadow(elevation = 2.dp, shape = RoundedCornerShape(10.dp)) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) Color(0xFF333333) else Color.Gray,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .background(if (selected) Color(0xFF2196F3).copy(alpha = 0.1f) else Color.Transparent, CircleShape)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(count.toString(), fontSize = 10.sp, color = if (selected) Color(0xFF2196F3) else Color.Gray)
            }
        }
    }
}

@Composable
fun StatusChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CustomerCard(customer: Customer, dateFormat: SimpleDateFormat, onFreeze: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, null, tint = Color(0xFF2196F3))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(customer.email, fontSize = 12.sp, color = Color.Gray)
                }

                // Menu
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color.White)) {
                        DropdownMenuItem(
                            text = { Text(if (customer.isFrozen) "Unfreeze" else "Freeze") },
                            onClick = { showMenu = false; onFreeze() },
                            leadingIcon = { Icon(if (customer.isFrozen) Icons.Default.LockOpen else Icons.Default.Lock, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Logins", value = "${customer.loginCount}")
                VerticalDivider(Modifier.height(24.dp))
                StatItem(label = "Orders", value = "${customer.orderCount}")
                VerticalDivider(Modifier.height(24.dp))
                StatItem(label = "Spent", value = "RM${String.format(Locale.getDefault(), "%.2f", customer.totalSpent)}", isMoney = true)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Joined: ${dateFormat.format(customer.createdAt.toDate())}", fontSize = 10.sp, color = Color.LightGray)
                StatusChip(
                    label = if (customer.isFrozen) "FROZEN" else "ACTIVE",
                    color = if (customer.isFrozen) Color.Red else Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun VendorCard(vendor: Vendor, dateFormat: SimpleDateFormat, onFreeze: () -> Unit, onDelete: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).background(Color(0xFFFF9800).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Store, null, tint = Color(0xFFFF9800))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(vendor.vendorName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(vendor.category, fontSize = 12.sp, color = Color.Gray)
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, null, tint = Color.Gray)
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }, modifier = Modifier.background(Color.White)) {
                        DropdownMenuItem(
                            text = { Text(if (vendor.isFrozen) "Unfreeze" else "Freeze") },
                            onClick = { showMenu = false; onFreeze() },
                            leadingIcon = { Icon(if (vendor.isFrozen) Icons.Default.LockOpen else Icons.Default.Lock, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = { showMenu = false; onDelete() },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Stats
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Revenue", value = "RM${String.format(Locale.getDefault(), "%.0f", vendor.totalRevenue)}", isMoney = true)
                VerticalDivider(Modifier.height(24.dp))
                StatItem(label = "Orders", value = "${vendor.orderCount}")
                VerticalDivider(Modifier.height(24.dp))
                StatItem(label = "Rating", value = String.format(Locale.getDefault(), "%.1f", vendor.rating))
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Contact: ${vendor.vendorContact}", fontSize = 10.sp, color = Color.LightGray)
                StatusChip(
                    label = if (vendor.isFrozen) "FROZEN" else "ACTIVE",
                    color = if (vendor.isFrozen) Color.Red else Color(0xFF4CAF50)
                )
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String, isMoney: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isMoney) Color(0xFF4CAF50) else Color.Black)
        Text(label, fontSize = 10.sp, color = Color.Gray)
    }
}

@Composable
fun EmptyState(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Default.SearchOff, null, tint = Color.LightGray, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(message, color = Color.Gray)
    }
}