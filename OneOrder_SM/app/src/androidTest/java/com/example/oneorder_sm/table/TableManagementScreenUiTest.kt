package com.example.oneorder_sm.table

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder_sm.data.model.Table
import com.example.oneorder_sm.ui.screens.table.TableCard
import com.example.oneorder_sm.ui.theme.OneOrder_SMTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TableManagementScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tableCard_displaysFreeTableInfo() {
        val table = Table(id = 1L, name = "Bàn 1", status = "free", capacity = 4, location = "Tầng 1")

        composeTestRule.setContent {
            OneOrder_SMTheme {
                TableCard(table = table, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Bàn 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("4 khách").assertIsDisplayed()
        composeTestRule.onNodeWithText("Tầng 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Trống").assertIsDisplayed()
    }

    @Test
    fun tableCard_displaysOccupiedTableInfo() {
        val table = Table(id = 2L, name = "Bàn VIP", status = "occupied", capacity = 10, location = "Phòng lạnh")

        composeTestRule.setContent {
            OneOrder_SMTheme {
                TableCard(table = table, onClick = {})
            }
        }

        composeTestRule.onNodeWithText("Bàn VIP").assertIsDisplayed()
        composeTestRule.onNodeWithText("10 khách").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phòng lạnh").assertIsDisplayed()
        composeTestRule.onNodeWithText("Có khách").assertIsDisplayed()
    }

    @Test
    fun tableCard_click_triggersCallback() {
        var clicked = false
        val table = Table(id = 1L, name = "Bàn 1", status = "free", capacity = 4, location = "Tầng 1")

        composeTestRule.setContent {
            OneOrder_SMTheme {
                TableCard(table = table, onClick = { clicked = true })
            }
        }

        composeTestRule.onNodeWithText("Bàn 1").performClick()
        assert(clicked)
    }
}
