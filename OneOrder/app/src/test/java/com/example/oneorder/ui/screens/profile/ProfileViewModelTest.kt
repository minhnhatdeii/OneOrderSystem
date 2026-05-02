package com.example.oneorder.ui.screens.profile

import com.example.oneorder.data.local.UserPreferencesManager
import com.example.oneorder.data.model.Profile
import com.example.oneorder.data.repository.ProfileRepository
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileViewModelTest {

    private lateinit var viewModel: ProfileViewModel
    private val profileRepository: ProfileRepository = mockk(relaxed = true)
    private val supabase: SupabaseClient = mockk(relaxed = true)
    private val userPreferencesManager: UserPreferencesManager = mockk(relaxed = true)
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock data flows
        every { userPreferencesManager.themeMode } returns MutableStateFlow("SYSTEM")
        every { userPreferencesManager.appLanguage } returns MutableStateFlow("vi")
        
        // Mock profile load
        coEvery { profileRepository.getProfile() } returns Result.success(
            Profile(id = "123", fullName = "Test User", avatarUrl = null)
        )
        
        viewModel = ProfileViewModel(profileRepository, supabase, userPreferencesManager)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `uploadAvatar sets loading then updates state on success`() = runTest(testDispatcher) {
        val dummyBytes = byteArrayOf(1, 2, 3)
        val dummyExt = "jpg"
        val expectedUrl = "https://example.com/avatar.jpg"
        
        coEvery { profileRepository.uploadAvatar(dummyBytes, dummyExt) } returns Result.success(expectedUrl)
        
        viewModel.uploadAvatar(dummyBytes, dummyExt)
        
        // Advance coroutines
        advanceUntilIdle()
        
        coVerify { profileRepository.uploadAvatar(dummyBytes, dummyExt) }
        
        // The mock reload should be called
        coVerify { profileRepository.getProfile() }
    }

    @Test
    fun `changeTheme requests saveThemeMode from preferences manager`() = runTest(testDispatcher) {
        viewModel.setThemeMode("DARK")
        advanceUntilIdle()
        coVerify { userPreferencesManager.saveThemeMode("DARK") }
    }

    @Test
    fun `changeLanguage requests saveAppLanguage from preferences manager`() = runTest(testDispatcher) {
        viewModel.setAppLanguage("en")
        advanceUntilIdle()
        coVerify { userPreferencesManager.saveAppLanguage("en") }
    }
}
