package com.example.oneorder_sm.ui.screens.foodpromotion

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodPromotionDetailScreen(
    postId: String,
    viewModel: FoodPromotionViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedPostForAction by remember { mutableStateOf<FoodPost?>(null) }
    var showDeleteDialog by remember { mutableStateOf<FoodPost?>(null) }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (uiState.posts.isNotEmpty()) {
            val initialPageIndex = uiState.posts.indexOfFirst { it.id == postId }.coerceAtLeast(0)
            val pagerState = rememberPagerState(
                initialPage = initialPageIndex,
                pageCount = { uiState.posts.size }
            )

            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val post = uiState.posts[page]
                FoodFeedCard(
                    post = post,
                    restaurantName = uiState.restaurantName,
                    restaurantAvatar = uiState.avatarUrl,
                    onActionClick = { selectedPostForAction = post }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Floating Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 8.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White
            )
        }
    }

    // Post Action Bottom Sheet
    if (selectedPostForAction != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedPostForAction = null }
        ) {
            val post = selectedPostForAction!!
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                ListItem(
                    headlineContent = { Text(post.menuItemName, fontWeight = FontWeight.Bold) },
                    supportingContent = {
                        Text(post.caption, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    },
                    leadingContent = {
                        AsyncImage(
                            model = post.images.firstOrNull()?.url,
                            contentDescription = null,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                )
                HorizontalDivider()
                ListItem(
                    headlineContent = {
                        Text(if (post.isActive) "Ẩn bài đăng này" else "Hiện bài đăng này")
                    },
                    leadingContent = {
                        Icon(
                            if (post.isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            null
                        )
                    },
                    modifier = Modifier.clickable {
                        viewModel.togglePostActive(post.id)
                        selectedPostForAction = null
                    }
                )
                ListItem(
                    headlineContent = {
                        Text("Xóa bài đăng", color = MaterialTheme.colorScheme.error)
                    },
                    leadingContent = {
                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                    },
                    modifier = Modifier.clickable {
                        showDeleteDialog = post
                        selectedPostForAction = null
                    }
                )
            }
        }
    }

    // Delete Confirmation
    if (showDeleteDialog != null) {
        val post = showDeleteDialog!!
        AlertDialog(
            onDismissRequest = { showDeleteDialog = null },
            icon = {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("Xóa bài đăng?") },
            text = {
                Text("Bài đăng \"${post.menuItemName}\" sẽ bị xóa khỏi feed. Hành động này không thể hoàn tác.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePost(post.id)
                        showDeleteDialog = null
                        if (uiState.posts.size <= 1) onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Xóa") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = null }) { Text("Hủy") }
            }
        )
    }
}
