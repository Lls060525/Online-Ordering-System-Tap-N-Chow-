package com.example.miniproject.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.model.Customer
import com.example.miniproject.model.CustomerAccount
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import com.example.miniproject.utils.ImageConverter
import com.example.miniproject.utils.rememberImagePicker
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAccountScreen(navController: NavController) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val imageConverter = remember { ImageConverter(context) }

    var customer by remember { mutableStateOf<Customer?>(null) }
    var customerAccount by remember { mutableStateOf<CustomerAccount?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showImagePickerDialog by remember { mutableStateOf(false) }

    val imagePicker = rememberImagePicker { uri ->
        if (uri != null) {
            coroutineScope.launch {
                // Convert image to Base64
                val base64Image = imageConverter.uriToBase64(uri)
                base64Image?.let { imageString ->
                    // Update customer profile with Base64 image
                    customer?.customerId?.let { customerId ->
                        databaseService.updateCustomerProfileImageBase64(customerId, imageString).let { result ->
                            if (result.isSuccess) {
                                // Update local customer state
                                customer = customer?.copy(profileImageBase64 = imageString)
                            }
                        }
                    }
                }
            }
        }
        showImagePickerDialog = false
    }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val currentCustomer = authService.getCurrentCustomer()
            customer = currentCustomer

            currentCustomer?.customerId?.let { customerId ->
                val account = databaseService.getCustomerAccount(customerId)
                customerAccount = account
            }

            isLoading = false
        }
    }

    if (showImagePickerDialog) {
        AlertDialog(
            onDismissRequest = { showImagePickerDialog = false },
            title = { Text("Change Profile Picture") },
            text = { Text("Choose an option to update your profile picture") },
            confirmButton = {
                Button(
                    onClick = {
                        imagePicker.pickImageFromGallery()
                    }
                ) {
                    Text("Choose from Gallery")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showImagePickerDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Account",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            CustomerAccountContent(
                modifier = Modifier.padding(paddingValues),
                customer = customer,
                customerAccount = customerAccount,
                imageConverter = imageConverter,
                onEditProfilePicture = { showImagePickerDialog = true },
                onViewProfile = {
                    // Navigate to profile screen - ADD THIS LINE
                    navController.navigate("customerProfile")
                },
                onOrderHistory = {

                    navController.navigate("orderHistory")
                },
                onFavourites = {
                    // Navigate to favourites
                    // navController.navigate("favourites")
                },
                onLogout = {
                    coroutineScope.launch {
                        authService.logout()
                        navController.navigate("login") {
                            popUpTo(0)
                        }
                    }
                }
            )
        }
    }
}

@Composable
fun CustomerAccountContent(
    modifier: Modifier = Modifier,
    customer: Customer?,
    customerAccount: CustomerAccount?,
    imageConverter: ImageConverter,
    onEditProfilePicture: () -> Unit,
    onViewProfile: () -> Unit,
    onOrderHistory: () -> Unit,
    onFavourites: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Avatar Section
        ProfileAvatarSection(
            customer = customer,
            imageConverter = imageConverter,
            onEditClick = onEditProfilePicture,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // User Info Section
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = customer?.name ?: "Guest User",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Tap N Chow Credit Section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = "Credit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Tap N Chow Credit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "RM ${String.format("%.2f", customerAccount?.tapNChowCredit ?: 0.0)}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Menu Options
        AccountMenuItem(
            icon = Icons.Default.Person,
            title = "View Profile",
            onClick = onViewProfile
        )

        AccountMenuItem(
            icon = Icons.Default.History,
            title = "Order History",
            onClick = onOrderHistory
        )

        AccountMenuItem(
            icon = Icons.Default.Favorite,
            title = "Favourites",
            onClick = onFavourites
        )

        // Logout Button
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            AccountMenuItem(
                icon = Icons.Default.ExitToApp,
                title = "LOG OUT",
                onClick = onLogout,
                textColor = MaterialTheme.colorScheme.onPrimary
            )
        }

        // Customer Details Section
        CustomerDetailsSection(customer = customer)
    }
}

@Composable
fun ProfileAvatarSection(
    customer: Customer?,
    imageConverter: ImageConverter,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Avatar with Edit Button
            Box(
                contentAlignment = Alignment.BottomEnd
            ) {
                // Profile Image from Base64
                if (!customer?.profileImageBase64.isNullOrEmpty()) {
                    val bitmap = imageConverter.base64ToImageBitmap(customer!!.profileImageBase64)
                    bitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(MaterialTheme.shapes.extraLarge),
                            contentScale = ContentScale.Crop
                        )
                    } ?: run {
                        // Fallback if bitmap conversion fails
                        DefaultProfileAvatar()
                    }
                } else {
                    DefaultProfileAvatar()
                }

                // Edit Button
                FloatingActionButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(40.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Edit Profile Picture",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = customer?.name ?: "Guest User",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Tap to edit profile picture",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
fun DefaultProfileAvatar() {
    Icon(
        imageVector = Icons.Default.Person,
        contentDescription = "Profile Picture",
        modifier = Modifier
            .size(120.dp)
            .clip(MaterialTheme.shapes.extraLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(32.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// Rest of your composable functions remain the same...
@Composable
fun AccountMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    textColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Navigate",
                modifier = Modifier.rotate(180f)
            )
        }
    }
}

@Composable
fun CustomerDetailsSection(customer: Customer?) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Customer Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            CustomerDetailRow("Customer ID", customer?.customerId ?: "N/A")
            CustomerDetailRow("Name", customer?.name ?: "N/A")
            CustomerDetailRow("Email", customer?.email ?: "N/A")
            CustomerDetailRow("Phone", customer?.phoneNumber ?: "N/A")

            if (!customer?.profileImageBase64.isNullOrEmpty()) {
                CustomerDetailRow("Profile Image", "Uploaded")
            }

            customer?.createdAt?.let { createdAt ->
                CustomerDetailRow(
                    "Member Since",
                    "${createdAt.toDate()}"
                )
            }
        }
    }
}

@Composable
fun CustomerDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}