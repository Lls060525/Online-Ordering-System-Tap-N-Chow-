package com.example.miniproject.paypal

object PayPalConfig {
    // Sandbox credentials
    const val CLIENT_ID = "AechXTGxwk5R7hdCGuEKzM4K5BiJj7IACb_37q66zuZT1xTRb_4dkQAylyWqXflBb2EDnVyP7bSexcYM"
    const val SECRET_KEY = "EJc2zG0x5WY5TewNT8lCIjNgIOXPdShLTJr8qhyaO2ropYGBJ4VBGrg1PA2CjYnAQkejrQ6i4fdtFSsB"

    // Sandbox URLs
    const val SANDBOX_BASE_URL = "https://api-m.sandbox.paypal.com"
    const val SANDBOX_AUTH_URL = "https://www.sandbox.paypal.com"

    // Return URL for deep linking
    const val RETURN_URL = "com.example.miniproject.paypeltest://demoapp"

    // Currency
    const val CURRENCY = "MYR"

    // Intent (AUTHORIZE or CAPTURE)
    const val INTENT = "CAPTURE" // Use CAPTURE for immediate payment

    // Shipping preference
    const val SHIPPING_PREFERENCE = "NO_SHIPPING" // Since it's pickup

    // User action (PAY_NOW for immediate payment)
    const val USER_ACTION = "PAY_NOW"
}