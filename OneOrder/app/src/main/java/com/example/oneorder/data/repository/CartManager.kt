package com.example.oneorder.data.repository

import com.example.oneorder.data.model.CartItem
import com.example.oneorder.data.model.MenuItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CartManager @Inject constructor() {
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

    fun addToCart(item: MenuItem, quantity: Int = 1) {
        _cartItems.update { currentItems ->
            val existingItem = currentItems.find { it.menuItem.id == item.id }
            if (existingItem != null) {
                currentItems.map {
                    if (it.menuItem.id == item.id) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                currentItems + CartItem(item, quantity)
            }
        }
    }

    fun removeFromCart(itemId: Long) {
        _cartItems.update { currentItems ->
            currentItems.filter { it.menuItem.id != itemId }
        }
    }
    
    fun updateQuantity(itemId: Long, quantity: Int) {
        if (quantity <= 0) {
            removeFromCart(itemId)
            return
        }
        _cartItems.update { currentItems ->
            currentItems.map {
                if (it.menuItem.id == itemId) it.copy(quantity = quantity) else it
            }
        }
    }

    fun clearCart() {
        _cartItems.value = emptyList()
    }

    fun getTotalPrice(): Double {
        return _cartItems.value.sumOf { it.totalPrice }
    }
}
