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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpOffset
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

    // Dialog States
    var showFreezeDialog by remember { mutableStateOf(false) }
    var vendorToFreeze by remember { mutableStateOf<Vendor?>(null) }
    var freezeAction by remember { mutableStateOf(true) } // true = freeze, false = unfreeze
    var showDeleteDialog by remember { mutableStateOf(false) }
    var vendorToDelete by remember { mutableStateOf<Vendor?>(null) }

    // Snackbar State
    val snackbarHostState = remember { SnackbarHostState() }

    // Initial Data Load
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                vendors = allVendors.map { vendor ->
                    val stats = databaseService.calculateVendorActualStats(vendor.vendorId)
                    // Create a copy with updated stats
                    vendor.copy(
                        orderCount = stats.first,
                        totalRevenue = stats.second,
                        rating = stats.third
                    )
                }
                isLoading = false
            } catch (e: Exception) {
                isLoading = false
            }
        }
    }

    // Filter Logic
    val filteredVendors = if (searchQuery.isBlank()) {
        vendors
    } else {
        vendors.filter { vendor ->
            vendor.vendorName.contains(searchQuery, ignoreCase = true) ||
                    vendor.vendorId.contains(searchQuery, ignoreCase = true)
        }
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
            // --- Header & Search Section ---
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
                        text = "Vendor Management",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )

                    // Refresh Button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                isLoading = true
                                // ... (refresh logic same as before)
                                try {
                                    val allVendors = databaseService.getAllVendors().filter { it.vendorId != "ADMIN001" }
                                    vendors = allVendors.map { vendor ->
                                        val freshVendor = databaseService.getVendorById(vendor.vendorId)
                                        if (freshVendor != null) {
                                            val stats = databaseService.calculateVendorActualStats(vendor.vendorId)
                                            freshVendor.copy(
                                                orderCount = stats.first,
                                                totalRevenue = stats.second,
                                                rating = stats.third
                                            )
                                        } else vendor
                                    }
                                } catch (_: Exception) {}
                                isLoading = false
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF5F6F9), CircleShape)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = Color.Black)
                    }
                }

                // Search Bar
                ModernSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Stats Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VendorStatChip(
                        label = "Total",
                        count = vendors.size,
                        color = Color(0xFF2196F3)
                    )
                    VendorStatChip(
                        label = "Active",
                        count = vendors.count { !it.isFrozen },
                        color = Color(0xFF4CAF50)
                    )
                    VendorStatChip(
                        label = "Frozen",
                        count = vendors.count { it.isFrozen },
                        color = Color(0xFFF44336)
                    )
                }
            }

            // --- Vendor List ---
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filteredVendors.isEmpty()) {
                EmptyStateView()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredVendors) { vendor ->
                        ModernVendorItem(
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
                                navController.navigate("adminVendorSalesReport/${vendor.vendorId}")
                            }
                        )
                    }
                }
            }
        }
    }

    // --- Dialogs (Logic remains largely the same, just keeping it clean) ---

    // Freeze Dialog
    if (showFreezeDialog && vendorToFreeze != null) {
        AlertDialog(
            onDismissRequest = { showFreezeDialog = false },
            title = {
                Text(
                    if (freezeAction) "Freeze Account" else "Unfreeze Account",
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text("Are you sure you want to ${if (freezeAction) "freeze" else "unfreeze"} ${vendorToFreeze?.vendorName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val vendor = vendorToFreeze!!
                            val result = if (freezeAction) databaseService.freezeVendorAccount(vendor.vendorId)
                            else databaseService.unfreezeVendorAccount(vendor.vendorId)
                            if (result.isSuccess) {
                                vendors = vendors.map { if (it.vendorId == vendor.vendorId) it.copy(isFrozen = freezeAction) else it }
                                snackbarHostState.showSnackbar("Status updated successfully")
                            }
                            showFreezeDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = if (freezeAction) Color(0xFFF44336) else Color(0xFF4CAF50))
                ) { Text("Confirm") }
            },
            dismissButton = { TextButton(onClick = { showFreezeDialog = false }) { Text("Cancel") } }
        )
    }

    // Delete Dialog
    if (showDeleteDialog && vendorToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Vendor", color = Color.Red, fontWeight = FontWeight.Bold) },
            text = { Text("This action cannot be undone. Delete ${vendorToDelete?.vendorName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (databaseService.deleteVendor(vendorToDelete!!.vendorId).isSuccess) {
                                vendors = vendors.filter { it.vendorId != vendorToDelete!!.vendorId }
                                snackbarHostState.showSnackbar("Vendor deleted")
                            }
                            showDeleteDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") } }
        )
    }
}

// --- UI Components ---

@Composable
fun ModernSearchBar(
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
        placeholder = { Text("Search by name or ID...", color = Color.Gray, fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
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
fun VendorStatChip(
    label: String,
    count: Int,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: $count",
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ModernVendorItem(
    vendor: Vendor,
    decimalFormat: DecimalFormat,
    onDeleteClick: () -> Unit,
    onFreezeClick: () -> Unit,
    onViewClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewClick), // Entire card is clickable
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // --- Top Row: Identity & Menu ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category Icon Box
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(getCategoryColor(vendor.category).copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        getCategoryIcon(vendor.category),
                        contentDescription = null,
                        tint = getCategoryColor(vendor.category),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Name and ID
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = vendor.vendorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFF333333)
                        )
                        // Status Dot
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (vendor.isFrozen) Color.Red else Color(0xFF4CAF50), CircleShape)
                        )
                    }
                    Text(
                        text = "ID: ${vendor.vendorId}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Menu Button (The Cleaner Approach)
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.Gray)
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier.background(Color.White)
                    ) {
                        DropdownMenuItem(
                            text = { Text("View Reports") },
                            onClick = {
                                showMenu = false
                                onViewClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Analytics, null) }
                        )
                        DropdownMenuItem(
                            text = { Text(if (vendor.isFrozen) "Unfreeze Account" else "Freeze Account") },
                            onClick = {
                                showMenu = false
                                onFreezeClick()
                            },
                            leadingIcon = {
                                Icon(
                                    if (vendor.isFrozen) Icons.Default.LockOpen else Icons.Default.Lock,
                                    null,
                                    tint = if (vendor.isFrozen) Color(0xFF4CAF50) else Color(0xFFFF9800)
                                )
                            }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Delete Vendor", color = Color.Red) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // --- Middle Row: Key Stats (Grey Box) ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Revenue
                Column {
                    Text("Total Revenue", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "RM${decimalFormat.format(vendor.totalRevenue)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF2E7D32)
                    )
                }

                // Vertical Divider
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray))

                // Orders
                Column {
                    Text("Total Orders", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "${vendor.orderCount}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }

                // Vertical Divider
                Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray))

                // Rating
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format("%.1f", vendor.rating),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Bottom Row: Contact Info (Subtle) ---
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, tint = Color.Gray, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = vendor.address.take(35) + if (vendor.address.length > 35) "..." else "",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun EmptyStateView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Outlined.Store, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text("No vendors found", fontSize = 18.sp, color = Color.Gray)
    }
}

// Helper functions for colors and icons
fun getCategoryColor(category: String): Color {
    return when (category.lowercase()) {
        "restaurant" -> Color(0xFF4CAF50)
        "cafe" -> Color(0xFFFF9800)
        "bakery" -> Color(0xFFE91E63)
        "grocery" -> Color(0xFF2196F3)
        else -> Color.Gray
    }
}

fun getCategoryIcon(category: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category.lowercase()) {
        "restaurant" -> Icons.Default.Restaurant
        "cafe" -> Icons.Default.LocalCafe
        "bakery" -> Icons.Default.Cake
        "grocery" -> Icons.Default.ShoppingBasket
        else -> Icons.Default.Store
    }
}