package com.example.oneorder.home

import com.example.oneorder.data.model.Category
import com.example.oneorder.data.model.MenuItem
import com.example.oneorder.data.repository.CartManager
import com.example.oneorder.data.repository.MenuRepository
import com.example.oneorder.data.repository.RestaurantRepository
import com.example.oneorder.data.repository.RestaurantStateManager
import com.example.oneorder.data.repository.TableRepository
import com.example.oneorder.ui.screens.home.HomeViewModel
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val menuRepository: MenuRepository = mockk()
    private val restaurantRepository: RestaurantRepository = mockk()
    private val tableRepository: TableRepository = mockk()
    private val cartManager: CartManager = mockk(relaxed = true)
    private val restaurantStateManager: RestaurantStateManager = mockk(relaxed = true)

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock state manager default values
        io.mockk.every { restaurantStateManager.restaurant.value } returns null
        io.mockk.every { restaurantStateManager.table.value } returns null

        coEvery { menuRepository.getCategories() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): HomeViewModel {
        return HomeViewModel(
            menuRepository,
            restaurantRepository,
            tableRepository,
            cartManager,
            restaurantStateManager
        )
    }

    @Test
    fun `initial state is empty when no restaurant is saved`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.restaurant)
        assertNull(state.table)
        assertTrue(state.categories.isEmpty())
    }

    @Test
    fun `loadData fetches categories and items when restaurant is saved`() = runTest(testDispatcher) {
        val mockRestaurant = com.example.oneorder.data.model.Restaurant("r1", "Res 1", "Address 1", "image")
        val mockTable = com.example.oneorder.data.model.TableInfo(id = 1L, name = "Bàn 1", status = "free")
        
        io.mockk.every { restaurantStateManager.restaurant.value } returns mockRestaurant
        io.mockk.every { restaurantStateManager.table.value } returns mockTable

        val mockCategories = listOf(Category(1L, "Món chính", null, true))
        val mockItems = listOf(MenuItem(id = 1L, category_id = 1L, name = "Phở", price = 50000.0, is_available = true))

        coEvery { menuRepository.getCategories() } returns Result.success(mockCategories)
        coEvery { menuRepository.getMenuItemsByCategory(1L) } returns Result.success(mockItems)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mockRestaurant, state.restaurant)
        assertEquals(mockTable, state.table)
        assertEquals(1, state.categories.size)
        assertEquals(1, state.categoryWithItems.size)
        assertEquals("Phở", state.categoryWithItems[0].items[0].name)
        assertFalse(state.isLoading)
    }

    @Test
    fun `setRestaurantAndTable fetches info and calls loadData`() = runTest(testDispatcher) {
        val mockRestaurant = com.example.oneorder.data.model.Restaurant("r1", "Res 1", "Address 1", "image")
        val mockTable = com.example.oneorder.data.model.TableInfo(id = 1L, name = "Bàn 1", status = "free")

        coEvery { restaurantRepository.getRestaurantById("r1") } returns Result.success(mockRestaurant)
        coEvery { tableRepository.getTableById(1L) } returns Result.success(mockTable)
        coEvery { menuRepository.getCategories() } returns Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setRestaurantAndTable("r1", 1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mockRestaurant, state.restaurant)
        assertEquals(mockTable, state.table)
        
        verify { restaurantStateManager.setRestaurantAndTable(mockRestaurant, mockTable) }
    }

    @Test
    fun `addToCart delegates to cartManager`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        val item = MenuItem(id = 1L, category_id = 1L, name = "Phở", price = 50000.0, is_available = true)
        
        viewModel.addToCart(item, 2)
        
        verify { cartManager.addToCart(item, 2) }
    }
}

