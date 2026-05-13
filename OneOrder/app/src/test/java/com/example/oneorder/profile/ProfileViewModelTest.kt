package com.example.oneorder.profile

import com.example.oneorder.data.local.SecurePreferencesManager
import com.example.oneorder.data.local.UserPreferencesManager
import com.example.oneorder.data.model.Profile
import com.example.oneorder.data.repository.FollowingRepository
import com.example.oneorder.data.repository.ProfileRepository
import com.example.oneorder.ui.screens.profile.ProfileViewModel
import io.github.jan.supabase.SupabaseClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ProfileViewModel.
 *
 * Các test này kiểm tra logic nghiệp vụ của ProfileViewModel:
 * - Tải hồ sơ người dùng
 * - Upload avatar
 * - Thay đổi theme và ngôn ngữ
 *
 * Mọi dependency ngoại vi (Supabase, Repository) đều được mock bằng MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    // ─── Mocks ────────────────────────────────────────────────────────────────
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val supabase: SupabaseClient = mockk(relaxed = true)
    private val userPreferencesManager: UserPreferencesManager = mockk(relaxed = true)
    private val securePreferencesManager: SecurePreferencesManager = mockk(relaxed = true)
    private val followingRepository: FollowingRepository = mockk(relaxed = true)

    private lateinit var viewModel: ProfileViewModel
    private val testDispatcher = StandardTestDispatcher()

    // ─── Sample data ──────────────────────────────────────────────────────────
    private val sampleProfile = Profile(
        id = "user-123",
        fullName = "Nguyễn Văn A",
        avatarUrl = null,
        phoneNumber = "0901234567"
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mock preference flows
        every { userPreferencesManager.themeMode } returns MutableStateFlow("SYSTEM")
        every { userPreferencesManager.appLanguage } returns MutableStateFlow("vi")

        // Mock following count flow
        every { followingRepository.getFollowingCountFlow() } returns MutableStateFlow(5)

        // Default: profile loads successfully
        coEvery { profileRepository.getProfile() } returns Result.success(sampleProfile)

        viewModel = ProfileViewModel(
            profileRepository     = profileRepository,
            supabase              = supabase,
            userPreferencesManager = userPreferencesManager,
            securePreferencesManager = securePreferencesManager,
            followingRepository   = followingRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ─── loadProfile ──────────────────────────────────────────────────────────

    @Test
    fun `loadProfile succeeds and updates uiState with profile`() = runTest(testDispatcher) {
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(sampleProfile, state.profile)
    }

    @Test
    fun `loadProfile sets error state when repository fails`() = runTest(testDispatcher) {
        coEvery { profileRepository.getProfile() } returns Result.failure(Exception("Network error"))

        // Re-create the ViewModel so it calls loadProfile with the new mock
        val vm = ProfileViewModel(
            profileRepository     = profileRepository,
            supabase              = supabase,
            userPreferencesManager = userPreferencesManager,
            securePreferencesManager = securePreferencesManager,
            followingRepository   = followingRepository
        )

        advanceUntilIdle()

        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
    }

    // ─── uploadAvatar ─────────────────────────────────────────────────────────

    @Test
    fun `uploadAvatar calls repository and reloads profile on success`() = runTest(testDispatcher) {
        val dummyBytes = byteArrayOf(1, 2, 3)
        val dummyExt  = "jpg"
        val expectedUrl = "https://cdn.example.com/avatar.jpg"

        coEvery { profileRepository.uploadAvatar(dummyBytes, dummyExt) } returns Result.success(expectedUrl)

        advanceUntilIdle() // let init complete

        viewModel.uploadAvatar(dummyBytes, dummyExt)
        advanceUntilIdle()

        coVerify { profileRepository.uploadAvatar(dummyBytes, dummyExt) }
        // getProfile should be called at least twice: once from init, once after avatar upload
        coVerify(atLeast = 2) { profileRepository.getProfile() }
    }

    @Test
    fun `uploadAvatar sets error on failure`() = runTest(testDispatcher) {
        val dummyBytes = byteArrayOf(1, 2, 3)
        coEvery { profileRepository.uploadAvatar(dummyBytes, "png") } returns
                Result.failure(Exception("Upload failed"))

        advanceUntilIdle()

        viewModel.uploadAvatar(dummyBytes, "png")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.error)
    }

    // ─── Preferences ──────────────────────────────────────────────────────────

    @Test
    fun `setThemeMode delegates to userPreferencesManager`() = runTest(testDispatcher) {
        viewModel.setThemeMode("DARK")
        advanceUntilIdle()
        coVerify { userPreferencesManager.saveThemeMode("DARK") }
    }

    @Test
    fun `setAppLanguage delegates to userPreferencesManager`() = runTest(testDispatcher) {
        viewModel.setAppLanguage("en")
        advanceUntilIdle()
        coVerify { userPreferencesManager.saveAppLanguage("en") }
    }

    // ─── clearError ───────────────────────────────────────────────────────────

    @Test
    fun `clearError resets error field in uiState`() = runTest(testDispatcher) {
        // Force an error
        coEvery { profileRepository.getProfile() } returns Result.failure(Exception("err"))
        viewModel.loadProfile()
        advanceUntilIdle()

        // Now clear it
        viewModel.clearError()
        assertNull(viewModel.uiState.value.error)
    }

    // ─── followingCount ───────────────────────────────────────────────────────

    @Test
    fun `followingCount reflects the value from followingRepository`() = runTest(testDispatcher) {
        advanceUntilIdle()
        assertEquals(5, viewModel.followingCount.value)
    }
}

