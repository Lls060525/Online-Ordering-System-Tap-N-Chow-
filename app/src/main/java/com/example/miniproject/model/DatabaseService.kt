package com.example.miniproject.service

import com.example.miniproject.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class DatabaseService {
    private val db = FirebaseFirestore.getInstance()

    // Product Operations
    suspend fun addProduct(product: Product): Result<String> {
        return try {
            val documentRef = db.collection("products").add(product).await()
            Result.success(documentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductsByVendor(vendorId: String): List<Product> {
        return try {
            db.collection("products")
                .whereEqualTo("vendorId", vendorId)
                .get()
                .await()
                .toObjects(Product::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getAllProducts(): List<Product> {
        return try {
            db.collection("products")
                .get()
                .await()
                .toObjects(Product::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateProductStock(productId: String, newStock: Int): Result<Boolean> {
        return try {
            db.collection("products").document(productId)
                .update("stock", newStock).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Order Operations
    suspend fun createOrder(order: Order, orderDetails: List<OrderDetail>): Result<String> {
        return try {
            // Create order
            val orderRef = db.collection("orders").add(order).await()
            val orderId = orderRef.id

            // Create order details
            orderDetails.forEach { detail ->
                db.collection("order_details").add(
                    detail.copy(orderId = orderId)
                ).await()
            }

            Result.success(orderId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerOrders(customerId: String): List<Order> {
        return try {
            db.collection("orders")
                .whereEqualTo("customerId", customerId)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOrderDetails(orderId: String): List<OrderDetail> {
        return try {
            db.collection("order_details")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()
                .toObjects(OrderDetail::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String): Result<Boolean> {
        return try {
            db.collection("orders").document(orderId)
                .update("status", status).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Payment Operations
    suspend fun createPayment(payment: Payment): Result<String> {
        return try {
            val paymentRef = db.collection("payments").add(payment).await()

            // Update order payment status
            db.collection("orders").document(payment.orderId)
                .update("paymentMethod", payment.paymentMethod).await()

            Result.success(paymentRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPaymentByOrder(orderId: String): Payment? {
        return try {
            db.collection("payments")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()
                .documents.firstOrNull()
                ?.toObject(Payment::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Feedback Operations
    suspend fun addFeedback(feedback: Feedback): Result<String> {
        return try {
            val feedbackRef = db.collection("feedbacks").add(feedback).await()
            Result.success(feedbackRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFeedbackByOrder(orderId: String): Feedback? {
        return try {
            db.collection("feedbacks")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()
                .documents.firstOrNull()
                ?.toObject(Feedback::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFeedbackByProduct(productId: String): List<Feedback> {
        return try {
            // This would require joining with order_details to get product-specific feedback
            // For simplicity, we'll get all feedback and filter by product in the app
            db.collection("feedbacks")
                .get()
                .await()
                .toObjects(Feedback::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Vendor Operations
    suspend fun getVendorById(vendorId: String): Vendor? {
        return try {
            db.collection("vendors").document(vendorId).get().await()
                .toObject(Vendor::class.java)
        } catch (e: Exception) {
            null
        }
    }

    // Customer Operations
    suspend fun getCustomerById(customerId: String): Customer? {
        return try {
            db.collection("customers").document(customerId).get().await()
                .toObject(Customer::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getCustomerByFirebaseUid(firebaseUid: String): Customer? {
        return try {
            val mapping = db.collection("user_mappings").document(firebaseUid).get().await()
            val customerId = mapping.getString("customerId")
            customerId?.let {
                db.collection("customers").document(it).get().await()
                    .toObject(Customer::class.java)
            }
        } catch (e: Exception) {
            null
        }
    }


}