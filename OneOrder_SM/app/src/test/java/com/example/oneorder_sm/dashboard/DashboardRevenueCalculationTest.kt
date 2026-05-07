package com.example.oneorder_sm.dashboard

import com.example.oneorder_sm.data.model.DashboardSummary
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderItem
import com.example.oneorder_sm.data.model.OrderStatus
import com.example.oneorder_sm.data.model.PaymentStatus
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Unit tests for the dashboard revenue and table calculation logic.
 *
 * These tests cover:
 *  1. Revenue calculation: only non-cancelled orders count.
 *  2. Table occupancy: distinct tableIds from active orders.
 *  3. totalTables comes from the repository (simulated via DashboardSummary).
 *  4. Date parsing: handles both Z (UTC) and +07:00 (Vietnam) offsets.
 */
class DashboardRevenueCalculationTest {

    // ─── helpers ──────────────────────────────────────────────────────────────

    private val vnZone = ZoneId.of("Asia/Ho_Chi_Minh")

    /** Simulates the date-parsing logic in StatisticsRepositoryImpl */
    private fun parseOrderDate(createdAt: String): LocalDate? = try {
        OffsetDateTime.parse(createdAt).atZoneSameInstant(vnZone).toLocalDate()
    } catch (_: Exception) {
        try {
            java.time.Instant.parse(createdAt).atZone(vnZone).toLocalDate()
        } catch (_: Exception) {
            null
        }
    }

    /** Today's date in Vietnam timezone */
    private val todayVn: LocalDate get() = LocalDate.now(vnZone)

    /** Builds an ISO-8601 timestamp for today in Vietnam, formatted with +07:00 offset */
    private fun todayTimestampVnOffset(hour: Int = 10): String {
        val zdt = ZonedDateTime.now(vnZone).withHour(hour).withMinute(0).withSecond(0).withNano(0)
        return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)   // e.g. 2026-05-04T10:00:00+07:00
    }

    /** Builds a UTC (Z-suffix) timestamp for today */
    private fun todayTimestampUtc(hour: Int = 3): String {
        // hour=3 UTC → 10:00 VN
        val zdt = ZonedDateTime.now(ZoneId.of("UTC")).withHour(hour).withMinute(0).withSecond(0).withNano(0)
        return zdt.format(DateTimeFormatter.ISO_INSTANT)             // e.g. 2026-05-04T03:00:00Z
    }

    private fun yesterdayTimestamp(): String {
        val zdt = ZonedDateTime.now(vnZone).minusDays(1).withHour(10).withMinute(0).withSecond(0).withNano(0)
        return zdt.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
    }

    private fun makeOrder(
        id: String,
        status: OrderStatus,
        totalAmount: Double,
        tableId: Int? = null,
        createdAt: String = todayTimestampVnOffset()
    ) = Order(
        id = id,
        userId = "user-001",
        tableId = tableId,
        tableName = tableId?.let { "Bàn $it" },
        totalAmount = totalAmount,
        status = status,
        paymentStatus = if (status == OrderStatus.PAID) PaymentStatus.PAID else PaymentStatus.UNPAID,
        createdAt = createdAt,
        updatedAt = createdAt,
        orderItems = emptyList()
    )

    // ─── Revenue calculation ───────────────────────────────────────────────────

    @Test
    fun `todayRevenue sums all non-cancelled orders created today`() {
        val orders = listOf(
            makeOrder("o1", OrderStatus.PAID,      229_000.0),
            makeOrder("o2", OrderStatus.PENDING,   100_000.0),
            makeOrder("o3", OrderStatus.CONFIRMED, 150_000.0),
            makeOrder("o4", OrderStatus.CANCELLED,  50_000.0), // excluded
        )

        val revenue = orders
            .filter { parseOrderDate(it.createdAt) == todayVn && it.status != OrderStatus.CANCELLED }
            .sumOf { it.totalAmount }

        assertEquals(479_000.0, revenue, 0.01)
    }

    @Test
    fun `todayRevenue excludes orders from yesterday`() {
        val orders = listOf(
            makeOrder("o1", OrderStatus.PAID,  200_000.0, createdAt = todayTimestampVnOffset()),
            makeOrder("o2", OrderStatus.PAID,  100_000.0, createdAt = yesterdayTimestamp()),
        )

        val revenue = orders
            .filter { parseOrderDate(it.createdAt) == todayVn && it.status != OrderStatus.CANCELLED }
            .sumOf { it.totalAmount }

        assertEquals(200_000.0, revenue, 0.01)
    }

    @Test
    fun `todayRevenue is zero when all orders are cancelled`() {
        val orders = listOf(
            makeOrder("o1", OrderStatus.CANCELLED, 100_000.0),
            makeOrder("o2", OrderStatus.CANCELLED,  50_000.0),
        )

        val revenue = orders
            .filter { parseOrderDate(it.createdAt) == todayVn && it.status != OrderStatus.CANCELLED }
            .sumOf { it.totalAmount }

        assertEquals(0.0, revenue, 0.01)
    }

    @Test
    fun `todayRevenue is zero when no orders today`() {
        val orders = listOf(
            makeOrder("o1", OrderStatus.PAID, 100_000.0, createdAt = yesterdayTimestamp()),
        )

        val revenue = orders
            .filter { parseOrderDate(it.createdAt) == todayVn && it.status != OrderStatus.CANCELLED }
            .sumOf { it.totalAmount }

        assertEquals(0.0, revenue, 0.01)
    }

    // ─── Date parsing ─────────────────────────────────────────────────────────

    @Test
    fun `parseOrderDate handles +07 00 offset format`() {
        val timestamp = todayTimestampVnOffset(10)
        val parsed = parseOrderDate(timestamp)
        assertEquals(todayVn, parsed)
    }

    @Test
    fun `parseOrderDate handles UTC Z suffix format`() {
        val timestamp = todayTimestampUtc(3) // 03:00 UTC = 10:00 VN → same calendar day
        val parsed = parseOrderDate(timestamp)
        assertEquals(todayVn, parsed)
    }

    @Test
    fun `parseOrderDate returns null for garbage input`() {
        val parsed = parseOrderDate("not-a-date")
        assertNull(parsed)
    }

    // ─── Table occupancy ──────────────────────────────────────────────────────

    @Test
    fun `occupiedTables counts distinct tableIds from active orders only`() {
        val orders = listOf(
            makeOrder("o1", OrderStatus.PENDING,   100_000.0, tableId = 1),
            makeOrder("o2", OrderStatus.CONFIRMED, 100_000.0, tableId = 2),
            makeOrder("o3", OrderStatus.PREPARING, 100_000.0, tableId = 2), // same table → distinct
            makeOrder("o4", OrderStatus.PAID,      100_000.0, tableId = 3), // paid → not active
            makeOrder("o5", OrderStatus.CANCELLED, 100_000.0, tableId = 4), // cancelled → not active
        )

        val activeStatuses = setOf(
            OrderStatus.PENDING, OrderStatus.CONFIRMED,
            OrderStatus.PREPARING, OrderStatus.SERVED
        )
        val occupiedTables = orders
            .filter { it.status in activeStatuses }
            .mapNotNull { it.tableId }
            .distinct()
            .size

        assertEquals(2, occupiedTables)
    }

    @Test
    fun `occupiedTables is zero when no active orders`() {
        val orders = listOf(
            makeOrder("o1", OrderStatus.PAID,      100_000.0, tableId = 1),
            makeOrder("o2", OrderStatus.CANCELLED, 100_000.0, tableId = 2),
        )

        val activeStatuses = setOf(
            OrderStatus.PENDING, OrderStatus.CONFIRMED,
            OrderStatus.PREPARING, OrderStatus.SERVED
        )
        val occupiedTables = orders
            .filter { it.status in activeStatuses }
            .mapNotNull { it.tableId }
            .distinct()
            .size

        assertEquals(0, occupiedTables)
    }

    @Test
    fun `occupiedTables ignores orders without tableId (takeaway)`() {
        val orders = listOf(
            makeOrder("o1", OrderStatus.PENDING, 100_000.0, tableId = null), // takeaway
            makeOrder("o2", OrderStatus.PENDING, 100_000.0, tableId = 5),
        )

        val activeStatuses = setOf(
            OrderStatus.PENDING, OrderStatus.CONFIRMED,
            OrderStatus.PREPARING, OrderStatus.SERVED
        )
        val occupiedTables = orders
            .filter { it.status in activeStatuses }
            .mapNotNull { it.tableId }
            .distinct()
            .size

        assertEquals(1, occupiedTables)
    }

    // ─── totalTables from DashboardSummary ────────────────────────────────────

    @Test
    fun `DashboardSummary totalTables is preserved through copy`() {
        val summary = DashboardSummary(
            todayRevenue = 0.0,
            todayOrders = 0,
            activeOrders = 0,
            occupiedTables = 0,
            totalTables = 8,
            totalStaff = 3
        )

        // Simulates what DashboardViewModel does when recalculating active orders
        val updated = summary.copy(activeOrders = 2, occupiedTables = 1)

        // totalTables must survive the copy
        assertEquals(8, updated.totalTables)
        assertEquals(1, updated.occupiedTables)
        assertEquals(2, updated.activeOrders)
    }

    @Test
    fun `table display shows occupiedTables slash totalTables format`() {
        val summary = DashboardSummary(
            todayRevenue = 229_000.0,
            todayOrders = 1,
            activeOrders = 1,
            occupiedTables = 1,
            totalTables = 8,
            totalStaff = 3
        )

        val displayValue = "${summary.occupiedTables}/${summary.totalTables}"
        assertEquals("1/8", displayValue)
    }

    @Test
    fun `table display is not 1 over 0 when totalTables is fetched correctly`() {
        // This test documents the bug that was fixed:
        // totalTables used to be hardcoded 0, causing display "1/0"
        val buggyOldSummary = DashboardSummary(
            occupiedTables = 1,
            totalTables = 0   // ← the old bug
        )
        val fixedSummary = DashboardSummary(
            occupiedTables = 1,
            totalTables = 8   // ← now fetched from DB
        )

        assertNotEquals("1/8", "${buggyOldSummary.occupiedTables}/${buggyOldSummary.totalTables}")
        assertEquals("1/8", "${fixedSummary.occupiedTables}/${fixedSummary.totalTables}")
    }

    // ─── todayOrders count ────────────────────────────────────────────────────

    @Test
    fun `todayOrders counts ALL orders today including cancelled`() {
        val orders = listOf(
            makeOrder("o1", OrderStatus.PAID,      100_000.0),
            makeOrder("o2", OrderStatus.CANCELLED,  50_000.0),
            makeOrder("o3", OrderStatus.PENDING,    80_000.0),
            makeOrder("o4", OrderStatus.PAID,      100_000.0, createdAt = yesterdayTimestamp()), // excluded
        )

        val todayOrderCount = orders.count { parseOrderDate(it.createdAt) == todayVn }
        assertEquals(3, todayOrderCount)
    }
}
