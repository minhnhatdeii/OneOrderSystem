package com.example.oneorder.cart

import com.example.oneorder.data.model.CartItem
import com.example.oneorder.data.model.MenuItem
import com.example.oneorder.data.repository.CartManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [CartManager].
 *
 * CartManager là lớp quản lý giỏ hàng in-memory không phụ thuộc vào bất kỳ
 * dependency bên ngoài nào, nên có thể test thuần JVM (không cần thiết bị/emulator).
 *
 * Các trường hợp được kiểm thử:
 *  - Thêm mặt hàng mới vào giỏ
 *  - Gộp số lượng khi cùng item được thêm hai lần
 *  - Xoá mặt hàng khỏi giỏ
 *  - Cập nhật số lượng
 *  - Xoá toàn bộ giỏ
 *  - Tính tổng tiền đúng
 *  - Trường hợp giỏ rỗng
 */
class CartManagerTest {

    private lateinit var cartManager: CartManager

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun makeItem(id: Long, price: Double, name: String = "Món $id") = MenuItem(
        id           = id,
        category_id  = 1L,
        name         = name,
        price        = price,
        is_available = true
    )

    @Before
    fun setup() {
        cartManager = CartManager()
    }

    // ─── addToCart ────────────────────────────────────────────────────────────

    @Test
    fun `addToCart adds a new item when cart is empty`() {
        val item = makeItem(1L, 50_000.0)
        cartManager.addToCart(item)

        val cart = cartManager.cartItems.value
        assertEquals(1, cart.size)
        assertEquals(item, cart.first().menuItem)
        assertEquals(1, cart.first().quantity)
    }

    @Test
    fun `addToCart increments quantity when same item added again`() {
        val item = makeItem(1L, 50_000.0)
        cartManager.addToCart(item, quantity = 2)
        cartManager.addToCart(item, quantity = 3)

        val cart = cartManager.cartItems.value
        assertEquals(1, cart.size)
        assertEquals(5, cart.first().quantity)
    }

    @Test
    fun `addToCart handles multiple distinct items independently`() {
        cartManager.addToCart(makeItem(1L, 50_000.0))
        cartManager.addToCart(makeItem(2L, 80_000.0))
        cartManager.addToCart(makeItem(3L, 30_000.0))

        assertEquals(3, cartManager.cartItems.value.size)
    }

    // ─── removeFromCart ───────────────────────────────────────────────────────

    @Test
    fun `removeFromCart removes the correct item`() {
        cartManager.addToCart(makeItem(1L, 50_000.0))
        cartManager.addToCart(makeItem(2L, 80_000.0))

        cartManager.removeFromCart(itemId = 1L)

        val cart = cartManager.cartItems.value
        assertEquals(1, cart.size)
        assertEquals(2L, cart.first().menuItem.id)
    }

    @Test
    fun `removeFromCart on non-existent item leaves cart unchanged`() {
        cartManager.addToCart(makeItem(1L, 50_000.0))
        cartManager.removeFromCart(itemId = 999L)

        assertEquals(1, cartManager.cartItems.value.size)
    }

    // ─── updateQuantity ───────────────────────────────────────────────────────

    @Test
    fun `updateQuantity changes quantity to new value`() {
        cartManager.addToCart(makeItem(1L, 50_000.0), quantity = 1)
        cartManager.updateQuantity(itemId = 1L, quantity = 4)

        assertEquals(4, cartManager.cartItems.value.first().quantity)
    }

    @Test
    fun `updateQuantity to zero removes item from cart`() {
        cartManager.addToCart(makeItem(1L, 50_000.0))
        cartManager.updateQuantity(itemId = 1L, quantity = 0)

        assertTrue(cartManager.cartItems.value.isEmpty())
    }

    @Test
    fun `updateQuantity with negative value removes item from cart`() {
        cartManager.addToCart(makeItem(1L, 50_000.0))
        cartManager.updateQuantity(itemId = 1L, quantity = -1)

        assertTrue(cartManager.cartItems.value.isEmpty())
    }

    // ─── clearCart ────────────────────────────────────────────────────────────

    @Test
    fun `clearCart empties the cart`() {
        cartManager.addToCart(makeItem(1L, 50_000.0))
        cartManager.addToCart(makeItem(2L, 30_000.0))
        cartManager.clearCart()

        assertTrue(cartManager.cartItems.value.isEmpty())
    }

    @Test
    fun `clearCart on already-empty cart is a no-op`() {
        cartManager.clearCart()
        assertTrue(cartManager.cartItems.value.isEmpty())
    }

    // ─── getTotalPrice ────────────────────────────────────────────────────────

    @Test
    fun `getTotalPrice returns zero when cart is empty`() {
        assertEquals(0.0, cartManager.getTotalPrice(), 0.01)
    }

    @Test
    fun `getTotalPrice sums price times quantity for all items`() {
        // 2 × 50_000 + 3 × 30_000 = 100_000 + 90_000 = 190_000
        cartManager.addToCart(makeItem(1L, 50_000.0), quantity = 2)
        cartManager.addToCart(makeItem(2L, 30_000.0), quantity = 3)

        assertEquals(190_000.0, cartManager.getTotalPrice(), 0.01)
    }

    @Test
    fun `getTotalPrice updates correctly after quantity change`() {
        cartManager.addToCart(makeItem(1L, 100_000.0), quantity = 1)
        cartManager.updateQuantity(itemId = 1L, quantity = 3)

        assertEquals(300_000.0, cartManager.getTotalPrice(), 0.01)
    }

    @Test
    fun `getTotalPrice updates correctly after removing an item`() {
        cartManager.addToCart(makeItem(1L, 50_000.0), quantity = 2) // 100_000
        cartManager.addToCart(makeItem(2L, 80_000.0), quantity = 1) // 80_000
        cartManager.removeFromCart(itemId = 2L)

        assertEquals(100_000.0, cartManager.getTotalPrice(), 0.01)
    }

    // ─── CartItem.totalPrice ──────────────────────────────────────────────────

    @Test
    fun `CartItem totalPrice is price times quantity`() {
        val item = makeItem(1L, 75_000.0)
        val cartItem = CartItem(menuItem = item, quantity = 4)
        assertEquals(300_000.0, cartItem.totalPrice, 0.01)
    }
}

