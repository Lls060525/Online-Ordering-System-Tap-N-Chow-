package com.example.miniproject.service

import com.example.miniproject.model.Customer
import com.example.miniproject.model.Vendor
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthService {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            if (result.user != null) {
                Result.success(result.user!!.uid)
            } else {
                Result.failure(Exception("Login failed - no user returned"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Login failed: ${e.message}"))
        }
    }
    suspend fun registerCustomer(
        name: String,
        email: String,
        phoneNumber: String,
        password: String
    ): Result<String> {
        return try {
            // Check if user already exists by trying to sign in first
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                return Result.failure(Exception("User already exists with this email"))
            } catch (e: Exception) {
                // User doesn't exist, proceed with registration
            }

            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUserId = authResult.user?.uid ?: throw Exception("Customer creation failed")

            // Generate custom customer ID (C0001 format)
            val customerId = Customer.generateCustomerId(db)

            // Update profile
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            authResult.user?.updateProfile(profileUpdates)?.await()

            // Create customer document with custom ID
            val customer = Customer(
                customerId = customerId,
                name = name,
                email = email,
                phoneNumber = phoneNumber,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            // Store using the custom customerId as document ID
            db.collection("customers").document(customerId).set(customer).await()

            // Also store the mapping between Firebase UID and custom customer ID
            db.collection("user_mappings").document(firebaseUserId).set(
                mapOf(
                    "customerId" to customerId,
                    "firebaseUid" to firebaseUserId
                )
            ).await()

            Result.success(customerId)

        } catch (e: Exception) {
            // Clean up auth user if Firestore fails
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
            // Create auth vendor
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUserId = authResult.user?.uid ?: throw Exception("Vendor creation failed")

            // Generate custom vendor ID (V0001 format)
            val vendorId = Vendor.generateVendorId(db)

            // Create vendor document with custom ID
            val vendor = Vendor(
                vendorId = vendorId,
                vendorName = vendorName,
                email = email,
                vendorContact = vendorContact,
                address = address,
                category = category
                // createdAt and updatedAt will use default values from data class
            )

            // Store using the custom vendorId as document ID
            db.collection("vendors").document(vendorId).set(vendor).await()

            // Also store the mapping between Firebase UID and custom vendor ID
            db.collection("user_mappings").document(firebaseUserId).set(
                mapOf(
                    "vendorId" to vendorId,
                    "firebaseUid" to firebaseUserId
                )
            ).await()

            Result.success(vendorId)
        } catch (e: Exception) {
            println("DEBUG: Registration error: ${e.message}") // Add logging
            e.printStackTrace() // Add stack trace
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
}
