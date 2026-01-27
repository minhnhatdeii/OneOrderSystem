package com.example.oneorder_sm.ui.screens.menu

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.oneorder_sm.data.model.MenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuManagementScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: MenuManagementViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showAddEditItemDialog by remember { mutableStateOf(false) }
    var showAddEditCategoryDialog by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<MenuItem?>(null) }

    // Content only - no Scaffold/TopAppBar (handled by MainScreen)
    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Action Row for Category
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Danh mục",
                    style = MaterialTheme.typography.titleSmall
                )
                TextButton(onClick = { showAddEditCategoryDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Thêm danh mục")
                }
            }

            // Category Tabs
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.categories) { category ->
                    CategoryChip(
                        category = category,
                        isSelected = uiState.selectedCategory?.id == category.id,
                        onClick = { viewModel.selectCategory(category) },
                        onLongClick = {
                            // Show delete confirmation
                            viewModel.showDeleteCategoryDialog(category)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (uiState.error != null) {
                Text(
                    text = uiState.error!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                if (uiState.menuItems.isEmpty() && uiState.selectedCategory != null) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Chưa có món nào trong \"${uiState.selectedCategory!!.name}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nhấn nút + để thêm món mới",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(uiState.menuItems) { item ->
                            MenuItemRow(
                                item = item,
                                onClick = {
                                    selectedItem = item
                                    showAddEditItemDialog = true
                                },
                                onToggleAvailability = {
                                    item.id?.let { id ->
                                        viewModel.toggleItemAvailability(id, item.isAvailable)
                                    }
                                },
                                onDelete = {
                                    item.id?.let { id ->
                                        viewModel.deleteMenuItem(id)
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                selectedItem = null
                showAddEditItemDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Thêm món")
        }

        // Add/Edit Item Dialog
        if (showAddEditItemDialog) {
            AddEditMenuItemDialog(
                item = selectedItem,
                categoryId = selectedItem?.categoryId ?: uiState.selectedCategory?.id ?: 0L,
                onDismiss = {
                    showAddEditItemDialog = false
                    selectedItem = null
                },
                onSave = { name, price, desc, imgBytes ->
                    val categoryId = selectedItem?.categoryId ?: uiState.selectedCategory?.id ?: 0L
                    viewModel.saveMenuItem(
                        selectedItem?.id,
                        name,
                        price,
                        desc,
                        categoryId,
                        imgBytes,
                        selectedItem?.imageUrl
                    )
                    showAddEditItemDialog = false
                    selectedItem = null
                }
            )
        }

        // Add/Edit Category Dialog
        if (showAddEditCategoryDialog) {
            AddEditCategoryDialog(
                onDismiss = { showAddEditCategoryDialog = false },
                onSave = { name ->
                    viewModel.saveCategory(
                        id = null,
                        name = name
                    )
                    showAddEditCategoryDialog = false
                }
            )
        }

        // Delete Category Confirmation Dialog
        if (uiState.categoryToDelete != null) {
            AlertDialog(
                onDismissRequest = { viewModel.dismissDeleteCategoryDialog() },
                title = { Text("Xóa danh mục?") },
                text = { 
                    Text("Bạn có chắc muốn xóa danh mục \"${uiState.categoryToDelete!!.name}\"?\n\nLưu ý: Các món ăn trong danh mục này sẽ không bị xóa.") 
                },
                confirmButton = {
                    Button(
                        onClick = {
                            uiState.categoryToDelete!!.id?.let { id ->
                                viewModel.deleteCategory(id)
                            }
                            viewModel.dismissDeleteCategoryDialog()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Xóa")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissDeleteCategoryDialog() }) {
                        Text("Hủy")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryChip(
    category: com.example.oneorder_sm.data.model.Category,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = FilterChipDefaults.shape,
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        border = FilterChipDefaults.filterChipBorder(enabled = true, selected = isSelected)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category.name,
                style = MaterialTheme.typography.labelLarge
            )
            // Delete icon button (always visible for discoverability)
            IconButton(
                onClick = onLongClick,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Xóa danh mục",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuItemRow(
    item: MenuItem,
    onClick: () -> Unit,
    onToggleAvailability: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onDelete
            )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Fixed size image with aspect ratio
            Card(
                modifier = Modifier.size(80.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${String.format("%,.0f", item.price)} VNĐ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (!item.description.isNullOrBlank()) {
                    Text(
                        item.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
            }
            Switch(
                checked = item.isAvailable,
                onCheckedChange = { onToggleAvailability() }
            )
        }
    }
}

@Composable
fun AddEditMenuItemDialog(
    item: MenuItem?,
    categoryId: Long,
    onDismiss: () -> Unit,
    onSave: (String, Double, String?, ByteArray?) -> Unit
) {
    var name by remember { mutableStateOf(item?.name ?: "") }
    var priceStr by remember { mutableStateOf(item?.price?.toString() ?: "") }
    var description by remember { mutableStateOf(item?.description ?: "") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        imageUri = uri
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (item == null) "Thêm Món" else "Sửa Món") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Tên món") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = priceStr,
                    onValueChange = { priceStr = it },
                    label = { Text("Giá (VNĐ)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả (tùy chọn)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                // Image selection button with preview
                Column(modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { launcher.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (imageUri != null) "✓ Đã chọn ảnh mới" 
                            else if (item?.imageUrl != null) "Thay đổi ảnh hiện tại"
                            else "Chọn ảnh món"
                        )
                    }
                    // Show current image indicator
                    if (item?.imageUrl != null && imageUri == null) {
                        Text(
                            text = "✓ Món này đã có ảnh",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val price = priceStr.toDoubleOrNull() ?: 0.0
                    val bytes = imageUri?.let { uri ->
                        context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    }
                    onSave(name, price, description, bytes)
                },
                enabled = name.isNotBlank() && (priceStr.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
