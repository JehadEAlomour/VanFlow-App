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
import com.jehadalomour.flowvan.screens.ai.AiAssistantScreen
import com.jehadalomour.flowvan.screens.collection.CollectionScreen
import com.jehadalomour.flowvan.screens.customer.CustomerDashboardScreen
import com.jehadalomour.flowvan.screens.customers.CustomerListScreen
import com.jehadalomour.flowvan.screens.endofday.EndOfDayScreen
import com.jehadalomour.flowvan.screens.home.HomeScreen
import com.jehadalomour.flowvan.screens.login.LoginScreen
import com.jehadalomour.flowvan.screens.request.RequestVoucherScreen
import com.jehadalomour.flowvan.screens.returns.ReturnVoucherScreen
import com.jehadalomour.flowvan.screens.route.RouteScreen
import com.jehadalomour.flowvan.screens.sale.SaleVoucherScreen
import com.jehadalomour.flowvan.screens.vanstock.VanStockScreen
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
    const val AI = "ai?customerId={customerId}"
    const val END_OF_DAY = "end_of_day"
    const val CUSTOMER = "customer/{customerId}"
    const val SALE = "sale/{customerId}"
    const val RETURN = "return/{customerId}"
    const val REQUEST = "request/{customerId}"
    const val COLLECTION = "collection/{customerId}"
    fun customer(id: String) = "customer/$id"
    fun sale(id: String) = "sale/$id"
    fun returns(id: String) = "return/$id"
    fun request(id: String) = "request/$id"
    fun collection(id: String) = "collection/$id"
    fun ai(customerId: String? = null) = if (customerId != null) "ai?customerId=$customerId" else "ai"
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
            LoginScreen(onLoggedIn = {
                navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
            })
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenRoute = { navController.navigate(Routes.ROUTE) },
                onOpenCustomers = { navController.navigate(Routes.CUSTOMERS) },
                onOpenVanStock = { navController.navigate(Routes.VAN_STOCK) },
                onOpenAi = { navController.navigate(Routes.ai()) },
                onOpenEndOfDay = { navController.navigate(Routes.END_OF_DAY) },
                onOpenCustomer = { id -> navController.navigate(Routes.customer(id)) },
                onLogout = {
                    logout()
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
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
            CustomerDashboardScreen(
                customerId = id,
                onBack = { navController.popBackStack() },
                onOpenSale = { cid -> navController.navigate(Routes.sale(cid)) },
                onOpenReturn = { cid -> navController.navigate(Routes.returns(cid)) },
                onOpenRequest = { cid -> navController.navigate(Routes.request(cid)) },
                onOpenCollection = { cid -> navController.navigate(Routes.collection(cid)) },
                onOpenAi = { cid -> navController.navigate(Routes.ai(cid)) },
            )
        }
        composable(
            Routes.SALE,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            SaleVoucherScreen(customerId = id, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.RETURN,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            ReturnVoucherScreen(customerId = id, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.REQUEST,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            RequestVoucherScreen(customerId = id, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.COLLECTION,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            CollectionScreen(customerId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.VAN_STOCK) {
            VanStockScreen(onBack = { navController.popBackStack() })
        }
        composable(
            Routes.AI,
            arguments = listOf(
                navArgument("customerId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val customerId = entry.arguments?.getString("customerId")
            AiAssistantScreen(customerId = customerId, onBack = { navController.popBackStack() })
        }
        composable(Routes.END_OF_DAY) {
            EndOfDayScreen(
                onLoggedOut = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(Routes.HOME) { inclusive = true }
                    }
                },
            )
        }
    }
}
