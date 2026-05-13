package com.example.oneorder_sm.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder_sm.ui.screens.foodpromotion.EmptyPromotionState
import com.example.oneorder_sm.ui.theme.OneOrder_SMTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodPromotionScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emptyPromotionState_displaysCorrectly() {
        composeTestRule.setContent {
            OneOrder_SMTheme {
                EmptyPromotionState(onAdd = {})
            }
        }

        composeTestRule.onNodeWithText("Tạo bài đăng đầu tiên").assertIsDisplayed()
        composeTestRule.onNodeWithText("Thêm chút gì đó cho không gian này đi").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tạo").assertIsDisplayed()
    }

    @Test
    fun emptyPromotionState_addButtonIsClickable() {
        var clicked = false
        composeTestRule.setContent {
            OneOrder_SMTheme {
                EmptyPromotionState(onAdd = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Tạo").performClick()
        assert(clicked) { "onAdd callback phải được gọi khi nhấn nút Tạo" }
    }
}
