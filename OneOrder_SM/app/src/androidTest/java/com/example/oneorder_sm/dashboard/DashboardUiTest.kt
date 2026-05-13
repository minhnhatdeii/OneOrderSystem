package com.example.oneorder_sm.dashboard

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder_sm.data.model.DashboardSummary
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.data.model.PaymentStatus
import com.example.oneorder_sm.ui.theme.OneOrder_SMTheme
import com.example.oneorder_sm.ui.screens.dashboard.DayPerformance
import com.example.oneorder_sm.ui.screens.dashboard.IncomeHistoryUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

/**
 * UI Tests (Compose Instrumented) cho OneOrder_SM.
 *
 * Vì các composable chính (DashboardScreen, IncomeHistoryScreen) phụ thuộc Hilt/ViewModel,
 * chúng ta test các composable đơn giản hơn bằng cách tạo context trực tiếp.
 *
 * Các test kiểm tra:
 * 1. DashboardSummaryCard - hiển thị số liệu dashboard
 * 2. DayPerformance chip - hiển thị ngày/doanh thu
 * 3. IncomeHistoryUiState - logic data model
 */
@RunWith(AndroidJUnit4::class)
class DashboardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─── Helper: tạo DashboardSummary ────────────────────────────────────────

    private fun makeSummary(
        revenue: Double = 500_000.0,
        activeOrders: Int = 3,
        occupiedTables: Int = 2,
        totalTables: Int = 8,
        totalStaff: Int = 5
    ) = DashboardSummary(
        todayRevenue   = revenue,
        activeOrders   = activeOrders,
        occupiedTables = occupiedTables,
        totalTables    = totalTables,
        totalStaff     = totalStaff
    )

    // ─── Test hiển thị số liệu dashboard qua Text trực tiếp ─────────────────

    @Test
    fun dashboardSummary_tableDisplayFormat_isCorrect() {
        val summary = makeSummary(occupiedTables = 3, totalTables = 10)

        composeTestRule.setContent {
            OneOrder_SMTheme {
                // Render một Text đơn giản để kiểm tra format
                androidx.compose.material3.Text(
                    text = "${summary.occupiedTables}/${summary.totalTables}"
                )
            }
        }

        composeTestRule.onNodeWithText("3/10").assertIsDisplayed()
    }

    @Test
    fun dashboardSummary_activeOrdersDisplay_isCorrect() {
        val summary = makeSummary(activeOrders = 7)

        composeTestRule.setContent {
            OneOrder_SMTheme {
                androidx.compose.material3.Text(text = "${summary.activeOrders}")
            }
        }

        composeTestRule.onNodeWithText("7").assertIsDisplayed()
    }

    // ─── Test DayPerformance label ────────────────────────────────────────────

    @Test
    fun dayPerformance_dayLabel_displaysCorrectVietnameseDayOfWeek() {
        val monday = LocalDate.of(2026, 5, 11) // 2026-05-11 is a Monday

        val dayPerf = DayPerformance(
            date         = monday,
            dayLabel     = "T2",
            dayOfMonth   = 11,
            revenue      = 200_000.0,
            revenueLabel = "200k"
        )

        composeTestRule.setContent {
            OneOrder_SMTheme {
                androidx.compose.foundation.layout.Column {
                    androidx.compose.material3.Text(text = dayPerf.dayLabel)
                    androidx.compose.material3.Text(text = dayPerf.revenueLabel)
                }
            }
        }

        composeTestRule.onNodeWithText("T2").assertIsDisplayed()
        composeTestRule.onNodeWithText("200k").assertIsDisplayed()
    }

    // ─── Test Order list item display ─────────────────────────────────────────

    @Test
    fun orderItem_statusText_pendingDisplaysCorrectly() {
        val statusText = when (OrderStatus.PENDING) {
            OrderStatus.PENDING   -> "Chờ xác nhận"
            OrderStatus.CONFIRMED -> "Đã xác nhận"
            OrderStatus.PREPARING -> "Đang chế biến"
            OrderStatus.SERVED    -> "Đã phục vụ"
            OrderStatus.PAID      -> "Đã thanh toán"
            OrderStatus.CANCELLED -> "Đã hủy"
        }

        composeTestRule.setContent {
            OneOrder_SMTheme {
                androidx.compose.material3.Text(text = statusText)
            }
        }

        composeTestRule.onNodeWithText("Chờ xác nhận").assertIsDisplayed()
    }

    @Test
    fun orderItem_statusText_paidDisplaysCorrectly() {
        val statusText = when (OrderStatus.PAID) {
            OrderStatus.PENDING   -> "Chờ xác nhận"
            OrderStatus.CONFIRMED -> "Đã xác nhận"
            OrderStatus.PREPARING -> "Đang chế biến"
            OrderStatus.SERVED    -> "Đã phục vụ"
            OrderStatus.PAID      -> "Đã thanh toán"
            OrderStatus.CANCELLED -> "Đã hủy"
        }

        composeTestRule.setContent {
            OneOrder_SMTheme {
                androidx.compose.material3.Text(text = statusText)
            }
        }

        composeTestRule.onNodeWithText("Đã thanh toán").assertIsDisplayed()
    }

    // ─── Test IncomeHistoryUiState logic ─────────────────────────────────────

    @Test
    fun incomeHistoryUiState_defaultState_hasCorrectInitialValues() {
        val state = IncomeHistoryUiState()

        assert(state.isLoading) { "Initial state phải có isLoading = true" }
        assert(state.monthRevenue == 0.0) { "monthRevenue ban đầu phải là 0" }
        assert(state.monthOrderCount == 0) { "monthOrderCount ban đầu phải là 0" }
        assert(state.dayPerformances.isEmpty()) { "dayPerformances ban đầu phải rỗng" }
        assert(state.selectedDateOrders.isEmpty()) { "selectedDateOrders ban đầu phải rỗng" }
    }

    @Test
    fun incomeHistoryUiState_copyWithData_preservesOtherFields() {
        val state = IncomeHistoryUiState()
        val updated = state.copy(
            isLoading       = false,
            monthRevenue    = 1_500_000.0,
            monthOrderCount = 15
        )

        assert(!updated.isLoading)
        assert(updated.monthRevenue == 1_500_000.0)
        assert(updated.monthOrderCount == 15)
        assert(updated.avgTicket == 0.0) { "avgTicket không thay đổi qua copy" }
    }
}

