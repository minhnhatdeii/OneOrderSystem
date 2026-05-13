package com.example.oneorder_sm.foodpromotion

import com.example.oneorder_sm.data.repository.FoodPostRepository
import com.example.oneorder_sm.domain.model.Tenant
import com.example.oneorder_sm.domain.repository.StaffRepository
import com.example.oneorder_sm.domain.repository.TenantRepository
import com.example.oneorder_sm.ui.screens.foodpromotion.FoodPost
import com.example.oneorder_sm.ui.screens.foodpromotion.FoodPromotionViewModel
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FoodPromotionViewModelTest {

    private val staffRepository: StaffRepository = mockk()
    private val tenantRepository: TenantRepository = mockk()
    private val foodPostRepository: FoodPostRepository = mockk()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock default success
        coEvery { tenantRepository.getCurrentTenant() } returns Result.success(
            Tenant("tenant1", "Nhà hàng Test", "owner", "Địa chỉ 1", "0123456789", "test@test.com", null, null, null)
        )
        coEvery { foodPostRepository.getFoodPostsByTenant("tenant1") } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): FoodPromotionViewModel {
        return FoodPromotionViewModel(staffRepository, tenantRepository, foodPostRepository)
    }

    @Test
    fun `loadRestaurantProfile updates state on success`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Nhà hàng Test", state.restaurantName)
        assertEquals("Địa chỉ 1", state.address)
        assertFalse(state.isLoading)
        assertNotNull(state.tenant)
    }

    @Test
    fun `togglePostActive updates post active status`() = runTest(testDispatcher) {
        val post = FoodPost("post1", "Món 1", emptyList(), "Ngon", 10, 5, true, "2023-01-01", 50000.0, "Đồ ăn", false)
        coEvery { foodPostRepository.getFoodPostsByTenant("tenant1") } returns Result.success(listOf(post))
        coEvery { foodPostRepository.updatePostActiveStatus("post1", false) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.togglePostActive("post1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.posts[0].isActive)
    }

    @Test
    fun `deletePost removes post from list on success`() = runTest(testDispatcher) {
        val post = FoodPost("post1", "Món 1", emptyList(), "Ngon", 10, 5, true, "2023-01-01", 50000.0, "Đồ ăn", false)
        coEvery { foodPostRepository.getFoodPostsByTenant("tenant1") } returns Result.success(listOf(post))
        coEvery { foodPostRepository.deleteFoodPost("post1") } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.deletePost("post1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.posts.isEmpty())
    }

    @Test
    fun `toggleLike updates like count and isLiked locally`() = runTest(testDispatcher) {
        val post = FoodPost("post1", "Món 1", emptyList(), "Ngon", 10, 5, true, "2023-01-01", 50000.0, "Đồ ăn", false)
        coEvery { foodPostRepository.getFoodPostsByTenant("tenant1") } returns Result.success(listOf(post))
        coEvery { foodPostRepository.toggleLike("post1", true) } returns Result.success(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleLike("post1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.posts[0].isLiked)
        assertEquals(11, state.posts[0].likeCount)
    }

    @Test
    fun `openComments sets showCommentSheet and loads comments`() = runTest(testDispatcher) {
        coEvery { foodPostRepository.getComments("post1") } returns Result.success(emptyList())

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openComments("post1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.showCommentSheet)
        assertEquals("post1", state.commentsForPostId)
    }

    @Test
    fun `submitComment adds comment and reloads comments`() = runTest(testDispatcher) {
        val post = FoodPost("post1", "Món 1", emptyList(), "Ngon", 10, 5, true, "2023-01-01", 50000.0, "Đồ ăn", false)
        coEvery { foodPostRepository.getFoodPostsByTenant("tenant1") } returns Result.success(listOf(post))
        coEvery { foodPostRepository.getComments("post1") } returns Result.success(emptyList())
        coEvery { foodPostRepository.addComment("post1", "Tuyệt vời") } returns Result.success(true)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.openComments("post1")
        advanceUntilIdle()

        viewModel.updateCommentInput("Tuyệt vời")
        viewModel.submitComment()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.commentInput)
        // Comment count should increase by 1
        assertEquals(6, state.posts[0].commentCount)
    }

    @Test
    fun `updateProfile updates tenant and ui state on success`() = runTest(testDispatcher) {
        coEvery { tenantRepository.updateTenant(any(), any(), any(), any(), any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.updateProfile("nhahangtest", "Nhà hàng mới", "Địa chỉ 2", "0987654321", "new@test.com", "Mô tả", "LEFT")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Nhà hàng mới", state.restaurantName)
        assertEquals("Địa chỉ 2", state.address)
        assertEquals("0987654321", state.phone)
        assertEquals("Cập nhật thông tin nhà hàng thành công!", state.successMessage)
    }

    @Test
    fun `createStaff sets tempPassword on success`() = runTest(testDispatcher) {
        coEvery { staffRepository.createStaffAccount("staff@test.com", "Staff 1", "0123", "staff") } returns Result.success("temp123")

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.createStaff("staff@test.com", "Staff 1", "0123", "staff")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("temp123", state.tempPassword)
        assertEquals("staff@test.com", state.createdEmail)
        assertFalse(state.showAddStaffDialog)
    }
}
