package com.jehadalomour.flowvan.core.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.model.CompanyInfo
import com.jehadalomour.flowvan.core.network.api.AuthApi

/**
 * Company profile for the printed voucher. Server-first when online — pulls `GET /company-info`,
 * caches it into [AppSettingsRepository] (the single config row) and returns it — otherwise (or on
 * any failure) returns the last cached values from the DB. Offline-first, never throws.
 */
class CompanyInfoRepository(
    private val authApi: AuthApi,
    private val connectivity: ConnectivityObserver,
    private val appSettings: AppSettingsRepository,
) {
    private val log = Logger.withTag("CompanyInfo")

    suspend fun getForPrint(): CompanyInfo {
        if (connectivity.isOnline()) {
            runCatching {
                val dto = authApi.companyInfo()
                val current = appSettings.get()
                val fresh = current.copy(
                    companyNameAr = dto.companyNameAr,
                    companyNameEn = dto.companyNameEn.orEmpty(),
                    companyTaxNumber = dto.sellerTin.orEmpty(),
                )
                if (fresh != current) appSettings.save(fresh)
                return CompanyInfo(fresh.companyNameAr, fresh.companyNameEn, fresh.companyTaxNumber)
            }.onFailure { log.w("company-info fetch failed, using cache: ${it.message}") }
        }
        val cached = appSettings.get()
        return CompanyInfo(cached.companyNameAr, cached.companyNameEn, cached.companyTaxNumber)
    }
}
