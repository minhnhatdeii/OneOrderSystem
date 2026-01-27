package com.example.oneorder_sm.ui.screens.dashboard

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.oneorder_sm.data.model.OrderStatistic
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.chart.line.lineChart
import com.patrykandpatrick.vico.core.entry.entryModelOf
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun RevenueChart(
    orderStats: List<OrderStatistic>,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("RevenueChart", "orderStats.size = ${orderStats.size}")
    
    if (orderStats.isEmpty()) {
        android.util.Log.d("RevenueChart", "Empty - no chart rendered")
        return
    }
    
    orderStats.forEachIndexed { index, stat ->
        android.util.Log.d("RevenueChart", "[$index] ${stat.orderDate}: ${stat.totalRevenue}₫, orders: ${stat.totalOrders}")
    }

    val chartEntryModel = remember(orderStats) {
        entryModelOf(*orderStats.map { it.totalRevenue.toFloat() }.toTypedArray())
    }

    Chart(
        chart = columnChart(),
        model = chartEntryModel,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ ->
                val index = value.toInt()
                if (index in orderStats.indices) {
                    val dateStr = orderStats[index].orderDate
                    try {
                        val date = LocalDate.parse(dateStr)
                        date.format(DateTimeFormatter.ofPattern("dd/MM"))
                    } catch (e: Exception) {
                        dateStr
                    }
                } else {
                    ""
                }
            }
        ),
        modifier = modifier
    )
}

@Composable
fun OrderCountChart(
    orderStats: List<OrderStatistic>,
    modifier: Modifier = Modifier
) {
    android.util.Log.d("OrderCountChart", "orderStats.size = ${orderStats.size}")
    
    if (orderStats.isEmpty()) {
        android.util.Log.d("OrderCountChart", "Empty - no chart rendered")
        return
    }
    
    orderStats.forEachIndexed { index, stat ->
        android.util.Log.d("OrderCountChart", "[$index] ${stat.orderDate}: ${stat.totalOrders} orders")
    }

    val chartEntryModel = remember(orderStats) {
        entryModelOf(*orderStats.map { it.totalOrders.toFloat() }.toTypedArray())
    }

    Chart(
        chart = columnChart(),
        model = chartEntryModel,
        startAxis = rememberStartAxis(),
        bottomAxis = rememberBottomAxis(
            valueFormatter = { value, _ ->
                val index = value.toInt()
                if (index in orderStats.indices) {
                    val dateStr = orderStats[index].orderDate
                     try {
                        val date = LocalDate.parse(dateStr)
                        date.format(DateTimeFormatter.ofPattern("dd/MM"))
                    } catch (e: Exception) {
                        dateStr
                    }
                } else {
                    ""
                }
            }
        ),
        modifier = modifier
    )
}
