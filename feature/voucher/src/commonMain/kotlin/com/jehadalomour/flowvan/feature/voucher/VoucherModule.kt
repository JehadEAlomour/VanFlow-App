package com.jehadalomour.flowvan.feature.voucher

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun voucherModule(): Module = module {
    viewModel { (customerId: String) ->
        ReturnVoucherViewModel(customerId, get(), get(), get(), get())
    }
    viewModel { (customerId: String) ->
        RequestVoucherViewModel(customerId, get(), get(), get(), get())
    }
    viewModel { (customerId: String, type: VoucherType) ->
        VoucherViewModel(
            customerId, type, get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(), get(), get(), get(), get(),
        )
    }
    viewModel { (customerId: String) ->
        CollectionViewModel(customerId, get(), get(), get())
    }
    viewModel { VanStockViewModel(get()) }
}
