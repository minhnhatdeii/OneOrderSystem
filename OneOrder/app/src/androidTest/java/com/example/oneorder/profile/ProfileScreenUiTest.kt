package com.example.oneorder.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder.ui.screens.profile.ProfileMenuItem
import com.example.oneorder.ui.theme.OneOrderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileMenuItem_displaysTitleAndValue() {
        composeTestRule.setContent {
            OneOrderTheme {
                ProfileMenuItem(
                    icon = Icons.Outlined.Person,
                    title = "Chỉnh sửa thông tin",
                    value = "Hello",
                    onClick = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Chỉnh sửa thông tin").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
    }
}
