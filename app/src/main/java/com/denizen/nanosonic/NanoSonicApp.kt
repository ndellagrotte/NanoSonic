package com.denizen.nanosonic

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.denizen.nanosonic.ui.screens.splash.SplashScreen
import com.denizen.nanosonic.ui.screens.wizard.WizardScreen
import com.denizen.nanosonic.ui.screens.main.MainScreen

/**
 * Main app navigation component for NanoSonic
 * Defines all routes and navigation logic
 */
@Composable  // ← Just a regular Composable, not @HiltAndroidApp
fun NanoSonicApp() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(route = Screen.Splash.route) {
            SplashScreen(
                onNavigateToWizard = {
                    navController.navigate(Screen.Wizard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
//
//        composable(route = Screen.Register.route) {
//            RegisterScreen(
//                onNavigateToLogin = {
//                    navController.navigate(Screen.Login.route) {
//                        popUpTo(Screen.Register.route) { inclusive = true }
//                    }
//                },
//                onNavigateToWizard = {
//                    navController.navigate(Screen.Wizard.route) {
//                        popUpTo(Screen.Register.route) { inclusive = true }
//                    }
//                }
//            )
//        }
//
//        composable(route = Screen.Login.route) {
//            LoginScreen(
//                onNavigateToRegister = {
//                    navController.navigate(Screen.Register.route) {
//                        popUpTo(Screen.Login.route) { inclusive = true }
//                    }
//                },
//                onNavigateToMain = {
//                    navController.navigate(Screen.Main.route) {
//                        popUpTo(0) { inclusive = true }
//                    }
//                }
//            )
//        }

        composable(route = Screen.Wizard.route) {
            WizardScreen(
                onNavigateToMain = {
                    navController.navigate(Screen.Main.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(route = Screen.Main.route) {
            MainScreen(
                onNavigateToWizard = {
                    navController.navigate(Screen.Wizard.route)
                }
            )
        }
    }
}

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Wizard : Screen("wizard")
    object Main : Screen("main")
}