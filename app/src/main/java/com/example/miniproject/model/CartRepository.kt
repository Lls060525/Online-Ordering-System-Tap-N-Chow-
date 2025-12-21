package com.example.miniproject.model

object CartRepository {
    // Stores the ID of the vendor currently owning the cart
    private var activeVendorId: String? = null

    // Stores the actual list of items
    private var activeCartItems: List<CartItem> = emptyList()

    // 1. Get items: Only return if the vendor matches the active one
    fun getCartItems(vendorId: String): List<CartItem> {
        return if (activeVendorId == vendorId) {
            activeCartItems
        } else {
            emptyList()
        }
    }

    // 2. Save items: Call this whenever the user adds/removes items
    fun saveCartItems(vendorId: String, items: List<CartItem>) {
        activeVendorId = vendorId
        activeCartItems = items
    }

    // 3. Clear: Call this after successful checkout
    fun clearCart() {
        activeVendorId = null
        activeCartItems = emptyList()
    }

    // --- Existing Checkout Logic ---
    private var checkoutCart: Cart? = null
    fun setCart(cart: Cart) {
        checkoutCart = cart
    }
    fun getCart(): Cart? = checkoutCart
}