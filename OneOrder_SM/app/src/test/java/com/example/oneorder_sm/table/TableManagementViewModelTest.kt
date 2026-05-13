package com.example.oneorder_sm.table

import android.graphics.Bitmap
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.data.model.PaymentStatus
import com.example.oneorder_sm.data.model.Table
import com.example.oneorder_sm.domain.usecase.*
import com.example.oneorder_sm.ui.screens.table.TableManagementViewModel
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
class TableManagementViewModelTest {

    private val getTablesUseCase: GetTablesUseCase = mockk()
    private val addTableUseCase: AddTableUseCase = mockk()
    private val updateTableUseCase: UpdateTableUseCase = mockk()
    private val deleteTableUseCase: DeleteTableUseCase = mockk()
    private val updateTableStatusUseCase: UpdateTableStatusUseCase = mockk()
    private val generateQRCodeUseCase: GenerateQRCodeUseCase = mockk()
    private val getActiveOrdersUseCase: GetActiveOrdersUseCase = mockk()
    private val updateOrderStatusUseCase: UpdateOrderStatusUseCase = mockk()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { getTablesUseCase() } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): TableManagementViewModel {
        return TableManagementViewModel(
            getTablesUseCase,
            addTableUseCase,
            updateTableUseCase,
            deleteTableUseCase,
            updateTableStatusUseCase,
            generateQRCodeUseCase,
            getActiveOrdersUseCase,
            updateOrderStatusUseCase
        )
    }

    @Test
    fun `loadTables updates state with tables on success`() = runTest(testDispatcher) {
        val mockTables = listOf(Table(id = 1L, name = "Bàn 1", status = "free", capacity = 4, location = "Tầng 1"))
        coEvery { getTablesUseCase() } returns Result.success(mockTables)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mockTables, state.tables)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `saveTable adds new table when id is null`() = runTest(testDispatcher) {
        coEvery { addTableUseCase(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveTable(id = null, tableName = "Bàn mới", capacity = 2)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Table added successfully", state.successMessage)
    }

    @Test
    fun `saveTable updates table when id is not null`() = runTest(testDispatcher) {
        coEvery { updateTableUseCase(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.saveTable(id = 1L, tableName = "Bàn cập nhật", capacity = 4)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Table updated successfully", state.successMessage)
    }

    @Test
    fun `deleteTable removes table on success`() = runTest(testDispatcher) {
        coEvery { deleteTableUseCase(1L) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deleteTable(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Table deleted successfully", state.successMessage)
    }

    @Test
    fun `updateStatus changes table status on success`() = runTest(testDispatcher) {
        val mockTables = listOf(Table(id = 1L, name = "Bàn 1", status = "free", capacity = 4, location = "Tầng 1"))
        coEvery { getTablesUseCase() } returns Result.success(mockTables)
        coEvery { updateTableStatusUseCase(1L, "occupied") } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateStatus(1L, "occupied")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("occupied", state.tables[0].status)
    }

    @Test
    fun `generateQRCode updates state with bitmap on success`() = runTest(testDispatcher) {
        val mockBitmap: Bitmap = mockk()
        coEvery { generateQRCodeUseCase(1L) } returns Result.success(mockBitmap)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.generateQRCode(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mockBitmap, state.generatedQRCode)
    }

    @Test
    fun `loadOrderForTable updates selectedTableOrder on success`() = runTest(testDispatcher) {
        val mockOrder = Order(
            id = "order1",
            tableId = 1,
            userId = "user1",
            totalAmount = 100000.0,
            status = OrderStatus.PENDING,
            paymentStatus = PaymentStatus.UNPAID,
            createdAt = "2026-05-11T10:00:00Z",
            updatedAt = "2026-05-11T10:00:00Z"
        )
        coEvery { getActiveOrdersUseCase() } returns Result.success(listOf(mockOrder))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadOrderForTable(1L)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(mockOrder, state.selectedTableOrder)
        assertFalse(state.isLoadingOrder)
    }

    @Test
    fun `checkoutTable updates order status to PAID and sets table to free`() = runTest(testDispatcher) {
        coEvery { updateOrderStatusUseCase("order1", OrderStatus.PAID) } returns Result.success(Unit)
        coEvery { updateTableStatusUseCase(1L, "free") } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.checkoutTable(1L, "order1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Thanh toán thành công!", state.successMessage)
        assertNull(state.selectedTableOrder)
    }
}
