package com.example.oneorder.cart

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder.data.model.CartItem
import com.example.oneorder.data.model.MenuItem
import com.example.oneorder.ui.screens.cart.CartItemCard
import com.example.oneorder.ui.theme.OneOrderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI Tests (Compose Instrumented) cho component CartItemCard của OneOrder.
 *
 * CartItemCard là composable thuần (không phụ thuộc Hilt/ViewModel), nên
 * có thể được inject dữ liệu giả và test trực tiếp.
 *
 * Công cụ: Compose UI Testing (androidx.compose.ui.test)
 */
@RunWith(AndroidJUnit4::class)
class CartItemCardUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─── Helper ───────────────────────────────────────────────────────────────

    private fun makeCartItem(
        id: Long = 1L,
        name: String = "Phở Bò Đặc Biệt",
        price: Double = 65_000.0,
        quantity: Int = 2
    ) = CartItem(
        menuItem = MenuItem(
            id           = id,
            category_id  = 1L,
            name         = name,
            price        = price,
            is_available = true
        ),
        quantity = quantity
    )

    // ─── Hiển thị tên món ────────────────────────────────────────────────────

    @Test
    fun cartItemCard_displaysItemName() {
        composeTestRule.setContent {
            OneOrderTheme {
                CartItemCard(
                    cartItem  = makeCartItem(name = "Bún Bò Huế"),
                    onIncrease = {},
                    onDecrease = {},
                    onRemove   = {}
                )
            }
        }
        composeTestRule.onNodeWithText("Bún Bò Huế").assertIsDisplayed()
    }

    // ─── Hiển thị số lượng ───────────────────────────────────────────────────

    @Test
    fun cartItemCard_displaysQuantity() {
        composeTestRule.setContent {
            OneOrderTheme {
                CartItemCard(
                    cartItem  = makeCartItem(quantity = 3),
                    onIncrease = {},
                    onDecrease = {},
                    onRemove   = {}
                )
            }
        }
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }

    // ─── Nút tăng số lượng ───────────────────────────────────────────────────

    @Test
    fun cartItemCard_callsOnIncrease_whenAddButtonClicked() {
        var incremented = false

        composeTestRule.setContent {
            OneOrderTheme {
                CartItemCard(
                    cartItem   = makeCartItem(),
                    onIncrease = { incremented = true },
                    onDecrease = {},
                    onRemove   = {}
                )
            }
        }

        // Click nút tăng (contentDescription = "Increase" / "Tăng")
        composeTestRule
            .onAllNodes(hasClickAction())
            .filterToOne(
                hasContentDescription("Increase", ignoreCase = true)
                    .or(hasContentDescription("increase", ignoreCase = true))
            )
            .performClick()

        assert(incremented) { "onIncrease phải được gọi khi nhấn nút +" }
    }

    // ─── Nút giảm số lượng ───────────────────────────────────────────────────

    @Test
    fun cartItemCard_callsOnDecrease_whenMinusButtonClicked() {
        var decremented = false

        composeTestRule.setContent {
            OneOrderTheme {
                CartItemCard(
                    cartItem   = makeCartItem(),
                    onIncrease = {},
                    onDecrease = { decremented = true },
                    onRemove   = {}
                )
            }
        }

        composeTestRule
            .onAllNodes(hasClickAction())
            .filterToOne(
                hasContentDescription("Decrease", ignoreCase = true)
                    .or(hasContentDescription("decrease", ignoreCase = true))
            )
            .performClick()

        assert(decremented) { "onDecrease phải được gọi khi nhấn nút -" }
    }

    // ─── Nút xoá ─────────────────────────────────────────────────────────────

    @Test
    fun cartItemCard_callsOnRemove_whenDeleteButtonClicked() {
        var removed = false

        composeTestRule.setContent {
            OneOrderTheme {
                CartItemCard(
                    cartItem   = makeCartItem(),
                    onIncrease = {},
                    onDecrease = {},
                    onRemove   = { removed = true }
                )
            }
        }

        composeTestRule
            .onAllNodes(hasClickAction())
            .filterToOne(
                hasContentDescription("Remove", ignoreCase = true)
                    .or(hasContentDescription("remove", ignoreCase = true))
            )
            .performClick()

        assert(removed) { "onRemove phải được gọi khi nhấn nút xoá" }
    }
}

