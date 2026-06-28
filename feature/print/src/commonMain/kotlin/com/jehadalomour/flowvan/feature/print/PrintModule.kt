package com.jehadalomour.flowvan.feature.print

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun printModule(): Module = module {
    viewModel { (invoiceId: String) ->
        VoucherDetailViewModel(invoiceId, get(), get())
    }
    viewModel { (invoiceId: String) ->
        VoucherPrintViewModel(invoiceId, get(), get(), get(), get(), get(), get())
    }
    viewModel { (paymentId: String) ->
        ReceiptDetailViewModel(paymentId, get())
    }
}
