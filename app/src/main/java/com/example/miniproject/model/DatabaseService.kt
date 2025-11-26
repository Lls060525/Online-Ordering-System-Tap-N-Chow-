package com.example.miniproject.service

import com.example.miniproject.model.*
import com.google.firebase.Timestamp
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

    suspend fun deleteProduct(productId: String): Result<Boolean> {
        return try {
            db.collection("products").document(productId).delete().await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateProduct(product: Product): Result<Boolean> {
        return try {
            db.collection("products").document(product.productId)
                .update(
                    mapOf(
                        "productName" to product.productName,
                        "productPrice" to product.productPrice,
                        "description" to product.description,
                        "stock" to product.stock,
                        "imageUrl" to product.imageUrl,
                        "category" to product.category,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getProductById(productId: String): Product? {
        return try {
            db.collection("products").document(productId).get().await()
                .toObject(Product::class.java)
        } catch (e: Exception) {
            null
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
                .update(
                    mapOf(
                        "stock" to newStock,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Order Operations - COMPLETELY FIXED VERSION
    suspend fun createOrderWithDetails(orderRequest: OrderRequest): Result<String> {
        return try {
            val orderId = Order.generateOrderId(db)
            val batch = db.batch()

            println("🛒 DEBUG: Starting order creation for ${orderRequest.items.size} items")

            // STEP 1: Validate stock for ALL items first
            val stockUpdates = mutableMapOf<String, Int>() // productId -> newStock
            val productDetails = mutableMapOf<String, Product>()

            for (item in orderRequest.items) {
                println("📦 DEBUG: Validating ${item.productName} - Quantity: ${item.quantity}")

                val product = getProductById(item.productId)
                if (product == null) {
                    println("❌ DEBUG: Product ${item.productId} not found")
                    return Result.failure(Exception("Product ${item.productName} not found"))
                }

                productDetails[item.productId] = product
                println("📊 DEBUG: ${product.productName} - Current Stock: ${product.stock}, Required: ${item.quantity}")

                if (product.stock < item.quantity) {
                    println("❌ DEBUG: Insufficient stock for ${product.productName}")
                    return Result.failure(Exception("Insufficient stock for ${item.productName}. Available: ${product.stock}, Requested: ${item.quantity}"))
                }

                // Calculate new stock
                val newStock = product.stock - item.quantity
                stockUpdates[item.productId] = newStock
                println("✅ DEBUG: ${product.productName} - Will update stock from ${product.stock} to $newStock")
            }

            // STEP 2: Update ALL product stocks in batch
            stockUpdates.forEach { (productId, newStock) ->
                val productRef = db.collection("products").document(productId)
                batch.update(productRef, "stock", newStock)
                batch.update(productRef, "updatedAt", Timestamp.now())
                println("🔄 DEBUG: Batch update for $productId -> stock: $newStock")
            }

            // STEP 3: Create order
            val order = Order(
                orderId = orderId,
                customerId = orderRequest.customerId,
                totalPrice = orderRequest.totalAmount,
                shippingAddress = orderRequest.deliveryAddress,
                paymentMethod = orderRequest.paymentMethod,
                status = "pending",
                orderDate = Timestamp.now()
            )
            val orderRef = db.collection("orders").document(orderId)
            batch.set(orderRef, order)
            println("📋 DEBUG: Order created with ID: $orderId")

            // STEP 4: Create order details
            orderRequest.items.forEach { item ->
                val orderDetail = OrderDetail(
                    orderId = orderId,
                    productId = item.productId,
                    productName = item.productName,
                    productPrice = item.productPrice,
                    quantity = item.quantity,
                    subtotal = item.productPrice * item.quantity
                )
                val orderDetailRef = db.collection("order_details").document()
                batch.set(orderDetailRef, orderDetail)
                println("📝 DEBUG: Order detail for ${item.productName} - Qty: ${item.quantity}")
            }

            // STEP 5: Create payment record
            val payment = Payment(
                orderId = orderId,
                amount = orderRequest.totalAmount,
                paymentMethod = orderRequest.paymentMethod,
                paymentStatus = "pending",
                transactionDate = Timestamp.now()
            )
            val paymentRef = db.collection("payments").document()
            batch.set(paymentRef, payment)

            // STEP 6: Commit the batch (ALL operations happen together)
            println("🚀 DEBUG: Committing batch transaction...")
            batch.commit().await()
            println("🎉 DEBUG: Order successfully created! Stock deducted for all products.")

            // STEP 7: Verify stock was actually updated
            stockUpdates.forEach { (productId, expectedStock) ->
                val updatedProduct = getProductById(productId)
                println("✅ VERIFICATION: ${updatedProduct?.productName} - Stock is now: ${updatedProduct?.stock} (expected: $expectedStock)")
            }

            Result.success(orderId)
        } catch (e: Exception) {
            println("💥 DEBUG: Error in createOrderWithDetails: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    suspend fun updatePaymentStatus(orderId: String, status: String): Result<Boolean> {
        return try {
            // Update payment status
            val paymentQuery = db.collection("payments")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            if (!paymentQuery.isEmpty) {
                val paymentDoc = paymentQuery.documents.first()
                paymentDoc.reference.update("paymentStatus", status).await()
            }

            // Update order status if payment is completed
            if (status == "completed") {
                db.collection("orders").document(orderId)
                    .update("status", "confirmed").await()
            }

            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerOrders(customerId: String): List<Order> {
        return try {
<<<<<<< HEAD
<<<<<<< HEAD
            println("DEBUG: Getting ALL orders and filtering for customer: $customerId")

            val allOrders = db.collection("orders")
                .get()
                .await()

            println("DEBUG: Total orders in database: ${allOrders.documents.size}")

            val orders = mutableListOf<Order>()
            allOrders.documents.forEach { doc ->
                val data = doc.data ?: emptyMap()
                println("DEBUG: Order ${doc.id} fields: ${data.keys}")

                // Check all possible customer ID field names
                val custId = data["customerId"] as? String ?:
                data["CustomerID"] as? String ?:
                data["customerID"] as? String ?: ""

                if (custId == customerId) {
                    println("DEBUG: Found matching order for customer $customerId: ${doc.id}")
                    val order = Order(
                        orderId = data["orderId"] as? String ?: doc.id,
                        documentId = doc.id,
                        customerId = custId,
                        orderDate = data["orderDate"] as? Timestamp ?: Timestamp.now(),
                        status = data["status"] as? String ?: "pending",
                        totalPrice = (data["totalPrice"] as? Double) ?: 0.0,
                        shippingAddress = data["shippingAddress"] as? String ?: "",
                        paymentMethod = data["paymentMethod"] as? String ?: "",
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                    )
                    orders.add(order)
                }
            }

=======
=======
>>>>>>> bafca0c93a1fde491674d3612618706a9464d8d4
            val orders = db.collection("orders")
                .whereEqualTo("customerId", customerId)
                .orderBy("orderDate", Query.Direction.DESCENDING)
                .get()
                .await()
                .toObjects(Order::class.java)
<<<<<<< HEAD
>>>>>>> bafca0c93a1fde491674d3612618706a9464d8d4
=======
>>>>>>> bafca0c93a1fde491674d3612618706a9464d8d4
            println("DEBUG: Found ${orders.size} orders for customer $customerId")
            orders
        } catch (e: Exception) {
            println("DEBUG: Error in getCustomerOrders: ${e.message}")
            emptyList()
        }
    }
    suspend fun getOrderDetails(orderId: String): List<OrderDetail> {
        return try {
            println("DEBUG: Getting order details for orderId: $orderId")

            val result = db.collection("order_details")
                .whereEqualTo("orderId", orderId)
                .get()
                .await()

            println("DEBUG: Found ${result.documents.size} order details for order: $orderId")

            result.toObjects(OrderDetail::class.java)
        } catch (e: Exception) {
            println("DEBUG: Error getting order details: ${e.message}")
            emptyList()
        }
    }

    suspend fun getAllOrders(): List<Order> {
        return try {
            db.collection("orders")
                .get()
                .await()
                .toObjects(Order::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getOrderById(orderId: String): Order? {
        return try {
            db.collection("orders").document(orderId).get().await()
                .toObject(Order::class.java)
        } catch (e: Exception) {
            null
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

    suspend fun getAllVendors(): List<Vendor> {
        return try {
            db.collection("vendors")
                .get()
                .await()
                .toObjects(Vendor::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun updateVendorProfileImageBase64(vendorId: String, base64Image: String): Result<Boolean> {
        return try {
            db.collection("vendors").document(vendorId)
                .update("profileImageBase64", base64Image).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVendorProfile(vendor: Vendor): Result<Boolean> {
        return try {
            db.collection("vendors").document(vendor.vendorId)
                .update(
                    mapOf(
                        "vendorName" to vendor.vendorName,
                        "email" to vendor.email,
                        "vendorContact" to vendor.vendorContact,
                        "address" to vendor.address,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCustomerAccount(customerId: String): CustomerAccount? {
        return try {
            db.collection("customer_accounts")
                .document(customerId)
                .get()
                .await()
                .toObject(CustomerAccount::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateCustomerProfile(customer: Customer): Result<Boolean> {
        return try {
            db.collection("customers").document(customer.customerId)
                .update(
                    mapOf(
                        "name" to customer.name,
                        "phoneNumber" to customer.phoneNumber,
                        "email" to customer.email,
                        "updatedAt" to Timestamp.now()
                    )
                ).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerCredit(customerId: String, newBalance: Double): Result<Boolean> {
        return try {
            val account = CustomerAccount(
                customerId = customerId,
                tapNChowCredit = newBalance,
                lastUpdated = Timestamp.now()
            )
            db.collection("customer_accounts")
                .document(customerId)
                .set(account)
                .await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateCustomerProfileImageBase64(customerId: String, base64Image: String): Result<Boolean> {
        return try {
            db.collection("customers").document(customerId)
                .update("profileImageBase64", base64Image).await()
            Result.success(true)
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

    // Restock function for vendors
    suspend fun restockProduct(productId: String, additionalStock: Int): Result<Boolean> {
        return try {
            val product = getProductById(productId)
            if (product != null) {
                val newStock = product.stock + additionalStock
                updateProductStock(productId, newStock)
            } else {
                Result.failure(Exception("Product not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Test function to manually deduct stock
    suspend fun manuallyDeductStock(productId: String, quantity: Int): Result<Boolean> {
        return try {
            val product = getProductById(productId)
            if (product == null) {
                return Result.failure(Exception("Product not found"))
            }

            println("🧪 DEBUG: Manual stock deduction for ${product.productName}")
            println("📊 DEBUG: Current stock: ${product.stock}, Deducting: $quantity")

            if (product.stock < quantity) {
                return Result.failure(Exception("Insufficient stock. Available: ${product.stock}, Requested: $quantity"))
            }

            val newStock = product.stock - quantity
            val result = updateProductStock(productId, newStock)

            println("✅ DEBUG: Stock updated to: $newStock")
            result
        } catch (e: Exception) {
            println("❌ DEBUG: Error in manual deduction: ${e.message}")
            Result.failure(e)
        }
    }
}