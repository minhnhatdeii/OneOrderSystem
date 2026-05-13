package com.example.oneorder.feed

import androidx.lifecycle.SavedStateHandle
import com.example.oneorder.data.model.FeedCommentUI
import com.example.oneorder.data.model.FeedPost
import com.example.oneorder.data.repository.AuthRepository
import com.example.oneorder.data.repository.FollowingRepository
import com.example.oneorder.data.repository.FoodFeedRepository
import com.example.oneorder.ui.screens.foodfeed.FoodFeedViewModel
import com.example.oneorder.utils.LocationProvider
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodFeedViewModelTest {

    private val repository: FoodFeedRepository = mockk(relaxed = true)
    private val authRepository: AuthRepository = mockk(relaxed = true)
    private val followingRepository: FollowingRepository = mockk(relaxed = true)
    private val locationProvider: LocationProvider = mockk(relaxed = true)
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()

    private val testDispatcher = StandardTestDispatcher()

    private val mockPost = FeedPost(
        id = "post1",
        restaurantId = "rest1",
        restaurantName = "Pho Bat Dan",
        restaurantLat = 21.0,
        restaurantLng = 105.0,
        menuItemName = "Pho Bo",
        price = 65000.0,
        caption = "Pho ngon",
        images = emptyList(),
        likeCount = 10,
        commentCount = 2,
        shareCount = 0,
        isLiked = false,
        isFollowing = false,
        distanceKm = 1.5
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { authRepository.getCurrentUser() } returns null
        coEvery { followingRepository.getFollowedTenantIdsFlow() } returns MutableStateFlow(emptySet())
        coEvery { followingRepository.getFollowingCount() } returns Result.success(0)
        coEvery { repository.checkUserProfileExists(any()) } returns true
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has correct default values`() = runTest(testDispatcher) {
        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertTrue(state.posts.isEmpty())
        assertFalse(state.showCommentSheet)
    }

    @Test
    fun `openComments shows comment sheet with correct postId`() = runTest(testDispatcher) {
        coEvery { repository.getComments("post1") } returns Result.success(emptyList())

        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        advanceUntilIdle()

        viewModel.openComments("post1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showCommentSheet)
        assertEquals("post1", state.commentsForPostId)
    }

    @Test
    fun `closeComments hides comment sheet`() = runTest(testDispatcher) {
        coEvery { repository.getComments("post1") } returns Result.success(emptyList())

        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        advanceUntilIdle()

        viewModel.openComments("post1")
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.showCommentSheet)

        viewModel.closeComments()

        assertFalse(viewModel.uiState.value.showCommentSheet)
        assertNull(viewModel.uiState.value.commentsForPostId)
    }

    @Test
    fun `openComments loads comments from repository`() = runTest(testDispatcher) {
        val comment = FeedCommentUI(
            id = "c1",
            userName = "Test User",
            avatarInitial = "T",
            content = "Test comment",
            timeAgo = "1 minute ago"
        )
        coEvery { repository.getComments("post1") } returns Result.success(listOf(comment))

        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        advanceUntilIdle()

        viewModel.openComments("post1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.comments.size)
        assertEquals("Test comment", state.comments[0].content)
    }

    @Test
    fun `updateCommentInput updates commentInput in state`() = runTest(testDispatcher) {
        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        advanceUntilIdle()

        viewModel.updateCommentInput("Hello world")

        assertEquals("Hello world", viewModel.uiState.value.commentInput)
    }

    @Test
    fun `submitComment calls repository and clears input`() = runTest(testDispatcher) {
        coEvery { repository.getComments("post1") } returns Result.success(emptyList())
        coEvery { authRepository.getCurrentUser() } returns mockk(relaxed = true) {
            io.mockk.every { id } returns "user1"
        }
        coEvery { repository.addComment("post1", "user1", "Nice food!") } returns Result.success(true)

        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        advanceUntilIdle()

        viewModel.openComments("post1")
        advanceUntilIdle()
        viewModel.updateCommentInput("Nice food!")
        viewModel.submitComment()
        advanceUntilIdle()

        assertEquals("", viewModel.uiState.value.commentInput)
    }

    @Test
    fun `toggleLike updates liked state for a post`() = runTest(testDispatcher) {
        coEvery { repository.getRecommendations(any(), any(), any(), any()) } returns Result.success(listOf(mockPost))
        coEvery { locationProvider.hasLocationPermission() } returns true
        coEvery { locationProvider.getCurrentLocation() } returns null
        coEvery { repository.toggleLike("post1", any(), any()) } returns Result.success(true)

        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        // Load feed
        viewModel.loadRecommendations(forceRefresh = true)
        advanceUntilIdle()

        val initialLiked = viewModel.uiState.value.posts.find { it.id == "post1" }?.isLiked ?: false

        viewModel.toggleLike("post1")
        advanceUntilIdle()

        val newLiked = viewModel.uiState.value.posts.find { it.id == "post1" }?.isLiked ?: false
        assertNotEquals(initialLiked, newLiked)
    }

    @Test
    fun `followRestaurant calls repository`() = runTest(testDispatcher) {
        coEvery { followingRepository.followRestaurant("rest1") } returns Result.success(Unit)

        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        advanceUntilIdle()

        viewModel.followRestaurant("rest1")
        advanceUntilIdle()

        coVerify { followingRepository.followRestaurant("rest1") }
    }

    @Test
    fun `unfollowRestaurant calls repository`() = runTest(testDispatcher) {
        coEvery { followingRepository.unfollowRestaurant("rest1") } returns Result.success(Unit)

        val viewModel = FoodFeedViewModel(
            repository, authRepository, followingRepository, locationProvider, savedStateHandle
        )
        advanceUntilIdle()

        viewModel.unfollowRestaurant("rest1")
        advanceUntilIdle()

        coVerify { followingRepository.unfollowRestaurant("rest1") }
    }
}