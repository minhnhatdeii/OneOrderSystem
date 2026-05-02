package com.example.oneorder_sm.ui.screens.foodpromotion

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.oneorder_sm.data.model.Category
import com.example.oneorder_sm.data.model.MenuItem
import java.io.ByteArrayOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateFoodPostScreen(
    onNavigateBack: () -> Unit,
    viewModel: CreateFoodPostViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var caption by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var selectedMenuItem by remember { mutableStateOf<MenuItem?>(null) }

    // Images
    var showImageSourceDialog by remember { mutableStateOf(false) }
    val selectedImageUris = remember { mutableStateListOf<Uri>() }
    val cameraBitmaps = remember { mutableStateListOf<Bitmap>() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        selectedImageUris.addAll(uris)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            cameraBitmaps.add(bitmap)
        }
    }

    LaunchedEffect(uiState.postSuccess) {
        if (uiState.postSuccess) {
            onNavigateBack()
        }
    }

    val hasImages = selectedImageUris.isNotEmpty() || cameraBitmaps.isNotEmpty()
    val isPostEnabled = hasImages && selectedMenuItem != null && !uiState.isPosting

    fun performPost() {
        if (isPostEnabled && selectedMenuItem != null) {
            val byteArrayList = mutableListOf<ByteArray>()
            selectedImageUris.forEach { uri ->
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) byteArrayList.add(bytes)
            }
            cameraBitmaps.forEach { bitmap ->
                val stream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
                byteArrayList.add(stream.toByteArray())
            }
            
            // Generate tags based on selected category name
            val tags = mutableListOf<String>()
            uiState.categories.find { it.id == selectedMenuItem!!.categoryId }?.let { cat ->
                tags.add(cat.name)
            }

            viewModel.createPost(
                menuItemId = selectedMenuItem!!.id!!,
                caption = caption,
                tags = tags,
                imageBytesList = byteArrayList
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Tạo bài đăng", 
                        color = MaterialTheme.colorScheme.primary, 
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.Close, contentDescription = "Trở về", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                actions = {
                    TextButton(
                        onClick = { performPost() },
                        enabled = isPostEnabled
                    ) {
                        Text("Đăng", color = if (isPostEnabled) MaterialTheme.colorScheme.primary else Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Button(
                    onClick = { performPost() },
                    enabled = isPostEnabled,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(25.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    if (uiState.isPosting) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Đăng bài", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // --- Photo Upload Box ---
                    val dashColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    val bgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .border(2.dp, dashColor, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgColor)
                            .clickable { showImageSourceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Thêm ảnh",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Nhấn để thêm ảnh",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // --- Selected Photos Row ---
                    if (hasImages) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(selectedImageUris) { uri ->
                                Box(modifier = Modifier.size(140.dp)) {
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { selectedImageUris.remove(uri) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .background(Color.Black.copy(alpha=0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            items(cameraBitmaps) { bitmap ->
                                Box(modifier = Modifier.size(140.dp)) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { cameraBitmaps.remove(bitmap) },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(22.dp)
                                            .background(Color.Black.copy(alpha=0.5f), CircleShape)
                                    ) {
                                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    // --- Caption ---
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgColor)
                            .padding(16.dp)
                            .heightIn(min = 80.dp)
                    ) {
                        if (caption.isEmpty()) {
                            Text("Viết mô tả...", color = Color.Gray)
                        }
                        BasicTextField(
                            value = caption,
                            onValueChange = { caption = it },
                            modifier = Modifier.fillMaxSize(),
                            textStyle = TextStyle(color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                        )
                    }

                    // --- Tag Dishes Title ---
                    Text(
                        "Thêm vào bài đăng: Gắn thẻ món ăn",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    // --- Categories Row ---
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // "All" chip
                        item {
                            val isSelected = selectedCategoryId == null
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable { selectedCategoryId = null }
                            ) {
                                Text(
                                    "Tất cả",
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        items(uiState.categories) { cat ->
                            val isSelected = selectedCategoryId == cat.id
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                modifier = Modifier.clickable { selectedCategoryId = cat.id }
                            ) {
                                Text(
                                    cat.name,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }

                    // --- Menu Items List ---
                    val filteredItems = if (selectedCategoryId == null) {
                        uiState.menuItems
                    } else {
                        uiState.menuItems.filter { it.categoryId == selectedCategoryId }
                    }

                    if (filteredItems.isEmpty()) {
                        Text("Không có món nào trong danh mục này", color = Color.Gray, modifier = Modifier.padding(vertical = 16.dp))
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            filteredItems.forEach { item ->
                                val isSelected = selectedMenuItem?.id == item.id
                                val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                val bgBox = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.05f) else Color.Transparent

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                                        .background(bgBox)
                                        .clickable { selectedMenuItem = item }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Item Image
                                    if (item.imageUrl != null) {
                                        AsyncImage(
                                            model = item.imageUrl,
                                            contentDescription = null,
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)).background(Color.LightGray),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray)
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    // Item Info
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(item.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                        Text("${item.price} VND", color = Color.Gray, fontSize = 14.sp)
                                    }
                                    
                                    // Checkbox / Tick
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .border(if (isSelected) 0.dp else 1.dp, if (isSelected) Color.Transparent else Color.Gray, RoundedCornerShape(4.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom button
                }
            }

            if (showImageSourceDialog) {
                AlertDialog(
                    onDismissRequest = { showImageSourceDialog = false },
                    title = { Text("Chọn nguồn ảnh", fontWeight = FontWeight.Bold) },
                    text = { Text("Bạn muốn lấy ảnh từ đâu?") },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                showImageSourceDialog = false
                                galleryLauncher.launch("image/*")
                            }
                        ) {
                            Text("Thư viện", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showImageSourceDialog = false
                                cameraLauncher.launch(null)
                            }
                        ) {
                            Text("Chụp ảnh", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }

            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp).padding(bottom = 60.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) { Text("OK", color = MaterialTheme.colorScheme.inversePrimary) }
                    }
                ) {
                    Text(uiState.error!!)
                }
            }
        }
    }
}
