package com.example.oneorder.ui.screens.menu

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder.data.model.MenuItem
import com.example.oneorder.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuState(
    val isLoading: Boolean = false,
    val items: List<MenuItem> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class MenuViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val cartManager: com.example.oneorder.data.repository.CartManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuState())
    val uiState: StateFlow<MenuState> = _uiState.asStateFlow()
    
    fun loadItems(categoryId: Long) {
        viewModelScope.launch {
            Log.d("MenuViewModel", "=== LOADING MENU ITEMS ===")
            Log.d("MenuViewModel", "Category ID: $categoryId")
            
            _uiState.value = MenuState(isLoading = true)
            
            try {
                val result = menuRepository.getMenuItems(categoryId)
                
                if (result.isSuccess) {
                    val items = result.getOrDefault(emptyList())
                    Log.d("MenuViewModel", "=== LOAD SUCCESS ===")
                    Log.d("MenuViewModel", "Items loaded: ${items.size}")
                    items.forEachIndexed { index, item ->
                        Log.d("MenuViewModel", "  [$index] ${item.name} - Category: ${item.category_id}")
                    }
                    
                    _uiState.value = MenuState(
                        items = items,
                        isLoading = false
                    )
                } else {
                    val error = result.exceptionOrNull()
                    Log.e("MenuViewModel", "=== LOAD FAILED ===")
                    Log.e("MenuViewModel", "Error message: ${error?.message}")
                    Log.e("MenuViewModel", "Exception:", error)
                    
                    _uiState.value = MenuState(
                        isLoading = false,
                        error = error?.message
                    )
                }
            } catch (e: Exception) {
                Log.e("MenuViewModel", "=== UNEXPECTED ERROR ===", e)
                _uiState.value = MenuState(
                    isLoading = false,
                    error = "Unexpected error: ${e.message}"
                )
            }
        }
    }
    
    fun addToCart(item: MenuItem) {
        Log.d("MenuViewModel", "Adding to cart: ${item.name}")
        cartManager.addToCart(item)
    }
}
