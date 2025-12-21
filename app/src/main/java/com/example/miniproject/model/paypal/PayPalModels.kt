// [file name]: PayPalModels.kt
package com.example.miniproject.model.paypal

import com.example.miniproject.config.PayPalConfig
import kotlinx.serialization.Serializable

@Serializable
data class PayPalAuthResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Int
)

@Serializable
data class PayPalCreateOrderRequest(
    val intent: String = PayPalConfig.INTENT,
    val purchase_units: List<PurchaseUnit>,
    val application_context: ApplicationContext = ApplicationContext()
)

@Serializable
data class PurchaseUnit(
    val amount: Amount,
    val description: String = "Food Order",
    val custom_id: String = "",
    val invoice_id: String = "",
    val payee: Payee? = null
)

@Serializable
data class Payee(
    val email_address: String? = null,
    val merchant_id: String? = null
)

@Serializable
data class Amount(
    val currency_code: String = PayPalConfig.CURRENCY,
    val value: String
)

@Serializable
data class ApplicationContext(
    val return_url: String = PayPalConfig.RETURN_URL,
    val cancel_url: String = "${PayPalConfig.RETURN_URL}/cancel",
    val brand_name: String = "Tap N Chow",
    val shipping_preference: String = PayPalConfig.SHIPPING_PREFERENCE,
    val user_action: String = PayPalConfig.USER_ACTION
)

@Serializable
data class PayPalCreateOrderResponse(
    val id: String,
    val status: String,
    val links: List<Link>
)

@Serializable
data class Link(
    val href: String,
    val rel: String,
    val method: String
)

@Serializable
data class PayPalCaptureOrderRequest(
    val payment_source: PaymentSource? = null
)

@Serializable
data class PaymentSource(
    val paypal: Paypal? = null
)

@Serializable
data class Paypal(
    val experience_context: ExperienceContext? = null
)

@Serializable
data class ExperienceContext(
    val payment_method_preference: String = "IMMEDIATE_PAYMENT_REQUIRED",
    val brand_name: String = "Tap N Chow",
    val locale: String = "en-US",
    val landing_page: String = "LOGIN",
    val shipping_preference: String = PayPalConfig.SHIPPING_PREFERENCE,
    val user_action: String = PayPalConfig.USER_ACTION,
    val return_url: String = PayPalConfig.RETURN_URL,
    val cancel_url: String = PayPalConfig.RETURN_URL
)

@Serializable
data class PayPalCaptureOrderResponse(
    val id: String,
    val status: String,
    val purchase_units: List<PurchaseUnitResponse>,
    val payer: Payer?,
    val links: List<Link>
)

@Serializable
data class PurchaseUnitResponse(
    val reference_id: String? = null,
    val amount: Amount,
    val payments: Payments?,
    val custom_id: String? = null,  // Add this field
    val invoice_id: String? = null   // Add this field
)

@Serializable
data class Payments(
    val captures: List<Capture>
)

@Serializable
data class Capture(
    val id: String,
    val status: String,
    val amount: Amount,
    val final_capture: Boolean = true,
    val seller_protection: SellerProtection?,
    val links: List<Link>,
    val create_time: String,
    val update_time: String,
    val custom_id: String? = null    // Add this field
)

@Serializable
data class SellerProtection(
    val status: String,
    val dispute_categories: List<String>
)

@Serializable
data class Payer(
    val name: PayerName,
    val email_address: String,
    val payer_id: String,
    val address: PayerAddress
)

@Serializable
data class PayerName(
    val given_name: String,
    val surname: String
)

@Serializable
data class PayerAddress(
    val country_code: String
)

@Serializable
data class PayPalErrorResponse(
    val name: String,
    val message: String,
    val debug_id: String,
    val details: List<ErrorDetail>? = null,
    val links: List<Link>? = null
)

@Serializable
data class ErrorDetail(
    val field: String? = null,
    val value: String? = null,
    val location: String? = null,
    val issue: String,
    val description: String? = null
)

@Serializable
data class PayPalRefundResponse(
    val id: String,
    val status: String
)