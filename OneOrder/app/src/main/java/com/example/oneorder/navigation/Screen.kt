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

    @Serializable
    data object FoodFeed : Screen

    @Serializable
    data object OrderMode : Screen  // Entry point to Home/Cart/Orders

    @Serializable
    data class RestaurantProfile(val restaurantId: String) : Screen

    @Serializable
    data class RestaurantPostDetail(val tenantId: String, val postId: String) : Screen

    @Serializable
    data object Following : Screen

    // MFA Security Routes
    @Serializable
    data class MfaVerification(val email: String) : Screen

    @Serializable
    data class MfaBackupCode(val email: String) : Screen

    @Serializable
    data object MfaSetup : Screen

    @Serializable
    data object SecuritySettings : Screen

    @Serializable
    data object ForgotPassword : Screen

    @Serializable
    data object ChangePassword : Screen

    @Serializable
    data class PasswordReset(val token: String? = null, val email: String? = null) : Screen
}
