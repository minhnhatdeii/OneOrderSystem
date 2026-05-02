package com.example.oneorder.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.oneorder.ui.screens.auth.LoginScreen
import com.example.oneorder.ui.screens.auth.MfaSetupScreen
import com.example.oneorder.ui.screens.auth.MfaVerificationScreen
import com.example.oneorder.ui.screens.auth.MfaBackupCodeScreen
import com.example.oneorder.ui.screens.auth.RegisterScreen
import com.example.oneorder.ui.screens.auth.ForgotPasswordScreen
import com.example.oneorder.ui.screens.auth.ChangePasswordScreen
import com.example.oneorder.ui.screens.auth.PasswordResetScreen
import com.example.oneorder.ui.screens.checkout.CheckoutScreen
import com.example.oneorder.ui.screens.foodfeed.FoodFeedScreen
import com.example.oneorder.ui.screens.foodfeed.FoodFeedViewModel
import com.example.oneorder.ui.screens.menu.MenuScreen
import com.example.oneorder.ui.screens.order.OrderDetailScreen
import com.example.oneorder.ui.screens.profile.ProfileScreen
import com.example.oneorder.ui.screens.profile.FollowingRestaurantsScreen
import com.example.oneorder.ui.screens.profile.RestaurantProfileScreen
import com.example.oneorder.ui.screens.profile.RestaurantPostDetailScreen
import com.example.oneorder.ui.screens.qr.QRScannerScreen
import com.example.oneorder.ui.screens.main.MainScreen
import com.example.oneorder.ui.screens.home.HomeViewModel
import com.example.oneorder.MainActivity

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    // Check for deep link on start
    LaunchedEffect(Unit) {
        MainActivity.pendingDeepLink?.let { deepLink ->
            when {
                deepLink.passwordResetToken != null -> {
                    navController.navigate(Screen.PasswordReset(
                        token = deepLink.passwordResetToken,
                        email = deepLink.passwordResetEmail
                    )) {
                        popUpTo(0) { inclusive = true }
                    }
                }
                deepLink.mfaToken != null -> {
                    // Navigate to MFA verification with token
                    navController.navigate(Screen.MfaVerification("")) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
            MainActivity.pendingDeepLink = null
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Login()  // Always require login first
    ) {

        // ─── FOOD FEED: Default screen (first thing user sees after login) ───
        // Named route + NavGraph-scoped ViewModel so state survives navigation
        composable(
            route = "food_feed",
            arguments = emptyList()
        ) { backStackEntry ->
            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry("food_feed")
            }
            val viewModel: FoodFeedViewModel = hiltViewModel(parentEntry)
            FoodFeedScreen(
                onNavigateToProfile = { navController.navigate(Screen.Profile) },
                onNavigateToOrderMode = { navController.navigate(Screen.OrderMode) },
                onNavigateToRestaurantProfile = { restaurantId ->
                    navController.navigate(Screen.RestaurantProfile(restaurantId))
                },
                viewModel = viewModel
            )
        }

        // ─── PROFILE: Accessed from FoodFeed top-left icon ───
        composable<Screen.Profile>(
            enterTransition = { slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(300)) }
        ) {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSecuritySettings = { navController.navigate(Screen.SecuritySettings) },
                onNavigateToFollowing = { navController.navigate(Screen.Following) },
                onSignedOut = {
                    navController.navigate(Screen.Login()) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ─── FOLLOWING RESTAURANTS ───
        composable<Screen.Following>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            FollowingRestaurantsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRestaurant = { tenantId ->
                    navController.navigate(Screen.RestaurantProfile(tenantId))
                }
            )
        }

        // ─── RESTAURANT PROFILE: Accessed from FoodFeed restaurant avatar ───
        composable<Screen.RestaurantProfile>(
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.RestaurantProfile>()
            RestaurantProfileScreen(
                restaurantId = route.restaurantId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPostDetail = { tenantId, postId ->
                    navController.navigate(Screen.RestaurantPostDetail(tenantId, postId))
                }
            )
        }

        composable<Screen.RestaurantPostDetail>(
            enterTransition = { slideInVertically(initialOffsetY = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutVertically(targetOffsetY = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.RestaurantPostDetail>()
            RestaurantPostDetailScreen(
                tenantId = route.tenantId,
                postId = route.postId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToRestaurantProfile = { navController.popBackStack() }
            )
        }

        // ─── ORDER MODE: Full ordering flow — accessed from FoodFeed top-right icon ───
        composable<Screen.OrderMode>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Screen.OrderMode)
            }
            val homeViewModel = hiltViewModel<HomeViewModel>(parentEntry)
            MainScreen(
                homeViewModel = homeViewModel,
                onNavigateBack = { navController.popBackStack() },  // ⇐ Back to FoodFeed
                onNavigateToMenu = { categoryId -> navController.navigate(Screen.Menu(categoryId)) },
                onNavigateToQRScanner = { navController.navigate(Screen.QRScanner) },
                onNavigateToCheckout = { navController.navigate(Screen.Checkout) },
                onNavigateToOrderDetail = { orderId -> navController.navigate(Screen.OrderDetail(orderId)) }
            )
        }

        // ─── AUTH ───
        composable<Screen.Login> { backStackEntry ->
            val login = backStackEntry.toRoute<Screen.Login>()
            LoginScreen(
                onNavigateToRegister = { navController.navigate(Screen.Register) },
                onNavigateToHome = {
                    navController.navigate("food_feed") {
                        popUpTo(Screen.Login()) { inclusive = true }
                    }
                },
                onNavigateToMfa = { email ->
                    navController.navigate(Screen.MfaVerification(email))
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword)
                },
                successMessage = login.message
            )
        }
        composable<Screen.Register> {
            RegisterScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = { message ->
                    navController.navigate(Screen.Login(message = message)) {
                        popUpTo(Screen.Login()) { inclusive = true }
                    }
                }
            )
        }

        // ─── MFA SECURITY SCREENS ───

        // MFA Verification (after login if MFA enabled)
        composable<Screen.MfaVerification>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.MfaVerification>()
            MfaVerificationScreen(
                email = route.email,
                onVerified = { verifiedEmail ->
                    // Register device after MFA verification
                    navController.navigate("food_feed") {
                        popUpTo(Screen.Login()) { inclusive = true }
                    }
                },
                onBack = {
                    // Go back to login
                    navController.popBackStack()
                },
                onUseBackupCode = {
                    navController.navigate(Screen.MfaBackupCode(route.email))
                }
            )
        }

        // MFA Backup Code Verification
        composable<Screen.MfaBackupCode>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val route = backStackEntry.toRoute<Screen.MfaBackupCode>()
            MfaBackupCodeScreen(
                email = route.email,
                onVerified = { verifiedEmail ->
                    navController.navigate("food_feed") {
                        popUpTo(Screen.Login()) { inclusive = true }
                    }
                },
                onBack = {
                    navController.popBackStack()
                },
                onBackToOtp = {
                    navController.popBackStack()
                }
            )
        }

        // MFA Setup Screen (from Profile)
        composable<Screen.MfaSetup>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            MfaSetupScreen(
                onBack = { navController.popBackStack() },
                onSetupComplete = { navController.popBackStack() },
                onNavigateToChangePassword = { navController.navigate(Screen.ChangePassword) }
            )
        }

        // Security Settings Screen (from Profile)
        composable<Screen.SecuritySettings>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            MfaSetupScreen(
                onBack = { navController.popBackStack() },
                onSetupComplete = { navController.popBackStack() },
                onNavigateToChangePassword = { navController.navigate(Screen.ChangePassword) }
            )
        }

        // Forgot Password Screen
        composable<Screen.ForgotPassword>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login()) {
                        popUpTo(Screen.Login()) { inclusive = true }
                    }
                }
            )
        }

        // Change Password Screen
        composable<Screen.ChangePassword>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            ChangePasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onPasswordChanged = { navController.popBackStack() }
            )
        }

        // Password Reset Screen (from email deep link)
        composable<Screen.PasswordReset> { backStackEntry ->
            val route: Screen.PasswordReset = backStackEntry.toRoute()
            PasswordResetScreen(
                token = route.token,
                email = route.email,
                onNavigateBack = {
                    navController.navigate(Screen.Login()) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onPasswordResetComplete = {
                    navController.navigate(Screen.Login(message = "Đặt lại mật khẩu thành công! Đăng nhập với mật khẩu mới.")) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        // ─── FULL SCREEN DESTINATIONS ───
        composable<Screen.Menu>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val menu = backStackEntry.toRoute<Screen.Menu>()
            MenuScreen(
                categoryId = menu.categoryId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable<Screen.Checkout>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            CheckoutScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToOrderHistory = {
                    navController.navigate(Screen.OrderMode) {
                        popUpTo(Screen.OrderMode) { inclusive = true }
                    }
                }
            )
        }

        composable<Screen.QRScanner>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) {
            val parentEntry = remember(it) {
                navController.getBackStackEntry(Screen.OrderMode)
            }
            val homeViewModel = hiltViewModel<HomeViewModel>(parentEntry)
            QRScannerScreen(
                onNavigateBack = { navController.popBackStack() },
                onScanSuccess = { restaurantId, tableId ->
                    homeViewModel.setRestaurantAndTable(restaurantId, tableId)
                    navController.popBackStack()
                }
            )
        }

        composable<Screen.OrderDetail>(
            enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) }
        ) { backStackEntry ->
            val orderDetail = backStackEntry.toRoute<Screen.OrderDetail>()
            OrderDetailScreen(
                orderId = orderDetail.orderId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
