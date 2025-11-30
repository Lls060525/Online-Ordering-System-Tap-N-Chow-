package com.example.miniproject.screens

import android.net.Uri
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

    var vendorId by remember { mutableStateOf<String?>(null) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // Get current vendor and load products - ADD THIS BACK!
    LaunchedEffect(Unit) {
        val vendor = authService.getCurrentVendor()
        vendor?.let {
            vendorId = it.vendorId
            val vendorProducts = databaseService.getProductsByVendor(it.vendorId)
            products = vendorProducts
            isLoading = false
        } ?: run {
            // If no vendor found, still stop loading
            isLoading = false
        }
    }

    // Debug products - KEEP THIS BUT FIX THE KEY
    LaunchedEffect(products) {
        if (products.isNotEmpty()) {
            products.forEach { product ->
                Log.d("ProductDebug", "Product: ${product.productName}")
                Log.d("ProductDebug", "Image URL length: ${product.imageUrl.length}")
                Log.d("ProductDebug", "Image URL starts with: ${product.imageUrl.take(50)}...")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(

                modifier = Modifier.padding(12.dp),
                title = {
                    Text(
                        "My Products",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
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
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        items(products) { product ->
                            ProductItem(
                                product = product,
                                onEditClick = {
                                    editingProduct = product
                                    showAddProductDialog = true
                                },
                                onDeleteClick = {
                                    productToDelete = product
                                    showDeleteDialog = true
                                }
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    // Add/Edit Product Dialog
    if (showAddProductDialog) {
        AddEditProductDialog(
            product = editingProduct,
            vendorId = vendorId ?: "",
            onDismiss = {
                showAddProductDialog = false
                editingProduct = null
            },
            onSave = { savedProduct ->
                if (editingProduct != null) {
                    // Update existing product in the list
                    products = products.map {
                        if (it.productId == savedProduct.productId) savedProduct else it
                    }
                } else {
                    // Add new product to the list
                    products = products + savedProduct
                }
                showAddProductDialog = false
                editingProduct = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        DeleteProductDialog(
            product = productToDelete,
            onDismiss = {
                showDeleteDialog = false
                productToDelete = null
            },
            onConfirm = { product ->
                // Remove from local list AND delete from Firebase
                products = products.filter { it.productId != product.productId }
                showDeleteDialog = false
                productToDelete = null
            }
        )
    }
}

@Composable
fun ProductItem(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Product Image - Using better image loading
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (product.category.isNotEmpty()) {
                    Text(
                        text = "Category: ${product.category}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val context = LocalContext.current
    val imageConverter = remember { ImageConverter(context) }

    val productBitmap = remember(imageUrl, imageConverter) {
        if (imageUrl.isNotEmpty()) {
            Log.d("VendorProductImage", "Processing product image, length: ${imageUrl.length}")
            val bitmap = imageConverter.base64ToBitmap(imageUrl)
            Log.d("VendorProductImage", "Bitmap created: ${bitmap != null}")
            bitmap
        } else {
            Log.d("VendorProductImage", "Empty image URL")
            null
        }
    }

    Box(
        modifier = modifier
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
                // No image - show placeholder
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = "No Image",
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
    val imageConverter = remember { ImageConverter(context) }

    // Declare all state variables FIRST
    var productName by remember { mutableStateOf(product?.productName ?: "") }
    var productPrice by remember { mutableStateOf(product?.productPrice?.toString() ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // State for image picking
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isImageLoading by remember { mutableStateOf(false) }

    // Image picker
    val imagePicker = rememberImagePicker { uri ->
        selectedImageUri = uri
        // Convert the selected image to base64 when image is picked
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
                    // Validation
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

                    // Use coroutine scope to call suspend functions
                    coroutineScope.launch {
                        // Save to Firebase
                        if (product == null) {
                            // Add new product
                            databaseService.addProduct(newProduct).onSuccess { productId ->
                                val productWithId = newProduct.copy(productId = productId)
                                onSave(productWithId)
                                isLoading = false
                            }.onFailure {
                                errorMessage = "Failed to add product: ${it.message}"
                                isLoading = false
                            }
                        } else {
                            // Update existing product
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
                        // Use coroutine scope to call suspend functions
                        coroutineScope.launch {
                            // FIXED: Actually delete from Firebase
                            databaseService.deleteProduct(product.productId).onSuccess {
                                // Only call onConfirm if Firebase deletion is successful
                                onConfirm(product)
                                isLoading = false
                            }.onFailure {
                                // Handle deletion error
                                isLoading = false
                                // You might want to show an error message here
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