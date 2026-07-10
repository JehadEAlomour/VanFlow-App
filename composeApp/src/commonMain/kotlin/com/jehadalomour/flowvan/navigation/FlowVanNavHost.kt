package com.jehadalomour.flowvan.navigation

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jehadalomour.flowvan.feature.ai.AiAssistantScreen
import com.jehadalomour.flowvan.feature.home.SettingsScreen
import com.jehadalomour.flowvan.feature.map.MapNavigationScreen
import com.jehadalomour.flowvan.feature.customer.AccountStatementScreen
import com.jehadalomour.flowvan.feature.reports.AllPaymentsReportScreen
import com.jehadalomour.flowvan.feature.reports.AllSalesReportScreen
import com.jehadalomour.flowvan.feature.reports.CashFlowReportScreen
import com.jehadalomour.flowvan.feature.reports.ItemsSalesReportScreen
import com.jehadalomour.flowvan.feature.reports.ReportsHubScreen
import com.jehadalomour.flowvan.feature.reports.VisitReportScreen
import com.jehadalomour.flowvan.feature.reports.PaymentReportScreen
import com.jehadalomour.flowvan.feature.print.ReceiptDetailScreen
import com.jehadalomour.flowvan.feature.reports.ReceivablesReportScreen
import com.jehadalomour.flowvan.feature.reports.TargetsScreen
import com.jehadalomour.flowvan.feature.reports.TransactionReportScreen
import com.jehadalomour.flowvan.feature.print.VoucherDetailScreen
import com.jehadalomour.flowvan.feature.reports.VoucherReportScreen
import com.jehadalomour.flowvan.feature.voucher.CollectionScreen
import com.jehadalomour.flowvan.feature.customer.CreateCustomerScreen
import com.jehadalomour.flowvan.feature.customer.CustomerDashboardScreen
import com.jehadalomour.flowvan.feature.customer.CustomerListScreen
import com.jehadalomour.flowvan.feature.home.EndOfDayScreen
import com.jehadalomour.flowvan.feature.home.HomeScreen
import com.jehadalomour.flowvan.feature.home.OffersScreen
import com.jehadalomour.flowvan.feature.auth.LoginScreen
import com.jehadalomour.flowvan.feature.home.TodayRouteScreen
import com.jehadalomour.flowvan.feature.print.VoucherPrintScreen
import com.jehadalomour.flowvan.feature.print.VoucherSummaryScreen
import com.jehadalomour.flowvan.feature.voucher.VoucherScreen
import com.jehadalomour.flowvan.feature.voucher.VoucherType
import com.jehadalomour.flowvan.feature.voucher.VanStockScreen
import com.jehadalomour.flowvan.core.domain.usecase.GetCurrentUserUseCase
import com.jehadalomour.flowvan.core.domain.usecase.LogoutUseCase
import com.jehadalomour.flowvan.core.datastore.SessionStore
import org.koin.compose.koinInject

object Routes {
    const val LOGIN = "login"
    const val HOME = "home"
    const val ROUTE = "route"
    const val CUSTOMERS = "customers"
    const val CREATE_CUSTOMER = "create_customer"
    const val VAN_STOCK = "van_stock"
    const val AI = "ai?customerId={customerId}"
    const val END_OF_DAY = "end_of_day"
    const val OFFERS = "offers"
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
    const val TARGETS_REPORT = "targets"
    const val VOUCHER_PRINT = "voucherprint/{invoiceId}"
    const val VOUCHER_SUMMARY = "vouchersummary"
    const val SETTINGS = "settings"
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
    fun voucherPrint(invoiceId: String) = "voucherprint/$invoiceId"
}

@Composable
fun FlowVanNavHost(
    navController: NavHostController = rememberNavController(),
) {
    val getCurrentUser: GetCurrentUserUseCase = koinInject()
    val logout: LogoutUseCase = koinInject()
    val sessionStore: SessionStore = koinInject()
    val scope = rememberCoroutineScope()

    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        startDestination = if (getCurrentUser() != null) Routes.HOME else Routes.LOGIN
    }

    LaunchedEffect(Unit) {
        sessionStore.unauthorizedEvents.collect {
            navController.navigate(Routes.LOGIN) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val dest = startDestination ?: run {
        CircularProgressIndicator()
        return
    }

    NavHost(navController = navController, startDestination = dest) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onOpenRoute = { navController.navigate(Routes.ROUTE) },
                onOpenCustomers = { navController.navigate(Routes.CUSTOMERS) },
                onOpenVanStock = { navController.navigate(Routes.VAN_STOCK) },
                onOpenAi = { navController.navigate(Routes.ai()) },
                onOpenEndOfDay = { navController.navigate(Routes.END_OF_DAY) },
                onOpenReports = { navController.navigate(Routes.REPORTS_HUB) },
                onOpenOffers = { navController.navigate(Routes.OFFERS) },
                onOpenCustomer = { id -> navController.navigate(Routes.customer(id)) },
                onLogout = {
                    scope.launch {
                        logout()
                        navController.navigate(Routes.LOGIN) { popUpTo(Routes.HOME) { inclusive = true } }
                    }
                },
            )
        }
        composable(Routes.ROUTE) {
            TodayRouteScreen(
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
                onAddCustomer = { navController.navigate(Routes.CREATE_CUSTOMER) },
            )
        }
        composable(Routes.CREATE_CUSTOMER) {
            CreateCustomerScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id ->
                    // Replace the form with the new customer's page (back returns to the list).
                    navController.navigate(Routes.customer(id)) {
                        popUpTo(Routes.CREATE_CUSTOMER) { inclusive = true }
                    }
                },
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
            VoucherScreen(
                customerId = id,
                type = VoucherType.SALE,
                onBack = { navController.popBackStack() },
                onPrint = { invoiceId ->
                    navController.navigate(Routes.voucherPrint(invoiceId)) {
                        popUpTo(Routes.customer(id)) { inclusive = false }
                    }
                },
            )
        }
        composable(
            Routes.RETURN,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            VoucherScreen(
                customerId = id,
                type = VoucherType.RETURN,
                onBack = { navController.popBackStack() },
                onPrint = { invoiceId ->
                    navController.navigate(Routes.voucherPrint(invoiceId)) {
                        popUpTo(Routes.customer(id)) { inclusive = false }
                    }
                },
            )
        }
        composable(
            Routes.REQUEST,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            VoucherScreen(
                customerId = id,
                type = VoucherType.ORDER,
                onBack = { navController.popBackStack() },
                onPrint = { invoiceId ->
                    navController.navigate(Routes.voucherPrint(invoiceId)) {
                        popUpTo(Routes.customer(id)) { inclusive = false }
                    }
                },
            )
        }
        composable(
            Routes.COLLECTION,
            arguments = listOf(navArgument("customerId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("customerId").orEmpty()
            CollectionScreen(
                customerId = id,
                onBack = { navController.popBackStack() },
                onSaved = { paymentId ->
                    // Replace the form with its printable receipt (back returns to the customer).
                    navController.navigate(Routes.receipt(paymentId)) {
                        popUpTo(Routes.COLLECTION) { inclusive = true }
                    }
                },
            )
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
        composable(Routes.OFFERS) {
            OffersScreen(onBack = { navController.popBackStack() })
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
            VoucherDetailScreen(
                invoiceId = id,
                onBack = { navController.popBackStack() },
                onPrint = { iid -> navController.navigate(Routes.voucherPrint(iid)) },
            )
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
                onOpenTargets = { navController.navigate(Routes.TARGETS_REPORT) },
                onOpenVoucherSummary = { navController.navigate(Routes.VOUCHER_SUMMARY) },
            )
        }
        composable(Routes.VOUCHER_SUMMARY) {
            VoucherSummaryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.RECEIVABLES_REPORT) {
            ReceivablesReportScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TARGETS_REPORT) {
            TargetsScreen(onBack = { navController.popBackStack() })
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
        composable(
            Routes.VOUCHER_PRINT,
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("invoiceId").orEmpty()
            VoucherPrintScreen(invoiceId = id, onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
