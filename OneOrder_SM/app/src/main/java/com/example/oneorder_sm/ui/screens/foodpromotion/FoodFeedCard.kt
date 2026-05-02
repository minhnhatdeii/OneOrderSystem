package com.example.oneorder_sm.ui.screens.foodpromotion

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun FoodFeedCard(
    post: FoodPost,
    restaurantName: String,
    restaurantAvatar: String?,
    onActionClick: () -> Unit
) {
    val context = LocalContext.current
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.likeCount) }

    val heartScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLiked) 1.2f else 1f,
        animationSpec = androidx.compose.animation.core.spring(
            dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
            stiffness = androidx.compose.animation.core.Spring.StiffnessLow
        )
    )

    val palette = rememberDominantPalette(
        imageUrl = post.images.firstOrNull()?.url ?: "",
        isDark = false
    )

    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            palette.background.copy(alpha = 0.15f),
            palette.background.copy(alpha = 0.4f)
        )
    )
    val onBgColor = palette.onBackground
    val accentColor = palette.accent

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        // Food image
        val firstImageUrl = post.images.firstOrNull()?.url
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(firstImageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Food Image",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Inactive overlay
        if (!post.isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.VisibilityOff,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Bài đăng đang bị ẩn", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Gradient for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                    )
                )
        )

        // Content
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 16.dp, start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Left: info
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
            ) {
                // Restaurant row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        if (restaurantAvatar != null) {
                            AsyncImage(
                                model = restaurantAvatar,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.Store,
                                null,
                                modifier = Modifier
                                    .align(Alignment.Center)
                                    .padding(4.dp),
                                tint = Color.White
                            )
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = restaurantName,
                        color = onBgColor,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Food name
                Text(
                    text = post.menuItemName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = onBgColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                // Price
                Text(
                    text = "${"%,.0f".format(post.price)} đ",
                    style = MaterialTheme.typography.titleLarge,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Caption
                if (post.caption.isNotEmpty()) {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = onBgColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(bottom = 12.dp, top = 8.dp)
                    )
                }

                // Category chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = post.categoryName,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White
                    )
                }
            }

            // Right: action buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(bottom = 16.dp, start = 8.dp)
            ) {
                // Manage button (3-dot)
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.Black.copy(alpha = 0.4f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Quản lý",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Like (visual indicator only — managers don't need to like their own posts)
                FeedActionBtn(
                    isActive = isLiked,
                    count = likeCount,
                    activeIcon = Icons.Filled.Favorite,
                    inactiveIcon = Icons.Filled.FavoriteBorder,
                    activeColor = Color.Red,
                    inactiveColor = Color.White,
                    bgTint = Color.Black.copy(alpha = 0.35f),
                    onClick = {
                        isLiked = !isLiked
                        likeCount = if (isLiked) likeCount + 1 else likeCount - 1
                    },
                    iconScale = heartScale
                )

                // Comment count display
                FeedActionBtn(
                    isActive = false,
                    count = post.commentCount,
                    activeIcon = Icons.Filled.ChatBubble,
                    inactiveIcon = Icons.Filled.ChatBubble,
                    activeColor = accentColor,
                    inactiveColor = Color.White,
                    bgTint = Color.Black.copy(alpha = 0.35f),
                    onClick = { }
                )
            }
        }
    }
}

@Composable
fun FeedActionBtn(
    isActive: Boolean,
    count: Int,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    inactiveColor: Color,
    bgTint: Color = Color.Transparent,
    iconScale: Float = 1f,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        val color by animateColorAsState(
            targetValue = if (isActive) activeColor else inactiveColor
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(bgTint)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isActive) activeIcon else inactiveIcon,
                contentDescription = null,
                tint = color,
                modifier = Modifier
                    .size(28.dp)
                    .scale(iconScale)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatFeedCount(count),
            color = Color.White,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.5f),
                    blurRadius = 4f
                )
            )
        )
    }
}

private fun formatFeedCount(count: Int): String = when {
    count >= 1_000_000 -> String.format("%.1fM", count / 1_000_000.0)
    count >= 10_000 -> String.format("%.1fK", count / 1_000.0)
    else -> count.toString()
}


