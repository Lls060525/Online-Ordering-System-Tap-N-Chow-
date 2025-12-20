package com.example.miniproject.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.miniproject.model.Customer
import com.example.miniproject.service.AuthService
import com.example.miniproject.service.DatabaseService
import kotlinx.coroutines.launch
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerProfileScreen(navController: NavController, isEditMode: Boolean = false) {
    val authService = AuthService()
    val databaseService = DatabaseService()
    val coroutineScope = rememberCoroutineScope()

    var customer by remember { mutableStateOf<Customer?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(isEditMode) }
    var showSaveSuccess by remember { mutableStateOf(false) }
    var showSaveError by remember { mutableStateOf(false) }
    var saveErrorMessage by remember { mutableStateOf("") }

    // Editable fields
    var editableName by remember { mutableStateOf("") }
    var editablePhone by remember { mutableStateOf("") }
    var editableEmail by remember { mutableStateOf("") }

    // Validation states
    var nameError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }

    // --- NEW: State to prevent double clicks ---
    var lastBackClickTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val currentCustomer = authService.getCurrentCustomer()
            customer = currentCustomer
            currentCustomer?.let {
                editableName = it.name
                editablePhone = it.phoneNumber
                editableEmail = it.email
            }
            isLoading = false
        }
    }

    // Validation functions (Keep existing code...)
    fun validateForm(): Boolean {
        var isValid = true

        if (editableName.isBlank()) {
            nameError = "Name is required"
            isValid = false
        } else if (editableName.length < 2) {
            nameError = "Name must be at least 2 characters"
            isValid = false
        } else {
            nameError = ""
        }

        if (editableEmail.isBlank()) {
            emailError = "Email is required"
            isValid = false
        } else if (!isValidEmail(editableEmail)) {
            emailError = "Please enter a valid email address"
            isValid = false
        } else {
            emailError = ""
        }

        if (editablePhone.isBlank()) {
            phoneError = "Phone number is required"
            isValid = false
        } else if (!isValidPhone(editablePhone)) {
            phoneError = "Please enter a valid phone number"
            isValid = false
        } else {
            phoneError = ""
        }

        return isValid
    }

    fun saveProfile() {
        if (!validateForm()) {
            return
        }

        coroutineScope.launch {
            customer?.let { currentCustomer ->
                val updatedCustomer = currentCustomer.copy(
                    name = editableName.trim(),
                    phoneNumber = editablePhone.trim(),
                    email = editableEmail.trim().lowercase()
                )

                databaseService.updateCustomerProfile(updatedCustomer).let { result ->
                    if (result.isSuccess) {
                        customer = updatedCustomer
                        isEditing = false
                        showSaveSuccess = true
                    } else {
                        saveErrorMessage = result.exceptionOrNull()?.message ?: "Failed to update profile"
                        showSaveError = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isEditing) "Edit Profile" else "My Profile",
                        fontWeight = FontWeight.Bold
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
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (!isEditing) {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Profile"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { saveProfile() }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Profile"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4CAF50),
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
            CustomerProfileContent(
                modifier = Modifier.padding(paddingValues),
                customer = customer,
                isEditing = isEditing,
                editableName = editableName,
                editablePhone = editablePhone,
                editableEmail = editableEmail,
                onNameChange = {
                    editableName = it
                    if (nameError.isNotEmpty()) nameError = ""
                },
                onPhoneChange = {
                    editablePhone = it
                    if (phoneError.isNotEmpty()) phoneError = ""
                },
                onEmailChange = {
                    editableEmail = it
                    if (emailError.isNotEmpty()) emailError = ""
                },
                nameError = nameError,
                phoneError = phoneError,
                emailError = emailError,
                showSaveSuccess = showSaveSuccess,
                showSaveError = showSaveError,
                saveErrorMessage = saveErrorMessage,
                onDismissSuccess = { showSaveSuccess = false },
                onDismissError = { showSaveError = false },
                onCancelEdit = {
                    isEditing = false
                    customer?.let {
                        editableName = it.name
                        editablePhone = it.phoneNumber
                        editableEmail = it.email
                    }
                    nameError = ""
                    phoneError = ""
                    emailError = ""
                },
                onSaveProfile = { saveProfile() }
            )
        }
    }
}

// ... (Rest of the file remains exactly the same: CustomerProfileContent, ProfileField, validation functions) ...

@Composable
fun CustomerProfileContent(
    modifier: Modifier = Modifier,
    customer: Customer?,
    isEditing: Boolean,
    editableName: String,
    editablePhone: String,
    editableEmail: String,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    nameError: String,
    phoneError: String,
    emailError: String,
    showSaveSuccess: Boolean,
    showSaveError: Boolean,
    saveErrorMessage: String,
    onDismissSuccess: () -> Unit,
    onDismissError: () -> Unit,
    onCancelEdit: () -> Unit,
    onSaveProfile: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Information Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Personal Information",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Customer ID (Non-editable)
                ProfileField(
                    label = "Customer ID",
                    value = customer?.customerId ?: "N/A",
                    isEditable = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Name Field
                ProfileField(
                    label = "Full Name *",
                    value = if (isEditing) editableName else customer?.name ?: "N/A",
                    isEditable = isEditing,
                    onValueChange = onNameChange,
                    errorMessage = nameError,
                    keyboardOptions = KeyboardOptions.Default
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Email Field
                ProfileField(
                    label = "Email Address *",
                    value = if (isEditing) editableEmail else customer?.email ?: "N/A",
                    isEditable = isEditing,
                    onValueChange = onEmailChange,
                    errorMessage = emailError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Phone Field
                ProfileField(
                    label = "Phone Number *",
                    value = if (isEditing) editablePhone else customer?.phoneNumber ?: "N/A",
                    isEditable = isEditing,
                    onValueChange = onPhoneChange,
                    errorMessage = phoneError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Member Since (Non-editable)
                customer?.createdAt?.let { createdAt ->
                    ProfileField(
                        label = "Member Since",
                        value = "${createdAt.toDate()}",
                        isEditable = false
                    )
                }
            }
        }

        if (isEditing) {
            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = onCancelEdit,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = onSaveProfile,
                    modifier = Modifier.weight(1f),
                    enabled = nameError.isEmpty() && phoneError.isEmpty() && emailError.isEmpty() &&
                            editableName.isNotBlank() && editablePhone.isNotBlank() && editableEmail.isNotBlank()
                ) {
                    Text("Save Changes")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "* Required fields",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }

    // Success Dialog
    if (showSaveSuccess) {
        AlertDialog(
            onDismissRequest = onDismissSuccess,
            title = { Text("Profile Updated") },
            text = { Text("Your profile has been updated successfully.") },
            confirmButton = {
                Button(onClick = onDismissSuccess) {
                    Text("OK")
                }
            }
        )
    }

    // Error Dialog
    if (showSaveError) {
        AlertDialog(
            onDismissRequest = onDismissError,
            title = { Text("Update Failed") },
            text = { Text(saveErrorMessage) },
            confirmButton = {
                Button(onClick = onDismissError) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ProfileField(
    label: String,
    value: String,
    isEditable: Boolean,
    onValueChange: (String) -> Unit = {},
    errorMessage: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        if (isEditable) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = keyboardOptions,
                singleLine = true,
                isError = errorMessage.isNotEmpty(),
                supportingText = {
                    if (errorMessage.isNotEmpty()) {
                        Text(text = errorMessage)
                    }
                }
            )
        } else {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
        }
    }
}

// Validation functions
private fun isValidEmail(email: String): Boolean {
    val emailRegex = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$"
    )
    return emailRegex.matcher(email).matches()
}

private fun isValidPhone(phone: String): Boolean {
    // Malaysian phone number format: starts with 01, 10-11 digits
    val cleanedPhone = phone.replace("[^0-9]".toRegex(), "")
    return cleanedPhone.length in 10..11 && cleanedPhone.startsWith("01")
}