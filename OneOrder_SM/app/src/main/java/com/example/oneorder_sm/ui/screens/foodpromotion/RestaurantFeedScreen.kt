package com.example.oneorder_sm.ui.screens.foodpromotion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.oneorder_sm.ui.theme.LocalDarkTheme

@Composable
fun RestaurantFeedScreen(
    viewModel: FoodPromotionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToPostDetail: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = LocalDarkTheme.current

    // Use the start index from state (set when user clicks a specific post)
    val startIndex = uiState.feedStartIndex.coerceIn(0, (uiState.posts.size - 1).coerceAtLeast(0))

    val verticalPagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { uiState.posts.size }
    )

    // Reset feed start index when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            viewModel.setFeedStartIndex(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isLoading && uiState.posts.isEmpty()) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.posts.isEmpty()) {
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.Restaurant,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Chưa có bài đăng nào",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            VerticalPager(
                state = verticalPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val post = uiState.posts[page]
                RestaurantFeedCard(
                    post = post,
                    restaurantName = uiState.restaurantName,
                    restaurantAvatar = uiState.avatarUrl,
                    isDark = isDark,
                    onLike = { viewModel.toggleLike(post.id) },
                    onComment = { viewModel.openComments(post.id) },
                    viewModel = viewModel
                )
            }
        }

        // Back button only (no logo, no profile, no order buttons)
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(12.dp)
                .size(40.dp)
                .background(Color.Black.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
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

@Composable
private fun RestaurantFeedCard(
    post: FoodPost,
    restaurantName: String,
    restaurantAvatar: String?,
    isDark: Boolean,
    onLike: () -> Unit,
    onComment: () -> Unit,
    viewModel: FoodPromotionViewModel
) {
    var showHeartAnim by remember { mutableStateOf(false) }
    var heartScale by remember { mutableFloatStateOf(0f) }
    var captionExpanded by remember { mutableStateOf(false) }

    val images = post.images.ifEmpty {
        listOf(PostImage(url = "", layout = "PORTRAIT"))
    }
    val imagePagerState = rememberPagerState(pageCount = { images.size })

    // Dynamic palette from current image
    val currentImageUrl = images.getOrNull(imagePagerState.currentPage)?.url ?: ""
    val palette = rememberDominantPalette(
        imageUrl = currentImageUrl,
        isDark = isDark
    )

    // Animate colors with smooth transition
    val animSpec = tween<Color>(durationMillis = 500)
    val bgColor by animateColorAsState(palette.background, animSpec, label = "bgColor")
    val bgEndColor by animateColorAsState(palette.backgroundEnd, animSpec, label = "bgEndColor")
    val onBgColor by animateColorAsState(palette.onBackground, animSpec, label = "onBgColor")
    val accentColor by animateColorAsState(palette.accent, animSpec, label = "accentColor")

    LaunchedEffect(showHeartAnim) {
        if (showHeartAnim) {
            heartScale = 1f
            kotlinx.coroutines.delay(700)
            heartScale = 0f
            showHeartAnim = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.0f to bgColor,
                        0.55f to bgColor.copy(alpha = 0.85f),
                        1.0f to bgEndColor
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Center: image card with action buttons
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 90.dp),
                contentAlignment = Alignment.Center
            ) {
                if (images.isEmpty() || images.first().url.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .aspectRatio(4f / 5f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(bgColor.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Restaurant,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    // Carousel images
                    HorizontalPager(
                        state = imagePagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) { idx ->
                        val feedImage = images[idx]

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .aspectRatio(getAspectRatio(feedImage.layout))
                                .clip(RoundedCornerShape(20.dp))
                                .background(bgColor.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (!post.isActive) onLike()
                                                showHeartAnim = true
                                            }
                                        )
                                    }
                            ) {
                                AsyncImage(
                                    model = feedImage.url,
                                    contentDescription = post.menuItemName,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            // Double-tap heart animation
                            if (showHeartAnim) {
                                val scale by animateFloatAsState(
                                    targetValue = heartScale,
                                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
                                    label = "heartAnim"
                                )
                                Icon(
                                    Icons.Default.Favorite, null,
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .size(80.dp)
                                        .scale(scale),
                                    tint = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }

                    // Image index badge (1/3)
                    if (images.size > 1) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 16.dp, end = 16.dp),
                            color = bgEndColor.copy(alpha = 0.65f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.PhotoLibrary, null,
                                    modifier = Modifier.size(12.dp),
                                    tint = onBgColor
                                )
                                Text(
                                    "${imagePagerState.currentPage + 1}/${images.size}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = onBgColor,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Dots indicator
                    if (images.size > 1) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(images.size) { dotIdx ->
                                val isCurrent = imagePagerState.currentPage == dotIdx
                                Box(
                                    modifier = Modifier
                                        .size(if (isCurrent) 8.dp else 6.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) onBgColor
                                            else onBgColor.copy(alpha = 0.4f)
                                        )
                                )
                            }
                        }
                    }
                }

                // Action buttons - TikTok style (right side)
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Avatar with follow button
                    Box(
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (restaurantAvatar != null) {
                                AsyncImage(
                                    model = restaurantAvatar,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.Store, null,
                                    tint = onBgColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Like button
                    RestaurantFeedActionBtn(
                        isActive = post.isLiked,
                        count = post.likeCount,
                        activeIcon = Icons.Filled.Favorite,
                        inactiveIcon = Icons.Filled.Favorite,
                        activeColor = Color(0xFFE91E63),
                        inactiveColor = Color.White,
                        bgTint = Color.Black.copy(alpha = 0.35f),
                        onClick = onLike
                    )

                    // Comment button
                    RestaurantFeedActionBtn(
                        isActive = false,
                        count = post.commentCount,
                        activeIcon = Icons.Filled.ChatBubble,
                        inactiveIcon = Icons.Filled.ChatBubble,
                        activeColor = accentColor,
                        inactiveColor = Color.White,
                        bgTint = Color.Black.copy(alpha = 0.35f),
                        onClick = onComment
                    )

                    // More options button
                    Box {
                        var showMenu by remember { mutableStateOf(false) }
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier
                                .size(48.dp)
                                .background(Color.Black.copy(alpha = 0.35f), CircleShape)
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Quản lý",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            val activeText = if (post.isActive) "Ẩn bài đăng" else "Hiện bài đăng"
                            val activeIcon = if (post.isActive) Icons.Default.VisibilityOff else Icons.Default.Visibility
                            DropdownMenuItem(
                                text = { Text(activeText) },
                                leadingIcon = { Icon(activeIcon, null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.togglePostActive(post.id)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xóa bài đăng") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                onClick = {
                                    showMenu = false
                                    viewModel.deletePost(post.id)
                                }
                            )
                        }
                    }
                }
            }

            // Bottom: food info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                bgEndColor.copy(alpha = 0.75f),
                                bgEndColor.copy(alpha = 0.96f)
                            ),
                            startY = 0f, endY = 120f
                        )
                    )
                    .padding(start = 16.dp, end = 70.dp, bottom = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Restaurant name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.Store, null,
                        modifier = Modifier.size(14.dp),
                        tint = onBgColor
                    )
                    MarqueeText(
                        text = restaurantName,
                        style = MaterialTheme.typography.labelMedium,
                        color = onBgColor,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Food name
                Text(
                    text = post.menuItemName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = onBgColor
                )

                // Price
                Surface(
                    color = onBgColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val displayPrice = ((post.price.toLong() + 999) / 1000) * 1000
                    Text(
                        text = "%,d đ".format(displayPrice.toInt()),
                        style = MaterialTheme.typography.titleSmall,
                        color = onBgColor,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                // Caption
                if (post.caption.isNotEmpty()) {
                    val captionMaxLines = if (captionExpanded) Int.MAX_VALUE else 2
                    Column {
                        Text(
                            text = post.caption,
                            style = MaterialTheme.typography.bodySmall,
                            color = onBgColor.copy(alpha = 0.85f),
                            maxLines = captionMaxLines,
                            overflow = if (captionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                        )
                        if (post.caption.length > 80) {
                            Text(
                                text = if (captionExpanded) "Ẩn bớt" else "Xem thêm",
                                style = MaterialTheme.typography.labelSmall,
                                color = onBgColor,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { captionExpanded = !captionExpanded }
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RestaurantFeedActionBtn(
    isActive: Boolean,
    count: Int,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    inactiveColor: Color,
    bgTint: Color,
    onClick: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isActive) activeColor else inactiveColor,
        animationSpec = tween(200),
        label = "iconColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(48.dp)
                .background(bgTint, CircleShape)
        ) {
            Icon(
                imageVector = if (isActive) activeIcon else inactiveIcon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(
            text = formatFeedCount(count),
            style = MaterialTheme.typography.labelSmall.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = 0.8f),
                    offset = Offset(0f, 2f),
                    blurRadius = 4f
                )
            ),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
        )
    }
}

private fun getAspectRatio(layout: String): Float = when (layout) {
    "SQUARE" -> 1f / 1f
    "LANDSCAPE" -> 16f / 9f
    "TALL" -> 3f / 4f
    else -> 4f / 5f // PORTRAIT
}

private fun formatFeedCount(count: Int): String = when {
    count >= 1000 -> "%.1fK".format(count / 1000.0)
    else -> count.toString()
}

@Composable
private fun MarqueeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    var textWidthPx by remember { mutableFloatStateOf(0f) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val shouldScroll = textWidthPx > containerWidthPx && containerWidthPx > 0f

    val scrollDistance = if (shouldScroll) textWidthPx - containerWidthPx else 0f
    val scrollDuration = if (shouldScroll) ((scrollDistance / 60f) * 1000f).toInt().coerceIn(2000, 9000) else 3000
    val pauseDuration = 1000
    val totalDuration = scrollDuration + pauseDuration * 2

    val infiniteTransition = rememberInfiniteTransition(label = "marquee_$text")
    val rawFraction by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = totalDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marqueeFraction"
    )

    val pauseFraction = pauseDuration.toFloat() / totalDuration
    val scrollFraction = scrollDuration.toFloat() / totalDuration
    val offsetX = when {
        !shouldScroll -> 0f
        rawFraction < pauseFraction -> 0f
        rawFraction < pauseFraction + scrollFraction -> {
            val t = (rawFraction - pauseFraction) / scrollFraction
            -scrollDistance * t
        }
        else -> -scrollDistance
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onGloballyPositioned { coords ->
                containerWidthPx = coords.size.width.toFloat()
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = text,
            style = style,
            color = color,
            fontWeight = fontWeight,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier
                .onGloballyPositioned { coords ->
                    textWidthPx = coords.size.width.toFloat()
                }
                .graphicsLayer { translationX = offsetX }
        )
    }
}
