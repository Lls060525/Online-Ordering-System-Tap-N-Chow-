package com.example.miniproject.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.model.Cart
import com.example.miniproject.model.CartItem
import com.example.miniproject.model.Product
import com.example.miniproject.model.Vendor
import com.example.miniproject.model.VendorCategory
import com.example.miniproject.model.Voucher
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

// Improved Image Converter
class ImageConverter(private val context: android.content.Context) {
    fun base64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrEmpty()) {
            return null
        }

        return try {
            // Remove data URL prefix if present
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }

            Log.d("ImageConverter", "Base64 string length: ${pureBase64.length}")

            val decodedBytes = Base64.decode(pureBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("ImageConverter", "Error decoding base64: ${e.message}")
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodMenuScreen(navController: NavController, vendorId: String?) {
    val databaseService = DatabaseService()
    val authService = AuthService()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var vendor by remember { mutableStateOf<Vendor?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var showCart by remember { mutableStateOf(false) }

    // --- Voucher States ---
    var vendorVouchers by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    var claimedVoucherIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // --- Auto Filter Logic ---
    var selectedCategory by remember { mutableStateOf("All") }

    // Calculate unique categories dynamically from the loaded products
    val categories = remember(products) {
        val uniqueCategories = products
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        listOf("All") + uniqueCategories
    }

    // Filter products based on selection
    val filteredProducts = remember(products, selectedCategory) {
        if (selectedCategory == "All") {
            products
        } else {
            products.filter { it.category == selectedCategory }
        }
    }

    LaunchedEffect(vendorId) {
        val targetVendorId = vendorId ?: authService.getCurrentVendor()?.vendorId

        if (targetVendorId != null) {
            // 1. Get Vendor Info
            val vendorData = databaseService.getVendorById(targetVendorId)
            vendor = vendorData

            // 2. Get Products
            val vendorProducts = databaseService.getProductsByVendor(targetVendorId)
            products = vendorProducts

            // 3. Get Vouchers for this vendor
            val allVouchers = databaseService.getVouchersByVendor(targetVendorId)
            // Filter: Active AND Not Expired
            vendorVouchers = allVouchers.filter {
                it.isActive && it.expiryDate.seconds > com.google.firebase.Timestamp.now().seconds
            }

            // 4. Check Claim Status (if user is logged in as customer)
            val customer = authService.getCurrentCustomer()
            if (customer != null) {
                val claimedList = mutableSetOf<String>()
                vendorVouchers.forEach { v ->
                    if (databaseService.isVoucherClaimed(customer.customerId, v.voucherId)) {
                        claimedList.add(v.voucherId)
                    }
                }
                claimedVoucherIds = claimedList
            }
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        vendor?.vendorName ?: "Menu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clickable { showCart = true }
                    ) {
                        Icon(
                            Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        if (cartItems.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .background(MaterialTheme.colorScheme.error, CircleShape)
                                    .align(Alignment.TopEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = cartItems.sumOf { it.quantity }.toString(),
                                    color = MaterialTheme.colorScheme.onError,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF4CAF50),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // 1. Vendor Header
                VendorHeaderSection(vendor = vendor)

                // 2. Vouchers Section (NEW)
                if (vendorVouchers.isNotEmpty()) {
                    Text(
                        text = "Vouchers",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )

                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(vendorVouchers) { voucher ->
                            VoucherClaimCard(
                                voucher = voucher,
                                isClaimed = claimedVoucherIds.contains(voucher.voucherId),
                                onClaim = {
                                    scope.launch {
                                        val customer = authService.getCurrentCustomer()
                                        if (customer != null) {
                                            val result = databaseService.claimVoucher(customer.customerId, voucher.voucherId)
                                            if (result.isSuccess) {
                                                // Update UI immediately
                                                claimedVoucherIds = claimedVoucherIds + voucher.voucherId
                                                Toast.makeText(context, "Voucher Claimed!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Failed to claim", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(context, "Please login as customer to claim", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(thickness = 0.5.dp, color = Color.LightGray)
                }

                // 3. Filter Chips (Only show if there are products and categories)
                if (products.isNotEmpty() && categories.size > 1) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
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
                                    selectedContainerColor = Color(0xFF4CAF50).copy(alpha = 0.2f),
                                    selectedLabelColor = Color(0xFF2E7D32)
                                )
                            )
                        }
                    }
                }

                // 4. Product List
                if (products.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Fastfood,
                                contentDescription = "No products",
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No products available",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (filteredProducts.isEmpty()) {
                    // Show message when filter returns nothing
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No items found in '$selectedCategory'",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        items(filteredProducts) { product ->
                            ProductMenuItem(
                                product = product,
                                onAddToCart = {
                                    val existingItem = cartItems.find { it.productId == product.productId }
                                    if (existingItem != null) {
                                        cartItems = cartItems.map { item ->
                                            if (item.productId == product.productId) {
                                                item.copy(quantity = item.quantity + 1)
                                            } else item
                                        }
                                    } else {
                                        cartItems = cartItems + CartItem(
                                            productId = product.productId,
                                            productName = product.productName,
                                            productPrice = product.productPrice,
                                            quantity = 1,
                                            vendorId = product.vendorId,
                                            imageUrl = product.imageUrl
                                        )
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        // Cart Dialog
        if (showCart) {
            CartDialog(
                cartItems = cartItems,
                vendor = vendor,
                onDismiss = { showCart = false },
                onUpdateQuantity = { productId, quantity ->
                    if (quantity == 0) {
                        cartItems = cartItems.filter { it.productId != productId }
                    } else {
                        cartItems = cartItems.map { item ->
                            if (item.productId == productId) {
                                item.copy(quantity = quantity)
                            } else item
                        }
                    }
                },
                onCheckout = {
                    showCart = false
                    // Create Cart object
                    val cart = Cart(
                        vendorId = vendor?.vendorId ?: "",
                        vendorName = vendor?.vendorName ?: "",
                        vendorAddress = vendor?.address ?: "",
                        vendorContact = vendor?.vendorContact ?: "",
                        items = cartItems,
                        subtotal = cartItems.sumOf { it.productPrice * it.quantity },
                        serviceFee = cartItems.sumOf { it.productPrice * it.quantity } * 0.10,
                        tax = cartItems.sumOf { it.productPrice * it.quantity } * 0.08,
                        total = cartItems.sumOf { it.productPrice * it.quantity } * 1.18
                    )

                    // Convert to JSON and navigate
                    val cartJson = Json.encodeToString(Cart.serializer(), cart)
                    val encodedCartJson = URLEncoder.encode(cartJson, "UTF-8")

                    navController.navigate("payment/${vendor?.vendorId ?: ""}/$encodedCartJson")
                },
                vendorName = vendor?.vendorName ?: "Vendor"
            )
        }
    }
}

@Composable
fun ProductMenuItem(product: Product, onAddToCart: () -> Unit) {
    val context = LocalContext.current
    val imageConverter = remember { ImageConverter(context) }

    val productBitmap = remember(product.imageUrl) {
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
                        // Fallback icon
                        Icon(
                            Icons.Default.Fastfood,
                            contentDescription = "Product Image",
                            modifier = Modifier.size(40.dp),
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

                    if (product.stock > 0) {
                        TextButton(
                            onClick = onAddToCart,
                            enabled = product.stock > 0
                        ) {
                            Text("Add to Cart")
                        }
                    } else {
                        Text(
                            text = "Out of Stock",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CartDialog(
    cartItems: List<CartItem>,
    vendor: Vendor?,
    onDismiss: () -> Unit,
    onUpdateQuantity: (String, Int) -> Unit,
    onCheckout: () -> Unit,
    vendorName: String
) {
    val subtotal = cartItems.sumOf { it.productPrice * it.quantity }
    val serviceFee = subtotal * 0.10
    val tax = subtotal * 0.08
    val finalTotal = subtotal + serviceFee + tax

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Your Cart - ${vendor?.vendorName ?: vendorName}", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                if (cartItems.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Your cart is empty", textAlign = TextAlign.Center)
                    }
                } else {
                    // Cart items
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 200.dp)
                    ) {
                        items(cartItems) { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.productName,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        "RM ${"%.2f".format(item.productPrice)}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    IconButton(
                                        onClick = { onUpdateQuantity(item.productId, item.quantity - 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("-", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                    Text(
                                        text = item.quantity.toString(),
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = { onUpdateQuantity(item.productId, item.quantity + 1) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text("+", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                }
                                Text(
                                    "RM ${"%.2f".format(item.productPrice * item.quantity)}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.width(70.dp)
                                )
                            }
                            Divider()
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Total calculation
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Subtotal", fontSize = 14.sp)
                            Text("RM ${"%.2f".format(subtotal)}", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Service Fee (10%)", fontSize = 14.sp)
                            Text("RM ${"%.2f".format(serviceFee)}", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Tax (8%)", fontSize = 14.sp)
                            Text("RM ${"%.2f".format(tax)}", fontSize = 14.sp)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(
                                "RM ${"%.2f".format(finalTotal)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (cartItems.isNotEmpty()) {
                Button(
                    onClick = onCheckout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Proceed to Checkout", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Continue Shopping")
            }
        }
    )
}

@Composable
fun VendorHeaderSection(vendor: Vendor?) {
    val context = LocalContext.current
    val imageConverter = remember { ImageConverter(context) }
    val databaseService = DatabaseService()

    // State for vendor rating
    var vendorRating by remember { mutableStateOf<com.example.miniproject.model.VendorRating?>(null) }
    var isLoadingRating by remember { mutableStateOf(true) }

    // Load vendor rating
    LaunchedEffect(vendor?.vendorId) {
        vendor?.vendorId?.let { vendorId ->
            vendorRating = databaseService.getVendorRating(vendorId)
        }
        isLoadingRating = false
    }

    val vendorImageString = vendor?.profileImageBase64

    val vendorBitmap = remember(vendorImageString) {
        imageConverter.base64ToBitmap(vendorImageString)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(12.dp),
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Rating display
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
                        Text(
                            text = if (isLoadingRating) {
                                "Loading rating..."
                            } else if (vendorRating != null && vendorRating!!.totalRatings > 0) {
                                "%.1f/5.0 • ${vendorRating!!.totalRatings} reviews".format(vendorRating!!.averageRating)
                            } else {
                                "No ratings yet"
                            },
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

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
                }
            }
        }
    }
}

@Composable
fun FoodContentWithVendors(navController: NavController) {
    val databaseService = DatabaseService()
    var vendors by remember { mutableStateOf<List<Vendor>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showCategoryFilter by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // Load all vendors from Firebase
        val allVendors = databaseService.getAllVendors()
        vendors = allVendors
        isLoading = false
    }

    // Filter vendors
    val filteredVendors = vendors.filter { vendor ->
        val isRestaurantType = vendor.category == VendorCategory.RESTAURANT ||
                vendor.category == VendorCategory.CAFE ||
                vendor.category == VendorCategory.BAKERY

        val matchesSearch = vendor.vendorName.contains(searchQuery, ignoreCase = true) ||
                vendor.address.contains(searchQuery, ignoreCase = true)

        val matchesCategory = selectedCategory == null || vendor.category == selectedCategory

        isRestaurantType && matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search for restaurants...") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
        )

        // Location and Filter Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "location...",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            // Category Filter Button
            FilterChip(
                selected = selectedCategory != null,
                onClick = { showCategoryFilter = true },
                label = {
                    Text(
                        text = selectedCategory?.let { VendorCategory.getDisplayName(it) } ?: "Category"
                    )
                },
                trailingIcon = {
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Filter by category"
                    )
                }
            )
        }

        // Show selected category filter
        if (selectedCategory != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filtering by: ",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
                Text(
                    text = VendorCategory.getDisplayName(selectedCategory!!),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { selectedCategory = null }) {
                    Text("Clear")
                }
            }
        }

        // Restaurants List
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 16.dp)
        ) {
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
                            contentDescription = "No restaurants",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "No restaurants found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn {
                    items(filteredVendors) { vendor ->
                        RestaurantCard(
                            vendor = vendor,
                            onClick = {
                                navController.navigate("foodMenu/${vendor.vendorId}")
                            }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }

    // Category Filter Dialog
    if (showCategoryFilter) {
        AlertDialog(
            onDismissRequest = { showCategoryFilter = false },
            title = { Text("Filter by Category") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selectedCategory = null
                                showCategoryFilter = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCategory == null,
                            onClick = {
                                selectedCategory = null
                                showCategoryFilter = false
                            }
                        )
                        Text(text = "All Categories", modifier = Modifier.padding(start = 8.dp))
                    }
                    VendorCategory.getAllCategories().forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedCategory = category
                                    showCategoryFilter = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = category
                                    showCategoryFilter = false
                                }
                            )
                            Text(
                                text = VendorCategory.getDisplayName(category),
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoryFilter = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun RestaurantCard(vendor: Vendor, onClick: () -> Unit) {
    val context = LocalContext.current
    val imageConverter = remember { ImageConverter(context) }
    val databaseService = DatabaseService()

    var vendorRating by remember { mutableStateOf<com.example.miniproject.model.VendorRating?>(null) }
    var isLoadingRating by remember { mutableStateOf(true) }

    LaunchedEffect(vendor.vendorId) {
        vendorRating = databaseService.getVendorRating(vendor.vendorId)
        isLoadingRating = false
    }

    val vendorImageString = vendor.profileImageBase64

    val vendorBitmap = remember(vendorImageString) {
        imageConverter.base64ToBitmap(vendorImageString)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                when {
                    vendorBitmap != null -> {
                        Image(
                            bitmap = vendorBitmap.asImageBitmap(),
                            contentDescription = "Vendor Logo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    else -> {
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Vendor Logo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = vendor.vendorName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Rating",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = if (isLoadingRating) {
                            "Loading..."
                        } else if (vendorRating != null && vendorRating!!.totalRatings > 0) {
                            "%.1f".format(vendorRating!!.averageRating) + " (${vendorRating!!.totalRatings} reviews)"
                        } else {
                            "No ratings yet"
                        },
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = "Location",
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                    Text(
                        text = vendor.address,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "View Menu",
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun VoucherClaimCard(
    voucher: Voucher,
    isClaimed: Boolean,
    onClaim: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isClaimed) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
        ),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .width(280.dp)
            .padding(end = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = voucher.code,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (voucher.discountType == "percentage")
                        "${voucher.discountValue.toInt()}% OFF"
                    else "RM${"%.2f".format(voucher.discountValue)} OFF",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Min Spend: RM${voucher.minSpend.toInt()}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onClaim,
                enabled = !isClaimed,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isClaimed) Color.Gray else MaterialTheme.colorScheme.primary
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Text(
                    text = if (isClaimed) "Claimed" else "Claim",
                    fontSize = 12.sp
                )
            }
        }
    }
}