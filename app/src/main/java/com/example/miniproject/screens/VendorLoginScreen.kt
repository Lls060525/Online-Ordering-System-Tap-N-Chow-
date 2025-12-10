package com.example.miniproject.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.miniproject.R
import com.example.miniproject.components.CustomTextField
import com.example.miniproject.service.AuthService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VendorLoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Create AuthService instance once
    val authService = remember { AuthService() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFA500))
    ) {
        Scaffold(
            modifier = Modifier
                .statusBarsPadding()
                .padding(top = 30.dp),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = { Text("Tap N Chow - Vendor") }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo2),
                        contentDescription = "Logo",
                        modifier = Modifier.size(150.dp)
                    )

                    Text(
                        text = "Vendor Login",
                        style = MaterialTheme.typography.headlineMedium,
                        fontSize = 32.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    CustomTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Vendor Email",
                        placeholder = "Type your vendor email"
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    CustomTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        placeholder = "Type your password",
                        visualTransformation = PasswordVisualTransformation()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    TextButton(
                        onClick = { navController.navigate("forgotPassword") }
                    ) {
                        Text("Forgot password?")
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            if (email.isNotEmpty() && password.isNotEmpty()) {
                                isLoading = true
                                CoroutineScope(Dispatchers.Main).launch {
                                    val result = authService.login(email, password)

                                    if (result.isSuccess) {
                                        // LOGIN SUCCESSFUL - Check Role
                                        val role = authService.getCurrentUserRole()

                                        if (role == "customer") {
                                            // BLOCKED: Customer trying to log in as Vendor
                                            authService.logout()
                                            isLoading = false
                                            errorMessage = "Invalid account type. Please use Customer Login."
                                        } else if (role == "admin") {
                                            // BLOCKED: Admin trying to log in as Vendor
                                            authService.logout()
                                            isLoading = false
                                            errorMessage = "Invalid account type. Please use Admin Login."
                                        } else {
                                            // SUCCESS: Vendor allowed
                                            isLoading = false
                                            navController.navigate("vendorHome") {
                                                popUpTo("vendorLogin") { inclusive = true }
                                            }
                                        }
                                    } else {
                                        isLoading = false
                                        val errorMsg = result.exceptionOrNull()?.message ?: "Login failed"
                                        errorMessage = errorMsg // Show general error first

                                        // Show specific snackbar if frozen
                                        if (errorMsg.contains("frozen", ignoreCase = true)) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = errorMsg,
                                                    duration = SnackbarDuration.Long
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                errorMessage = "Please fill all fields"
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Enter as Vendor")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        TextButton(
                            onClick = { navController.navigate("vendorRegister") }
                        ) {
                            Text("Don't have a vendor account? Register")
                        }

                        TextButton(
                            onClick = { navController.navigate("login") }
                        ) {
                            Text("Are you a customer? Customer Login")
                        }
                    }

                    errorMessage?.let { message ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}