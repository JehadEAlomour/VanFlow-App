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
import com.jehadalomour.flowvan.screens.map.MapNavigationScreen
import com.jehadalomour.flowvan.screens.reports.AccountStatementScreen
import com.jehadalomour.flowvan.screens.reports.AllPaymentsReportScreen
import com.jehadalomour.flowvan.screens.reports.AllSalesReportScreen
import com.jehadalomour.flowvan.screens.reports.CashFlowReportScreen
import com.jehadalomour.flowvan.screens.reports.ItemsSalesReportScreen
import com.jehadalomour.flowvan.screens.reports.ReportsHubScreen
import com.jehadalomour.flowvan.screens.reports.VisitReportScreen
import com.jehadalomour.flowvan.screens.reports.PaymentReportScreen
import com.jehadalomour.flowvan.screens.reports.ReceiptDetailScreen
import com.jehadalomour.flowvan.screens.reports.ReceivablesReportScreen
import com.jehadalomour.flowvan.screens.reports.TransactionReportScreen
import com.jehadalomour.flowvan.screens.reports.VoucherDetailScreen
import com.jehadalomour.flowvan.screens.reports.VoucherReportScreen
import com.jehadalomour.flowvan.screens.collection.CollectionScreen
import com.jehadalomour.flowvan.screens.customer.CustomerDashboardScreen
import com.jehadalomour.flowvan.screens.customers.CustomerListScreen
import com.jehadalomour.flowvan.screens.endofday.EndOfDayScreen
import com.jehadalomour.flowvan.screens.home.HomeScreen
import com.jehadalomour.flowvan.screens.login.LoginScreen
import com.jehadalomour.flowvan.screens.route.RouteScreen
import com.jehadalomour.flowvan.screens.voucher.VoucherScreen
import com.jehadalomour.flowvan.shared.presentation.feature.voucher.VoucherType
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
    const val MAP = "map/{customerId}"
    const val TRANSACTION_REPORT = "txnreport/{customerId}"
    const val PAYMENT_REPORT = "payreport/{customerId}"
    const val ACCOUNT_STATEMENT = "statement/{customerId}"
    const val VOUCHER_REPORT = "voucherreport/{customerId}"
    const val VOUCHER_DETAIL = "voucher/{invoiceId}"
    const val RECEIPT_DETAIL = "receipt/{paymentId}"
    const val REPORTS_HUB = "reports"
    const val ALL_SALES_REPORT = "allsales"
    const val ALL_PAYMENTS_REPORT = "allpayments"
    const val VISIT_REPORT = "visitreport"
    const val CASH_FLOW_REPORT = "cashflow"
    const val ITEMS_SALES_REPORT = "itemssales"
    const val RECEIVABLES_REPORT = "receivables"
    fun customer(id: String) = "customer/$id"
    fun sale(id: String) = "sale/$id"
    fun returns(id: String) = "return/$id"
    fun request(id: String) = "request/$id"
    fun collection(id: String) = "collection/$id"
    fun ai(customerId: String? = null) = if (customerId != null) "ai?customerId=$customerId" else "ai"
    fun map(customerId: String) = "map/$customerId"
    fun txnReport(customerId: String) = "txnreport/$customerId"
    fun payReport(customerId: String) = "payreport/$customerId"
    fun statement(customerId: String) = "statement/$customerId"
    fun voucherReport(customerId: String) = "voucherreport/$customerId"
    fun voucher(invoiceId: String) = "voucher/$invoiceId"
    fun receipt(paymentId: String) = "receipt/$paymentId"
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
                onOpenReports = { navController.navigate(Routes.REPORTS_HUB) },
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
                onNavigateTo = { id -> navController.navigate(Routes.map(id)) },
            )
        }
        composable(Routes.CUSTOMERS) {
            CustomerListScreen(
                onBack = { navController.popBackStack() },
                onOpenCustomer = { id -> navController.navigate(Routes.customer(id)) },
                onNavigateTo = { id -> navController.navigate(Routes.map(id)) },
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
                onOpenVoucherReport = { cid -> navController.navigate(Routes.voucherReport(cid)) },
                onOpenPaymentReport = { cid -> navController.navigate(Routes.payReport(cid)) },
                onOpenAccountStatement = { cid -> navController.navigate(Routes.statement(cid)) },
            )
        }
        composable(
            Routes.SALE,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            VoucherScreen(customerId = id, type = VoucherType.SALE, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.RETURN,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            VoucherScreen(customerId = id, type = VoucherType.RETURN, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.REQUEST,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            VoucherScreen(customerId = id, type = VoucherType.ORDER, onBack = { navController.popBackStack() })
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
        composable(
            Routes.MAP,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            MapNavigationScreen(customerId = id, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.TRANSACTION_REPORT,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            TransactionReportScreen(customerId = id, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.PAYMENT_REPORT,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            PaymentReportScreen(
                customerId = id,
                onBack = { navController.popBackStack() },
                onOpenReceipt = { pid -> navController.navigate(Routes.receipt(pid)) },
            )
        }
        composable(
            Routes.ACCOUNT_STATEMENT,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            AccountStatementScreen(
                customerId = id,
                onBack = { navController.popBackStack() },
                onOpenInvoice = { iid -> navController.navigate(Routes.voucher(iid)) },
                onOpenReceipt = { pid -> navController.navigate(Routes.receipt(pid)) },
            )
        }
        composable(
            Routes.VOUCHER_REPORT,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            VoucherReportScreen(
                customerId = id,
                onBack = { navController.popBackStack() },
                onOpenVoucher = { iid -> navController.navigate(Routes.voucher(iid)) },
            )
        }
        composable(
            Routes.VOUCHER_DETAIL,
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("invoiceId").orEmpty()
            VoucherDetailScreen(invoiceId = id, onBack = { navController.popBackStack() })
        }
        composable(
            Routes.RECEIPT_DETAIL,
            arguments = listOf(navArgument("paymentId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("paymentId").orEmpty()
            ReceiptDetailScreen(paymentId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.REPORTS_HUB) {
            ReportsHubScreen(
                onBack = { navController.popBackStack() },
                onOpenSalesReport = { navController.navigate(Routes.ALL_SALES_REPORT) },
                onOpenPaymentsReport = { navController.navigate(Routes.ALL_PAYMENTS_REPORT) },
                onOpenVisitReport = { navController.navigate(Routes.VISIT_REPORT) },
                onOpenCashFlow = { navController.navigate(Routes.CASH_FLOW_REPORT) },
                onOpenItemsSales = { navController.navigate(Routes.ITEMS_SALES_REPORT) },
                onOpenReceivables = { navController.navigate(Routes.RECEIVABLES_REPORT) },
            )
        }
        composable(Routes.RECEIVABLES_REPORT) {
            ReceivablesReportScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ALL_SALES_REPORT) {
            AllSalesReportScreen(
                onBack = { navController.popBackStack() },
                onOpenVoucher = { id -> navController.navigate(Routes.voucher(id)) },
            )
        }
        composable(Routes.ALL_PAYMENTS_REPORT) {
            AllPaymentsReportScreen(
                onBack = { navController.popBackStack() },
                onOpenReceipt = { id -> navController.navigate(Routes.receipt(id)) },
            )
        }
        composable(Routes.VISIT_REPORT) {
            VisitReportScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CASH_FLOW_REPORT) {
            CashFlowReportScreen(
                onBack = { navController.popBackStack() },
                onOpenVoucher = { id -> navController.navigate(Routes.voucher(id)) },
                onOpenReceipt = { id -> navController.navigate(Routes.receipt(id)) },
            )
        }
        composable(Routes.ITEMS_SALES_REPORT) {
            ItemsSalesReportScreen(onBack = { navController.popBackStack() })
        }
    }
}
