package com.example.oneorder_sm.menu

import com.example.oneorder_sm.data.model.Category
import com.example.oneorder_sm.data.model.MenuItem
import com.example.oneorder_sm.domain.usecase.*
import com.example.oneorder_sm.ui.screens.menu.MenuManagementViewModel
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MenuManagementViewModelTest {

    private val getCategoriesUseCase: GetCategoriesUseCase = mockk()
    private val getMenuItemsUseCase: GetMenuItemsUseCase = mockk()
    private val addMenuItemUseCase: AddMenuItemUseCase = mockk()
    private val updateMenuItemUseCase: UpdateMenuItemUseCase = mockk()
    private val deleteMenuItemUseCase: DeleteMenuItemUseCase = mockk()
    private val toggleItemAvailabilityUseCase: ToggleItemAvailabilityUseCase = mockk()
    private val addCategoryUseCase: AddCategoryUseCase = mockk()
    private val updateCategoryUseCase: UpdateCategoryUseCase = mockk()
    private val deleteCategoryUseCase: DeleteCategoryUseCase = mockk()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Default mock behaviors for initialization
        coEvery { getCategoriesUseCase() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MenuManagementViewModel {
        return MenuManagementViewModel(
            getCategoriesUseCase,
            getMenuItemsUseCase,
            addMenuItemUseCase,
            updateMenuItemUseCase,
            deleteMenuItemUseCase,
            toggleItemAvailabilityUseCase,
            addCategoryUseCase,
            updateCategoryUseCase,
            deleteCategoryUseCase
        )
    }

    @Test
    fun `loadData sets categories and selects first category if none selected`() =
        runTest(testDispatcher) {
            val mockCategories = listOf(Category(id = 1L, name = "Đồ ăn", isActive = true))
            val mockItems = listOf(
                MenuItem(
                    id = 1L,
                    categoryId = 1L,
                    name = "Cơm bò",
                    price = 55000.0,
                    isAvailable = true
                )
            )

            coEvery { getCategoriesUseCase() } returns Result.success(mockCategories)
            coEvery { getMenuItemsUseCase(1L) } returns Result.success(mockItems)

            val viewModel = createViewModel()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals(mockCategories, state.categories)
            assertEquals(mockCategories[0], state.selectedCategory)
            assertEquals(mockItems, state.menuItems)
            assertFalse(state.isLoading)
            assertNull(state.error)
        }

    @Test
    fun `selectCategory loads items for the selected category`() = runTest(testDispatcher) {
        val category1 = Category(id = 1L, name = "Đồ ăn", isActive = true)
        val category2 = Category(id = 2L, name = "Đồ uống", isActive = true)
        val itemsCat2 = listOf(
            MenuItem(
                id = 2L,
                categoryId = 2L,
                name = "Trà sữa",
                price = 30000.0,
                isAvailable = true
            )
        )

        coEvery { getCategoriesUseCase() } returns Result.success(listOf(category1, category2))
        coEvery { getMenuItemsUseCase(1L) } returns Result.success(emptyList())
        coEvery { getMenuItemsUseCase(2L) } returns Result.success(itemsCat2)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Initially selects category 1. Now select category 2
        viewModel.selectCategory(category2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(category2, state.selectedCategory)
        assertEquals(itemsCat2, state.menuItems)
    }

    @Test
    fun `saveMenuItem adds new item when id is null`() = runTest(testDispatcher) {
        val category = Category(id = 1L, name = "Đồ ăn", isActive = true)
        coEvery { getCategoriesUseCase() } returns Result.success(listOf(category))
        coEvery { getMenuItemsUseCase(1L) } returns Result.success(emptyList())
        coEvery { addMenuItemUseCase(any(), any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveMenuItem(
            id = null,
            name = "Món mới",
            price = 40000.0,
            description = "Ngon",
            categoryId = 1L,
            imageData = null,
            currentImageUrl = null
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Món đã được thêm", state.successMessage)
    }

    @Test
    fun `toggleItemAvailability updates state locally on success`() = runTest(testDispatcher) {
        val category = Category(id = 1L, name = "Đồ ăn", isActive = true)
        val item =
            MenuItem(id = 1L, categoryId = 1L, name = "Phở", price = 50000.0, isAvailable = true)

        coEvery { getCategoriesUseCase() } returns Result.success(listOf(category))
        coEvery { getMenuItemsUseCase(1L) } returns Result.success(listOf(item))
        coEvery { toggleItemAvailabilityUseCase(1L, false) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleItemAvailability(1L, currentAvailability = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.menuItems.size)
        assertFalse(state.menuItems[0].isAvailable)
    }

    @Test
    fun `saveCategory adds new category when id is null`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns Result.success(emptyList())
        coEvery { addCategoryUseCase(any(), any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveCategory(id = null, name = "Danh mục mới", isActive = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Danh mục đã được thêm", state.successMessage)
    }

    @Test
    fun `saveMenuItem updates item when id is not null`() = runTest(testDispatcher) {
        val category = Category(id = 1L, name = "Đồ ăn", isActive = true)
        coEvery { getCategoriesUseCase() } returns Result.success(listOf(category))
        coEvery { getMenuItemsUseCase(1L) } returns Result.success(emptyList())
        coEvery { updateMenuItemUseCase(any(), any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveMenuItem(
            id = 1L,
            name = "Món cập nhật",
            price = 45000.0,
            description = "Ngon",
            categoryId = 1L,
            imageData = null,
            currentImageUrl = "http://example.com/image.jpg"
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Món đã được cập nhật", state.successMessage)
    }

    @Test
    fun `deleteMenuItem removes item on success`() = runTest(testDispatcher) {
        val category = Category(id = 1L, name = "Đồ ăn", isActive = true)
        val item =
            MenuItem(id = 1L, categoryId = 1L, name = "Cơm bò", price = 55000.0, isAvailable = true)

        coEvery { getCategoriesUseCase() } returns Result.success(listOf(category))
        coEvery { getMenuItemsUseCase(1L) } returns Result.success(listOf(item))
        coEvery { deleteMenuItemUseCase(1L) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // Mock the second call to return empty after delete
        coEvery { getMenuItemsUseCase(1L) } returns Result.success(emptyList())

        viewModel.deleteMenuItem(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Món đã được xóa", state.successMessage)
        assertEquals(emptyList<MenuItem>(), state.menuItems)
    }

    @Test
    fun `saveCategory updates category when id is not null`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns Result.success(emptyList())
        coEvery { updateCategoryUseCase(any(), any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveCategory(id = 1L, name = "Danh mục cập nhật", isActive = true)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Danh mục đã được cập nhật", state.successMessage)
    }

    @Test
    fun `deleteCategory removes category on success`() = runTest(testDispatcher) {
        coEvery { getCategoriesUseCase() } returns Result.success(emptyList())
        coEvery { deleteCategoryUseCase(1L) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteCategory(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Danh mục đã được xóa", state.successMessage)
    }
}


