package com.example.miniproject

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.miniproject.screens.LoginScreen
import com.example.miniproject.screens.RegisterScreen
import com.example.miniproject.screens.UserHomeScreen
import com.example.miniproject.screens.VendorHomeScreen
import com.example.miniproject.screens.VendorLoginScreen
import com.example.miniproject.screens.VendorRegisterScreen
import com.example.miniproject.ui.theme.MiniProjectTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MiniProjectTheme {
                // Navigation setup
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "login"
                ) {
                    composable("login") {
                        LoginScreen(navController = navController)
                    }
                    composable("register") {
                        RegisterScreen(navController = navController)
                    }
                    composable("vendorRegister") {
                        VendorRegisterScreen(navController = navController)
                    }
                    composable("vendorLogin") {
                        VendorLoginScreen(navController = navController)
                    }
                    composable("home") {
                        UserHomeScreen(navController = navController)
                    }
                    composable("vendorHome") {
                        VendorHomeScreen(navController = navController)
                    }
                }
            }
        }
    }
}