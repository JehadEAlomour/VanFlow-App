package com.jehadalomour.flowvan.feature.customer

import com.jehadalomour.flowvan.feature.prospecting.FindCustomersViewModel

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun customerModule(): Module = module {
    // The one place customer ERP money is pulled into the offline cache — shared
    // by the dashboard and the statement screen.
    single { ErpCustomerSync(get(), get()) }

    viewModel { CustomerListViewModel(get(), get()) }
    // ONE definition — Koin keys viewModel by type, so a second def for the same
    // type silently replaces the first. The prefill is optional: the plain
    // create screen passes none, the from-search screen passes one.
    viewModel { params ->
        CreateCustomerViewModel(
            get(), get(), get(), get(), get(),
            params.getOrNull<CreateCustomerPrefill>(),
        )
    }
    viewModel { FindCustomersViewModel(get(), get()) }
    viewModel { (customerId: String) ->
        CustomerDashboardViewModel(customerId, get(), get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { (customerId: String) ->
        AccountStatementViewModel(customerId, get(), get(), get(), get(), get())
    }
}
