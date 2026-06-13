package com.jehadalomour.flowvan.feature.ai

import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

fun aiModule(): Module = module {
    viewModel { (customerId: String?) ->
        AiAssistantViewModel(customerId, get(), get(), get(), get(), get(), get(), get())
    }
}
