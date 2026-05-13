package com.example.oneorder_sm.ui.screens.foodpromotion

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontStyle
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.oneorder_sm.ui.screens.staff.CreateStaffDialog
import com.example.oneorder_sm.ui.screens.staff.SuccessDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPromotionScreen(
    viewModel: FoodPromotionViewModel = hiltViewModel(),
    onNavigateToEditProfile: () -> Unit = {},
    onNavigateToCreatePost: () -> Unit = {},
    onNavigateToPostDetail: (String) -> Unit = {},
    onNavigateToFeedView: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var isRefreshing by remember { mutableStateOf(false) }

    val pullToRefreshState = rememberPullToRefreshState()

    // Handle pull-to-refresh
    LaunchedEffect(uiState.posts) {
        if (isRefreshing) {
            isRefreshing = false
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var viewAllPosts by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                viewModel.refreshPosts()
            },
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize()
        ) {
            if (uiState.isLoading && uiState.restaurantName.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Header item
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                        
                        // ── Cover Photo & Header ───────────────────────────────────────────────
                        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                            // Cover Image
                            if (uiState.coverUrl != null) {
                                AsyncImage(
                                    model = uiState.coverUrl,
                                    contentDescription = "Cover photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                // Fallback gradient
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            brush = Brush.verticalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                                    MaterialTheme.colorScheme.primary
                                                )
                                            )
                                        )
                                )
                            }
                            
                            // Dark overlay at bottom for text readability
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(80.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                                        )
                                    )
                            )
                            
                            // Avatar & Info Overlay
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.BottomStart)
                                    .padding(start = 16.dp, bottom = 12.dp, end = 16.dp),
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Avatar
                                Box(
                                    modifier = Modifier
                                        .size(86.dp)
                                        .border(3.dp, MaterialTheme.colorScheme.surface, CircleShape)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (uiState.avatarUrl != null) {
                                        AsyncImage(
                                            model = uiState.avatarUrl,
                                            contentDescription = "Logo",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                                    }
                                    
                                    // Verified badge
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .size(24.dp)
                                            .background(MaterialTheme.colorScheme.surface, CircleShape)
                                            .padding(2.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Verified, 
                                            contentDescription = null, 
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Name and ID
                                Column(modifier = Modifier.padding(bottom = 4.dp)) {
                                    Text(
                                        text = uiState.restaurantName.ifEmpty { "Nhà hàng" },
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val rawId = uiState.restaurantId.ifEmpty { "nhahang" }
                                    Text(
                                        text = "@$rawId",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Stats ───────────────────────────────────────────────
                        val totalLikes = uiState.posts.sumOf { it.likeCount }
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProfileStatItem("Bài đăng", formatCount(uiState.posts.size))
                                Box(modifier = Modifier.height(30.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                                ProfileStatItem("Theo dõi", formatCount(uiState.followers))
                                Box(modifier = Modifier.height(30.dp).width(1.dp).background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)))
                                ProfileStatItem("Lượt thích", formatCount(totalLikes))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // ── Contact & Bio Card ──────────────────────────────────────────
                        val descriptionText = uiState.description.ifEmpty { uiState.bio } // fallback to bio
                        if (uiState.address.isNotEmpty() || uiState.phone.isNotEmpty() || descriptionText.isNotEmpty()) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                ),
                                elevation = CardDefaults.cardElevation(0.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp).animateContentSize(), 
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    if (uiState.address.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.LocationOn, 
                                                contentDescription = "Address",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = uiState.address,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    if (uiState.phone.isNotEmpty()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Outlined.Phone, 
                                                contentDescription = "Phone",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = uiState.phone,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    
                                    if (descriptionText.isNotEmpty()) {
                                        if (uiState.address.isNotEmpty() || uiState.phone.isNotEmpty()) {
                                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                        }
                                        
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            val maxChars = 100
                                            val isLong = descriptionText.length > maxChars
                                            val displayText = if (isLong && !isDescriptionExpanded) {
                                                descriptionText.take(maxChars) + "..."
                                            } else {
                                                descriptionText
                                            }

                                            Text(
                                                text = displayText,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.fillMaxWidth(),
                                                textAlign = when (uiState.descriptionAlignment) {
                                                    "CENTER" -> TextAlign.Center
                                                    "RIGHT" -> TextAlign.Right
                                                    else -> TextAlign.Left
                                                }
                                            )
                                            
                                            if (isLong) {
                                                Text(
                                                    text = if (isDescriptionExpanded) "Ẩn bớt" else "Xem thêm",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(top = 8.dp)
                                                        .clickable { isDescriptionExpanded = !isDescriptionExpanded },
                                                    textAlign = TextAlign.Center
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // ── Posts Header ────────────────────────────────────────────────────
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Posts",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Nút Play đã được gỡ bỏ theo yêu cầu
                                if (uiState.posts.size > 8) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { viewAllPosts = !viewAllPosts }
                                    ) {
                                        Text(
                                            text = if (viewAllPosts) "Show Less" else "View All",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            if (viewAllPosts) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } // End of Header Item

                // ── Posts Grid Items ────────────────────────────────────────────────────
                val displayPosts = if (viewAllPosts || uiState.posts.size <= 8) uiState.posts else uiState.posts.take(8)

                itemsIndexed(displayPosts, key = { _, post -> post.id }) { index, post ->
                    val firstImage = post.images.firstOrNull()
                    val actualIndex = uiState.posts.indexOf(post)
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable {
                                viewModel.setFeedStartIndex(actualIndex)
                                onNavigateToFeedView()
                            }
                    ) {
                        AsyncImage(
                            model = firstImage?.url,
                            contentDescription = "Ảnh bài đăng",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Image count badge if multiple images
                        if (post.images.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("${post.images.size}", color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }

                        // Likes badge overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.FavoriteBorder, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(formatCount(post.likeCount), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }

                        // Inactive overlay
                        if (!post.isActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = Color.White)
                            }
                        }
                    }
                }
                
                // Add Button as the last item - always visible for managers
                item {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .clickable { onNavigateToCreatePost() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Thêm bài đăng",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            } // End of LazyVerticalGrid
        }

        // Snackbar
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
    }

    // Add Staff dialog ──────────────────────────────────────────────────────
    if (uiState.showAddStaffDialog) {
        CreateStaffDialog(
            onDismiss = { viewModel.hideAddStaffDialog() },
            onCreate = { email, fullName, phone, role ->
                viewModel.createStaff(email, fullName, phone, role)
            }
        )
    }

    // ── Success: show temp credentials ────────────────────────────────────────
    if (uiState.tempPassword != null) {
        SuccessDialog(
            email = uiState.createdEmail ?: "",
            password = uiState.tempPassword!!,
            onDismiss = { viewModel.clearTempPassword() }
        )
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun ProfileStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// EditRestaurantDialog has been removed since it is now its own screen (EditRestaurantProfileScreen.kt)

@Composable
internal fun EmptyPromotionState(onAdd: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "Tạo bài đăng đầu tiên",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Thêm chút gì đó cho không gian này đi",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            TextButton(onClick = onAdd) {
                Text(
                    text = "Tạo",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 10_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}
