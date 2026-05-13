package com.example.oneorder_sm.ui.screens.dashboard

import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatistic
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.data.model.PaymentStatus
import com.example.oneorder_sm.domain.repository.OrderRepository
import com.example.oneorder_sm.domain.usecase.GetOrderStatisticsUseCase
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
import java.time.LocalDate
import java.time.YearMonth

/**
 * Unit tests for [IncomeHistoryViewModel].
 *
 * Kiểm thử các hành vi:
 * - Tính toán doanh thu tháng từ thống kê đơn hàng
 * - Tính avgTicket (doanh thu trung bình / đơn)
 * - Lọc đơn hàng theo ngày được chọn (client-side)
 * - Chuyển tháng và tải dữ liệu mới
 * - Trường hợp không có đơn hàng (doanh thu = 0)
 */
@OptIn(ExperimentalCoroutinesApi::class)
class IncomeHistoryViewModelTest {

    // ─── Mocks ────────────────────────────────────────────────────────────────
    private val getOrderStatisticsUseCase: GetOrderStatisticsUseCase = mockk()
    private val orderRepository: OrderRepository = mockk()

    private lateinit var viewModel: IncomeHistoryViewModel
    private val testDispatcher = StandardTestDispatcher()

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private val currentMonth = YearMonth.now()
    private val today = LocalDate.now()
    private val todayStr = today.toString()

    private fun makeOrderStatistic(dateStr: String, revenue: Double, orders: Int) = OrderStatistic(
        orderDate    = dateStr,
        totalOrders  = orders,
        totalRevenue = revenue
    )

    private fun makeOrder(
        id: String,
        status: OrderStatus = OrderStatus.PAID,
        totalAmount: Double = 100_000.0,
        createdAt: String = todayStr + "T10:00:00+07:00"
    ) = Order(
        id            = id,
        userId        = "user-001",
        totalAmount   = totalAmount,
        status        = status,
        paymentStatus = PaymentStatus.PAID,
        createdAt     = createdAt,
        updatedAt     = createdAt
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Default: no stats, no active orders
        coEvery { getOrderStatisticsUseCase(any(), any()) } returns Result.success(emptyList())
        coEvery { orderRepository.getActiveOrders() } returns Result.success(emptyList())
        coEvery { orderRepository.subscribeToOrders() } returns emptyFlow()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): IncomeHistoryViewModel {
        return IncomeHistoryViewModel(getOrderStatisticsUseCase, orderRepository)
    }

    // ─── Initial load ─────────────────────────────────────────────────────────

    @Test
    fun `initial state has isLoading true`() {
        val vm = createViewModel()
        assertTrue(vm.uiState.value.isLoading)
    }

    @Test
    fun `after init with empty data monthRevenue is zero`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals(0.0, state.monthRevenue, 0.01)
        assertEquals(0, state.monthOrderCount)
        assertEquals(0.0, state.avgTicket, 0.01)
    }

    @Test
    fun `monthRevenue sums totalRevenue from all stats in current month`() = runTest(testDispatcher) {
        val monthPrefix = currentMonth.toString() // e.g. "2026-05"
        coEvery { getOrderStatisticsUseCase(any(), any()) } returns Result.success(
            listOf(
                makeOrderStatistic("$monthPrefix-01", revenue = 200_000.0, orders = 2),
                makeOrderStatistic("$monthPrefix-02", revenue = 150_000.0, orders = 1),
                makeOrderStatistic("$monthPrefix-03", revenue = 50_000.0,  orders = 1),
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(400_000.0, vm.uiState.value.monthRevenue, 0.01)
        assertEquals(4, vm.uiState.value.monthOrderCount)
    }

    @Test
    fun `avgTicket is monthRevenue divided by monthOrderCount`() = runTest(testDispatcher) {
        val monthPrefix = currentMonth.toString()
        coEvery { getOrderStatisticsUseCase(any(), any()) } returns Result.success(
            listOf(
                makeOrderStatistic("$monthPrefix-10", revenue = 300_000.0, orders = 3)
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        // 300_000 / 3 = 100_000
        assertEquals(100_000.0, vm.uiState.value.avgTicket, 0.01)
    }

    @Test
    fun `avgTicket is zero when no orders exist`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        assertEquals(0.0, vm.uiState.value.avgTicket, 0.01)
    }

    // ─── dayPerformances ──────────────────────────────────────────────────────

    @Test
    fun `dayPerformances contains one entry per day in the current month`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val expectedDays = currentMonth.lengthOfMonth()
        assertEquals(expectedDays, vm.uiState.value.dayPerformances.size)
    }

    @Test
    fun `dayPerformances entry for a date with stats has correct revenue`() = runTest(testDispatcher) {
        val monthPrefix = currentMonth.toString()
        val targetDate = "$monthPrefix-05"
        coEvery { getOrderStatisticsUseCase(any(), any()) } returns Result.success(
            listOf(makeOrderStatistic(targetDate, revenue = 500_000.0, orders = 5))
        )

        val vm = createViewModel()
        advanceUntilIdle()

        val dayEntry = vm.uiState.value.dayPerformances.find { it.date.toString() == targetDate }
        assertNotNull(dayEntry)
        assertEquals(500_000.0, dayEntry!!.revenue, 0.01)
    }

    @Test
    fun `dayPerformances entry for a date with no stats has revenue zero`() = runTest(testDispatcher) {
        val vm = createViewModel()
        advanceUntilIdle()

        val dayEntry = vm.uiState.value.dayPerformances.firstOrNull()
        assertNotNull(dayEntry)
        assertEquals(0.0, dayEntry!!.revenue, 0.01)
    }

    // ─── selectDate: lọc đơn hàng theo ngày ──────────────────────────────────

    @Test
    fun `selectDate filters orders by selected date only`() = runTest(testDispatcher) {
        val yesterdayStr = today.minusDays(1).toString()
        coEvery { orderRepository.getActiveOrders() } returns Result.success(
            listOf(
                makeOrder("o1", createdAt = "${todayStr}T09:00:00+07:00"),
                makeOrder("o2", createdAt = "${todayStr}T14:00:00+07:00"),
                makeOrder("o3", createdAt = "${yesterdayStr}T10:00:00+07:00"), // yesterday
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        // Default selectedDate is today
        assertEquals(2, vm.uiState.value.selectedDateOrders.size)
    }

    @Test
    fun `selectDate with different date updates selectedDateOrders`() = runTest(testDispatcher) {
        val tomorrowStr = today.plusDays(1).toString()
        coEvery { orderRepository.getActiveOrders() } returns Result.success(
            listOf(
                makeOrder("o1", createdAt = "${todayStr}T09:00:00+07:00"),
                makeOrder("o2", createdAt = "${tomorrowStr}T10:00:00+07:00"),
            )
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectDate(today.plusDays(1))
        advanceUntilIdle()

        assertEquals(1, vm.uiState.value.selectedDateOrders.size)
        assertEquals("o2", vm.uiState.value.selectedDateOrders.first().id)
    }

    @Test
    fun `selectedDateOrders is empty when no orders match selected date`() = runTest(testDispatcher) {
        coEvery { orderRepository.getActiveOrders() } returns Result.success(
            listOf(makeOrder("o1", createdAt = "${todayStr}T09:00:00+07:00"))
        )

        val vm = createViewModel()
        advanceUntilIdle()

        vm.selectDate(today.minusDays(10))
        advanceUntilIdle()

        assertTrue(vm.uiState.value.selectedDateOrders.isEmpty())
    }

    // ─── Error handling ───────────────────────────────────────────────────────

    @Test
    fun `when getOrderStatistics fails error is set in uiState`() = runTest(testDispatcher) {
        coEvery { getOrderStatisticsUseCase(any(), any()) } throws RuntimeException("Server error")

        val vm = createViewModel()
        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }
}
