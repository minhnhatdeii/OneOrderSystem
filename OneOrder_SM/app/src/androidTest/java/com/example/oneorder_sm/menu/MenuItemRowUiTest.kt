package com.example.oneorder_sm.menu

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder_sm.data.model.MenuItem
import com.example.oneorder_sm.ui.screens.menu.MenuItemRow
import com.example.oneorder_sm.ui.theme.OneOrder_SMTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MenuItemRowUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun makeMenuItem(
        id: Long = 1L,
        name: String = "Cà phê sữa đá",
        price: Double = 25_000.0,
        description: String = "Cà phê đậm đà",
        isAvailable: Boolean = true
    ) = MenuItem(
        id = id,
        categoryId = 1L,
        name = name,
        price = price,
        description = description,
        isAvailable = isAvailable
    )

    @Test
    fun menuItemRow_displaysNameAndPrice() {
        val item = makeMenuItem(name = "Bạc Xỉu", price = 30_000.0)

        composeTestRule.setContent {
            OneOrder_SMTheme {
                MenuItemRow(
                    item = item,
                    onClick = {},
                    onToggleAvailability = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Bạc Xỉu").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("30", substring = true).onFirst().assertIsDisplayed()
    }

    @Test
    fun menuItemRow_displaysDescription() {
        val item = makeMenuItem(description = "Thức uống giải khát mùa hè")

        composeTestRule.setContent {
            OneOrder_SMTheme {
                MenuItemRow(
                    item = item,
                    onClick = {},
                    onToggleAvailability = {},
                    onDelete = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Thức uống giải khát mùa hè").assertIsDisplayed()
    }

    @Test
    fun menuItemRow_toggleSwitch_callsCallback() {
        var toggleCalled = false
        val item = makeMenuItem(isAvailable = true)

        composeTestRule.setContent {
            OneOrder_SMTheme {
                MenuItemRow(
                    item = item,
                    onClick = {},
                    onToggleAvailability = { toggleCalled = true },
                    onDelete = {}
                )
            }
        }

        // Find the Switch by checking for a toggleable node and toggle it
        composeTestRule
            .onNode(isToggleable())
            .performClick()

        assert(toggleCalled) { "onToggleAvailability must be called when the switch is clicked" }
    }
}

