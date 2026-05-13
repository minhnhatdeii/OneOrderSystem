package com.example.oneorder.feed

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.oneorder.data.model.FeedPost
import com.example.oneorder.ui.screens.foodfeed.FoodFeedCard
import com.example.oneorder.ui.theme.OneOrderTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodFeedScreenUiTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun foodFeedCard_displaysPostInfo() {
        val post = FeedPost(
            id = "post1",
            restaurantId = "rest1",
            restaurantName = "Phở Bát Đàn",
            restaurantLat = 21.0,
            restaurantLng = 105.0,
            menuItemName = "Phở Bò Đặc Biệt",
            price = 65000.0,
            caption = "Phở bò truyền thống cực ngon",
            images = emptyList(),
            likeCount = 100,
            commentCount = 20,
            shareCount = 5,
            isLiked = false,
            isFollowing = true,
            distanceKm = 2.5
        )

        composeTestRule.setContent {
            OneOrderTheme {
                FoodFeedCard(
                    post = post,
                    onLike = {},
                    onComment = {},
                    onNavigateToRestaurantProfile = {},
                    onFollow = {},
                    onUnfollow = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Phở Bát Đàn").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phở Bò Đặc Biệt").assertIsDisplayed()
        composeTestRule.onNodeWithText("Phở bò truyền thống cực ngon").assertIsDisplayed()
    }
}
