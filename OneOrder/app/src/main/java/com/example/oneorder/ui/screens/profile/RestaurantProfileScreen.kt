package com.example.oneorder.ui.screens.profile

import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.oneorder.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantProfileScreen(
    restaurantId: String,
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (String, String) -> Unit,
    viewModel: RestaurantProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    var viewAllPosts by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(restaurantId) {
        viewModel.loadProfile(restaurantId)
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error,
                duration = SnackbarDuration.Short
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.isLoading && uiState.name.isEmpty()) {
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
                            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
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
                                                colors = listOf(
                                                    Color.Transparent,
                                                    Color.Black.copy(alpha = 0.7f)
                                                )
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
                                            .border(
                                                3.dp,
                                                MaterialTheme.colorScheme.surface,
                                                CircleShape
                                            )
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
                                            Icon(
                                                Icons.Default.Store,
                                                contentDescription = null,
                                                modifier = Modifier.size(40.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }

                                        // Verified badge
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomEnd)
                                                .size(24.dp)
                                                .background(
                                                    MaterialTheme.colorScheme.surface,
                                                    CircleShape
                                                )
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
                                            text = uiState.name.ifEmpty { "Nhà hàng" },
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
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                        alpha = 0.5f
                                    )
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
                                    ProfileStatItem(
                                        stringResource(R.string.stats_posts),
                                        formatCount(uiState.stats.totalPosts)
                                    )
                                    Box(
                                        modifier = Modifier.height(30.dp).width(1.dp).background(
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                    ProfileStatItem(
                                        stringResource(R.string.stats_followers),
                                        formatCount(uiState.stats.followers)
                                    )
                                    Box(
                                        modifier = Modifier.height(30.dp).width(1.dp).background(
                                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    )
                                    ProfileStatItem(
                                        stringResource(R.string.stats_likes),
                                        formatCount(uiState.stats.likes)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Contact & Bio Card ──────────────────────────────────────────
                            val descriptionText =
                                uiState.description.ifEmpty { uiState.bio } // fallback to bio
                            if (uiState.address.isNotEmpty() || uiState.phone.isNotEmpty() || descriptionText.isNotEmpty()) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                                            alpha = 0.5f
                                        )
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
                                                HorizontalDivider(
                                                    modifier = Modifier.padding(
                                                        vertical = 4.dp
                                                    ),
                                                    color = MaterialTheme.colorScheme.outlineVariant.copy(
                                                        alpha = 0.5f
                                                    )
                                                )
                                            }

                                            Column(modifier = Modifier.fillMaxWidth()) {
                                                val maxChars = 100
                                                val isLong = descriptionText.length > maxChars
                                                val displayText =
                                                    if (isLong && !isDescriptionExpanded) {
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
                                                            .clickable {
                                                                isDescriptionExpanded =
                                                                    !isDescriptionExpanded
                                                            },
                                                        textAlign = TextAlign.Center
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // ── Action Buttons (Follow / Location / Share) ──────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = { viewModel.toggleFollow() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (uiState.isFollowing)
                                            MaterialTheme.colorScheme.primaryContainer
                                        else
                                            MaterialTheme.colorScheme.primary,
                                        contentColor = if (uiState.isFollowing)
                                            MaterialTheme.colorScheme.onPrimaryContainer
                                        else
                                            Color.White
                                    )
                                ) {
                                    if (uiState.isFollowing) {
                                        Text(
                                            text = "Đang theo dõi",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.feed_follow),
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                // Vị trí Button
                                if (uiState.isFollowing) {
                                    OutlinedButton(
                                        onClick = { /* Vị trí */ },
                                        modifier = Modifier.size(44.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(
                                            Icons.Outlined.LocationOn,
                                            contentDescription = "Vị trí",
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { /* Vị trí */ },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "Vị trí",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelLarge
                                        )
                                    }
                                }

                                OutlinedButton(
                                    onClick = { /* Share */ },
                                    modifier = Modifier.size(44.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Share,
                                        contentDescription = stringResource(R.string.feed_share),
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            // ── Posts Header ────────────────────────────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Posts",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (uiState.postsGrid.size > 8) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable {
                                            viewAllPosts = !viewAllPosts
                                        }
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
                    } // End of Header Item

                    // ── Posts Grid Items ────────────────────────────────────────────────────
                    val displayPosts =
                        if (viewAllPosts || uiState.postsGrid.size <= 8) uiState.postsGrid else uiState.postsGrid.take(
                            8
                        )

                    items(displayPosts, key = { it.id }) { post ->
                        Box(
                            modifier = Modifier
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { onNavigateToPostDetail(uiState.restaurantId, post.id) }
                        ) {
                            AsyncImage(
                                model = post.imageUrl,
                                contentDescription = "Ảnh bài đăng",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            // Likes badge overlay
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Outlined.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    formatCount(post.likeCount),
                                    color = Color.White,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Floating Back Button
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 16.dp, top = 8.dp)
                    .background(Color.Black.copy(alpha = 0.4f), CircleShape)
            ) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    tint = Color.White
                )
            }
        }
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

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 10_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}

private fun String.toIdString(): String {
    if (this.isEmpty()) return "@nhahang"
    val normalized = java.text.Normalizer.normalize(this, java.text.Normalizer.Form.NFD)
    return "@" + normalized.replace("\\p{Mn}+".toRegex(), "").replace("đ", "d").replace("Đ", "D").replace(" ", "").lowercase()
}
