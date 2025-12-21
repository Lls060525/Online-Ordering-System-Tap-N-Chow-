package com.example.miniproject.screens.customer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.example.miniproject.model.*
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import com.example.miniproject.repository.CartRepository
import com.google.firebase.Timestamp


class ImageConverter(private val context: Context) {
    fun base64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrEmpty()) {
            return null
        }
        return try {
            val pureBase64 = if (base64String.contains(",")) {
                base64String.substringAfter(",")
            } else {
                base64String
            }
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

    var cartItems by remember {
        mutableStateOf(CartRepository.getCartItems(vendorId ?: ""))
    }

    var showCart by remember { mutableStateOf(false) }

    // --- Voucher States ---
    var vendorVouchers by remember { mutableStateOf<List<Voucher>>(emptyList()) }
    var claimedVoucherIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    // --- Customization States ---
    var showCustomizationDialog by remember { mutableStateOf(false) }
    var selectedProductForCustomization by remember { mutableStateOf<Product?>(null) }

    // --- Auto Filter Logic ---
    var selectedCategory by remember { mutableStateOf("All") }

    // --- NEW: State to prevent double clicks ---
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    val categories = remember(products) {
        val uniqueCategories = products
            .map { it.category }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("All") + uniqueCategories
    }

    val filteredProducts = remember<List<Product>>(products, selectedCategory) {
        if (selectedCategory == "All") {
            products
        } else {
            products.filter { it.category == selectedCategory }
        }
    }

    LaunchedEffect(vendorId) {
        val targetVendorId = vendorId ?: authService.getCurrentVendor()?.vendorId

        if (targetVendorId != null) {
            val vendorData = databaseService.getVendorById(targetVendorId)
            vendor = vendorData

            val vendorProducts = databaseService.getProductsByVendor(targetVendorId)
            products = vendorProducts

            val allVouchers = databaseService.getVouchersByVendor(targetVendorId)
            vendorVouchers = allVouchers.filter {
                it.isActive && it.expiryDate.seconds > Timestamp.now().seconds
            }

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
                    // --- UPDATED: Safe Back Button Logic ---
                    IconButton(onClick = {
                        val currentTime = System.currentTimeMillis()
                        // 500ms delay: Prevents clicks closer than half a second
                        if (currentTime - lastBackClickTime > 500) {
                            lastBackClickTime = currentTime
                            navController.popBackStack()
                        }
                    }) {
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
                VendorHeaderSection(vendor = vendor)

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
                                                claimedVoucherIds = claimedVoucherIds + voucher.voucherId
                                                Toast.makeText(context, "Voucher Claimed!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, result.exceptionOrNull()?.message ?: "Failed to claim", Toast.LENGTH_SHORT).show()
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
                                    // 1. Calculate how many of THIS product ID are already in the cart
                                    // (Summing up all variations, e.g., spicy + non-spicy)
                                    val currentQtyInCart = cartItems
                                        .filter { it.productId == product.productId }
                                        .sumOf { it.quantity }

                                    // 2. Validation Check
                                    if (currentQtyInCart >= product.stock) {
                                        Toast.makeText(context, "Maximum stock reached (${product.stock})", Toast.LENGTH_SHORT).show()
                                    } else {
                                        // ... (Your existing logic for Customization / Adding) ...
                                        if (product.customizations.isNotEmpty()) {
                                            selectedProductForCustomization = product
                                            showCustomizationDialog = true
                                        } else {
                                            val existingItem = cartItems.find {
                                                it.productId == product.productId &&
                                                        it.productName == product.productName &&
                                                        it.productPrice == product.productPrice
                                            }

                                            if (existingItem != null) {
                                                cartItems = cartItems.map { item ->
                                                    if (item.cartItemId == existingItem.cartItemId) {
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
                                                    imageUrl = product.imageUrl,
                                                    maxStock = product.stock // <--- PASS THE STOCK HERE
                                                )
                                            }
                                            CartRepository.saveCartItems(vendorId ?: "", cartItems)

                                            Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }
        }

        if (showCustomizationDialog && selectedProductForCustomization != null) {
            ProductCustomizationDialog(
                product = selectedProductForCustomization!!,
                onDismiss = {
                    showCustomizationDialog = false
                    selectedProductForCustomization = null
                },
                onAddToCart = { customCartItem ->
                    cartItems = cartItems + customCartItem
                    CartRepository.saveCartItems(vendorId ?: "", cartItems)
                    showCustomizationDialog = false
                    selectedProductForCustomization = null
                    Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show()
                }
            )
        }

        if (showCart) {
            CartDialog(
                cartItems = cartItems,
                vendor = vendor,
                onDismiss = { showCart = false },

                // UPDATED: Receive 'id' (cartItemId) instead of productId
                onUpdateQuantity = { id, quantity ->
                    if (quantity == 0) {
                        // Filter by cartItemId
                        cartItems = cartItems.filter { it.cartItemId != id }
                    } else {
                        // Update specific item by cartItemId
                        cartItems = cartItems.map { item ->
                            if (item.cartItemId == id) {
                                item.copy(quantity = quantity)
                            } else item
                        }
                    }

                    CartRepository.saveCartItems(vendorId ?: "", cartItems)

                },
                onCheckout = {
                    showCart = false
                    // 1. Create Cart object
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

                    // 2. CHANGED: Save to Singleton Repository
                    CartRepository.setCart(cart)

                    // 3. CHANGED: Navigate WITHOUT JSON
                    navController.navigate("payment/${vendor?.vendorId ?: ""}")
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
        modifier = Modifier.fillMaxWidth(),
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

                // --- NEW: Stock Display ---
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Stock available: ${product.stock}",
                    fontSize = 12.sp,
                    // Highlight red if stock is low (< 10), otherwise grey
                    color = if (product.stock < 10) MaterialTheme.colorScheme.error else Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                // --------------------------

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "RM ${"%.2f".format(product.productPrice)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Show "Customizable" label if applicable
                        if (product.customizations.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Customizable",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (product.stock > 0) {
                        //  Check for maxStock logic in onAddToCart here
                        TextButton(
                            onClick = onAddToCart,
                            enabled = product.stock > 0
                        ) {
                            Text("Add")
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
    onUpdateQuantity: (String, Int) -> Unit, // Using cartItemId here
    onCheckout: () -> Unit,
    vendorName: String
) {
    val context = LocalContext.current

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
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp)
                    ) {
                        items(cartItems) { item ->

                            // Calculate Total Quantity for this Product ID ---
                            // This checks how many of this specific physical product are in the cart
                            // across ALL variations (e.g. 50 Spicy + 50 Non-Spicy = 100 Total)
                            val totalQuantityOfProduct = cartItems
                                .filter { it.productId == item.productId }
                                .sumOf { it.quantity }

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
                                        fontSize = 16.sp,
                                        maxLines = 2
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
                                    // MINUS BUTTON
                                    IconButton(
                                        // Uses cartItemId to target this specific row
                                        onClick = { onUpdateQuantity(item.cartItemId, item.quantity - 1) },
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

                                    // PLUS BUTTON (With Global Stock Validation)
                                    IconButton(
                                        onClick = {
                                            // Check if the TOTAL quantity (across all variants) is less than stock
                                            if (totalQuantityOfProduct < item.maxStock) {
                                                onUpdateQuantity(item.cartItemId, item.quantity + 1)
                                            } else {
                                                Toast.makeText(context, "Stock limit reached for this product!", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        // Disable visually if limit reached
                                        enabled = totalQuantityOfProduct < item.maxStock,
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Text(
                                            "+",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 18.sp,
                                            color = if (totalQuantityOfProduct < item.maxStock) Color.Black else Color.Gray
                                        )
                                    }
                                }
                                Text(
                                    "RM ${"%.2f".format(item.productPrice * item.quantity)}",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    modifier = Modifier.width(70.dp),
                                    textAlign = TextAlign.End
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
    var vendorRating by remember { mutableStateOf<VendorRating?>(null) }
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

    // --- NEW: State to prevent multiple clicks ---
    var lastClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        // Load all vendors from Firebase
        val allVendors = databaseService.getAllVendors()

        // This ensures frozen accounts do not show up in the card list
        vendors = allVendors.filter { vendor ->
            !vendor.isFrozen
        }
        isLoading = false
    }

    // Filter vendors (Search & Category logic)
    val filteredVendors = vendors.filter { vendor ->
        // 1. Check Search
        val matchesSearch = vendor.vendorName.contains(searchQuery, ignoreCase = true)

        // 2. Check Category Dropdown (If null, show all. If selected, match specific category)
        val matchesCategory = selectedCategory == null || vendor.category == selectedCategory

        // Combine them
        matchesSearch && matchesCategory
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
                                // --- UPDATED CLICK LOGIC ---
                                val currentTime = System.currentTimeMillis()
                                // Only allow click if 1 second (1000ms) has passed since last click
                                if (currentTime - lastClickTime > 1000) {
                                    lastClickTime = currentTime
                                    navController.navigate("foodMenu/${vendor.vendorId}")
                                }
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

    var vendorRating by remember { mutableStateOf<VendorRating?>(null) }
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
        // --- START OF ROW (Wraps Image, Text, and Arrow) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Vendor Logo
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

            // 2. Vendor Details (Name, Rating, Address)
            Column(
                modifier = Modifier.weight(1f) // Takes up remaining horizontal space
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

            // 3. Arrow Icon
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
fun ProductCustomizationDialog(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCart: (CartItem) -> Unit
) {
    // State to store selected options: Map<GroupTitle, List<Option>>
    val selections = remember { mutableStateMapOf<String, List<CustomizationOption>>() }

    // Calculate Total Price dynamically
    val totalPrice = remember(selections.toMap()) {
        var total = product.productPrice
        selections.values.flatten().forEach { total += it.price }
        total
    }

    // Validation: Check if all REQUIRED groups have a selection
    val isValid = remember(selections.toMap()) {
        product.customizations.all { group ->
            if (group.required) {
                !selections[group.title].isNullOrEmpty()
            } else {
                true
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.85f),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Customize Order",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                // Header: Product Info
                Text(
                    text = product.productName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Base Price: RM ${"%.2f".format(product.productPrice)}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Customization Groups
                product.customizations.forEach { group ->
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = group.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            if (group.required) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.errorContainer,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "Required",
                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            } else {
                                Text(" (Optional)", fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Options List
                        group.options.forEach { option ->
                            val isSelected = selections[group.title]?.contains(option) == true

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val currentList = selections[group.title]?.toMutableList() ?: mutableListOf()

                                        if (group.singleSelection) {
                                            // Radio Button Logic
                                            selections[group.title] = listOf(option)
                                        } else {
                                            // Checkbox Logic
                                            if (isSelected) {
                                                currentList.remove(option)
                                            } else {
                                                currentList.add(option)
                                            }
                                            selections[group.title] = currentList
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (group.singleSelection) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = null // Handled by Row clickable
                                    )
                                } else {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = null // Handled by Row clickable
                                    )
                                }

                                Text(
                                    text = option.name,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )

                                if (option.price > 0) {
                                    Text(
                                        text = "+ RM ${"%.2f".format(option.price)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 1. Generate modified name: "Burger (Large, Spicy)"
                    val selectedOptionsList = selections.values.flatten()
                    val optionsString = if (selectedOptionsList.isNotEmpty()) {
                        " (" + selectedOptionsList.joinToString(", ") { it.name } + ")"
                    } else ""

                    val finalName = product.productName + optionsString

                    // 2. Create Cart Item with NEW price and name
                    val cartItem = CartItem(
                        productId = product.productId,
                        productName = finalName,
                        productPrice = totalPrice, // Use calculated total price
                        quantity = 1,
                        vendorId = product.vendorId,
                        imageUrl = product.imageUrl,
                        maxStock = product.stock
                    )

                    onAddToCart(cartItem)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = isValid // Only enable if required fields are filled
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Add to Cart")
                    Text(
                        "RM ${"%.2f".format(totalPrice)}",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        dismissButton = null
    )
}

@Composable
fun VoucherClaimCard(voucher: Voucher, isClaimed: Boolean, onClaim: () -> Unit) {
    Card(
        modifier = Modifier.width(280.dp), // Fixed width for horizontal scrolling cards
        colors = CardDefaults.cardColors(
            containerColor = if (isClaimed) Color(0xFFF0F0F0) else Color(0xFFE3F2FD)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
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

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.MonetizationOn, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color(0xFFFFD700))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${voucher.coinCost} Coins",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }
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