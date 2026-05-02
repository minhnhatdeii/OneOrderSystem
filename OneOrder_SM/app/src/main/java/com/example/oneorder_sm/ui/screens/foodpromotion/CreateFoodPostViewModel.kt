package com.example.oneorder_sm.ui.screens.foodpromotion

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.oneorder_sm.data.model.Category
import com.example.oneorder_sm.data.model.MenuItem
import com.example.oneorder_sm.data.repository.FoodPostRepository
import com.example.oneorder_sm.data.repository.MenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateFoodPostUiState(
    val isLoading: Boolean = false,
    val categories: List<Category> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val isPosting: Boolean = false,
    val postSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateFoodPostViewModel @Inject constructor(
    private val menuRepository: MenuRepository,
    private val foodPostRepository: FoodPostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateFoodPostUiState())
    val uiState: StateFlow<CreateFoodPostUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            // Load Categories
            val catResult = menuRepository.getCategories()
            val allCategories = catResult.getOrNull() ?: emptyList()

            // Load Menu Items
            val menuResult = menuRepository.getMenuItems()
            if (menuResult.isSuccess) {
                val items = menuResult.getOrNull() ?: emptyList()
                
                _uiState.update { 
                    it.copy(
                        categories = allCategories,
                        menuItems = items, 
                        isLoading = false
                    ) 
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = menuResult.exceptionOrNull()?.message) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun createPost(
        menuItemId: Long,
        caption: String,
        tags: List<String>,
        imageBytesList: List<ByteArray>
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPosting = true, error = null) }
            
            // Upload images first
            val imageUrls = mutableListOf<String>()
            for (i in imageBytesList.indices) {
                val bytes = imageBytesList[i]
                val fileName = "post_${System.currentTimeMillis()}_$i.jpg"
                val uploadResult = foodPostRepository.uploadPostImage(bytes, fileName)
                if (uploadResult.isSuccess) {
                    imageUrls.add(uploadResult.getOrNull()!!)
                } else {
                    _uiState.update { 
                        it.copy(
                            isPosting = false, 
                            error = "Lỗi tải ảnh lên: ${uploadResult.exceptionOrNull()?.message}"
                        )
                    }
                    return@launch
                }
            }
            
            // Create post record
            val postResult = foodPostRepository.createFoodPost(
                menuItemId = menuItemId,
                caption = caption,
                tags = tags,
                imageUrls = imageUrls
            )
            
            postResult.onSuccess {
                _uiState.update { it.copy(isPosting = false, postSuccess = true) }
            }.onFailure { e ->
                _uiState.update { it.copy(isPosting = false, error = e.message) }
            }
        }
    }
}
