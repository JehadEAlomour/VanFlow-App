package com.jehadalomour.flowvan.di

import com.jehadalomour.flowvan.feature.ai.aiModule
import com.jehadalomour.flowvan.feature.auth.authModule
import com.jehadalomour.flowvan.feature.customer.customerModule
import com.jehadalomour.flowvan.feature.home.homeModule
import com.jehadalomour.flowvan.feature.map.mapModule
import com.jehadalomour.flowvan.feature.print.printModule
import com.jehadalomour.flowvan.feature.reports.reportsModule
import com.jehadalomour.flowvan.feature.voucher.voucherModule
import org.koin.core.module.Module

/**
 * Koin modules contributed by the feature modules. Aggregated here in :composeApp
 * (the only module that depends on every :feature:*) and registered at Koin startup
 * on both Android and iOS. As features are extracted, add their module here.
 */
fun appFeatureModules(): List<Module> = listOf(
    authModule(),
    homeModule(),
    customerModule(),
    voucherModule(),
    reportsModule(),
    printModule(),
    aiModule(),
    mapModule(),
)
