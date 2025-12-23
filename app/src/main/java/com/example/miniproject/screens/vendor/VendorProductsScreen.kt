package com.example.miniproject.screens.vendor

import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.CustomizationOption
import com.example.miniproject.model.Product
import com.example.miniproject.model.ProductCustomization
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

    // Dialog States
    var showAddProductDialog by remember { mutableStateOf(false) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    // Restock State
    var showRestockDialog by remember { mutableStateOf(false) }
    var productToRestock by remember { mutableStateOf<Product?>(null) }

    // --- Filter State ---
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


    // Get current vendor and load products
    LaunchedEffect(Unit) {
        val vendor = authService.getCurrentVendor()
        vendor?.let {
            vendorId = it.vendorId
            val vendorProducts = databaseService.getProductsByVendor(it.vendorId)
            products = vendorProducts
            isLoading = false
        } ?: run {
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
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // --- Auto Filter Chips ---
                        if (categories.size > 1) {
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
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    )
                                }
                            }
                        }

                        // Product List
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 16.dp)
                        ) {
                            // Instructional Hint
                            item {
                                Text(
                                    text = "Tip: Long press a product to restock",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )
                            }

                            if (filteredProducts.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "No products found in '$selectedCategory'",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            items(filteredProducts) { product ->
                                ProductItem(
                                    product = product,
                                    onEditClick = {
                                        editingProduct = product
                                        showAddProductDialog = true
                                    },
                                    onDeleteClick = {
                                        productToDelete = product
                                        showDeleteDialog = true
                                    },
                                    onRestockClick = {
                                        productToRestock = product
                                        showRestockDialog = true
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
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
                    products = products.map {
                        if (it.productId == savedProduct.productId) savedProduct else it
                    }
                } else {
                    products = products + savedProduct
                }
                showAddProductDialog = false
                editingProduct = null
            }
        )
    }

    // Restock Dialog
    if (showRestockDialog) {
        RestockProductDialog(
            product = productToRestock,
            onDismiss = {
                showRestockDialog = false
                productToRestock = null
            },
            onConfirm = { product, addedQuantity ->
                coroutineScope.launch {
                    val newStock = product.stock + addedQuantity
                    val updatedProduct = product.copy(
                        stock = newStock,
                        updatedAt = Timestamp.now()
                    )

                    databaseService.updateProduct(updatedProduct).onSuccess {
                        // Update local list
                        products = products.map {
                            if (it.productId == product.productId) updatedProduct else it
                        }
                        showRestockDialog = false
                        productToRestock = null
                    }
                }
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
                products = products.filter { it.productId != product.productId }
                showDeleteDialog = false
                productToDelete = null
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductItem(
    product: Product,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onRestockClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(
                onClick = { /* Standard click (optional: show details) */ },
                onLongClick = onRestockClick
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            ProductImage(
                imageUrl = product.imageUrl,
                modifier = Modifier.size(80.dp)
            )

            Spacer(modifier = Modifier.size(16.dp))

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
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
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

                Spacer(modifier = Modifier.height(16.dp))

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
fun RestockProductDialog(
    product: Product?,
    onDismiss: () -> Unit,
    onConfirm: (Product, Int) -> Unit
) {
    var quantityToAdd by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Restock Product", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Current Stock: ${product?.stock ?: 0}")
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = quantityToAdd,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            quantityToAdd = it
                        }
                    },
                    label = { Text("Quantity to Add") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorMessage != null
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityToAdd.toIntOrNull()
                    if (qty == null || qty <= 0) {
                        errorMessage = "Please enter a valid quantity greater than 0"
                    } else {
                        product?.let { onConfirm(it, qty) }
                    }
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text("Cancel")
            }
        }
    )
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
            val bitmap = imageConverter.base64ToBitmap(imageUrl)
            bitmap
        } else {
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

    // Existing States
    var productName by remember { mutableStateOf(product?.productName ?: "") }
    var productPrice by remember { mutableStateOf(product?.productPrice?.toString() ?: "") }
    var description by remember { mutableStateOf(product?.description ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var imageUrl by remember { mutableStateOf(product?.imageUrl ?: "") }

    // NEW STATE for Customizations
    var customizations by remember { mutableStateOf(product?.customizations ?: emptyList()) }

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
        modifier = Modifier.fillMaxWidth().fillMaxHeight(0.9f), // Taller dialog
        title = { Text(if (product == null) "Add New Product" else "Edit Product") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(androidx.compose.foundation.rememberScrollState())
            ) {
                if (errorMessage != null) {
                    Text(text = errorMessage!!, color = MaterialTheme.colorScheme.error)
                }

                //  Image Picker Section
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                    Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                        if (isImageLoading) CircularProgressIndicator() else ProductImage(imageUrl = imageUrl, modifier = Modifier.size(80.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { imagePicker.pickImageFromGallery() }) { Text("Choose Image") }
                }

                //  Basic Fields
                OutlinedTextField(value = productName, onValueChange = { productName = it }, singleLine = true, label = { Text("Product Name *") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = productPrice,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) productPrice = it },
                    label = { Text("Price (RM) *") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = stock,
                    onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) stock = it },
                    label = { Text("Stock *") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth(),singleLine = true,)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth(), maxLines = 3,singleLine = true,)

                // NEW CUSTOMIZATION EDITOR
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(16.dp))

                CustomizationEditor(
                    customizations = customizations,
                    onUpdate = { updatedList -> customizations = updatedList }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (productName.isBlank() || productPrice.isBlank() || stock.isBlank()) {
                        errorMessage = "Please fill in all required fields"
                        return@Button
                    }
                    isLoading = true
                    val newProduct = Product(
                        productId = product?.productId ?: "",
                        vendorId = vendorId,
                        productName = productName,
                        productPrice = productPrice.toDouble(),
                        description = description,
                        stock = stock.toInt(),
                        imageUrl = imageUrl,
                        category = category.trim(),
                        customizations = customizations, // SAVE CUSTOMIZATIONS
                        createdAt = product?.createdAt ?: Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )

                    coroutineScope.launch {
                        if (product == null) {
                            databaseService.addProduct(newProduct).onSuccess { id ->
                                onSave(newProduct.copy(productId = id))
                            }
                        } else {
                            databaseService.updateProduct(newProduct).onSuccess {
                                onSave(newProduct)
                            }
                        }
                        isLoading = false
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (product == null) "Add" else "Update")
            }
        },
        dismissButton = { Button(onClick = onDismiss) { Text("Cancel") } }
    )
}


@Composable
fun CustomizationEditor(
    customizations: List<ProductCustomization>,
    onUpdate: (List<ProductCustomization>) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Product Customizations", fontWeight = FontWeight.Bold, fontSize = 16.sp)

        customizations.forEachIndexed { index, customization ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Group ${index + 1}", fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            val newList = customizations.toMutableList()
                            newList.removeAt(index)
                            onUpdate(newList)
                        }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    // Title Input
                    OutlinedTextField(
                        value = customization.title,
                        onValueChange = { newTitle ->
                            val newList = customizations.toMutableList()
                            newList[index] = customization.copy(title = newTitle)
                            onUpdate(newList)
                        },
                        label = { Text("Title (e.g. Size, Spiciness)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                    )

                    // Required & Single Selection
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = customization.required,
                            onCheckedChange = { isRequired ->
                                val newList = customizations.toMutableList()
                                newList[index] = customization.copy(required = isRequired)
                                onUpdate(newList)
                            }
                        )
                        Text("Required", fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(16.dp))
                        Checkbox(
                            checked = customization.singleSelection,
                            onCheckedChange = { isSingle ->
                                val newList = customizations.toMutableList()
                                newList[index] = customization.copy(singleSelection = isSingle)
                                onUpdate(newList)
                            }
                        )
                        Text("Single Select", fontSize = 12.sp)
                    }

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Options List
                    customization.options.forEachIndexed { optIndex, option ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Option Name
                            OutlinedTextField(
                                value = option.name,
                                onValueChange = { name ->
                                    val newOptions = customization.options.toMutableList()
                                    newOptions[optIndex] = option.copy(name = name)
                                    val newList = customizations.toMutableList()
                                    newList[index] = customization.copy(options = newOptions)
                                    onUpdate(newList)
                                },
                                placeholder = { Text("Option Name") },
                                modifier = Modifier.weight(0.6f),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            // Price Field - FIXED DECIMAL INPUT
                            OutlinedTextField(
                                value = if (option.price == 0.0) "" else option.price.toString(),
                                onValueChange = { priceStr ->
                                    val newOptions = customization.options.toMutableList()

                                    if (priceStr.isEmpty()) {
                                        // Handle empty input as 0.0
                                        newOptions[optIndex] = option.copy(price = 0.0)
                                    } else {
                                        // Allow free typing, only validate when needed
                                        // Accept numbers with optional decimal point
                                        if (priceStr.matches(Regex("^\\d*\\.?\\d*$"))) {
                                            val newPrice = priceStr.toDoubleOrNull() ?: 0.0
                                            newOptions[optIndex] = option.copy(price = newPrice)
                                        }
                                        // If doesn't match regex, don't update the model but keep the text
                                        // This allows typing like "1." which will be validated later
                                    }

                                    val newList = customizations.toMutableList()
                                    newList[index] = customization.copy(options = newOptions)
                                    onUpdate(newList)
                                },
                                label = { Text("+ RM") },
                                placeholder = { Text("Free") },
                                modifier = Modifier.weight(0.5f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                singleLine = true
                            )

                            // Delete Button
                            IconButton(onClick = {
                                val newOptions = customization.options.toMutableList()
                                newOptions.removeAt(optIndex)
                                val newList = customizations.toMutableList()
                                newList[index] = customization.copy(options = newOptions)
                                onUpdate(newList)
                            }) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Add Option Button
                    TextButton(onClick = {
                        val newOptions = customization.options + CustomizationOption("", 0.0)
                        val newList = customizations.toMutableList()
                        newList[index] = customization.copy(options = newOptions)
                        onUpdate(newList)
                    }) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                        Text("Add Option")
                    }
                }
            }
        }


        Button(
            onClick = {
                onUpdate(customizations + ProductCustomization("", required = false, singleSelection = true, options = listOf(CustomizationOption("", 0.0))))
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
        ) {
            Icon(Icons.Default.Add, null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Customization Group")
        }
    }
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