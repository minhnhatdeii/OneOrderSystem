package com.example.oneorder.orders

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder.data.model.Order
import com.example.oneorder.ui.screens.order.OrderCardModern
import com.example.oneorder.ui.theme.OneOrderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OrderScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun orderCardModern_displaysOrderInfo() {
        val order = Order(
            id = "order12345",
            userId = "user1",
            tenantId = "rest1",
            tableId = 1L,
            totalAmount = 150000.0,
            status = "pending",
            createdAt = "2023-01-01T10:00:00Z"
        )

        composeTestRule.setContent {
            OneOrderTheme {
                OrderCardModern(order = order, onClick = {})
            }
        }

        // The id takes first 8 characters
        composeTestRule.onNodeWithText("Đơn #order123", substring = true).assertIsDisplayed()
        composeTestRule.onNodeWithText("PENDING").assertIsDisplayed()
    }
}
