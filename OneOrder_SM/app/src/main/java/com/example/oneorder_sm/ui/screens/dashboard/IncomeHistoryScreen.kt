package com.example.oneorder_sm.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder_sm.data.model.Order
import com.example.oneorder_sm.data.model.OrderStatistic
import com.example.oneorder_sm.data.model.OrderStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeHistoryScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit = {},
    viewModel: IncomeHistoryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currencyFormat = remember { NumberFormat.getCurrencyInstance(Locale("vi", "VN")) }

    // Calendar picker state
    var showDatePicker by remember { mutableStateOf(false) }

    // Date picker dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
                .atStartOfDay(java.time.ZoneId.of("UTC"))
                .toInstant()
                .toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val selectedDate = java.time.Instant.ofEpochMilli(millis)
                                .atZone(java.time.ZoneId.of("UTC"))
                                .toLocalDate()
                            viewModel.selectDateFromCalendar(
                                selectedDate.year,
                                selectedDate.monthValue,
                                selectedDate.dayOfMonth
                            )
                        }
                        showDatePicker = false
                    }
                ) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Hủy") }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Chọn ngày",
                        modifier = Modifier.padding(PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp))
                    )
                },
                headline = {
                    val millis = datePickerState.selectedDateMillis ?: uiState.selectedDate.atStartOfDay(java.time.ZoneId.of("UTC")).toInstant().toEpochMilli()
                    val date = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.of("UTC")).toLocalDate()
                    
                    val dayOfWeek = when (date.dayOfWeek.value) {
                        1 -> "Thứ 2"
                        2 -> "Thứ 3"
                        3 -> "Thứ 4"
                        4 -> "Thứ 5"
                        5 -> "Thứ 6"
                        6 -> "Thứ 7"
                        7 -> "Chủ nhật"
                        else -> ""
                    }
                    val dateStr = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    
                    Text(
                        text = "$dayOfWeek, $dateStr",
                        modifier = Modifier.padding(PaddingValues(start = 24.dp, end = 12.dp, bottom = 12.dp)),
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử thu nhập") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Month Revenue Hero Card
                item {
                    MonthRevenueCard(
                        monthLabel = getVietnameseMonth(uiState.currentMonth.monthValue),
                        revenue = uiState.monthRevenue,
                        currencyFormat = currencyFormat,
                        monthStats = uiState.monthStats
                    )
                }

                // Two Metric Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MetricCard(
                            icon = Icons.Default.Receipt,
                            title = "TB/Đơn",
                            value = currencyFormat.format(uiState.avgTicket),
                            modifier = Modifier.weight(1f)
                        )
                        MetricCard(
                            icon = Icons.Default.People,
                            title = "Tổng đơn hàng",
                            value = "${uiState.monthOrderCount}",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Daily Performance Section
                item {
                    DailyPerformanceSection(
                        dayPerformances = uiState.dayPerformances,
                        selectedDate = uiState.selectedDate,
                        onDaySelected = { viewModel.selectDate(it) },
                        onCalendarClick = { showDatePicker = true }
                    )
                }

                // Order History Header
                item {
                    val selectedDateStr = uiState.selectedDate.format(
                        DateTimeFormatter.ofPattern("dd/MM")
                    )
                    val dayOfWeekStr = when (uiState.selectedDate.dayOfWeek.value) {
                        1 -> "Thứ 2"
                        2 -> "Thứ 3"
                        3 -> "Thứ 4"
                        4 -> "Thứ 5"
                        5 -> "Thứ 6"
                        6 -> "Thứ 7"
                        7 -> "Chủ nhật"
                        else -> ""
                    }
                    Text(
                        text = "Lịch sử đơn hàng ($dayOfWeekStr, $selectedDateStr)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                // Loading orders
                if (uiState.isLoadingOrders) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }

                // Orders list
                if (uiState.selectedDateOrders.isEmpty() && !uiState.isLoadingOrders) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F7F9)
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Không có đơn hàng nào trong ngày này.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(uiState.selectedDateOrders) { order ->
                    HistoryOrderCard(
                        order = order,
                        currencyFormat = currencyFormat,
                        onClick = { onNavigateToOrderDetail(order.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthRevenueCard(
    monthLabel: String,
    revenue: Double,
    currencyFormat: NumberFormat,
    monthStats: List<OrderStatistic>
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF228BE2),
                            Color(0xFF1565C0)
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DOANH THU $monthLabel",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currencyFormat.format(revenue),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniBarChart(
    stats: List<OrderStatistic>,
    modifier: Modifier = Modifier
) {
    val maxRevenue = stats.maxOfOrNull { it.totalRevenue } ?: 1.0

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        stats.forEachIndexed { index, stat ->
            val heightFraction = if (maxRevenue > 0) (stat.totalRevenue / maxRevenue).toFloat() else 0f
            val isLast = index == stats.lastIndex

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction.coerceAtLeast(0.05f))
                    .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                    .background(
                        if (isLast) Color.White
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F7F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DailyPerformanceSection(
    dayPerformances: List<DayPerformance>,
    selectedDate: LocalDate,
    onDaySelected: (LocalDate) -> Unit,
    onCalendarClick: () -> Unit
) {
    Column {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hiệu suất ngày",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onCalendarClick) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Xem lịch",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day chips (scrollable)
        val listState = rememberLazyListState()

        // Auto-scroll to selected date on first load
        LaunchedEffect(dayPerformances.size) {
            val selectedIndex = dayPerformances.indexOfFirst { it.date == selectedDate }
            if (selectedIndex >= 0) {
                listState.animateScrollToItem(selectedIndex)
            }
        }

        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            itemsIndexed(dayPerformances) { _, dayPerf ->
                DayChip(
                    dayPerformance = dayPerf,
                    isSelected = dayPerf.date == selectedDate,
                    onClick = { onDaySelected(dayPerf.date) }
                )
            }
        }
    }
}

@Composable
private fun DayChip(
    dayPerformance: DayPerformance,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color(0xFFF5F7F9)
    }
    val contentColor = if (isSelected) {
        Color.White
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val subtitleColor = if (isSelected) {
        Color.White.copy(alpha = 0.8f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = dayPerformance.dayLabel,
                style = MaterialTheme.typography.labelSmall,
                color = subtitleColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${dayPerformance.dayOfMonth}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dayPerformance.revenueLabel,
                style = MaterialTheme.typography.labelSmall,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HistoryOrderCard(
    order: Order,
    currencyFormat: NumberFormat,
    onClick: () -> Unit
) {
    val orderNumber = order.id.takeLast(4).uppercase()
    val locationText = order.tableName ?: "Mang đi"

    // Extract time from createdAt
    val timeText = try {
        val instant = java.time.Instant.parse(order.createdAt)
        val localTime = instant.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        localTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    } catch (e: Exception) {
        ""
    }

    // Table number badge
    val tableBadge = order.tableName?.replace("Bàn ", "")?.replace("bàn ", "")?.trim() ?: "TD"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F7F9)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Table badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = tableBadge,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Order details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "#$orderNumber · $timeText",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = locationText,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Total amount
            Text(
                text = currencyFormat.format(order.totalAmount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.width(8.dp))

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun getVietnameseMonth(month: Int): String {
    return when (month) {
        1 -> "THÁNG 1"
        2 -> "THÁNG 2"
        3 -> "THÁNG 3"
        4 -> "THÁNG 4"
        5 -> "THÁNG 5"
        6 -> "THÁNG 6"
        7 -> "THÁNG 7"
        8 -> "THÁNG 8"
        9 -> "THÁNG 9"
        10 -> "THÁNG 10"
        11 -> "THÁNG 11"
        12 -> "THÁNG 12"
        else -> "THÁNG $month"
    }
}
