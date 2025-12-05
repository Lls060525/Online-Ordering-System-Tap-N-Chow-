package com.example.miniproject.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Vendor
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVendorListScreen(navController: NavController) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val decimalFormat = DecimalFormat("#,##0.00")

    // State variables
    var vendors by remember { mutableStateOf<List<Vendor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var showFreezeDialog by remember { mutableStateOf(false) }
    var vendorToFreeze by remember { mutableStateOf<Vendor?>(null) }
    var freezeAction by remember { mutableStateOf(true) } // true = freeze, false = unfreeze
    var showDeleteDialog by remember { mutableStateOf(false) }
    var vendorToDelete by remember { mutableStateOf<Vendor?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                // FIXED: Get vendors from SERVER to ensure fresh data including isFrozen status
                val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }

                // Calculate actual stats for each vendor
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
                        isFrozen = vendor.isFrozen, // Use the value from Firestore
                        lastLogin = vendor.lastLogin,
                        loginCount = vendor.loginCount,
                        orderCount = stats.first,      // Use calculated value
                        totalRevenue = stats.second,    // Use calculated value
                        rating = stats.third,           // Use calculated value
                        reviewCount = vendor.reviewCount
                    )
                }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }
    // Filter vendors based on search
    val filteredVendors = if (searchQuery.isBlank()) {
        vendors
    } else {
        vendors.filter { vendor ->
            vendor.vendorName.contains(searchQuery, ignoreCase = true) ||
                    vendor.email.contains(searchQuery, ignoreCase = true) ||
                    vendor.vendorContact.contains(searchQuery, ignoreCase = true)
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
                                    val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }

                                    vendors = allVendors.map { vendor ->
                                        val stats = databaseService.calculateVendorActualStats(vendor.vendorId)
                                        vendor.copy(
                                            orderCount = stats.first,
                                            totalRevenue = stats.second,
                                            rating = stats.third,
                                            isFrozen = vendor.isFrozen  // Preserve frozen status
                                        )
                                    }
                                } catch (e: Exception) {
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
                        "Vendor Management",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFFF5F5F5)
                )
            )

            // Search Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
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
                        placeholder = { Text("Search vendors...") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // Stats
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
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            vendors.size.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Total Vendors",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            filteredVendors.size.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Showing",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            vendors.filter { it.isFrozen }.size.toString(),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Frozen",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (filteredVendors.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "No vendors",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "No Vendors Found",
                            fontSize = 18.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isNotBlank()) {
                            Text(
                                "Try a different search term",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredVendors) { vendor ->
                        AdminVendorItem(
                            vendor = vendor,
                            decimalFormat = decimalFormat,
                            onDeleteClick = {
                                vendorToDelete = vendor
                                showDeleteDialog = true
                            },
                            onFreezeClick = {
                                vendorToFreeze = vendor
                                freezeAction = !vendor.isFrozen
                                showFreezeDialog = true
                            },
                            onViewClick = {
                                // Navigate to sales report instead of details
                                navController.navigate("adminVendorSalesReport/${vendor.vendorId}")
                            }
                        )
                    }
                }
            }
        }
    }

// Freeze/Unfreeze Confirmation Dialog
    if (showFreezeDialog && vendorToFreeze != null) {
        AlertDialog(
            onDismissRequest = {
                showFreezeDialog = false
                vendorToFreeze = null
            },
            title = {
                Text(
                    if (freezeAction) "Freeze Account" else "Unfreeze Account",
                    fontWeight = FontWeight.Bold,
                    color = if (freezeAction) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
            },
            text = {
                Text("Are you sure you want to ${if (freezeAction) "freeze" else "unfreeze"} \"${vendorToFreeze?.vendorName}\"?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val vendor = vendorToFreeze!!
                                // Use DatabaseService directly
                                val result = if (freezeAction) {
                                    databaseService.freezeVendorAccount(vendor.vendorId)
                                } else {
                                    databaseService.unfreezeVendorAccount(vendor.vendorId)
                                }

                                if (result.isSuccess) {
                                    // Update local state immediately
                                    vendors = vendors.map { v ->
                                        if (v.vendorId == vendor.vendorId) {
                                            v.copy(isFrozen = freezeAction)
                                        } else {
                                            v
                                        }
                                    }
                                    snackbarMessage = "Account ${if (freezeAction) "frozen" else "unfrozen"} successfully"
                                    showSnackbar = true
                                } else {
                                    snackbarMessage = "Failed to update account status: ${result.exceptionOrNull()?.message}"
                                    showSnackbar = true
                                }
                            } catch (e: Exception) {
                                snackbarMessage = "Error: ${e.message}"
                                showSnackbar = true
                            }
                            showFreezeDialog = false
                            vendorToFreeze = null
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
                        vendorToFreeze = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    // Delete Confirmation Dialog
    if (showDeleteDialog && vendorToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                vendorToDelete = null
            },
            title = {
                Text(
                    "Delete Vendor",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            },
            text = {
                Text("Are you sure you want to delete \"${vendorToDelete?.vendorName}\"? This action cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val vendor = vendorToDelete!!
                                val result = databaseService.deleteVendor(vendor.vendorId)

                                if (result.isSuccess) {
                                    // Remove from local list
                                    vendors = vendors.filter { it.vendorId != vendor.vendorId }
                                    snackbarMessage = "Vendor deleted successfully"
                                    showSnackbar = true
                                } else {
                                    snackbarMessage = "Failed to delete vendor"
                                    showSnackbar = true
                                }
                            } catch (e: Exception) {
                                snackbarMessage = "Error: ${e.message}"
                                showSnackbar = true
                            }
                            showDeleteDialog = false
                            vendorToDelete = null
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
                        vendorToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Snackbar
    if (showSnackbar) {
        LaunchedEffect(showSnackbar) {
            kotlinx.coroutines.delay(3000)
            showSnackbar = false
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 80.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    snackbarMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun AdminVendorItem(
    vendor: Vendor,
    decimalFormat: DecimalFormat,
    onDeleteClick: () -> Unit,
    onFreezeClick: () -> Unit,
    onViewClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewClick),
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
                            color = when (vendor.category.lowercase()) {
                                "restaurant" -> Color(0xFF4CAF50)
                                "grocery" -> Color(0xFF2196F3)
                                "cafe" -> Color(0xFFFF9800)
                                "bakery" -> Color(0xFF9C27B0)
                                else -> Color(0xFF607D8B)
                            },
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        vendor.category.replaceFirstChar { it.uppercase() },
                        fontSize = 10.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = "Phone",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            vendor.vendorContact,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            vendor.address.take(40) + if (vendor.address.length > 40) "..." else "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        "Vendor ID",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        vendor.vendorId,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Stats Row
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        vendor.orderCount.toString(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "Orders",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "RM${decimalFormat.format(vendor.totalRevenue)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        "Revenue",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        String.format("%.1f", vendor.rating),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        "Rating",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = if (vendor.isFrozen) Color(0xFFF44336) else Color(0xFF4CAF50),
                                shape = RoundedCornerShape(6.dp)
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
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // View Button
                OutlinedButton(
                    onClick = onViewClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = "View",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("View Sales")
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Delete Button (Replaced Edit with Delete)
                OutlinedButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
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

                Spacer(modifier = Modifier.width(8.dp))

                // Freeze/Unfreeze Button
                Button(
                    onClick = onFreezeClick,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (vendor.isFrozen) Color(0xFF4CAF50) else Color(0xFFF44336),
                        contentColor = Color.White
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
            }
        }
    }
}