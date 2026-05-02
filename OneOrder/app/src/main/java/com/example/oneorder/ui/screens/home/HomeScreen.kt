package com.example.oneorder.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.oneorder.R
import com.example.oneorder.data.model.Category
import com.example.oneorder.data.model.MenuItem
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    onNavigateToMenu: (Long) -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToProfile: () -> Unit,
    onNavigateToQRScanner: () -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val squareSize = 16.dp.toPx()
                val strokeWidth = 1.dp.toPx()
                drawRect(color = Color.White)
                val gridColor = Color.Black.copy(alpha = 0.1f)
                var x = 0f
                while (x < size.width) {
                    drawLine(
                        color = gridColor,
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = strokeWidth
                    )
                    x += squareSize
                }
                var y = 0f
                while (y < size.height) {
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = strokeWidth
                    )
                    y += squareSize
                }
            }
    ) {
        // Custom Header
        HomeHeader(
            onNavigateBack = onNavigateBack,
            onScanQR = onNavigateToQRScanner
        )
        
        // Content
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.restaurant == null) {
                // Empty state - No QR scanned
                EmptyStatePrompt(onScanQR = onNavigateToQRScanner)
            } else {
                // Has restaurant data - Show menu with scroll-based categories
                val listState = androidx.compose.foundation.lazy.rememberLazyListState()
                val coroutineScope = rememberCoroutineScope()
                
                // Track which category is currently visible
                val firstVisibleItemIndex by remember {
                    derivedStateOf { listState.firstVisibleItemIndex }
                }
                
                // Calculate which category index is active based on scroll position
                val activeCategoryIndex by remember {
                    derivedStateOf {
                        if (uiState.categoryWithItems.isEmpty()) return@derivedStateOf -1
                        
                        var currentIndex = 1 // Start after RestaurantInfo (0)
                        var foundCategoryIndex = 0
                        
                        for (categoryIndex in uiState.categoryWithItems.indices) {
                            val categoryWithItems = uiState.categoryWithItems[categoryIndex]
                            val categoryHeaderIndex = currentIndex
                            val itemsCount = categoryWithItems.items.size
                            
                            // Check if first visible item is within this category's range
                            if (firstVisibleItemIndex >= categoryHeaderIndex && 
                                firstVisibleItemIndex < categoryHeaderIndex + itemsCount + 1) {
                                foundCategoryIndex = categoryIndex
                                break
                            }
                            
                            currentIndex += 1 + itemsCount // Header + items
                        }
                        
                        foundCategoryIndex
                    }
                }
                
                Column(modifier = Modifier.fillMaxSize()) {
                    // Categories horizontal scroll bar - STICKY at top
                    if (uiState.categories.isNotEmpty()) {
                        val categoryListState = androidx.compose.foundation.lazy.rememberLazyListState()
                        
                        LaunchedEffect(activeCategoryIndex) {
                            if (activeCategoryIndex >= 0) {
                                categoryListState.animateScrollToItem(activeCategoryIndex)
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shadowElevation = 0.dp,
                            color = Color.Transparent // Let tablecloth show through or use semi-transparent white
                        ) {
                            LazyRow(
                                state = categoryListState,
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(uiState.categories.size) { index ->
                                    val category = uiState.categories[index]
                                    CategoryChip(
                                        category = category,
                                        isSelected = index == activeCategoryIndex,
                                        onClick = {
                                            // Scroll to the category section
                                            coroutineScope.launch {
                                                // Calculate item index: RestaurantInfo (1) + previous categories
                                                var targetIndex = 1 // Start after restaurant info
                                                for (i in 0 until index) {
                                                    targetIndex++ // Category header
                                                    targetIndex += uiState.categoryWithItems.getOrNull(i)?.items?.size ?: 0
                                                }
                                                listState.animateScrollToItem(targetIndex)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Scrollable content - Restaurant info will scroll away
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Restaurant Info - will scroll away
                        item {
                            RestaurantInfoSection(
                                restaurantName = uiState.restaurant?.name ?: "",
                                restaurantAddress = uiState.restaurant?.address,
                                tableName = uiState.table?.name ?: ""
                            )
                        }
                        
                        // For each category, show header + items
                        uiState.categoryWithItems.forEach { categoryWithItems ->
                            // Category Header
                            item {
                                CategoryHeader(categoryWithItems.category)
                            }
                            
                            // Items in this category
                            items(categoryWithItems.items.size) { index ->
                                val item = categoryWithItems.items[index]
                                MenuItemCard(
                                    item = item,
                                    onAddToCart = { quantity ->
                                        viewModel.addToCart(item, quantity)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(
    onNavigateBack: () -> Unit,
    onScanQR: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shadowElevation = 0.dp,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            // Center: OneOrder logo
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold
                    )) { append("One") }
                    withStyle(style = SpanStyle(
                        color = Color(0xFF228BE2),
                        fontWeight = FontWeight.ExtraBold
                    )) { append("Order") }
                },
                style = MaterialTheme.typography.titleLarge
            )

            // Left: Back arrow
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Right: QR Scanner button
                IconButton(onClick = onScanQR) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.scan_qr),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyStatePrompt(onScanQR: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCodeScanner,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(120.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.welcome_oneorder),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(R.string.scan_qr_prompt),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onScanQR,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(stringResource(R.string.scan_qr), style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun RestaurantInfoSection(
    restaurantName: String,
    restaurantAddress: String?,
    tableName: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = restaurantName,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            restaurantAddress?.let { address ->
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.table_label, tableName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun CategoriesSection(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.categories),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(categories) { category ->
                CategoryChip(
                    category = category,
                    isSelected = category.id == selectedCategory?.id,
                    onClick = { onCategorySelected(category) }
                )
            }
        }
    }
}

@Composable
fun CategoryChip(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.height(40.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
fun CategoryHeader(category: Category) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

@Composable
fun MenuItemCard(
    item: MenuItem,
    onAddToCart: (Int) -> Unit  // Pass quantity
) {
    var showNoteDialog by remember { mutableStateOf(false) }
    var itemNote by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf(1) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Image
            AsyncImage(
                model = item.image_url,
                contentDescription = item.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentScale = ContentScale.Crop
            )
            
            // Info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                item.description?.let { desc ->
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Text(
                    text = com.example.oneorder.utils.CurrencyFormatter.formatVND(item.price),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            // Add button
            Button(
                onClick = { showNoteDialog = true },
                modifier = Modifier.height(40.dp)
            ) {
                Text(stringResource(R.string.add))
            }
        }
    }
    
    // Note dialog
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text(stringResource(R.string.add_to_cart)) },
            text = {
                Column {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Quantity selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.quantity),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Decrease button
                            IconButton(
                                onClick = { if (quantity > 1) quantity-- },
                                enabled = quantity > 1
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.decrease)
                                )
                            }
                            
                            // Quantity display
                            Text(
                                text = "$quantity",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.widthIn(min = 40.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            
                            // Increase button
                            IconButton(
                                onClick = { quantity++ }
                            ) {
                                Icon(
                                    imageVector = androidx.compose.material.icons.Icons.Default.Add,
                                    contentDescription = stringResource(R.string.increase)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Note input
                    OutlinedTextField(
                        value = itemNote,
                        onValueChange = { itemNote = it },
                        label = { Text(stringResource(R.string.note_optional)) },
                        placeholder = { Text(stringResource(R.string.note_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Pass quantity to cart
                        onAddToCart(quantity)
                        showNoteDialog = false
                        itemNote = ""
                        quantity = 1
                    }
                ) {
                    Text(stringResource(R.string.add_to_cart_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showNoteDialog = false
                    itemNote = ""
                    quantity = 1
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
