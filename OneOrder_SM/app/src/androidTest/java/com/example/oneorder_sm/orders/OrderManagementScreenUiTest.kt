package com.example.oneorder_sm.orders

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.data.model.PaymentStatus
import com.example.oneorder_sm.ui.screens.orders.OrderCard
import com.example.oneorder_sm.ui.theme.OneOrder_SMTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderManagementScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun orderCard_displaysOrderInfo() {
        val order = Order(
            id = "order1",
            tableId = 1,
            userId = "user1",
            totalAmount = 50000.0,
            status = OrderStatus.PENDING,
            paymentStatus = PaymentStatus.UNPAID,
            createdAt = "2023-01-01T10:00:00Z",
            updatedAt = "2023-01-01T10:00:00Z",
            orderItems = emptyList(),
            tableName = "Bàn 5"
        )

        composeTestRule.setContent {
            OneOrder_SMTheme {
                OrderCard(order = order, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Bàn: Bàn 5").assertIsDisplayed()
        composeTestRule.onNodeWithText("Chờ").assertIsDisplayed()
    }
}
