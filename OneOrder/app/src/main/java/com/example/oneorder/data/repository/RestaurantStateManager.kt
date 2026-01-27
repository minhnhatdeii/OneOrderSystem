package com.example.oneorder.data.repository

import com.example.oneorder.data.model.Restaurant
import com.example.oneorder.data.model.TableInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager to persist restaurant and table info across navigation
 * Data is kept until new QR scan or app closure
 */
@Singleton
class RestaurantStateManager @Inject constructor() {
    
    private val _restaurant = MutableStateFlow<Restaurant?>(null)
    val restaurant: StateFlow<Restaurant?> = _restaurant.asStateFlow()
    
    private val _table = MutableStateFlow<TableInfo?>(null)
    val table: StateFlow<TableInfo?> = _table.asStateFlow()
    
    /**
     * Set restaurant and table info (called after QR scan)
     */
    fun setRestaurantAndTable(restaurant: Restaurant, table: TableInfo) {
        _restaurant.value = restaurant
        _table.value = table
    }
    
    /**
     * Clear restaurant and table info (called on new QR scan or logout)
     */
    fun clear() {
        _restaurant.value = null
        _table.value = null
    }
    
    /**
     * Check if restaurant info is available
     */
    fun hasRestaurantInfo(): Boolean {
        return _restaurant.value != null && _table.value != null
    }
}
