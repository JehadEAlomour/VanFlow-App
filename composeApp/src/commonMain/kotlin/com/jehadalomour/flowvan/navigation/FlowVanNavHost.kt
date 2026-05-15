package com.jehadalomour.flowvan.navigation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jehadalomour.flowvan.screens.components.ComingSoonScreen
import com.jehadalomour.flowvan.screens.customers.CustomerListScreen
import com.jehadalomour.flowvan.screens.home.HomeScreen
import com.jehadalomour.flowvan.screens.login.LoginScreen
import com.jehadalomour.flowvan.screens.route.RouteScreen
import com.jehadalomour.flowvan.shared.data.seeder.DemoSeeder
import com.jehadalomour.flowvan.shared.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.shared.domain.usecase.LogoutUseCase
import org.koin.compose.koinInject

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ROUTE = "route"
    const val CUSTOMERS = "customers"
    const val VAN_STOCK = "van_stock"
    const val AI = "ai"
    const val END_OF_DAY = "end_of_day"
    const val CUSTOMER = "customer/{customerId}"
    fun customer(id: String) = "customer/$id"
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

    val dest = startDestination ?: run {
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
            HomeScreen(
                onOpenRoute = { navController.navigate(Routes.ROUTE) },
                onOpenCustomers = { navController.navigate(Routes.CUSTOMERS) },
                onOpenVanStock = { navController.navigate(Routes.VAN_STOCK) },
                onOpenAi = { navController.navigate(Routes.AI) },
                onOpenEndOfDay = { navController.navigate(Routes.END_OF_DAY) },
                onOpenCustomer = { id -> navController.navigate(Routes.customer(id)) },
                onLogout = {
                    logout()
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.ROUTE) {
            RouteScreen(
                onBack = { navController.popBackStack() },
                onOpenCustomer = { id -> navController.navigate(Routes.customer(id)) },
            )
        }
        composable(Routes.CUSTOMERS) {
            CustomerListScreen(
                onBack = { navController.popBackStack() },
                onOpenCustomer = { id -> navController.navigate(Routes.customer(id)) },
            )
        }
        composable(
            Routes.CUSTOMER,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            ComingSoonScreen(
                titleAr = "بطاقة العميل ($id)",
                titleEn = "Customer dashboard",
                phaseLabel = "P3 — M07",
                onBack = { navController.popBackStack() },
            )
        }
        composable(Routes.VAN_STOCK) {
            ComingSoonScreen("مخزون الفان", "Van Stock", "P4 — M14") { navController.popBackStack() }
        }
        composable(Routes.AI) {
            ComingSoonScreen("المساعد الذكي", "AI Assistant", "P4 — M13") { navController.popBackStack() }
        }
        composable(Routes.END_OF_DAY) {
            ComingSoonScreen("نهاية اليوم", "End of Day", "P4 — M15") { navController.popBackStack() }
        }
    }
}
