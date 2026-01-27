package com.example.oneorder.ui.screens.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.Category
import com.example.oneorder.data.model.MenuItem
import com.example.oneorder.data.model.Restaurant
import com.example.oneorder.data.model.TableInfo
import com.example.oneorder.data.repository.MenuRepository
import com.example.oneorder.data.repository.RestaurantRepository
import com.example.oneorder.data.repository.TableRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CategoryWithItems(
    val category: Category,
    val items: List<MenuItem>
)

data class HomeState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val categoryWithItems: List<CategoryWithItems> = emptyList(), // All categories with their items
    val error: String? = null,
    val restaurant: Restaurant? = null,
    val table: TableInfo? = null
)


@HiltViewModel
class HomeViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val restaurantRepository: RestaurantRepository,
    private val tableRepository: TableRepository,
    private val cartManager: com.example.oneorder.data.repository.CartManager,
    private val restaurantStateManager: com.example.oneorder.data.repository.RestaurantStateManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeState())
    val uiState: StateFlow<HomeState> = _uiState.asStateFlow()

    init {
        Log.d("HomeViewModel", "=== HOME VIEW MODEL INITIALIZED ===" )
        Log.d("HomeViewModel", "Instance: ${this.hashCode()}")
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            Log.d("HomeViewModel", "=== LOADING HOME DATA ===")
            
            // Check if we have restaurant info in state manager (from previous session)
            val savedRestaurant = restaurantStateManager.restaurant.value
            val savedTable = restaurantStateManager.table.value
            
            if (savedRestaurant != null && savedTable != null) {
                Log.d("HomeViewModel", "Found saved restaurant: ${savedRestaurant.name}")
                
                // Restore from state manager
                _uiState.value = _uiState.value.copy(
                    restaurant = savedRestaurant,
                    table = savedTable
                )
                
                // Continue to load menu data
            } else {
                Log.d("HomeViewModel", "No saved restaurant, showing empty state")
                _uiState.value = HomeState(isLoading = false)
                return@launch
            }
            
            // Set loading state while preserving restaurant and table
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Fetch Categories
                Log.d("HomeViewModel", "Fetching categories...")
                val categoriesResult = menuRepository.getCategories()
                
                if (categoriesResult.isFailure) {
                    Log.e("HomeViewModel", "Failed to load categories", categoriesResult.exceptionOrNull())
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = categoriesResult.exceptionOrNull()?.message ?: "Failed to load categories"
                    )
                    return@launch
                }
                
                val categories = categoriesResult.getOrDefault(emptyList())
                Log.d("HomeViewModel", "Categories loaded: ${categories.size}")
                
                // Load items for ALL categories
                val categoryWithItemsList = mutableListOf<CategoryWithItems>()
                
                for (category in categories) {
                    val itemsResult = menuRepository.getMenuItemsByCategory(category.id)
                    val items = itemsResult.getOrDefault(emptyList())
                    Log.d("HomeViewModel", "Category ${category.name}: ${items.size} items")
                    
                    categoryWithItemsList.add(CategoryWithItems(category, items))
                }
                
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    categoryWithItems = categoryWithItemsList,
                    isLoading = false
                )
                
                Log.d("HomeViewModel", "✅ All data loaded: ${categoryWithItemsList.size} categories with items")
                
            } catch (e: Exception) {
                Log.e("HomeViewModel", "=== UNEXPECTED ERROR ===", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}\""
                )
            }
        }
    }
    
    fun setRestaurantAndTable(restaurantId: String, tableId: Long) {
        viewModelScope.launch {
            Log.d("HomeViewModel", "=== SETTING RESTAURANT & TABLE ===")
            Log.d("HomeViewModel", "Instance: ${this@HomeViewModel.hashCode()}")
            Log.d("HomeViewModel", "=== QR CODE DATA ===")
            Log.d("HomeViewModel", "Restaurant ID: '$restaurantId'")
            Log.d("HomeViewModel", "Restaurant ID length: ${restaurantId.length}")
            Log.d("HomeViewModel", "Restaurant ID type: ${restaurantId::class.simpleName}")
            Log.d("HomeViewModel", "Restaurant ID is empty: ${restaurantId.isEmpty()}")
            Log.d("HomeViewModel", "Restaurant ID is blank: ${restaurantId.isBlank()}")
            Log.d("HomeViewModel", "Restaurant ID equals 'null': ${restaurantId == "null"}")
            Log.d("HomeViewModel", "Table ID: $tableId")
            Log.d("HomeViewModel", "Current state BEFORE: restaurant=${_uiState.value.restaurant?.name}, table=${_uiState.value.table?.name}")
            
            try {
                // Fetch restaurant info
                Log.d("HomeViewModel", "Fetching restaurant from repository...")
                val restaurantResult = restaurantRepository.getRestaurantById(restaurantId)
                Log.d("HomeViewModel", "Fetching table from repository...")
                val tableResult = tableRepository.getTableById(tableId)
                
                if (restaurantResult.isSuccess && tableResult.isSuccess) {
                    val restaurant = restaurantResult.getOrNull()
                    val table = tableResult.getOrNull()
                    
                    Log.d("HomeViewModel", "Restaurant fetched: ${restaurant?.name}")
                    Log.d("HomeViewModel", "Table fetched: ${table?.name}")
                    
                    // Save to state manager for persistence across navigation
                    if (restaurant != null && table != null) {
                        restaurantStateManager.setRestaurantAndTable(restaurant, table)
                        Log.d("HomeViewModel", "✅ Saved to RestaurantStateManager")
                    }
                    
                    _uiState.value = _uiState.value.copy(
                        restaurant = restaurant,
                        table = table
                    )
                    
                    Log.d("HomeViewModel", "State AFTER update: restaurant=${_uiState.value.restaurant?.name}, table=${_uiState.value.table?.name}")
                    
                    // Load menu data after setting restaurant
                    Log.d("HomeViewModel", "Calling loadData()...")
                    loadData()
                } else {
                    Log.e("HomeViewModel", "Failed to load restaurant or table info")
                    Log.e("HomeViewModel", "Restaurant result: ${restaurantResult.isSuccess}")
                    Log.e("HomeViewModel", "Table result: ${tableResult.isSuccess}")
                    if (restaurantResult.isFailure) {
                        Log.e("HomeViewModel", "Restaurant error: ${restaurantResult.exceptionOrNull()?.message}")
                    }
                    if (tableResult.isFailure) {
                        Log.e("HomeViewModel", "Table error: ${tableResult.exceptionOrNull()?.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading restaurant/table info", e)
            }
        }
    }
    
    fun addToCart(item: MenuItem, quantity: Int = 1) {
        cartManager.addToCart(item, quantity)
    }
}
