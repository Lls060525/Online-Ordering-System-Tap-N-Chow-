package com.example.miniproject.model

// 這是一個單例對象，用於在屏幕之間暫存數據
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