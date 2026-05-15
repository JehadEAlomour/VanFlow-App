package com.jehadalomour.flowvan.navigation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jehadalomour.flowvan.screens.home.HomePlaceholderScreen
import com.jehadalomour.flowvan.screens.login.LoginScreen
import com.jehadalomour.flowvan.shared.data.seeder.DemoSeeder
import com.jehadalomour.flowvan.shared.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.LogoutUseCase
import org.koin.compose.koinInject

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
}

@Composable
fun FlowVanNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val seeder: DemoSeeder = koinInject()
    val getCurrentUser: GetCurrentUserUseCase = koinInject()
    val logout: LogoutUseCase = koinInject()

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        seeder.seedIfNeeded()
        startDestination = if (getCurrentUser() != null) Routes.HOME else Routes.LOGIN
    }

    val dest = startDestination
    if (dest == null) {
        CircularProgressIndicator()
        return
    }

    NavHost(navController = navController, startDestination = dest) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.HOME) {
            HomePlaceholderScreen(
                onLogout = {
                    logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
