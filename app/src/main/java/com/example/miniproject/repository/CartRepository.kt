package com.example.miniproject.repository

import com.example.miniproject.model.Cart
import com.example.miniproject.model.CartItem

object CartRepository {
    // Stores the ID of the vendor currently owning the cart
    private var activeVendorId: String? = null

    // Stores the actual list of items
    private var activeCartItems: List<CartItem> = emptyList()

    // Get items, only return if the vendor matches the active one
    fun getCartItems(vendorId: String): List<CartItem> {
        return if (activeVendorId == vendorId) {
            activeCartItems
        } else {
            emptyList()
        }
    }

    // Save items,call this whenever the user adds/removes items
    fun saveCartItems(vendorId: String, items: List<CartItem>) {
        activeVendorId = vendorId
        activeCartItems = items
    }

    // Call this after successful checkout
    fun clearCart() {
        activeVendorId = null
        activeCartItems = emptyList()
    }

    // Existing Checkout Logic
    private var checkoutCart: Cart? = null
    fun setCart(cart: Cart) {
        checkoutCart = cart
    }
    fun getCart(): Cart? = checkoutCart
}