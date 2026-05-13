package com.example.oneorder_sm.orders

import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.data.model.PaymentStatus
import com.example.oneorder_sm.domain.usecase.GetActiveOrdersUseCase
import com.example.oneorder_sm.domain.usecase.GetOrdersPagedUseCase
import com.example.oneorder_sm.domain.usecase.SubscribeToOrdersUseCase
import com.example.oneorder_sm.domain.usecase.UpdateOrderStatusUseCase
import com.example.oneorder_sm.ui.screens.orders.OrderListViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderManagementViewModelTest {

    private val getActiveOrdersUseCase: GetActiveOrdersUseCase = mockk()
    private val subscribeToOrdersUseCase: SubscribeToOrdersUseCase = mockk()
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase = mockk()
    private val getOrdersPagedUseCase: GetOrdersPagedUseCase = mockk()

    private val testDispatcher = StandardTestDispatcher()

    private val mockOrder = Order(
        id = "order1",
        tableId = 1,
        userId = "user1",
        totalAmount = 50000.0,
        status = OrderStatus.PENDING,
        paymentStatus = PaymentStatus.UNPAID,
        createdAt = "2026-05-11T10:00:00Z",
        updatedAt = "2026-05-11T10:00:00Z",
        orderItems = emptyList(),
        tableName = "Ban 1"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getActiveOrdersUseCase() } returns Result.success(emptyList())
        coEvery { subscribeToOrdersUseCase() } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is empty and not loading`() = runTest(testDispatcher) {
        val viewModel = OrderListViewModel(
            getActiveOrdersUseCase,
            subscribeToOrdersUseCase,
            updateOrderStatusUseCase,
            getOrdersPagedUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.orders.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `fetchOrders populates orders list on success`() = runTest(testDispatcher) {
        coEvery { getActiveOrdersUseCase() } returns Result.success(listOf(mockOrder))

        val viewModel = OrderListViewModel(
            getActiveOrdersUseCase,
            subscribeToOrdersUseCase,
            updateOrderStatusUseCase,
            getOrdersPagedUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(1, state.orders.size)
        assertEquals("order1", state.orders[0].id)
        assertEquals(OrderStatus.PENDING, state.orders[0].status)
    }

    @Test
    fun `fetchOrders sets error on failure`() = runTest(testDispatcher) {
        coEvery { getActiveOrdersUseCase() } returns Result.failure(Exception("Network error"))

        val viewModel = OrderListViewModel(
            getActiveOrdersUseCase,
            subscribeToOrdersUseCase,
            updateOrderStatusUseCase,
            getOrdersPagedUseCase
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.orders.isEmpty())
    }

    @Test
    fun `setSelectedTab updates selectedTabIndex`() = runTest(testDispatcher) {
        val viewModel = OrderListViewModel(
            getActiveOrdersUseCase,
            subscribeToOrdersUseCase,
            updateOrderStatusUseCase,
            getOrdersPagedUseCase
        )
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.selectedTabIndex)

        viewModel.setSelectedTab(2)

        assertEquals(2, viewModel.uiState.value.selectedTabIndex)
    }

    @Test
    fun `updateFilterDate updates filterDateMillis`() = runTest(testDispatcher) {
        val viewModel = OrderListViewModel(
            getActiveOrdersUseCase,
            subscribeToOrdersUseCase,
            updateOrderStatusUseCase,
            getOrdersPagedUseCase
        )
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.filterDateMillis)

        val testMillis = 1715000000000L
        viewModel.updateFilterDate(testMillis)

        assertEquals(testMillis, viewModel.uiState.value.filterDateMillis)
    }

    @Test
    fun `updateFilterDate with null clears filter`() = runTest(testDispatcher) {
        val viewModel = OrderListViewModel(
            getActiveOrdersUseCase,
            subscribeToOrdersUseCase,
            updateOrderStatusUseCase,
            getOrdersPagedUseCase
        )
        advanceUntilIdle()

        viewModel.updateFilterDate(1715000000000L)
        assertNotNull(viewModel.uiState.value.filterDateMillis)

        viewModel.updateFilterDate(null)
        assertNull(viewModel.uiState.value.filterDateMillis)
    }

    @Test
    fun `fetchOrders respects cache and does not refetch if cache still valid`() = runTest(testDispatcher) {
        coEvery { getActiveOrdersUseCase() } returns Result.success(listOf(mockOrder))

        val viewModel = OrderListViewModel(
            getActiveOrdersUseCase,
            subscribeToOrdersUseCase,
            updateOrderStatusUseCase,
            getOrdersPagedUseCase
        )
        advanceUntilIdle()

        // At this point, cache is populated - calling fetchOrders without forceRefresh should skip
        coEvery { getActiveOrdersUseCase() } returns Result.success(emptyList())
        viewModel.fetchOrders(forceRefresh = false)
        advanceUntilIdle()

        // Still should have 1 order because cache was used
        assertEquals(1, viewModel.uiState.value.orders.size)
    }

    @Test
    fun `fetchOrders force refresh bypasses cache`() = runTest(testDispatcher) {
        coEvery { getActiveOrdersUseCase() } returns Result.success(listOf(mockOrder))

        val viewModel = OrderListViewModel(
            getActiveOrdersUseCase,
            subscribeToOrdersUseCase,
            updateOrderStatusUseCase,
            getOrdersPagedUseCase
        )
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.orders.size)

        coEvery { getActiveOrdersUseCase() } returns Result.success(emptyList())
        viewModel.fetchOrders(forceRefresh = true)
        advanceUntilIdle()

        // After force refresh with empty result, should now be empty
        assertTrue(viewModel.uiState.value.orders.isEmpty())
    }
}