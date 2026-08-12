package com.jehadalomour.flowvan.feature.reports

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun reportsModule(): Module = module {
    viewModel { (customerId: String) ->
        TransactionReportViewModel(customerId, get(), get())
    }
    viewModel { (customerId: String) ->
        DetailedTxnReportViewModel(customerId, get(), get())
    }
    viewModel { (customerId: String) ->
        PaymentReportViewModel(customerId, get())
    }
    viewModel { (customerId: String) ->
        VoucherReportViewModel(customerId, get(), get())
    }
    viewModel { AllSalesReportViewModel(get(), get()) }
    viewModel { AllPaymentsReportViewModel(get(), get()) }
    viewModel { VisitReportViewModel(get(), get()) }
    viewModel { CashFlowReportViewModel(get(), get()) }
    viewModel { ItemsSalesReportViewModel(get()) }
    viewModel { ReceivablesReportViewModel(get()) }
    viewModel { TargetsViewModel(get()) }
}
