package com.jehadalomour.flowvan.feature.reports

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun reportsModule(): Module = module {
    viewModel { (customerId: String) ->
        TransactionReportViewModel(customerId, get())
    }
    viewModel { (customerId: String) ->
        PaymentReportViewModel(customerId, get())
    }
    viewModel { (customerId: String) ->
        VoucherReportViewModel(customerId, get())
    }
    viewModel { AllSalesReportViewModel(get()) }
    viewModel { AllPaymentsReportViewModel(get()) }
    viewModel { VisitReportViewModel(get(), get()) }
    viewModel { CashFlowReportViewModel(get(), get()) }
    viewModel { ItemsSalesReportViewModel(get()) }
    viewModel { ReceivablesReportViewModel(get()) }
}
