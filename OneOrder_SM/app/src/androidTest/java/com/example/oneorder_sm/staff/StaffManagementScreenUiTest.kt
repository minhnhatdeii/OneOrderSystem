package com.example.oneorder_sm.staff

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder_sm.domain.model.Profile
import com.example.oneorder_sm.ui.screens.staff.StaffCard
import com.example.oneorder_sm.ui.theme.OneOrder_SMTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StaffManagementScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun staffCard_displaysManagerInfo() {
        val profile = Profile(
            id = "staff1",
            fullName = "Nguyễn Văn A",
            role = "manager",
            isActive = true
        )

        composeTestRule.setContent {
            OneOrder_SMTheme {
                StaffCard(
                    profile = profile,
                    isManager = true,
                    attendanceList = emptyList(),
                    dailyNotes = emptyList(),
                    onFetchAttendance = { _, _ -> },
                    onApproveAttendance = {},
                    onRejectAttendance = {},
                    onSubmitAttendance = { _, _ -> },
                    onSubmitDayOff = { _, _ -> },
                    onSaveNote = { _, _ -> },
                    onDeleteNote = { _, _ -> },
                    onToggleActive = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Nguyễn Văn A").assertIsDisplayed()
        composeTestRule.onNodeWithText("Quản lý").assertIsDisplayed()
    }

    @Test
    fun staffCard_displaysStaffInfo() {
        val profile = Profile(
            id = "staff2",
            fullName = "Trần B",
            role = "staff",
            isActive = true
        )

        composeTestRule.setContent {
            OneOrder_SMTheme {
                StaffCard(
                    profile = profile,
                    isManager = false,
                    attendanceList = emptyList(),
                    dailyNotes = emptyList(),
                    onFetchAttendance = { _, _ -> },
                    onApproveAttendance = {},
                    onRejectAttendance = {},
                    onSubmitAttendance = { _, _ -> },
                    onSubmitDayOff = { _, _ -> },
                    onSaveNote = { _, _ -> },
                    onDeleteNote = { _, _ -> },
                    onToggleActive = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Trần B").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nhân viên").assertIsDisplayed()
    }
}

