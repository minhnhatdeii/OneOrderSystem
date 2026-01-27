package com.example.oneorder.ui.screens.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.CartItem
import com.example.oneorder.data.repository.CartManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class CartUiState(
    val items: List<CartItem> = emptyList(),
    val total: Double = 0.0
)

@HiltViewModel
class CartViewModel @Inject constructor(
    private val cartManager: CartManager
) : ViewModel() {

    val uiState: StateFlow<CartUiState> = cartManager.cartItems.map { items ->
        CartUiState(
            items = items,
            total = items.sumOf { it.totalPrice }
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CartUiState()
    )

    fun increaseQuantity(itemId: Long) {
        val item = uiState.value.items.find { it.menuItem.id == itemId } ?: return
        cartManager.updateQuantity(itemId, item.quantity + 1)
    }

    fun decreaseQuantity(itemId: Long) {
        val item = uiState.value.items.find { it.menuItem.id == itemId } ?: return
        cartManager.updateQuantity(itemId, item.quantity - 1)
    }
    
    fun removeItem(itemId: Long) {
        cartManager.removeFromCart(itemId)
    }
}
