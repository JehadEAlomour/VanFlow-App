package com.jehadalomour.flowvan.feature.print

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun printModule(): Module = module {
    viewModel { (invoiceId: String) ->
        VoucherDetailViewModel(invoiceId, get(), get(), get())
    }
    viewModel { (invoiceId: String) ->
        VoucherPrintViewModel(invoiceId, get(), get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { (paymentId: String) ->
        ReceiptDetailViewModel(paymentId, get(), get(), get(), get(), get())
    }
    viewModel {
        VoucherSummaryViewModel(get(), get(), get(), get(), get(), get())
    }
    viewModel { (customerId: String, fromMillis: Long, toMillis: Long) ->
        TxnReportPrintViewModel(
            customerId, fromMillis, toMillis,
            get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { (customerId: String, fromMillis: Long, toMillis: Long) ->
        StatementPrintViewModel(
            customerId, fromMillis, toMillis,
            get(), get(), get(), get(), get(), get(), get(),
        )
    }
}
