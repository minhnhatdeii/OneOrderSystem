package com.example.oneorder.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data class Login(val message: String? = null) : Screen

    @Serializable
    data object Register : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data class Menu(val categoryId: Long) : Screen

    @Serializable
    data object Cart : Screen

    @Serializable
    data object Checkout : Screen

    @Serializable
    data object OrderHistory : Screen

    @Serializable
    data object Profile : Screen

    @Serializable
    data object QRScanner : Screen
    
    @Serializable
    data class OrderDetail(val orderId: String) : Screen
}
