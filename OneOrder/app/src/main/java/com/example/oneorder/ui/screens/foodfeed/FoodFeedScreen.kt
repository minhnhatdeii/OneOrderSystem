package com.example.oneorder.ui.screens.foodfeed

import android.Manifest
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.oneorder.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import kotlinx.coroutines.delay
import com.example.oneorder.data.model.CardLayout
import com.example.oneorder.data.model.FeedPost
import com.example.oneorder.LocalDarkTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FoodFeedScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToOrderMode: () -> Unit,
    onNavigateToRestaurantProfile: (String) -> Unit,
    viewModel: FoodFeedViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Restore pager position from saved state (survives navigation + process death)
    val initialPage = uiState.currentPageIndex.coerceAtLeast(0)
    val verticalPagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { uiState.posts.size }
    )

    // Sync pager position → ViewModel (for SavedStateHandle persistence)
    LaunchedEffect(verticalPagerState.currentPage, uiState.posts) {
        viewModel.onPageChanged(verticalPagerState.currentPage, uiState.posts)
    }

    // Restore pager to saved post when posts list becomes available after load
    LaunchedEffect(uiState.posts) {
        if (uiState.posts.isNotEmpty() && verticalPagerState.currentPage == 0 && uiState.currentPostId != null) {
            val targetIndex = viewModel.getRestorePageIndex(uiState.posts)
            if (targetIndex > 0) {
                verticalPagerState.scrollToPage(targetIndex)
            }
        }
    }

    // ── Location Permission ─────────────────────────────────────────────
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    // Lắng nghe sự thay đổi quyền location.
    // Nếu có quyền, load bằng GPS thật. Nếu chưa có, load bằng lat=0/lng=0 và yêu cầu quyền.
    // Chỉ trigger khi posts đang trống (tránh reload khi quay về từ màn hình khác).
    val anyPermissionGranted = locationPermissions.permissions.any { it.status.isGranted }
    LaunchedEffect(anyPermissionGranted, uiState.posts) {
        // Only trigger load if we have no posts yet (don't reload on every permission change during navigation)
        if (uiState.posts.isEmpty()) {
            if (anyPermissionGranted) {
                viewModel.loadRecommendations(forceRefresh = true)
            } else {
                viewModel.loadRecommendations(
                    onPermissionNeeded = {
                        locationPermissions.launchMultiplePermissionRequest()
                    },
                    forceRefresh = true
                )
            }
        }
    }

    // Lắng nghe sự kiện người dùng vuốt đến cuối danh sách để lazy load
    LaunchedEffect(verticalPagerState.currentPage) {
        if (uiState.posts.isNotEmpty() && verticalPagerState.currentPage >= uiState.posts.size - 3) {
            viewModel.loadMore()
        }
    }

    // Log VIEW interaction khi người dùng nán lại > 3s trên 1 bài post
    LaunchedEffect(verticalPagerState.settledPage, uiState.posts) {
        val page = verticalPagerState.settledPage
        if (uiState.posts.isNotEmpty() && page in uiState.posts.indices) {
            delay(3000L)
            viewModel.logInteraction(uiState.posts[page].id, "VIEW")
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (uiState.error != null) {
            Text(text = "Error: ${uiState.error}", modifier = Modifier.align(Alignment.Center).padding(16.dp))
        } else {
            VerticalPager(
                state = verticalPagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val post = uiState.posts[page]
                FoodFeedCard(
                    post = post,
                    onLike = { viewModel.toggleLike(post.id) },
                    onComment = { 
                        viewModel.logInteraction(post.id, "CLICK")
                        viewModel.openComments(post.id) 
                    },
                    onNavigateToRestaurantProfile = { 
                        viewModel.logInteraction(post.id, "CLICK")
                        onNavigateToRestaurantProfile(post.restaurantId ?: "") 
                    },
                    onFollow = {
                        post.restaurantId?.let { viewModel.followRestaurant(it) }
                    },
                    onUnfollow = {
                        post.restaurantId?.let { viewModel.unfollowRestaurant(it) }
                    }
                )
            }
        }

        // TopBar nổi phía trên
        TopBarOverlay(
            onNavigateToProfile = onNavigateToProfile,
            onNavigateToOrderMode = onNavigateToOrderMode,
            locationAvailable = uiState.locationAvailable,
            onRefreshFeed = { viewModel.loadRecommendations(forceRefresh = true) }
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

    if (uiState.showOnboarding) {
        OnboardingBottomSheet(
            onSubmit = { selectedTags ->
                viewModel.submitOnboardingTags(selectedTags)
            }
        )
    }
}

// ─────────────────────────────────────────
//  TOP BAR
// ─────────────────────────────────────────
@Composable
private fun TopBarOverlay(
    onNavigateToProfile: () -> Unit,
    onNavigateToOrderMode: () -> Unit,
    locationAvailable: Boolean = false,
    onRefreshFeed: () -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Transparent,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateToProfile,
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.AccountCircle, stringResource(R.string.feed_profile),
                    tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(24.dp))
            }

            // Title + GPS status badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onRefreshFeed() }.padding(4.dp)
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)) { append("One") }
                        withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.ExtraBold)) { append("Order") }
                    },
                    fontSize = 20.sp
                )
                // Nhỏ: GPS indicator
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Icon(
                        imageVector = if (locationAvailable) Icons.Default.LocationOn else Icons.Default.LocationOff,
                        contentDescription = null,
                        modifier = Modifier.size(10.dp),
                        tint = if (locationAvailable)
                            Color(0xFF4CAF50) // xanh lá = có GPS
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (locationAvailable) stringResource(R.string.feed_near_you) else stringResource(R.string.feed_nationwide),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 9.sp,
                        color = if (locationAvailable)
                            Color(0xFF4CAF50)
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }

            IconButton(
                onClick = onNavigateToOrderMode,
                modifier = Modifier.size(40.dp).background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
            ) {
                Icon(Icons.Default.RestaurantMenu, stringResource(R.string.feed_order_mode), tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─────────────────────────────────────────
//  MỖI BÀI POST — Dynamic Palette Background
// ─────────────────────────────────────────
@Composable
fun FoodFeedCard(
    post: FeedPost,
    showDistance: Boolean = true,
    showMapButton: Boolean = true,
    onLike: () -> Unit,
    onComment: () -> Unit,
    onNavigateToRestaurantProfile: () -> Unit,
    onFollow: (() -> Unit)? = null,
    onUnfollow: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = LocalDarkTheme.current
    var showHeartAnim by remember { mutableStateOf(false) }
    var heartScale by remember { mutableFloatStateOf(0f) }
    val imagePagerState = rememberPagerState(pageCount = { post.images.size })
    var captionExpanded by remember { mutableStateOf(false) }

    // ── Dynamic Palette ──────────────────────────────────────────────────
    // Lấy màu từ ảnh đầu tiên (ảnh chính của post)
    val firstImageUrl = post.images.firstOrNull()?.url ?: ""
    val palette = rememberDominantPalette(
        imageUrl = firstImageUrl,
        isDark = isDark
    )

    // Animate tất cả màu → transition mượt khi user cuộn sang post khác
    val animSpec = tween<Color>(durationMillis = 500)
    val bgColor by animateColorAsState(
        targetValue = palette.background,
        animationSpec = animSpec,
        label = "bgColor"
    )
    val bgEndColor by animateColorAsState(
        targetValue = palette.backgroundEnd,
        animationSpec = animSpec,
        label = "bgEndColor"
    )
    val onBgColor by animateColorAsState(
        targetValue = palette.onBackground,
        animationSpec = animSpec,
        label = "onBgColor"
    )
    val accentColor by animateColorAsState(
        targetValue = palette.accent,
        animationSpec = animSpec,
        label = "accentColor"
    )

    LaunchedEffect(showHeartAnim) {
        if (showHeartAnim) { heartScale = 1f; delay(700); heartScale = 0f; showHeartAnim = false }
    }

    // ── Full-screen background gradient từ palette ───────────────────────
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
            // ── Phần giữa: thẻ ảnh + action buttons bên phải ──────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 90.dp),
                contentAlignment = Alignment.Center
            ) {
                if (post.images.isEmpty()) {
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
                            contentDescription = stringResource(R.string.feed_no_image),
                            modifier = Modifier.size(60.dp),
                            tint = onBgColor.copy(alpha = 0.5f)
                        )
                    }
                } else {
                    // Carousel ảnh
                    HorizontalPager(
                        state = imagePagerState,
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically
                    ) { idx ->
                        val feedImage = post.images[idx]
    
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .aspectRatio(feedImage.layout.aspectRatio())
                                .clip(RoundedCornerShape(20.dp))
                                // Subtle shadow bằng nền palette bên trong clip
                                .background(bgColor.copy(alpha = 0.3f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(
                                            onDoubleTap = {
                                                if (!post.isLiked) onLike()
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
    
                            // Double-tap heart anim
                            if (showHeartAnim) {
                                val scale by animateFloatAsState(
                                    targetValue = heartScale,
                                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 200f),
                                    label = "heartAnim"
                                )
                                Icon(
                                    Icons.Default.Favorite, null,
                                    modifier = Modifier.align(Alignment.Center).size(80.dp).scale(scale),
                                    tint = Color.White.copy(alpha = 0.95f)
                                )
                            }
                        }
                    }
    
                    // ─── Image index badge ───
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
                                "${imagePagerState.currentPage + 1}/${post.images.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = onBgColor,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // ─── Dots indicator ───
                if (post.images.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(post.images.size) { dotIdx ->
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

                // ─── Action buttons — TikTok style ───
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 10.dp, bottom = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Avatar + Follow button — TikTok style
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.clickable { onNavigateToRestaurantProfile() }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (post.restaurantAvatar != null) {
                                    AsyncImage(
                                        model = post.restaurantAvatar,
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

                            // Follow button: show check if already following, + if not
                            if (post.isFollowing) {
                                // Already following — green check badge (clickable to unfollow)
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 12.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF4CAF50))
                                        .clickable { onUnfollow?.invoke() ?: onNavigateToRestaurantProfile() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = "Following",
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else if (onFollow != null) {
                                // Not following — show + button
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .offset(y = 12.dp)
                                        .size(22.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF228BE2))
                                        .clickable { onFollow() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Add,
                                        contentDescription = stringResource(R.string.feed_follow),
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Like
                    FeedActionBtn(
                        isActive = post.isLiked,
                        count = post.likeCount,
                        activeIcon = Icons.Filled.Favorite,
                        inactiveIcon = Icons.Filled.Favorite,
                        activeColor = Color(0xFFE91E63),
                        inactiveColor = Color.White,
                        bgTint = Color.Black.copy(alpha = 0.35f),
                        onClick = onLike
                    )
                    // Comment
                    FeedActionBtn(
                        isActive = false,
                        count = post.commentCount,
                        activeIcon = Icons.Filled.ChatBubble,
                        inactiveIcon = Icons.Filled.ChatBubble,
                        activeColor = accentColor,
                        inactiveColor = Color.White,
                        bgTint = Color.Black.copy(alpha = 0.35f),
                        onClick = onComment
                    )
                    // Share
                    val shareText = stringResource(R.string.feed_share_text, post.menuItemName, post.restaurantName)
                    val shareLabel = stringResource(R.string.feed_share)
                    FeedActionBtn(
                        isActive = false,
                        count = post.shareCount,
                        activeIcon = Icons.Default.Share,
                        inactiveIcon = Icons.Default.Share,
                        activeColor = accentColor,
                        inactiveColor = Color.White,
                        bgTint = Color.Black.copy(alpha = 0.35f),
                        onClick = {
                            val shareIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareText)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(shareIntent, shareLabel))
                        }
                    )
                }
            }

            // ── Phần dưới: thông tin món ăn ───────────────────────────────────
            // Gradient fade từ trong suốt → màu palette cuối
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
                    .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Restaurant + khoảng cách
                Row(
                    modifier = Modifier
                        .clickable { onNavigateToRestaurantProfile() }
                        .fillMaxWidth(0.75f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        Icons.Default.Store, null,
                        modifier = Modifier.size(14.dp),
                        tint = onBgColor
                    )
                    // Giới hạn chiều rộng tên nhà hàng và scroll nếu dài
                    Box(modifier = Modifier.weight(1f, fill = false).widthIn(max = 130.dp)) {
                        MarqueeText(
                            text = post.restaurantName,
                            style = MaterialTheme.typography.labelMedium,
                            color = onBgColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (showDistance) {
                        Text("•", color = onBgColor.copy(alpha = 0.5f), fontSize = 10.sp)
                        Icon(
                            Icons.Default.LocationOn, null,
                            modifier = Modifier.size(12.dp),
                            tint = onBgColor.copy(alpha = 0.65f)
                        )
                        Text(
                            "${"%.1f".format(post.distanceKm)} km",
                            style = MaterialTheme.typography.labelSmall,
                            color = onBgColor.copy(alpha = 0.65f),
                            maxLines = 1
                        )
                    }
                }

                // Tên món ăn
                Text(
                    text = post.menuItemName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = onBgColor
                )

                // Giá — làm tròn về 1000 đ (định dạng Việt Nam)
                Surface(
                    color = onBgColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val displayPrice = ((post.price.toLong() + 999) / 1000) * 1000
                    Text(
                        text = "%,d đ".format(displayPrice),
                        style = MaterialTheme.typography.titleSmall,
                        color = onBgColor,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                    )
                }

                // Caption
                val captionMaxLines = if (captionExpanded) Int.MAX_VALUE else 2
                Column {
                    Text(
                        text = post.caption,
                        style = MaterialTheme.typography.bodySmall,
                        color = onBgColor.copy(alpha = 0.85f),  // slightly softened but still readable
                        maxLines = captionMaxLines,
                        overflow = if (captionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis
                    )
                    if (post.caption.length > 80) {
                        Text(
                            text = if (captionExpanded) stringResource(R.string.feed_collapse) else stringResource(R.string.feed_see_more),
                            style = MaterialTheme.typography.labelSmall,
                            color = onBgColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { captionExpanded = !captionExpanded }
                                .padding(top = 2.dp)
                        )
                    }
                }

                // Nút bản đồ với border màu palette
                if (showMapButton) {
                    Spacer(Modifier.height(2.dp))
                    OutlinedButton(
                        onClick = {
                            val uri = Uri.parse(
                                "geo:${post.restaurantLat},${post.restaurantLng}" +
                                "?q=${post.restaurantLat},${post.restaurantLng}" +
                                "(${Uri.encode(post.restaurantName)})"
                            )
                            try {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, uri).apply {
                                        setPackage("com.google.android.apps.maps")
                                    }
                                )
                            } catch (e: Exception) {
                                context.startActivity(
                                    Intent(
                                        Intent.ACTION_VIEW,
                                        Uri.parse("https://maps.google.com/?q=${post.restaurantLat},${post.restaurantLng}")
                                    )
                                )
                            }
                        },
                        modifier = Modifier.height(36.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = onBgColor
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, onBgColor.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(Icons.Default.Map, null, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.feed_view_map), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────
//  ACTION BUTTON (palette-aware)
// ─────────────────────────────────────────
@Composable
fun FeedActionBtn(
    isActive: Boolean,
    count: Int,
    activeIcon: androidx.compose.ui.graphics.vector.ImageVector,
    inactiveIcon: androidx.compose.ui.graphics.vector.ImageVector,
    activeColor: Color,
    inactiveColor: Color = Color.White,
    bgTint: Color = Color.White.copy(alpha = 0.15f),
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
            text = formatCount(count),
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

// ─── CardLayout helpers ───
private fun CardLayout.aspectRatio(): Float = when (this) {
    CardLayout.PORTRAIT  -> 4f / 5f
    CardLayout.SQUARE    -> 1f / 1f
    CardLayout.LANDSCAPE -> 16f / 9f
    CardLayout.TALL      -> 3f / 4f
}

private fun CardLayout.label(): String = when (this) {
    CardLayout.PORTRAIT  -> "4:5"
    CardLayout.SQUARE    -> "1:1"
    CardLayout.LANDSCAPE -> "16:9"
    CardLayout.TALL      -> "3:4"
}

private fun formatCount(count: Int): String = when {
    count >= 1000 -> "${"%.1f".format(count / 1000.0)}k"
    else -> count.toString()
}

/**
 * MarqueeText: hiển thị text trong một Box clip cố định.
 * Nếu text dài hơn container, nó sẽ cuộn liên tục từ phải sang trái
 * với khoảng dừng đầu mỗi vòng (pause at start → slide left → back to start).
 */
@Composable
fun MarqueeText(
    text: String,
    style: androidx.compose.ui.text.TextStyle,
    color: Color,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    // Đo chiều rộng thực của text để quyết định có scroll không
    var textWidthPx by remember { mutableFloatStateOf(0f) }
    var containerWidthPx by remember { mutableFloatStateOf(0f) }
    val shouldScroll = textWidthPx > containerWidthPx && containerWidthPx > 0f

    // Khoảng cách cần translate = textWidth - containerWidth (luôn dương khi shouldScroll)
    val scrollDistance = if (shouldScroll) textWidthPx - containerWidthPx else 0f

    // Tốc độ: ~80px/s, tạm dừng 1s ở điểm đầu và điểm cuối
    val scrollDuration = if (shouldScroll) ((scrollDistance / 60f) * 1000f).toInt().coerceIn(2000, 9000) else 3000
    val pauseDuration = 1000 // ms dừng ở mỗi đầu
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

    // Map rawFraction → offset: pause đầu → scroll → pause cuối → reset
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

// ─────────────────────────────────────────
//  ONBOARDING BOTTOM SHEET
// ─────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun OnboardingBottomSheet(onSubmit: (List<Pair<String, String>>) -> Unit) {
    val availableTags = listOf(
        Pair("cuisine", "vietnamese") to "Đồ Việt",
        Pair("cuisine", "korean") to "Đồ Hàn",
        Pair("cuisine", "japanese") to "Đồ Nhật",
        Pair("flavor", "spicy") to "Món Cay",
        Pair("cuisine", "vegetarian") to "Ăn Chay",
        Pair("cooking_method", "fried") to "Đồ chiên rán",
        Pair("dish_type", "noodle") to "Bún/Phở",
        Pair("flavor", "sweet") to "Đồ ngọt",
        Pair("dish_type", "drink") to "Đồ uống"
    )

    var selectedTags by remember { mutableStateOf(setOf<Pair<String, String>>()) }

    ModalBottomSheet(
        onDismissRequest = { }, // Bắt buộc chọn mới cho tắt
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Chào mừng bạn mới! \uD83C\uDF89", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Để OneOrder gợi ý món ăn chuẩn gu bạn hơn, hãy chọn tối thiểu 3 hương vị bạn thích nhé!", 
                style = MaterialTheme.typography.bodyMedium, 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableTags.forEach { (tagPair, label) ->
                    val isSelected = selectedTags.contains(tagPair)
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            selectedTags = if (isSelected) selectedTags - tagPair else selectedTags + tagPair
                        },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSubmit(selectedTags.toList()) },
                enabled = selectedTags.size >= 3,
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(if (selectedTags.size < 3) "Chọn thêm ${3 - selectedTags.size} món" else "Bắt đầu khám phá")
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
