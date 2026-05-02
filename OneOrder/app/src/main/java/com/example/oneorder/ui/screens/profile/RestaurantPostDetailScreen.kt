package com.example.oneorder.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.oneorder.ui.screens.foodfeed.FoodFeedCard
import com.example.oneorder.ui.screens.foodfeed.CommentBottomSheet

@Composable
fun RestaurantPostDetailScreen(
    tenantId: String,
    postId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRestaurantProfile: () -> Unit,
    viewModel: RestaurantPostDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(tenantId, postId) {
        viewModel.loadPosts(tenantId, postId)
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null) {
            Text(text = "Lỗi: ${uiState.error}", modifier = Modifier.align(Alignment.Center).padding(16.dp))
        } else if (uiState.posts.isNotEmpty()) {
            val pagerState = rememberPagerState(
                initialPage = uiState.initialPageIndex,
                pageCount = { uiState.posts.size }
            )
            
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val post = uiState.posts[page]
                FoodFeedCard(
                    post = post,
                    showDistance = false,
                    showMapButton = false,
                    onLike = { viewModel.toggleLike(post.id) },
                    onComment = { viewModel.openComments(post.id) },
                    onNavigateToRestaurantProfile = onNavigateToRestaurantProfile
                )
            }
        }

        // Floating Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.4f), CircleShape)
        ) {
            Icon(
                Icons.Default.ArrowBack,
                contentDescription = "Quay lại",
                tint = Color.White
            )
        }

        if (uiState.showCommentSheet && uiState.commentsForPostId != null) {
            CommentBottomSheet(
                postId = uiState.commentsForPostId!!,
                comments = uiState.comments,
                commentInput = uiState.commentInput,
                onCommentInputChange = { viewModel.updateCommentInput(it) },
                onSubmit = { viewModel.submitComment() },
                onDismiss = { viewModel.closeComments() }
            )
        }
    }
}
