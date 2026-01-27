package com.example.oneorder.data.model

@kotlinx.serialization.Serializable
data class CartItem(
    val menuItem: MenuItem,
    val quantity: Int,
    val note: String = ""
) {
    val totalPrice: Double
        get() = menuItem.price * quantity
}
