package com.example.oneorder.home

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder.data.model.MenuItem
import com.example.oneorder.ui.screens.home.MenuItemCard
import com.example.oneorder.ui.theme.OneOrderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuItemCardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun makeMenuItem(
        id: Long = 1L,
        name: String = "Cơm Sườn",
        price: Double = 45_000.0,
        description: String = "Sườn nướng mật ong thơm ngon"
    ) = MenuItem(
        id = id,
        category_id = 1L,
        name = name,
        price = price,
        description = description,
        is_available = true
    )

    @Test
    fun menuItemCard_displaysNameAndPrice() {
        val item = makeMenuItem(name = "Phở Gà", price = 50_000.0)

        composeTestRule.setContent {
            OneOrderTheme {
                MenuItemCard(
                    item = item,
                    onAddToCart = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Phở Gà").assertIsDisplayed()
        // The price is formatted as "50.000 đ" or similar, so we check for substring
        composeTestRule.onAllNodesWithText("50", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun menuItemCard_displaysDescription_ifPresent() {
        val item = makeMenuItem(description = "Sợi phở dai ngon")

        composeTestRule.setContent {
            OneOrderTheme {
                MenuItemCard(
                    item = item,
                    onAddToCart = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Sợi phở dai ngon").assertIsDisplayed()
    }

    @Test
    fun menuItemCard_addButton_opensNoteDialog() {
        composeTestRule.setContent {
            OneOrderTheme {
                MenuItemCard(
                    item = makeMenuItem(),
                    onAddToCart = {}
                )
            }
        }

        // Click Add button (Thêm)
        composeTestRule
            .onNodeWithText("Thêm", ignoreCase = true)
            .performClick()

        // Verify that the dialog is open by looking for the confirm button "Thêm vào giỏ"
        composeTestRule
            .onNodeWithText("Thêm vào giỏ", ignoreCase = true)
            .assertIsDisplayed()
    }
}

