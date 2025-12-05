package com.example.miniproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<Any?>(null) }
    var showFreezeDialog by remember { mutableStateOf(false) }
    var userToFreeze by remember { mutableStateOf<Any?>(null) }
    var freezeAction by remember { mutableStateOf(true) }

// In the LaunchedEffect section
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // Get customers with real stats AND isFrozen status
                val allCustomers = databaseService.getAllCustomers()
                customers = allCustomers.map { customer ->
                    val stats = databaseService.calculateCustomerActualStats(customer.customerId)
                    Customer(
                        customerId = customer.customerId,
                        name = customer.name,
                        email = customer.email,
                        phoneNumber = customer.phoneNumber,
                        profileImageBase64 = customer.profileImageBase64,
                        createdAt = customer.createdAt,
                        updatedAt = customer.updatedAt,
                        isFrozen = customer.isFrozen, // This comes from server
                        lastLogin = customer.lastLogin,
                        loginCount = customer.loginCount,
                        orderCount = stats.first,
                        totalSpent = stats.second
                    )
                }

                // Get vendors with real stats AND isFrozen status
                val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                vendors = allVendors.map { vendor ->
                    val stats = databaseService.calculateVendorActualStats(vendor.vendorId)
                    Vendor(
                        vendorId = vendor.vendorId,
                        vendorName = vendor.vendorName,
                        email = vendor.email,
                        vendorContact = vendor.vendorContact,
                        address = vendor.address,
                        category = vendor.category,
                        profileImageBase64 = vendor.profileImageBase64,
                        isFrozen = vendor.isFrozen, // This comes from server
                        lastLogin = vendor.lastLogin,
                        loginCount = vendor.loginCount,
                        orderCount = stats.first,
                        totalRevenue = stats.second,
                        rating = stats.third,
                        reviewCount = vendor.reviewCount
                    )
                }

                isLoading = false
            } catch (_: Exception) {
                isLoading = false
            }
        }
    }

    // Filter users based on search
    val filteredCustomers = customers.filter { customer ->
        searchQuery.isBlank() ||
                customer.name.contains(searchQuery, ignoreCase = true) ||
                customer.email.contains(searchQuery, ignoreCase = true) ||
                customer.customerId.contains(searchQuery, ignoreCase = true)
    }

    val filteredVendors = vendors.filter { vendor ->
        searchQuery.isBlank() ||
                vendor.vendorName.contains(searchQuery, ignoreCase = true) ||
                vendor.email.contains(searchQuery, ignoreCase = true) ||
                vendor.vendorId.contains(searchQuery, ignoreCase = true)
    }

    // Calculate stats
    val totalUsers = customers.size + vendors.size
    val activeCustomers = customers.count { !it.isFrozen }
    val activeVendors = vendors.count { !it.isFrozen }
    val frozenCustomers = customers.count { it.isFrozen }
    val frozenVendors = vendors.count { it.isFrozen }

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
                            "Tap N Chow - Admin",
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
                    actions = {
                        IconButton(onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                try {
                                    // Get customers with real stats
                                    val allCustomers = databaseService.getAllCustomers()
                                    customers = allCustomers.map { customer ->
                                        val stats = databaseService.calculateCustomerActualStats(customer.customerId)
                                        Customer(
                                            customerId = customer.customerId,
                                            name = customer.name,
                                            email = customer.email,
                                            phoneNumber = customer.phoneNumber,
                                            profileImageBase64 = customer.profileImageBase64,
                                            createdAt = customer.createdAt,
                                            updatedAt = customer.updatedAt,
                                            isFrozen = customer.isFrozen,
                                            lastLogin = customer.lastLogin,
                                            loginCount = customer.loginCount,
                                            orderCount = stats.first,
                                            totalSpent = stats.second
                                        )
                                    }

                                    // Get vendors with real stats
                                    val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                                    vendors = allVendors.map { vendor ->
                                        val stats = databaseService.calculateVendorActualStats(vendor.vendorId)
                                        Vendor(
                                            vendorId = vendor.vendorId,
                                            vendorName = vendor.vendorName,
                                            email = vendor.email,
                                            vendorContact = vendor.vendorContact,
                                            address = vendor.address,
                                            category = vendor.category,
                                            profileImageBase64 = vendor.profileImageBase64,
                                            isFrozen = vendor.isFrozen,
                                            lastLogin = vendor.lastLogin,
                                            loginCount = vendor.loginCount,
                                            orderCount = stats.first,
                                            totalRevenue = stats.second,
                                            rating = stats.third,
                                            reviewCount = vendor.reviewCount
                                        )
                                    }
                                } catch (_: Exception) {
                                    // Handle error
                                }
                                isLoading = false
                            }
                        }) {
                            Icon(
                                Icons.Default.Refresh,
                                contentDescription = "Refresh",
                                tint = Color.White
                            )
                        }
                        IconButton(onClick = {
                            navController.navigate("adminLogin") {
                                popUpTo("adminDashboard") { inclusive = true }
                            }
                        }) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Logout",
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
        bottomBar = {
            AdminBottomNavigation(navController)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "User Management",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )

            // Stats Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminUserStatCard(
                    title = "Total Users",
                    value = totalUsers.toString(),
                    icon = Icons.Default.People,
                    color = Color(0xFF2196F3),
                    modifier = Modifier.weight(1f)
                )
                AdminUserStatCard(
                    title = "Active",
                    value = "${activeCustomers + activeVendors}",
                    icon = Icons.Default.CheckCircle,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                AdminUserStatCard(
                    title = "Frozen",
                    value = "${frozenCustomers + frozenVendors}",
                    icon = Icons.Default.DoNotDisturb,
                    color = Color(0xFFF44336),
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = { Text("Search users...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Customers")
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(
                                containerColor = if (selectedTab == 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            ) {
                                Text(customers.size.toString())
                            }
                        }
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Vendors")
                            Spacer(modifier = Modifier.width(4.dp))
                            Badge(
                                containerColor = if (selectedTab == 1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            ) {
                                Text(vendors.size.toString())
                            }
                        }
                    }
                )
            }

            // Content based on selected tab
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (selectedTab) {
                    0 -> {
                        if (filteredCustomers.isEmpty()) {
                            EmptyState(
                                message = "No customers found",
                                icon = Icons.Default.Person
                            )
                        } else {
                            UserList(
                                users = filteredCustomers,
                                isCustomer = true,
                                dateFormat = dateFormat,
                                onFreezeClick = { customer ->
                                    userToFreeze = customer
                                    freezeAction = !customer.isFrozen
                                    showFreezeDialog = true
                                },
                                onDeleteClick = { customer ->
                                    userToDelete = customer
                                    showDeleteDialog = true
                                }
                            )
                        }
                    }
                    1 -> {
                        if (filteredVendors.isEmpty()) {
                            EmptyState(
                                message = "No vendors found",
                                icon = Icons.Default.Store
                            )
                        } else {
                            UserList(
                                users = filteredVendors,
                                isCustomer = false,
                                dateFormat = dateFormat,
                                onFreezeClick = { vendor ->
                                    userToFreeze = vendor
                                    freezeAction = !(vendor as Vendor).isFrozen
                                    showFreezeDialog = true
                                },
                                onDeleteClick = { vendor ->
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

// Freeze/Unfreeze Dialog
    if (showFreezeDialog && userToFreeze != null) {
        AlertDialog(
            onDismissRequest = {
                showFreezeDialog = false
                userToFreeze = null
            },
            title = {
                Text(
                    if (freezeAction) "Freeze Account" else "Unfreeze Account",
                    fontWeight = FontWeight.Bold,
                    color = if (freezeAction) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            },
            text = {
                when (userToFreeze) {
                    is Customer -> {
                        val customer = userToFreeze as Customer
                        Text("Are you sure you want to ${if (freezeAction) "freeze" else "unfreeze"} ${customer.name}'s account?")
                    }
                    is Vendor -> {
                        val vendor = userToFreeze as Vendor
                        Text("Are you sure you want to ${if (freezeAction) "freeze" else "unfreeze"} ${vendor.vendorName}'s account?")
                    }
                    else -> Text("Invalid user")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                when (userToFreeze) {
                                    is Customer -> {
                                        val customer = userToFreeze as Customer
                                        // Use DatabaseService directly
                                        val result = if (freezeAction) {
                                            databaseService.freezeCustomerAccount(customer.customerId)
                                        } else {
                                            databaseService.unfreezeCustomerAccount(customer.customerId)
                                        }

                                        if (result.isSuccess) {
                                            // Update local list immediately
                                            customers = customers.map { c ->
                                                if (c.customerId == customer.customerId) {
                                                    c.copy(isFrozen = freezeAction)
                                                } else {
                                                    c
                                                }
                                            }
                                        }
                                    }
                                    is Vendor -> {
                                        val vendor = userToFreeze as Vendor
                                        // Use DatabaseService directly
                                        val result = if (freezeAction) {
                                            databaseService.freezeVendorAccount(vendor.vendorId)
                                        } else {
                                            databaseService.unfreezeVendorAccount(vendor.vendorId)
                                        }

                                        if (result.isSuccess) {
                                            // Update local list immediately
                                            vendors = vendors.map { v ->
                                                if (v.vendorId == vendor.vendorId) {
                                                    v.copy(isFrozen = freezeAction)
                                                } else {
                                                    v
                                                }
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Handle error
                                println("Error freezing/unfreezing account: ${e.message}")
                            }
                            showFreezeDialog = false
                            userToFreeze = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (freezeAction) Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                ) {
                    Text(if (freezeAction) "Freeze" else "Unfreeze")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showFreezeDialog = false
                        userToFreeze = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    // Delete Dialog
    if (showDeleteDialog && userToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                userToDelete = null
            },
            title = {
                Text(
                    "Delete Account",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                when (userToDelete) {
                    is Customer -> {
                        val customer = userToDelete as Customer
                        Text("Are you sure you want to delete ${customer.name}'s account? This action cannot be undone.")
                    }
                    is Vendor -> {
                        val vendor = userToDelete as Vendor
                        Text("Are you sure you want to delete ${vendor.vendorName}'s account? This action cannot be undone.")
                    }
                    else -> Text("Invalid user")
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                when (userToDelete) {
                                    is Customer -> {
                                        val customer = userToDelete as Customer
                                        databaseService.deleteCustomer(customer.customerId)
                                        customers = databaseService.getAllCustomers()
                                    }
                                    is Vendor -> {
                                        val vendor = userToDelete as Vendor
                                        databaseService.deleteVendor(vendor.vendorId)
                                        vendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                                    }
                                }
                            } catch (_: Exception) {
                                // Handle error
                            }
                            showDeleteDialog = false
                            userToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        userToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminUserStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                modifier = Modifier.size(20.dp),
                tint = color
            )
            Text(
                value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                title,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun <T> UserList(
    users: List<T>,
    isCustomer: Boolean,
    dateFormat: SimpleDateFormat,
    onFreezeClick: (T) -> Unit,
    onDeleteClick: (T) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(users) { user ->
            when {
                isCustomer && user is Customer -> {
                    CustomerItem(
                        customer = user,
                        dateFormat = dateFormat,
                        onFreezeClick = { onFreezeClick(user) },
                        onDeleteClick = { onDeleteClick(user) }
                    )
                }
                !isCustomer && user is Vendor -> {
                    VendorItem(
                        vendor = user,
                        dateFormat = dateFormat,
                        onFreezeClick = { onFreezeClick(user) },
                        onDeleteClick = { onDeleteClick(user) }
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerItem(
    customer: Customer,
    dateFormat: SimpleDateFormat,
    onFreezeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        customer.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        customer.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = if (customer.isFrozen) Color(0xFFF44336) else Color(0xFF4CAF50),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (customer.isFrozen) "FROZEN" else "ACTIVE",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "ID: ${customer.customerId}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Phone: ${customer.phoneNumber}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Joined: ${dateFormat.format(customer.createdAt.toDate())}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (customer.lastLogin != null) {
                        Text(
                            "Last Login: ${dateFormat.format(customer.lastLogin.toDate())}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActivityStat(
                    label = "Logins",
                    value = customer.loginCount.toString(),
                    icon = Icons.Default.Login
                )
                ActivityStat(
                    label = "Orders",
                    value = customer.orderCount.toString(),
                    icon = Icons.Default.ShoppingCart
                )
                ActivityStat(
                    label = "Spent",
                    value = "RM${String.format(Locale.getDefault(), "%.2f", customer.totalSpent)}",
                    icon = Icons.Default.AttachMoney
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onFreezeClick,
                    modifier = Modifier.padding(end = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (customer.isFrozen) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                ) {
                    Icon(
                        if (customer.isFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Freeze",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (customer.isFrozen) "Unfreeze" else "Freeze")
                }

                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun VendorItem(
    vendor: Vendor,
    dateFormat: SimpleDateFormat,
    onFreezeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
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
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        vendor.vendorName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        vendor.email,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .background(
                            color = if (vendor.isFrozen) Color(0xFFF44336) else Color(0xFF4CAF50),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (vendor.isFrozen) "FROZEN" else "ACTIVE",
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "ID: ${vendor.vendorId}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Category: ${vendor.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Contact: ${vendor.vendorContact}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Address: ${vendor.address}",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ActivityStat(
                    label = "Logins",
                    value = vendor.loginCount.toString(),
                    icon = Icons.Default.Login
                )
                ActivityStat(
                    label = "Orders",
                    value = vendor.orderCount.toString(),
                    icon = Icons.Default.Receipt
                )
                ActivityStat(
                    label = "Revenue",
                    value = "RM${String.format(Locale.getDefault(), "%.2f", vendor.totalRevenue)}",
                    icon = Icons.Default.AttachMoney
                )
                ActivityStat(
                    label = "Rating",
                    value = String.format(Locale.getDefault(), "%.1f", vendor.rating),
                    icon = Icons.Default.Star
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onFreezeClick,
                    modifier = Modifier.padding(end = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (vendor.isFrozen) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                ) {
                    Icon(
                        if (vendor.isFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
                        contentDescription = "Freeze",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (vendor.isFrozen) "Unfreeze" else "Freeze")
                }

                Button(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete")
                }
            }
        }
    }
}

@Composable
fun ActivityStat(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            contentDescription = label,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyState(
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = "Empty",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                message,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}