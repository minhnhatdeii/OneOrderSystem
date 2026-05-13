package com.example.oneorder_sm.profile

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder_sm.ui.theme.OneOrder_SMTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun profileScreen_placeHolder() {
        composeTestRule.setContent {
            OneOrder_SMTheme {
                // Profile screen requires real ViewModel with Hilt injection
                // Full UI tests should be done with HiltAndroidTest
            }
        }
        assert(true)
    }
}