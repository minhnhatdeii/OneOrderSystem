package com.example.oneorder_sm.profile

import com.example.oneorder_sm.data.local.UserPreferencesManager
import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.domain.repository.ProfileRepository
import com.example.oneorder_sm.ui.screens.profile.ProfileViewModel
import io.mockk.coEvery
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
class ProfileViewModelTest {

    private val profileRepository: ProfileRepository = mockk()
    private val userPreferencesManager: UserPreferencesManager = mockk()

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        // Mock default flows
        coEvery { userPreferencesManager.themeMode } returns MutableStateFlow("SYSTEM")
        coEvery { userPreferencesManager.appLanguage } returns MutableStateFlow("vi")
        
        // Mock default load profile success
        coEvery { profileRepository.getCurrentProfile() } returns Result.success(Pair(
            Profile(id = "user1", fullName = "John Doe", role = "manager"),
            "test@test.com"
        ))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ProfileViewModel {
        return ProfileViewModel(profileRepository, userPreferencesManager)
    }

    @Test
    fun `loadProfile updates state with profile on success`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.profile)
        assertEquals("John Doe", state.profile?.fullName)
        assertEquals("test@test.com", state.email)
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `startEditing and cancelEditing toggles edit state`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startEditing()
        assertTrue(viewModel.uiState.value.isEditing)
        assertEquals("John Doe", viewModel.uiState.value.editFullName)

        viewModel.cancelEditing()
        assertFalse(viewModel.uiState.value.isEditing)
    }

    @Test
    fun `saveProfile updates profile and reloads on success`() = runTest(testDispatcher) {
        coEvery { profileRepository.updateProfile(any(), any()) } returns Result.success(Unit)
        
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.startEditing()
        viewModel.updateFullName("Jane Doe")
        viewModel.saveProfile()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Profile updated successfully", state.successMessage)
        assertFalse(state.isEditing)
    }

    @Test
    fun `changePassword updates password on success`() = runTest(testDispatcher) {
        coEvery { profileRepository.changePassword(any(), any()) } returns Result.success(Unit)
        
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showPasswordDialog()
        viewModel.updateCurrentPassword("oldPass")
        viewModel.updateNewPassword("newPass123")
        viewModel.updateConfirmPassword("newPass123")
        viewModel.changePassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Password changed successfully", state.successMessage)
        assertFalse(state.showPasswordDialog)
    }

    @Test
    fun `changePassword fails on validation error`() = runTest(testDispatcher) {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.showPasswordDialog()
        viewModel.updateCurrentPassword("oldPass")
        viewModel.updateNewPassword("newPass123")
        viewModel.updateConfirmPassword("differentPass")
        viewModel.changePassword()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Passwords do not match", state.passwordError)
        assertTrue(state.showPasswordDialog)
    }

    @Test
    fun `signOut sets isSignedOut to true`() = runTest(testDispatcher) {
        coEvery { profileRepository.signOut() } returns Unit
        
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.signOut()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.isSignedOut)
    }
}
