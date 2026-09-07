package com.jehadalomour.flowvan.core.network.api

import com.jehadalomour.flowvan.core.model.print.PrintTemplatesResponse
import com.jehadalomour.flowvan.core.network.http.FlowVanApiClient
import com.jehadalomour.flowvan.core.network.http.getData

/**
 * `GET /invoice-templates/resolve-all?storeNumber=<whNumber>` — every voucher kind's
 * resolved print template (store-pinned → company default → built-in) in one call, for the
 * device to cache. See cash-van `docs/SPEC-print-templates.md` §2.
 */
class PrintTemplateApi(private val client: FlowVanApiClient) {

    /** [storeNumber] is the van's `whNumber`; null asks for the company defaults only. */
    suspend fun resolveAll(storeNumber: String? = null): PrintTemplatesResponse =
        client.getData("invoice-templates/resolve-all", mapOf("storeNumber" to storeNumber))
}
