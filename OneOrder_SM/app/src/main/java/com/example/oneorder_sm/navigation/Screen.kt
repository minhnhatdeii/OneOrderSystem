package com.example.oneorder_sm.navigation

import kotlinx.serialization.Serializable

sealed interface Screen {
    @Serializable
    data object Welcome : Screen
    
    @Serializable
    data object RestaurantLogin : Screen  // Login for managers
    
    @Serializable
    data object StaffLogin : Screen  // Login for staff
    
    @Serializable
    data object Register : Screen  // Register new restaurant

    @Serializable
    data object Main : Screen  // Main screen with sidebar
    
    @Serializable
    data object Dashboard : Screen
    
    @Serializable
    data object OrderList : Screen
    
    @Serializable
    data class OrderDetail(val orderId: String) : Screen
    
    @Serializable
    data object MenuManagement : Screen
    
    @Serializable
    data object TableManagement : Screen
    
    @Serializable
    data object StaffManagement : Screen
    
    @Serializable
    data object RestaurantSettings : Screen
    
    @Serializable
    data object Profile : Screen
    
    // Password recovery routes
    @Serializable
    data object ForgotPassword : Screen

    @Serializable
    data class PasswordReset(val token: String? = null, val email: String? = null) : Screen

    @Serializable
    data object RestaurantFeed : Screen
}
