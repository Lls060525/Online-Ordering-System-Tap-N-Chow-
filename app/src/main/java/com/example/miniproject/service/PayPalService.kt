// [file name]: PayPalService.kt
package com.example.miniproject.service

import com.example.miniproject.config.PayPalConfig
import com.example.miniproject.model.paypal.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit


class PayPalService {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val gson = Gson()

    private suspend fun getAccessToken(): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val credentials = Credentials.basic(PayPalConfig.CLIENT_ID, PayPalConfig.SECRET_KEY)

            val formBody = "grant_type=client_credentials"
                .toRequestBody("application/x-www-form-urlencoded".toMediaType())

            val request = Request.Builder()
                .url("${PayPalConfig.SANDBOX_BASE_URL}/v1/oauth2/token")
                .header("Authorization", credentials)
                .header("Accept", "application/json")
                .header("Accept-Language", "en_US")
                .post(formBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val authResponse = gson.fromJson(responseBody, PayPalAuthResponse::class.java)
                Result.success(authResponse.access_token)
            } else {
                Result.failure(Exception("Failed to get access token: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createOrder(
        amount: Double,
        orderId: String,
        description: String = "Food Order",
        vendorPayPalEmail: String? = null
    ): Result<PayPalCreateOrderResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val tokenResult = getAccessToken()
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
            }

            val accessToken = tokenResult.getOrThrow()

            val payeeData = if (!vendorPayPalEmail.isNullOrEmpty()) {
                Payee(email_address = vendorPayPalEmail)
            } else {
                null
            }

            val createOrderRequest = PayPalCreateOrderRequest(
                purchase_units = listOf(
                    PurchaseUnit(
                        amount = Amount(value = "%.2f".format(amount)),
                        description = description,
                        custom_id = orderId,
                        invoice_id = orderId,
                        payee = payeeData
                    )
                )
            )

            val requestBody = gson.toJson(createOrderRequest)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${PayPalConfig.SANDBOX_BASE_URL}/v2/checkout/orders")
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .header("PayPal-Request-Id", orderId)
                .header("Prefer", "return=representation")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val orderResponse = gson.fromJson(responseBody, PayPalCreateOrderResponse::class.java)
                Result.success(orderResponse)
            } else {
                val errorBody = response.body?.string()
                val errorResponse = try {
                    gson.fromJson(errorBody, PayPalErrorResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                val errorMessage = errorResponse?.message ?: "Failed to create order: ${response.code}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun captureOrder(orderId: String): Result<PayPalCaptureOrderResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val tokenResult = getAccessToken()
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
            }

            val accessToken = tokenResult.getOrThrow()

            val captureRequest = PayPalCaptureOrderRequest()
            val requestBody = gson.toJson(captureRequest)
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url("${PayPalConfig.SANDBOX_BASE_URL}/v2/checkout/orders/$orderId/capture")
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .header("Prefer", "return=representation")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val captureResponse = gson.fromJson(responseBody, PayPalCaptureOrderResponse::class.java)
                Result.success(captureResponse)
            } else {
                val errorBody = response.body?.string()
                val errorResponse = try {
                    gson.fromJson(errorBody, PayPalErrorResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                val errorMessage = errorResponse?.message ?: "Failed to capture order: ${response.code}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getOrderDetails(orderId: String): Result<PayPalCaptureOrderResponse> = withContext(Dispatchers.IO) {
        return@withContext try {
            val tokenResult = getAccessToken()
            if (tokenResult.isFailure) {
                return@withContext Result.failure(tokenResult.exceptionOrNull()!!)
            }

            val accessToken = tokenResult.getOrThrow()

            val request = Request.Builder()
                .url("${PayPalConfig.SANDBOX_BASE_URL}/v2/checkout/orders/$orderId")
                .header("Authorization", "Bearer $accessToken")
                .header("Content-Type", "application/json")
                .build()

            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                val orderResponse = gson.fromJson(responseBody, PayPalCaptureOrderResponse::class.java)
                Result.success(orderResponse)
            } else {
                val errorBody = response.body?.string()
                val errorResponse = try {
                    gson.fromJson(errorBody, PayPalErrorResponse::class.java)
                } catch (e: Exception) {
                    null
                }

                val errorMessage = errorResponse?.message ?: "Failed to get order details: ${response.code}"
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}