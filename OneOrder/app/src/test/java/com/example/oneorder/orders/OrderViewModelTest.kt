package com.example.oneorder.orders

import com.example.oneorder.data.model.Order
import com.example.oneorder.data.model.Restaurant
import com.example.oneorder.data.repository.OrderRepository
import com.example.oneorder.data.repository.RestaurantRepository
import com.example.oneorder.ui.screens.order.OrderHistoryViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    private val orderRepository: OrderRepository = mockk()
    private val restaurantRepository: RestaurantRepository = mockk(relaxed = true)
    private val testDispatcher = StandardTestDispatcher()

    private val mockOrder1 = Order(
        id = "order001",
        userId = "user1",
        tenantId = "rest1",
        totalAmount = 100000.0,
        status = "pending",
        createdAt = "2026-05-11T10:00:00Z"
    )
    private val mockOrder2 = Order(
        id = "order002",
        userId = "user1",
        tenantId = "rest1",
        totalAmount = 250000.0,
        status = "paid",
        createdAt = "2026-05-10T08:00:00Z"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { orderRepository.getOrders() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is not loading with empty orders`() = runTest(testDispatcher) {
        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.orders.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `loadOrders fetches orders and updates state successfully`() = runTest(testDispatcher) {
        coEvery { orderRepository.getOrders() } returns Result.success(listOf(mockOrder1, mockOrder2))

        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(2, state.orders.size)
        assertEquals("order001", state.orders[0].id)
    }

    @Test
    fun `loadOrders sets error on repository failure`() = runTest(testDispatcher) {
        coEvery { orderRepository.getOrders() } returns Result.failure(Exception("Server error"))

        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.orders.isEmpty())
    }

    @Test
    fun `updateSearchQuery filters visible orders by ID`() = runTest(testDispatcher) {
        coEvery { orderRepository.getOrders() } returns Result.success(listOf(mockOrder1, mockOrder2))

        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("order001")

        val state = viewModel.uiState.value
        assertEquals(2, state.orders.size)
        assertEquals(1, state.visibleOrders.size)
        assertEquals("order001", state.visibleOrders[0].id)
    }

    @Test
    fun `updateSearchQuery with empty string shows all orders`() = runTest(testDispatcher) {
        coEvery { orderRepository.getOrders() } returns Result.success(listOf(mockOrder1, mockOrder2))

        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        viewModel.updateSearchQuery("order001")
        viewModel.updateSearchQuery("")

        val state = viewModel.uiState.value
        assertEquals(2, state.visibleOrders.size)
    }

    @Test
    fun `updateFilterStatus filters visible orders by status`() = runTest(testDispatcher) {
        coEvery { orderRepository.getOrders() } returns Result.success(listOf(mockOrder1, mockOrder2))

        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        viewModel.updateFilterStatus("paid")

        val state = viewModel.uiState.value
        assertEquals(1, state.visibleOrders.size)
        assertEquals("paid", state.visibleOrders[0].status)
    }

    @Test
    fun `updateFilterStatus with null shows all orders`() = runTest(testDispatcher) {
        coEvery { orderRepository.getOrders() } returns Result.success(listOf(mockOrder1, mockOrder2))

        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        viewModel.updateFilterStatus("pending")
        assertEquals(1, viewModel.uiState.value.visibleOrders.size)

        viewModel.updateFilterStatus(null)
        assertEquals(2, viewModel.uiState.value.visibleOrders.size)
    }

    @Test
    fun `updateFilterDate filters orders by date string`() = runTest(testDispatcher) {
        coEvery { orderRepository.getOrders() } returns Result.success(listOf(mockOrder1, mockOrder2))

        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        viewModel.updateFilterDate("2026-05-11")

        val state = viewModel.uiState.value
        // Only mockOrder1 has createdAt starting with 2026-05-11
        assertEquals(1, state.visibleOrders.size)
        assertEquals("order001", state.visibleOrders[0].id)
    }

    @Test
    fun `updateFilterDate with null removes date filter`() = runTest(testDispatcher) {
        coEvery { orderRepository.getOrders() } returns Result.success(listOf(mockOrder1, mockOrder2))

        val viewModel = OrderHistoryViewModel(orderRepository, restaurantRepository)
        advanceUntilIdle()

        viewModel.updateFilterDate("2026-05-11")
        assertEquals(1, viewModel.uiState.value.visibleOrders.size)

        viewModel.updateFilterDate(null)
        assertEquals(2, viewModel.uiState.value.visibleOrders.size)
    }
}