package com.denizen.nanosonic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.denizen.nanosonic.ui.main.MainScreen
import com.denizen.nanosonic.ui.welcome.WelcomeScreen
import com.denizen.nanosonic.ui.wizard.WizardScreen
import kotlinx.serialization.Serializable

@Serializable data object WelcomeRoute

/** @property firstRun true during initial setup; finishing then opens the main screen. */
@Serializable data class WizardRoute(val firstRun: Boolean)

@Serializable data object MainRoute

@Composable
fun NanoSonicNavHost(startDestination: Any) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = startDestination) {
        composable<WelcomeRoute> {
            WelcomeScreen(onGetStarted = { navController.navigate(WizardRoute(firstRun = true)) })
        }

        composable<WizardRoute> { entry ->
            val route = entry.toRoute<WizardRoute>()
            WizardScreen(
                canExit = !route.firstRun,
                onExit = { navController.popBackStack() },
                onFinished = {
                    if (route.firstRun) {
                        navController.navigate(MainRoute) { popUpTo(navController.graph.id) { inclusive = true } }
                    } else {
                        navController.popBackStack()
                    }
                },
            )
        }

        composable<MainRoute> {
            MainScreen(onOpenWizard = { navController.navigate(WizardRoute(firstRun = false)) })
        }
    }
}
