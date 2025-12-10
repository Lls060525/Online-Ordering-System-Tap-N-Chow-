// [file name]: AuthService.kt
package com.example.miniproject.service

import com.example.miniproject.model.Customer
import com.example.miniproject.model.Vendor
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ActionCodeSettings
import java.util.Date

class AuthService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // HARDCODED ADMIN CREDENTIALS
    private val ADMIN_ID = "ADMIN001"
    private val ADMIN_EMAIL = "admin@admin.com.my"
    private val ADMIN_PASSWORD = "administrator"

    suspend fun resetPasswordWithOTP(email: String): Result<Boolean> {
        return try {
            // First check if the email exists in our system
            val databaseService = DatabaseService()

            // Check if it's a customer
            val customers = databaseService.getAllCustomers()
            val isCustomer = customers.any { it.email.equals(email, ignoreCase = true) }

            // Check if it's a vendor
            val vendors = databaseService.getAllVendors()
            val isVendor = vendors.any { it.email.equals(email, ignoreCase = true) }

            // Check if it's an admin
            val isAdmin = email == ADMIN_EMAIL

            if (!isCustomer && !isVendor && !isAdmin) {
                return Result.failure(Exception("Email not found in our system"))
            }

            // Generate OTP (6-digit code)
            val otp = (100000..999999).random().toString()

            // Store OTP in Firestore with expiration (5 minutes)
            val otpData = hashMapOf(
                "email" to email,
                "otp" to otp,
                "createdAt" to Timestamp.now(),
                "expiresAt" to Timestamp(Date(Date().time + 5 * 60 * 1000)), // 5 minutes
                "used" to false
            )

            db.collection("password_reset_otps")
                .document(email)
                .set(otpData)
                .await()

            // In a real app, you would send the OTP via email/SMS
            // For development, we'll just log it
            println("DEBUG: OTP for $email is: $otp")

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOTP(email: String, otp: String): Result<Boolean> {
        return try {
            val otpDoc = db.collection("password_reset_otps")
                .document(email)
                .get()
                .await()

            if (!otpDoc.exists()) {
                return Result.failure(Exception("OTP not found or expired"))
            }

            val data = otpDoc.data ?: return Result.failure(Exception("Invalid OTP"))

            val storedOTP = data["otp"] as? String
            val expiresAt = data["expiresAt"] as? Timestamp
            val used = data["used"] as? Boolean ?: false

            // Check if OTP is used
            if (used) {
                return Result.failure(Exception("OTP has already been used"))
            }

            // Check if OTP is expired
            val now = Timestamp.now()
            if (expiresAt == null || expiresAt.seconds < now.seconds) {
                return Result.failure(Exception("OTP has expired"))
            }

            // Verify OTP
            if (storedOTP == otp) {
                // Mark OTP as used
                db.collection("password_reset_otps")
                    .document(email)
                    .update("used", true)
                    .await()

                Result.success(true)
            } else {
                Result.failure(Exception("Invalid OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, newPassword: String, confirmPassword: String): Result<Boolean> {
        return try {
            // Validate passwords
            if (newPassword != confirmPassword) {
                return Result.failure(Exception("Passwords do not match"))
            }

            if (newPassword.length < 6) {
                return Result.failure(Exception("Password must be at least 6 characters"))
            }

            // Update password in Firebase Auth
            val currentUser = auth.currentUser

            if (currentUser?.email == email) {
                // If user is currently logged in, update their password
                currentUser.updatePassword(newPassword).await()
            } else {
                // For password reset without login
                // Note: Firebase Admin SDK is needed for this on server-side
                // For client-side, we need to re-authenticate or use email link
                return Result.failure(Exception("Please complete OTP verification first"))
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Add this function for sending password reset email (alternative method)
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            // 1. Attempt to sign in with Firebase Auth
            val result = auth.signInWithEmailAndPassword(email, password).await()

            if (result.user != null) {
                val uid = result.user!!.uid

                // 2. Get user mapping to identify if Customer, Vendor, or Admin
                val mapping = db.collection("user_mappings").document(uid).get().await()

                val databaseService = DatabaseService()

                if (mapping.exists()) {
                    val isAdmin = mapping.getBoolean("isAdmin") ?: false
                    val customerId = mapping.getString("customerId")
                    val vendorId = mapping.getString("vendorId")

                    // 3. CHECK FREEZE STATUS (only for customers and vendors, NOT admin)
                    if (!customerId.isNullOrEmpty()) {
                        // It's a Customer - check if frozen
                        val customer = databaseService.getCustomerById(customerId)
                        if (customer == null) {
                            auth.signOut()
                            return Result.failure(Exception("User profile not found."))
                        }
                        if (customer.isFrozen) {
                            // CRITICAL: LOG OUT IMMEDIATELY
                            auth.signOut()
                            return Result.failure(Exception("Your account has been frozen. Please contact support."))
                        }
                        databaseService.updateCustomerLoginActivity(customerId)

                    } else if (!vendorId.isNullOrEmpty()) {
                        // It's a Vendor - check if frozen
                        val vendor = databaseService.getVendorById(vendorId)
                        if (vendor == null) {
                            auth.signOut()
                            return Result.failure(Exception("Vendor profile not found."))
                        }
                        if (vendor.isFrozen) {
                            // CRITICAL: LOG OUT IMMEDIATELY
                            auth.signOut()
                            return Result.failure(Exception("Your vendor account has been frozen. Please contact support."))
                        }
                        databaseService.updateVendorLoginActivity(vendorId)

                    } else if (isAdmin) {
                        // It's an Admin - NO FREEZE CHECK NEEDED
                        // Update admin login activity if you have that function
                        println("Admin logged in successfully")
                    }
                }

                Result.success(uid)
            } else {
                Result.failure(Exception("Login failed - no user returned"))
            }
        } catch (e: Exception) {
            // Ensure signed out if exception occurs during checks
            auth.signOut()
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }
    // HARDCODED ADMIN LOGIN - SEPARATE FUNCTION
    suspend fun adminLogin(email: String, password: String): Result<String> {
        return try {
            // Check hardcoded credentials FIRST
            if (email != ADMIN_EMAIL || password != ADMIN_PASSWORD) {
                return Result.failure(Exception("Invalid admin credentials"))
            }

            // First check if admin exists in database
            val databaseService = DatabaseService()
            val adminExists = databaseService.adminExists()

            if (!adminExists) {
                // Create admin if doesn't exist
                databaseService.createAdmin()
            }

            // Sign in with Firebase
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user!!.uid
                ensureAdminMapping(uid)
                Result.success(uid)
            } catch (e: Exception) {
                // If Firebase account doesn't exist, create it
                try {
                    val createResult = auth.createUserWithEmailAndPassword(email, password).await()
                    val uid = createResult.user!!.uid
                    ensureAdminMapping(uid)
                    Result.success(uid)
                } catch (createError: Exception) {
                    Result.failure(Exception("Admin account setup failed: ${createError.message}"))
                }
            }
        } catch (e: Exception) {
            auth.signOut()
            Result.failure(Exception("Admin login failed: ${e.message}"))
        }
    }

    private suspend fun ensureAdminMapping(firebaseUid: String) {
        try {
            // Create admin mapping without vendorId
            db.collection("user_mappings").document(firebaseUid).set(
                mapOf(
                    "adminId" to ADMIN_ID,
                    "firebaseUid" to firebaseUid,
                    "isAdmin" to true,
                    "createdAt" to Timestamp.now()
                )
            ).await()
        } catch (e: Exception) {
            println("Error ensuring admin mapping: ${e.message}")
        }
    }

    suspend fun isAdminUser(): Boolean {
        return try {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                if (user.email == ADMIN_EMAIL) return true
                val mapping = db.collection("user_mappings").document(user.uid).get().await()
                mapping.getBoolean("isAdmin") ?: false
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registerCustomer(
        name: String,
        email: String,
        phoneNumber: String,
        password: String
    ): Result<String> {
        return try {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                return Result.failure(Exception("User already exists with this email"))
            } catch (e: Exception) {
                // Proceed
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUserId = authResult.user?.uid ?: throw Exception("Customer creation failed")

            val customerId = Customer.generateCustomerId(db)

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            authResult.user?.updateProfile(profileUpdates)?.await()

            val customer = Customer(
                customerId = customerId,
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            db.collection("customers").document(customerId).set(customer).await()

            db.collection("user_mappings").document(firebaseUserId).set(
                mapOf(
                    "customerId" to customerId,
                    "firebaseUid" to firebaseUserId
                )
            ).await()

            Result.success(customerId)

        } catch (e: Exception) {
            auth.currentUser?.delete()?.await()
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }

    suspend fun registerVendor(
        vendorName: String,
        email: String,
        vendorContact: String,
        address: String,
        category: String,
        password: String
    ): Result<String> {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUserId = authResult.user?.uid ?: throw Exception("Vendor creation failed")

            val vendorId = Vendor.generateVendorId(db)

            val vendor = Vendor(
                vendorId = vendorId,
                vendorName = vendorName,
                email = email,
                vendorContact = vendorContact,
                address = address,
                category = category
            )

            db.collection("vendors").document(vendorId).set(vendor).await()

            db.collection("user_mappings").document(firebaseUserId).set(
                mapOf(
                    "vendorId" to vendorId,
                    "firebaseUid" to firebaseUserId,
                    "isAdmin" to false
                )
            ).await()

            Result.success(vendorId)
        } catch (e: Exception) {
            auth.currentUser?.delete()?.await()
            Result.failure(e)
        }
    }

    private suspend fun getVendorIdFromFirebaseUid(firebaseUid: String): String? {
        return try {
            val doc = db.collection("user_mappings").document(firebaseUid).get().await()
            doc.getString("vendorId")
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun getCustomerIdFromFirebaseUid(firebaseUid: String): String? {
        return try {
            val doc = db.collection("user_mappings").document(firebaseUid).get().await()
            doc.getString("customerId")
        } catch (e: Exception) {
            null
        }
    }

    // Get current customer data
    suspend fun getCurrentCustomer(): Customer? {
        return try {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                val customerId = getCustomerIdFromFirebaseUid(user.uid)
                customerId?.let {
                    db.collection("customers").document(it).get().await()
                        .toObject(Customer::class.java)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // Get current vendor data
    suspend fun getCurrentVendor(): Vendor? {
        return try {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                val vendorId = getVendorIdFromFirebaseUid(user.uid)
                vendorId?.let {
                    db.collection("vendors").document(it).get().await()
                        .toObject(Vendor::class.java)
                }
            }
        } catch (e: Exception) {
            null
        }
    }

    // Add this function to DatabaseService.kt
    suspend fun getVendorByFirebaseUid(firebaseUid: String): Vendor? {
        return try {
            val mapping = db.collection("user_mappings").document(firebaseUid).get().await()
            val vendorId = mapping.getString("vendorId")
            vendorId?.let {
                db.collection("vendors").document(it).get().await()
                    .toObject(Vendor::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }

    // Update existing getVendorById function to use the new ID format
    suspend fun getVendorById(vendorId: String): Vendor? {
        return try {
            db.collection("vendors").document(vendorId).get().await()
                .toObject(Vendor::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun getCurrentUser() = auth.currentUser
    fun logout() = auth.signOut()

    // Helper function to check if current user should see admin features
    suspend fun shouldShowAdminFeatures(): Boolean {
        return isAdminUser()
    }

    // Get current user role
    suspend fun getCurrentUserRole(): String {
        return try {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                if (user.email == ADMIN_EMAIL) return "admin"
                val mapping = db.collection("user_mappings").document(user.uid).get().await()
                when {
                    mapping.getBoolean("isAdmin") == true -> "admin"
                    mapping.getString("vendorId") != null -> "vendor"
                    mapping.getString("customerId") != null -> "customer"
                    else -> "unknown"
                }
            } ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    // Add function to check if user is frozen
    suspend fun isUserFrozen(): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val mapping = db.collection("user_mappings").document(currentUser.uid).get().await()

            if (mapping.exists()) {
                val isAdmin = mapping.getBoolean("isAdmin") ?: false
                if (isAdmin) return false // Admin cannot be frozen

                val customerId = mapping.getString("customerId")
                val vendorId = mapping.getString("vendorId")

                if (!customerId.isNullOrEmpty()) {
                    val customer = DatabaseService().getCustomerById(customerId)
                    return customer?.isFrozen ?: false
                }

                if (!vendorId.isNullOrEmpty()) {
                    val vendor = DatabaseService().getVendorById(vendorId)
                    return vendor?.isFrozen ?: false
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }
}