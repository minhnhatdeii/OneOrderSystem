package com.example.oneorder_sm.domain.usecase

import com.example.oneorder_sm.data.model.Category
import com.example.oneorder_sm.data.model.MenuItem
import com.example.oneorder_sm.data.repository.MenuRepository
import javax.inject.Inject

/**
 * Use case to get all categories
 */
class GetCategoriesUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(): Result<List<Category>> {
        return menuRepository.getCategories()
    }
}

/**
 * Use case to get menu items, optionally filtered by category
 */
class GetMenuItemsUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(categoryId: Long? = null): Result<List<MenuItem>> {
        return menuRepository.getMenuItems(categoryId)
    }
}

/**
 * Use case to add a new menu item with optional image
 */
class AddMenuItemUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(
        item: MenuItem,
        imageBytes: ByteArray? = null
    ): Result<Unit> {
        // Upload image first if provided
        val imageUrl = if (imageBytes != null) {
            val fileName = "item_${System.currentTimeMillis()}.jpg"
            val uploadResult = menuRepository.uploadImage(imageBytes, fileName)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Image upload failed"))
            }
            uploadResult.getOrNull()
        } else {
            item.imageUrl
        }

        // Create item with image URL
        val itemWithImage = item.copy(imageUrl = imageUrl)
        return menuRepository.createMenuItem(itemWithImage)
    }
}

/**
 * Use case to update a menu item with optional new image
 */
class UpdateMenuItemUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(
        item: MenuItem,
        imageBytes: ByteArray? = null
    ): Result<Unit> {
        // Upload new image if provided
        val imageUrl = if (imageBytes != null) {
            val fileName = "item_${item.id}_${System.currentTimeMillis()}.jpg"
            val uploadResult = menuRepository.uploadImage(imageBytes, fileName)
            if (uploadResult.isFailure) {
                return Result.failure(uploadResult.exceptionOrNull() ?: Exception("Image upload failed"))
            }
            uploadResult.getOrNull()
        } else {
            item.imageUrl
        }

        // Update item with image URL
        val itemWithImage = item.copy(imageUrl = imageUrl)
        return menuRepository.updateMenuItem(itemWithImage)
    }
}

/**
 * Use case to delete a menu item
 */
class DeleteMenuItemUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return menuRepository.deleteMenuItem(id)
    }
}

/**
 * Use case to toggle menu item availability
 */
class ToggleItemAvailabilityUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(id: Long, isAvailable: Boolean): Result<Unit> {
        return menuRepository.toggleItemAvailability(id, isAvailable)
    }
}

/**
 * Use case to add a new category
 */
class AddCategoryUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(
        category: Category,
        imageBytes: ByteArray? = null
    ): Result<Unit> {
        return menuRepository.addCategory(category, imageBytes)
    }
}

/**
 * Use case to update a category
 */
class UpdateCategoryUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(
        category: Category,
        imageBytes: ByteArray? = null
    ): Result<Unit> {
        return menuRepository.updateCategory(category, imageBytes)
    }
}

/**
 * Use case to delete a category
 */
class DeleteCategoryUseCase @Inject constructor(
    private val menuRepository: MenuRepository
) {
    suspend operator fun invoke(id: Long): Result<Unit> {
        return menuRepository.deleteCategory(id)
    }
}
