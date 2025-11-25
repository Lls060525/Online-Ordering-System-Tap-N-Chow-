package com.example.miniproject.screens

import android.net.Uri
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.miniproject.model.Product
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.utils.ImageConverter
import com.example.miniproject.utils.rememberImagePicker
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorProductsContent(navController: NavController) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    var vendorId by remember { mutableStateOf<String?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }
    var showRestockDialog by remember { mutableStateOf(false) }
    var productToRestock by remember { mutableStateOf<Product?>(null) }
    var refreshTrigger by remember { mutableStateOf(0) }

    // Refresh products function
    val refreshProducts: () -> Unit = {
        coroutineScope.launch {
            vendorId?.let { id ->
                val vendorProducts = databaseService.getProductsByVendor(id)
                products = vendorProducts
                println("DEBUG: Products screen refreshed: ${vendorProducts.size} items")
                vendorProducts.forEach { product ->
                    println("DEBUG: ${product.productName} - Stock: ${product.stock}")
                }
            }
        }
    }

    // Get current vendor and load products
    LaunchedEffect(refreshTrigger) {
        val vendor = authService.getCurrentVendor()
        vendor?.let {
            vendorId = it.vendorId
            val vendorProducts = databaseService.getProductsByVendor(it.vendorId)
            products = vendorProducts
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "My Products",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                actions = {
                    IconButton(
                        onClick = {
                            isLoading = true
                            refreshTrigger++
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingProduct = null
                    showAddProductDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                if (products.isEmpty()) {
                    EmptyProductsState()
                } else {
                    ProductsList(
                        products = products,
                        onEditClick = { product ->
                            editingProduct = product
                            showAddProductDialog = true
                        },
                        onDeleteClick = { product ->
                            productToDelete = product
                            showDeleteDialog = true
                        },
                        onRestockClick = { product ->
                            productToRestock = product
                            showRestockDialog = true
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showAddProductDialog) {
        AddEditProductDialog(
            product = editingProduct,
            vendorId = vendorId ?: "",
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            },
            onSave = {
                refreshTrigger++
                showAddProductDialog = false
                editingProduct = null
            }
        )
    }

    if (showDeleteDialog) {
        DeleteProductDialog(
            product = productToDelete,
            onDismiss = {
                showDeleteDialog = false
                productToDelete = null
            },
            onConfirm = {
                refreshTrigger++
                showDeleteDialog = false
                productToDelete = null
            }
        )
    }

    if (showRestockDialog) {
        RestockProductDialog(
            product = productToRestock,
            onDismiss = {
                showRestockDialog = false
                productToRestock = null
            },
            onConfirm = {
                refreshTrigger++
                showRestockDialog = false
                productToRestock = null
            }
        )
    }
}

@Composable
private fun EmptyProductsState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.ShoppingCart,
            contentDescription = "No Products",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "No Products Yet",
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "Tap the + button to add your first product",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProductsList(
    products: List<Product>,
    onEditClick: (Product) -> Unit,
    onDeleteClick: (Product) -> Unit,
    onRestockClick: (Product) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        items(products) { product ->
            ProductItem(
                product = product,
                onEditClick = { onEditClick(product) },
                onDeleteClick = { onDeleteClick(product) },
                onRestockClick = { onRestockClick(product) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRestockClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Product Image
            ProductImage(
                imageUrl = product.imageUrl,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.size(16.dp))

            // Product Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = product.productName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "RM ${"%.2f".format(product.productPrice)}",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Stock: ${product.stock}",
                    fontSize = 12.sp,
                    color = if (product.stock > 0) {
                        if (product.stock < 10) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    } else MaterialTheme.colorScheme.error
                )

                if (product.category.isNotEmpty()) {
                    Text(
                        text = "Category: ${product.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Low stock warning
                if (product.stock < 10 && product.stock > 0) {
                    Text(
                        text = "⚠ Low stock",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                } else if (product.stock == 0) {
                    Text(
                        text = "🛑 Out of stock",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Action Buttons
            Column {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit Product",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onRestockClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = "Restock Product",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete Product",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
fun ProductImage(
    imageUrl: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        when {
            imageUrl.isEmpty() -> {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "No Image",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            imageUrl.startsWith("data:image") -> {
                val imageConverter = ImageConverter(LocalContext.current)
                val bitmap = imageConverter.base64ToBitmap(imageUrl)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Product Image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = "Invalid Image",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            imageUrl.startsWith("http") -> {
                Image(
                    painter = rememberAsyncImagePainter(model = imageUrl),
                    contentDescription = "Product Image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            else -> {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "Invalid Image URL",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditProductDialog(
    product: Product?,
    vendorId: String,
    onDismiss: () -> Unit,
    onSave: (Product) -> Unit
) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageConverter = ImageConverter(context)

    var productName by remember { mutableStateOf(product?.productName ?: "") }
    var productPrice by remember { mutableStateOf(product?.productPrice?.toString() ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isImageLoading by remember { mutableStateOf(false) }

    val imagePicker = rememberImagePicker { uri ->
        selectedImageUri = uri
        if (uri != null) {
            isImageLoading = true
            coroutineScope.launch {
                val base64 = imageConverter.uriToBase64(uri)
                if (base64 != null) {
                    imageUrl = "data:image/jpeg;base64,$base64"
                } else {
                    errorMessage = "Failed to process image"
                }
                isImageLoading = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (product == null) "Add New Product" else "Edit Product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                // Image Preview and Picker
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImageLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            ProductImage(
                                imageUrl = imageUrl,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.size(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Button(
                            onClick = { imagePicker.pickImageFromGallery() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isImageLoading
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Pick Image",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Choose Image")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        if (imageUrl.isNotEmpty()) {
                            Button(
                                onClick = {
                                    imageUrl = ""
                                    selectedImageUri = null
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Remove Image",
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text("Remove Image")
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = if (imageUrl.isEmpty()) "No image selected" else "Image selected",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(
                    value = productName,
                    onValueChange = { productName = it },
                    label = { Text("Product Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = productName.isBlank()
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = productPrice,
                    onValueChange = {
                        if (it.isEmpty() || it.toDoubleOrNull() != null) {
                            productPrice = it
                        }
                    },
                    label = { Text("Price (RM) *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = productPrice.isBlank() || productPrice.toDoubleOrNull() == null
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = stock,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            stock = it
                        }
                    },
                    label = { Text("Stock Quantity *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = stock.isBlank() || stock.toIntOrNull() == null
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (productName.isBlank()) {
                        errorMessage = "Product name is required"
                        return@Button
                    }

                    if (productPrice.isBlank() || productPrice.toDoubleOrNull() == null) {
                        errorMessage = "Valid price is required"
                        return@Button
                    }

                    if (stock.isBlank() || stock.toIntOrNull() == null) {
                        errorMessage = "Valid stock quantity is required"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    val newProduct = Product(
                        productId = product?.productId ?: "",
                        vendorId = vendorId,
                        productName = productName,
                        productPrice = productPrice.toDouble(),
                        description = description,
                        stock = stock.toInt(),
                        imageUrl = imageUrl,
                        category = category,
                        createdAt = product?.createdAt ?: Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )

                    coroutineScope.launch {
                        if (product == null) {
                            databaseService.addProduct(newProduct).onSuccess { productId ->
                                val productWithId = newProduct.copy(productId = productId)
                                onSave(productWithId)
                                isLoading = false
                            }.onFailure {
                                errorMessage = "Failed to add product: ${it.message}"
                                isLoading = false
                            }
                        } else {
                            databaseService.updateProduct(newProduct).onSuccess {
                                onSave(newProduct)
                                isLoading = false
                            }.onFailure {
                                errorMessage = "Failed to update product: ${it.message}"
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading && !isImageLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(if (product == null) "Add Product" else "Update Product")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isLoading && !isImageLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun DeleteProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onConfirm: (Product) -> Unit
) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Delete Product",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Text("Are you sure you want to delete \"${product?.productName}\"? This action cannot be undone.")
        },
        confirmButton = {
            Button(
                onClick = {
                    if (product != null) {
                        isLoading = true
                        coroutineScope.launch {
                            databaseService.deleteProduct(product.productId).onSuccess {
                                onConfirm(product)
                                isLoading = false
                            }.onFailure {
                                isLoading = false
                            }
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RestockProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    var additionalStock by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Restock Product",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                Text("Product: ${product?.productName ?: "Unknown"}")
                Text("Current Stock: ${product?.stock ?: 0}")

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = additionalStock,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) {
                            additionalStock = it
                        }
                    },
                    label = { Text("Additional Stock Quantity *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    isError = additionalStock.isBlank() || additionalStock.toIntOrNull() == null || additionalStock.toInt() <= 0
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (additionalStock.isBlank() || additionalStock.toIntOrNull() == null || additionalStock.toInt() <= 0) {
                        errorMessage = "Please enter a valid stock quantity"
                        return@Button
                    }

                    if (product == null) {
                        errorMessage = "Product not found"
                        return@Button
                    }

                    isLoading = true
                    errorMessage = null

                    coroutineScope.launch {
                        databaseService.restockProduct(
                            product.productId,
                            additionalStock.toInt()
                        ).onSuccess {
                            onConfirm()
                            isLoading = false
                        }.onFailure {
                            errorMessage = "Failed to restock: ${it.message}"
                            isLoading = false
                        }
                    }
                },
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Restock")
                }
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                enabled = !isLoading
            ) {
                Text("Cancel")
            }
        }
    )
}