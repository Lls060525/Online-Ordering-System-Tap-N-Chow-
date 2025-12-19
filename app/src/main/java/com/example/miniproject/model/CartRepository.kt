package com.example.miniproject.model

// This is a singleton object used to temporarily store data between screens.
object CartRepository {
    private var currentCart: Cart? = null

    fun setCart(cart: Cart) {
        currentCart = cart
    }

    fun getCart(): Cart? {
        return currentCart
    }

    fun clear() {
        currentCart = null
    }
}