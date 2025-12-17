package com.example.miniproject.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.model.Product
import com.example.miniproject.model.Vendor
import com.example.miniproject.model.VendorRating
import com.example.miniproject.screens.VendorOrdersScreen.VendorOrdersContent
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.utils.ImageConverter
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.graphics.Color

sealed class VendorScreen(val title: String, val icon: ImageVector) {
    object Dashboard : VendorScreen("Dashboard", Icons.Default.Store)
    object Products : VendorScreen("Products", Icons.Default.Inventory)
    object Orders : VendorScreen("Orders", Icons.Default.Receipt)
    object Analytics : VendorScreen("Analytics", Icons.Default.Analytics)
    object Account : VendorScreen("Account", Icons.Default.AccountCircle)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorHomeScreen(navController: NavController) {
    var currentScreen by remember { mutableStateOf<VendorScreen>(VendorScreen.Dashboard) }

    Scaffold(topBar = {
        // Orange background with status bar padding
        Box(
            modifier = Modifier
                .background(Color(0xFFFFA500)) // Orange color
                .statusBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        "Tap N Chow - Vendor",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.White // White text for better contrast on orange
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent, // Make the TopAppBar transparent
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                modifier = Modifier.background(Color.Transparent)
            )
        }
    },

        bottomBar = {
            NavigationBar {
                listOf(
                    VendorScreen.Dashboard,
                    VendorScreen.Products,
                    VendorScreen.Orders,
                    VendorScreen.Analytics,
                    VendorScreen.Account
                ).forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(screen.title) },
                        selected = currentScreen == screen,
                        onClick = { currentScreen = screen }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentScreen) {
                is VendorScreen.Dashboard -> VendorDashboardContent(navController)
                is VendorScreen.Products -> VendorProductsContent(navController)
                is VendorScreen.Orders -> VendorOrdersContent()
                is VendorScreen.Analytics -> VendorAnalyticsContent(navController)
                is VendorScreen.Account -> VendorAccountScreen(navController)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorDashboardContent(navController: NavController) {
    val authService = AuthService()
    val databaseService = DatabaseService()

    var vendor by remember { mutableStateOf<Vendor?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // --- Filter Logic ---
    var selectedCategory by remember { mutableStateOf("All") }

    // Calculate unique categories dynamically
    val categories = remember(products) {
        val uniqueCategories = products
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        listOf("All") + uniqueCategories
    }

    // Filter products
    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "All") {
            products
        } else {
            products.filter { it.category == selectedCategory }
        }
    }
    // --------------------

    LaunchedEffect(Unit) {
        val currentVendor = authService.getCurrentVendor()
        vendor = currentVendor

        currentVendor?.let {
            val vendorProducts = databaseService.getProductsByVendor(it.vendorId)
            products = vendorProducts
        }
        isLoading = false
    }

    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                // Vendor Information Card
                VendorInfoCard(vendor = vendor)
                Spacer(modifier = Modifier.height(24.dp))

                // Quick Stats
                QuickStatsCard(products = products)
                Spacer(modifier = Modifier.height(24.dp))

                // Products Header with navigation to full products screen
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Your Products",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${filteredProducts.size} items",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                    }
                }

                // --- Category Filter Chips Row ---
                if (categories.size > 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(categories) { category ->
                            val isSelected = selectedCategory == category
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = category },
                                label = { Text(category) },
                                leadingIcon = if (isSelected) {
                                    {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                } else null,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Logic to display filtered products
            if (filteredProducts.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp), // Slightly smaller height for dashboard
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Inventory,
                                contentDescription = "No products",
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (selectedCategory == "All") "No products added yet" else "No products in '$selectedCategory'",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Show only first 3 products from the FILTERED list on dashboard
                val displayProducts = if (filteredProducts.size > 3) filteredProducts.take(3) else filteredProducts

                items(displayProducts) { product ->
                    ProductDashboardItem(product = product)
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Show "View All" if there are more products
                if (filteredProducts.size > 3) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { /* Navigate to products */ },
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "View All ${filteredProducts.size} Products",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
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
fun VendorInfoCard(vendor: Vendor?) {
    val databaseService = DatabaseService()
    var vendorRating by remember { mutableStateOf<VendorRating?>(null) }
    var isLoadingRating by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val imageConverter = remember { ImageConverter(context) }

    LaunchedEffect(vendor?.vendorId) {
        vendor?.vendorId?.let { vendorId ->
            vendorRating = databaseService.getVendorRating(vendorId)
            isLoadingRating = false
        } ?: run {
            isLoadingRating = false
        }
    }

    val vendorBitmap = remember(vendor?.profileImageBase64, imageConverter) {
        vendor?.profileImageBase64?.let { base64 ->
            if (base64.isNotEmpty()) {
                val bitmap = imageConverter.base64ToBitmap(base64)
                bitmap
            } else {
                null
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Vendor Profile Picture
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        vendorBitmap != null -> {
                            Image(
                                bitmap = vendorBitmap.asImageBitmap(),
                                contentDescription = "Vendor Profile",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        else -> {
                            Image(
                                painter = painterResource(id = R.drawable.logo2),
                                contentDescription = "Vendor Logo",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.size(16.dp))

                // Vendor Info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = vendor?.vendorName ?: "Vendor Name",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Category Badge
                    vendor?.category?.let { category ->
                        Box(
                            modifier = Modifier
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = vendor.category.replaceFirstChar { it.uppercase() },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = "Location",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = vendor?.address ?: "Address not available",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    if (!vendor?.vendorContact.isNullOrEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = "Contact",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text(
                                text = "Contact: ${vendor?.vendorContact}",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dynamic Rating from Firebase
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Rating",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.size(8.dp))

                        if (isLoadingRating) {
                            Text(
                                text = "Loading ratings...",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else if (vendorRating != null && vendorRating!!.totalRatings > 0) {
                            Text(
                                text = "${"%.1f".format(vendorRating!!.averageRating)} ★ (${vendorRating!!.totalRatings} reviews)",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "No reviews yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QuickStatsCard(products: List<Product>) {
    val totalProducts = products.size
    val totalValue = products.sumOf { it.productPrice * it.stock }
    val lowStockProducts = products.count { it.stock < 10 }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Total Products
            StatItem(
                value = totalProducts.toString(),
                label = "Products",
                modifier = Modifier.weight(1f)
            )

            // Total Inventory Value
            StatItem(
                value = "RM${"%.2f".format(totalValue)}",
                label = "Inventory Value",
                modifier = Modifier.weight(1.5f)
            )

            // Low Stock Alert
            StatItem(
                value = lowStockProducts.toString(),
                label = "Low Stock",
                modifier = Modifier.weight(1f),
                isAlert = lowStockProducts > 0
            )
        }
    }
}
@Composable
fun StatItem(value: String, label: String, modifier: Modifier = Modifier, isAlert: Boolean = false) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (isAlert) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ProductDashboardItem(product: Product) {
    val context = LocalContext.current
    val imageConverter = remember { ImageConverter(context) }

    val productBitmap = remember(product.imageUrl, imageConverter) {
        if (product.imageUrl.isNotEmpty()) {
            val bitmap = imageConverter.base64ToBitmap(product.imageUrl)
            bitmap
        } else {
            null
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Product Image
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    productBitmap != null -> {
                        Image(
                            bitmap = productBitmap.asImageBitmap(),
                            contentDescription = "Product Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = "Product Image",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Product Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.description.ifEmpty { "No description available" },
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "RM ${"%.2f".format(product.productPrice)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (product.stock > 0) "Stock: ${product.stock}" else "Out of Stock",
                        fontSize = 12.sp,
                        color = if (product.stock > 0) {
                            if (product.stock < 10) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        } else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
