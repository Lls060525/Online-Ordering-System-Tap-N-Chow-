package com.example.miniproject.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.model.Cart
import com.example.miniproject.model.CartItem
import com.example.miniproject.model.Product
import com.example.miniproject.model.Vendor
import com.example.miniproject.model.VendorCategory
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.URLEncoder

// Improved Image Converter
class ImageConverter(private val context: android.content.Context) {
    fun base64ToBitmap(base64String: String): Bitmap? {
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

    var vendor by remember { mutableStateOf<Vendor?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var showCart by remember { mutableStateOf(false) }

    LaunchedEffect(vendorId) {
        if (vendorId != null) {
            val vendorData = databaseService.getVendorById(vendorId)
            vendor = vendorData
            val vendorProducts = databaseService.getProductsByVendor(vendorId)
            products = vendorProducts
        } else {
            val currentVendor = authService.getCurrentVendor()
            vendor = currentVendor
            currentVendor?.let {
                val vendorProducts = databaseService.getProductsByVendor(it.vendorId)
                products = vendorProducts
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
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
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(products) { product ->
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
                        vendorProfileImage = vendor?.profileImageBase64 ?: "",
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
            Log.d("ProductImage", "Processing product image: ${product.productName}")
            val bitmap = imageConverter.base64ToBitmap(product.imageUrl)
            Log.d("ProductImage", "Bitmap created: ${bitmap != null}")
            bitmap
        } else {
            Log.d("ProductImage", "No image URL for product: ${product.productName}")
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

    val vendorBitmap = remember(vendor?.profileImageBase64) {
        vendor?.profileImageBase64?.let { base64 ->
            if (base64.isNotEmpty()) {
                Log.d("VendorImage", "Processing vendor image")
                val bitmap = imageConverter.base64ToBitmap(base64)
                Log.d("VendorImage", "Vendor bitmap created: ${bitmap != null}")
                bitmap
            } else {
                null
            }
        }
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

                    Spacer(modifier = Modifier.height(4.dp))

                    // Rating (you can add actual rating logic later)
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
                            text = "4.5 ★ (128 reviews)",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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

    // Filter vendors based on search query, category, and restaurant type
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
            modifier = Modifier
                .fillMaxWidth(),
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
                            text = when {
                                searchQuery.isNotEmpty() && selectedCategory != null ->
                                    "No restaurants found for '$searchQuery' in ${VendorCategory.getDisplayName(selectedCategory!!)}"
                                searchQuery.isNotEmpty() ->
                                    "No restaurants found for '$searchQuery'"
                                selectedCategory != null ->
                                    "No restaurants available in ${VendorCategory.getDisplayName(selectedCategory!!)}"
                                else -> "No restaurants available"
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        if (searchQuery.isNotEmpty() || selectedCategory != null) {
                            Text(
                                text = "Try a different search term or clear filters",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
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
                    // All categories option
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
                        Text(
                            text = "All Categories",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    // Specific category options
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

    val vendorBitmap = remember(vendor.profileImageBase64) {
        if (!vendor.profileImageBase64.isNullOrEmpty()) {
            Log.d("RestaurantCard", "Processing vendor image: ${vendor.vendorName}")
            val bitmap = imageConverter.base64ToBitmap(vendor.profileImageBase64)
            Log.d("RestaurantCard", "Vendor bitmap created: ${bitmap != null}")
            bitmap
        } else {
            null
        }
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
            // Vendor Logo/Image from Firebase
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
                        // Default icon if no image
                        Icon(
                            Icons.Default.Store,
                            contentDescription = "Vendor Logo",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.size(16.dp))

            // Vendor Info from Firebase
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
                        maxLines = 2
                    )
                }

                // Optional: Show contact info
                if (vendor.vendorContact.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Contact: ${vendor.vendorContact}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Navigation arrow
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "View Menu",
                modifier = Modifier
                    .size(20.dp)
                    .rotate(180f), // Rotate to make it point right
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}