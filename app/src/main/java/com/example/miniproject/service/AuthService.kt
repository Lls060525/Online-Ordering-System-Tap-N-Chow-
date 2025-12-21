
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

    // ... (Keep resetPasswordWithOTP and verifyOTP functions as they are) ...

    suspend fun resetPasswordWithOTP(email: String): Result<Boolean> {
        return try {
            val databaseService = DatabaseService()
            val customers = databaseService.getAllCustomers()
            val isCustomer = customers.any { it.email.equals(email, ignoreCase = true) }
            val vendors = databaseService.getAllVendors()
            val isVendor = vendors.any { it.email.equals(email, ignoreCase = true) }
            val isAdmin = email == ADMIN_EMAIL

            if (!isCustomer && !isVendor && !isAdmin) {
                return Result.failure(Exception("Email not found in our system"))
            }

            val otp = (100000..999999).random().toString()
            val otpData = hashMapOf(
                "email" to email,
                "otp" to otp,
                "createdAt" to Timestamp.now(),
                "expiresAt" to Timestamp(Date(Date().time + 5 * 60 * 1000)),
                "used" to false
            )

            db.collection("password_reset_otps").document(email).set(otpData).await()
            println("DEBUG: OTP for $email is: $otp")
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyOTP(email: String, otp: String): Result<Boolean> {
        // ... (Keep existing implementation) ...
        return try {
            val otpDoc = db.collection("password_reset_otps").document(email).get().await()
            if (!otpDoc.exists()) return Result.failure(Exception("OTP not found or expired"))

            val data = otpDoc.data ?: return Result.failure(Exception("Invalid OTP"))
            val storedOTP = data["otp"] as? String
            val expiresAt = data["expiresAt"] as? Timestamp
            val used = data["used"] as? Boolean ?: false

            if (used) return Result.failure(Exception("OTP has already been used"))

            val now = Timestamp.now()
            if (expiresAt == null || expiresAt.seconds < now.seconds) {
                return Result.failure(Exception("OTP has expired"))
            }

            if (storedOTP == otp) {
                db.collection("password_reset_otps").document(email).update("used", true).await()
                Result.success(true)
            } else {
                Result.failure(Exception("Invalid OTP"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun resetPassword(email: String, newPassword: String, confirmPassword: String): Result<Boolean> {
        // ... (Keep existing implementation) ...
        return try {
            if (newPassword != confirmPassword) return Result.failure(Exception("Passwords do not match"))
            if (newPassword.length < 6) return Result.failure(Exception("Password must be at least 6 characters"))

            val currentUser = auth.currentUser
            if (currentUser?.email == email) {
                currentUser.updatePassword(newPassword).await()
            } else {
                return Result.failure(Exception("Please complete OTP verification first"))
            }
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- UPDATED FUNCTION ---
    suspend fun sendPasswordResetEmail(email: String): Result<Boolean> {
        return try {
            // 1. Check if email matches Admin
            val isAdmin = email == ADMIN_EMAIL

            // 2. Check if email exists in Customers collection
            val customerQuery = db.collection("customers")
                .whereEqualTo("email", email)
                .get()
                .await()

            // 3. Check if email exists in Vendors collection
            val vendorQuery = db.collection("vendors")
                .whereEqualTo("email", email)
                .get()
                .await()

            // If email is not found in any valid group, return specific error
            if (customerQuery.isEmpty && vendorQuery.isEmpty && !isAdmin) {
                return Result.failure(Exception("This email address is not registered."))
            }

            // 4. If registered, proceed to send Firebase reset email
            auth.sendPasswordResetEmail(email).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ... (Keep the rest of the file: login, registerCustomer, registerVendor, etc. exactly as they are) ...

    suspend fun login(email: String, password: String): Result<String> {
        // ... (Existing login logic) ...
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user != null) {
                val user = result.user!!
                val uid = user.uid
                val mapping = db.collection("user_mappings").document(uid).get().await()
                val databaseService = DatabaseService()

                if (mapping.exists()) {
                    val isAdmin = mapping.getBoolean("isAdmin") ?: false
                    val customerId = mapping.getString("customerId")
                    val vendorId = mapping.getString("vendorId")

                    if (!customerId.isNullOrEmpty()) {
                        user.reload().await()
                        if (!user.isEmailVerified) {
                            auth.signOut()
                            return Result.failure(Exception("Please verify your email address. Check your inbox."))
                        }
                        val customer = databaseService.getCustomerById(customerId)
                        if (customer == null) {
                            auth.signOut()
                            return Result.failure(Exception("User profile not found."))
                        }
                        if (customer.isFrozen) {
                            auth.signOut()
                            return Result.failure(Exception("Your account has been frozen. Please contact support."))
                        }
                        databaseService.updateCustomerLoginActivity(customerId)
                    } else if (!vendorId.isNullOrEmpty()) {
                        val vendor = databaseService.getVendorById(vendorId)
                        if (vendor == null) {
                            auth.signOut()
                            return Result.failure(Exception("Vendor profile not found."))
                        }
                        if (vendor.isFrozen) {
                            auth.signOut()
                            return Result.failure(Exception("Your vendor account has been frozen. Please contact support."))
                        }
                        databaseService.updateVendorLoginActivity(vendorId)
                    }
                }
                Result.success(uid)
            } else {
                Result.failure(Exception("Login failed - no user returned"))
            }
        } catch (e: Exception) {
            auth.signOut()
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }

    suspend fun adminLogin(email: String, password: String): Result<String> {
        // ... (Existing adminLogin logic) ...
        return try {
            if (email != ADMIN_EMAIL || password != ADMIN_PASSWORD) {
                return Result.failure(Exception("Invalid admin credentials"))
            }
            val databaseService = DatabaseService()
            val adminExists = databaseService.adminExists()
            if (!adminExists) {
                databaseService.createAdmin()
            }
            try {
                val result = auth.signInWithEmailAndPassword(email, password).await()
                val uid = result.user!!.uid
                ensureAdminMapping(uid)
                Result.success(uid)
            } catch (e: Exception) {
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
        // ... (Existing registerCustomer logic) ...
        return try {
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                return Result.failure(Exception("User already exists with this email"))
            } catch (e: Exception) { }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: throw Exception("Customer creation failed")
            val firebaseUserId = user.uid

            try {
                user.sendEmailVerification().await()
            } catch (e: Exception) {
                println("Failed to send verification email: ${e.message}")
            }

            val customerId = Customer.generateCustomerId(db)
            val profileUpdates = UserProfileChangeRequest.Builder().setDisplayName(name).build()
            user.updateProfile(profileUpdates).await()

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
                mapOf("customerId" to customerId, "firebaseUid" to firebaseUserId)
            ).await()

            auth.signOut()
            Result.success(customerId)
        } catch (e: Exception) {
            auth.currentUser?.delete()?.await()
            Result.failure(Exception("Registration failed: ${e.message}"))
        }
    }

    suspend fun resendVerificationEmail(email: String, password: String): Result<Boolean> {
        // ... (Existing logic) ...
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                if (user.isEmailVerified) return Result.failure(Exception("Email is already verified."))
                user.sendEmailVerification().await()
                auth.signOut()
                Result.success(true)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun registerVendor(
        vendorName: String,
        email: String,
        vendorContact: String,
        address: String,
        category: String,
        password: String,
        latitude: Double = 0.0,
        longitude: Double = 0.0
    ): Result<String> {
        // ... (Existing logic) ...
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
                category = category,
                latitude = latitude,
                longitude = longitude
            )
            db.collection("vendors").document(vendorId).set(vendor).await()
            db.collection("user_mappings").document(firebaseUserId).set(
                mapOf("vendorId" to vendorId, "firebaseUid" to firebaseUserId, "isAdmin" to false)
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
        } catch (e: Exception) { null }
    }

    private suspend fun getCustomerIdFromFirebaseUid(firebaseUid: String): String? {
        return try {
            val doc = db.collection("user_mappings").document(firebaseUid).get().await()
            doc.getString("customerId")
        } catch (e: Exception) { null }
    }

    suspend fun getCurrentCustomer(): Customer? {
        return try {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                val customerId = getCustomerIdFromFirebaseUid(user.uid)
                customerId?.let { db.collection("customers").document(it).get().await().toObject(Customer::class.java) }
            }
        } catch (e: Exception) { null }
    }

    suspend fun getCurrentVendor(): Vendor? {
        return try {
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                val vendorId = getVendorIdFromFirebaseUid(user.uid)
                vendorId?.let { db.collection("vendors").document(it).get().await().toObject(Vendor::class.java) }
            }
        } catch (e: Exception) { null }
    }

    suspend fun getVendorByFirebaseUid(firebaseUid: String): Vendor? {
        return try {
            val mapping = db.collection("user_mappings").document(firebaseUid).get().await()
            val vendorId = mapping.getString("vendorId")
            vendorId?.let { db.collection("vendors").document(it).get().await().toObject(Vendor::class.java) }
        } catch (e: Exception) { null }
    }

    suspend fun getVendorById(vendorId: String): Vendor? {
        return try {
            db.collection("vendors").document(vendorId).get().await().toObject(Vendor::class.java)
        } catch (e: Exception) { null }
    }

    fun getCurrentUser() = auth.currentUser
    fun logout() = auth.signOut()

    suspend fun shouldShowAdminFeatures(): Boolean {
        return isAdminUser()
    }

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
        } catch (e: Exception) { "unknown" }
    }

    suspend fun isUserFrozen(): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val mapping = db.collection("user_mappings").document(currentUser.uid).get().await()

            if (mapping.exists()) {
                val isAdmin = mapping.getBoolean("isAdmin") ?: false
                if (isAdmin) return false

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
        } catch (e: Exception) { false }
    }
}