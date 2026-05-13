package com.example.oneorder_sm.staff

import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.domain.repository.StaffRepository
import com.example.oneorder_sm.ui.screens.staff.StaffManagementViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class StaffManagementViewModelTest {

    private val staffRepository: StaffRepository = mockk()
    private val testDispatcher = StandardTestDispatcher()

    private val mockProfile = Profile(
        id = "staff1",
        fullName = "Nguyen Van A",
        role = "manager",
        isActive = true
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        coEvery { staffRepository.getStaffList() } returns Result.success(emptyList())
        coEvery { staffRepository.getAllStaffAttendance(any(), any(), any()) } returns Result.success(emptyList())
        coEvery { staffRepository.getAllDailyNotes(any(), any(), any()) } returns Result.success(emptyList())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has isLoading false after successful load`() = runTest(testDispatcher) {
        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `loadData shows error when repository fails`() = runTest(testDispatcher) {
        coEvery { staffRepository.getStaffList() } returns Result.failure(Exception("Network error"))
        coEvery { staffRepository.getCurrentStaffProfile() } returns Result.failure(Exception("Not found"))

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNotNull(state.error)
        assertTrue(state.staff.isEmpty())
    }

    @Test
    fun `loadData populates staff list on success`() = runTest(testDispatcher) {
        coEvery { staffRepository.getStaffList() } returns Result.success(listOf(mockProfile))

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.staff.size)
        assertEquals("staff1", state.staff[0].id)
        assertTrue(state.isManager)
    }

    @Test
    fun `loadData sets isManager false when only manager view staff list fails`() = runTest(testDispatcher) {
        coEvery { staffRepository.getStaffList() } returns Result.failure(Exception("Only managers can view staff list"))
        coEvery { staffRepository.getCurrentStaffProfile() } returns Result.success(mockProfile.copy(role = "staff"))

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isManager)
        assertEquals(1, state.staff.size)
    }

    @Test
    fun `createStaff success sets tempPassword`() = runTest(testDispatcher) {
        coEvery { staffRepository.createStaffAccount(any(), any(), any(), any()) } returns Result.success("TempPass123")

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        viewModel.createStaff("new@test.com", "New Staff", "0123456789", "staff")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("TempPass123", state.tempPassword)
        assertEquals("new@test.com", state.createdEmail)
    }

    @Test
    fun `createStaff failure sets error`() = runTest(testDispatcher) {
        coEvery { staffRepository.createStaffAccount(any(), any(), any(), any()) } returns Result.failure(Exception("Email exists"))

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        viewModel.createStaff("existing@test.com", "Staff", null, "staff")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.error)
        assertTrue(state.error!!.contains("Email exists"))
    }

    @Test
    fun `clearTempPassword nullifies tempPassword`() = runTest(testDispatcher) {
        coEvery { staffRepository.createStaffAccount(any(), any(), any(), any()) } returns Result.success("TempPass123")

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()
        viewModel.createStaff("new@test.com", "Staff", null, "staff")
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.tempPassword)

        viewModel.clearTempPassword()

        assertNull(viewModel.uiState.value.tempPassword)
        assertNull(viewModel.uiState.value.createdEmail)
    }

    @Test
    fun `deactivateStaff updates staff isActive to false`() = runTest(testDispatcher) {
        coEvery { staffRepository.getStaffList() } returns Result.success(listOf(mockProfile))
        coEvery { staffRepository.deactivateStaff(any()) } returns Result.success(Unit)

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        viewModel.deactivateStaff("staff1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val updated = state.staff.find { it.id == "staff1" }
        assertNotNull(updated)
        assertFalse(updated!!.isActive)
    }

    @Test
    fun `reactivateStaff updates staff isActive to true`() = runTest(testDispatcher) {
        val inactiveProfile = mockProfile.copy(isActive = false)
        coEvery { staffRepository.getStaffList() } returns Result.success(listOf(inactiveProfile))
        coEvery { staffRepository.reactivateStaff(any()) } returns Result.success(Unit)

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        viewModel.reactivateStaff("staff1")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        val updated = state.staff.find { it.id == "staff1" }
        assertNotNull(updated)
        assertTrue(updated!!.isActive)
    }

    @Test
    fun `submitTimekeeping success sets timekeepingSuccessMessage`() = runTest(testDispatcher) {
        coEvery { staffRepository.submitAttendance(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { staffRepository.getStaffAttendance(any(), any(), any()) } returns Result.success(emptyList())

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()

        viewModel.submitTimekeeping("staff1", "Nguyen Van A", "2026-05-11", "On time")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.timekeepingSuccessMessage)
        assertTrue(state.timekeepingSuccessMessage!!.contains("Nguyen Van A"))
    }

    @Test
    fun `clearTimekeepingMessage nullifies timekeepingSuccessMessage`() = runTest(testDispatcher) {
        coEvery { staffRepository.submitAttendance(any(), any(), any(), any(), any(), any()) } returns Result.success(Unit)
        coEvery { staffRepository.getStaffAttendance(any(), any(), any()) } returns Result.success(emptyList())

        val viewModel = StaffManagementViewModel(staffRepository)
        advanceUntilIdle()
        viewModel.submitTimekeeping("staff1", "Nguyen Van A", "2026-05-11", "note")
        advanceUntilIdle()

        viewModel.clearTimekeepingMessage()

        assertNull(viewModel.uiState.value.timekeepingSuccessMessage)
    }
}