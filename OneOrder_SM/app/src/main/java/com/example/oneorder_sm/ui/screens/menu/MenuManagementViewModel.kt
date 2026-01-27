package com.example.oneorder_sm.ui.screens.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.Category
import com.example.oneorder_sm.data.model.MenuItem
import com.example.oneorder_sm.domain.usecase.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MenuState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val error: String? = null,
    val selectedCategory: Category? = null,
    val successMessage: String? = null,
    val categoryToDelete: Category? = null
)

@HiltViewModel
class MenuManagementViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val getMenuItemsUseCase: GetMenuItemsUseCase,
    private val addMenuItemUseCase: AddMenuItemUseCase,
    private val updateMenuItemUseCase: UpdateMenuItemUseCase,
    private val deleteMenuItemUseCase: DeleteMenuItemUseCase,
    private val toggleItemAvailabilityUseCase: ToggleItemAvailabilityUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val updateCategoryUseCase: UpdateCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MenuState())
    val uiState: StateFlow<MenuState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val catResult = getCategoriesUseCase()
            if (catResult.isSuccess) {
                val categories = catResult.getOrDefault(emptyList())
                _uiState.value = _uiState.value.copy(categories = categories)
                // Default to first category if available and none selected
                if (_uiState.value.selectedCategory == null && categories.isNotEmpty()) {
                    selectCategory(categories.first())
                } else if (_uiState.value.selectedCategory != null) {
                    // Refresh current category
                    selectCategory(_uiState.value.selectedCategory!!)
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = catResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun selectCategory(category: Category) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedCategory = category, isLoading = true, error = null)
            // Check if category has valid ID
            if (category.id == null) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Cannot select category without valid ID"
                )
                return@launch
            }
            val itemResult = getMenuItemsUseCase(category.id)
            if (itemResult.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    menuItems = itemResult.getOrDefault(emptyList()),
                    isLoading = false
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = itemResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun saveMenuItem(
        id: Long?,
        name: String,
        price: Double,
        description: String?,
        categoryId: Long,
        imageData: ByteArray?,
        currentImageUrl: String?,
        isAvailable: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)

            // Log the attempt for debugging
            android.util.Log.d("MenuViewModel", "Saving menu item: name=$name, id=$id, hasImage=${imageData != null}")

            val item = MenuItem(
                id = id, // null for new items (auto-generated), actual id for updates
                name = name,
                price = price,
                description = description,
                categoryId = categoryId,
                imageUrl = currentImageUrl,
                isAvailable = isAvailable
            )

            val result = if (id == null || id == 0L) {
                addMenuItemUseCase(item, imageData)
            } else {
                updateMenuItemUseCase(item, imageData)
            }

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = if (id == null) "Món đã được thêm" else "Món đã được cập nhật"
                )
                // Refresh
                _uiState.value.selectedCategory?.let { selectCategory(it) }
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error occurred"
                android.util.Log.e("MenuViewModel", "Error saving menu item: $errorMessage")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi khi lưu món: $errorMessage"
                )
            }
        }
    }

    fun deleteMenuItem(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = deleteMenuItemUseCase(id)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Món đã được xóa")
                // Refresh
                _uiState.value.selectedCategory?.let { selectCategory(it) }
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun toggleItemAvailability(id: Long, currentAvailability: Boolean) {
        viewModelScope.launch {
            val newAvailability = !currentAvailability
            val result = toggleItemAvailabilityUseCase(id, newAvailability)
            if (result.isSuccess) {
                // Update local state immediately for better UX
                val updatedItems = _uiState.value.menuItems.map {
                    if (it.id == id) it.copy(isAvailable = newAvailability) else it
                }
                _uiState.value = _uiState.value.copy(menuItems = updatedItems)
            } else {
                _uiState.value = _uiState.value.copy(error = result.exceptionOrNull()?.message)
            }
        }
    }

    fun saveCategory(
        id: Long? = null,
        name: String,
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null, successMessage = null)

            // Log the attempt for debugging
            android.util.Log.d("MenuViewModel", "Saving category: name=$name, id=$id")

            val category = Category(
                id = id, // null for new items (auto-generated), actual id for updates
                name = name,
                imageUrl = null, // Categories no longer use images
                isActive = isActive
            )

            val result = if (id == null || id == 0L) {
                addCategoryUseCase(category, imageBytes = null)
            } else {
                updateCategoryUseCase(category, imageBytes = null)
            }

            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(
                    successMessage = if (id == null) "Danh mục đã được thêm" else "Danh mục đã được cập nhật"
                )
                loadData()
            } else {
                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error occurred"
                android.util.Log.e("MenuViewModel", "Error saving category: $errorMessage")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Lỗi khi lưu danh mục: $errorMessage"
                )
            }
        }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = deleteCategoryUseCase(id)
            if (result.isSuccess) {
                _uiState.value = _uiState.value.copy(successMessage = "Danh mục đã được xóa")
                loadData()
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message
                )
            }
        }
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(error = null, successMessage = null)
    }

    fun showDeleteCategoryDialog(category: Category) {
        _uiState.value = _uiState.value.copy(categoryToDelete = category)
    }

    fun dismissDeleteCategoryDialog() {
        _uiState.value = _uiState.value.copy(categoryToDelete = null)
    }
}

