package com.jehadalomour.flowvan.feature.customer

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun customerModule(): Module = module {
    viewModel { CustomerListViewModel(get(), get()) }
    viewModel { CreateCustomerViewModel(get(), get(), get(), get(), get()) }
    viewModel { (customerId: String) ->
        CustomerDashboardViewModel(customerId, get(), get(), get(), get(), get(), get())
    }
    viewModel { (customerId: String) ->
        AccountStatementViewModel(customerId, get(), get(), get())
    }
}
