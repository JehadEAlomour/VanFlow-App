package com.jehadalomour.flowvan.core.model

import kotlinx.serialization.Serializable

/**
 * Cash-vs-credit settlement shown on the printed voucher. Labels live on the enum (not in
 * Res) because core/model can't depend on the resource layer, and because the printed value
 * (نقدي / آجل) is the same in every market — color, by contrast, belongs in [VoucherTemplate].
 */
@Serializable
enum class PaymentType(val labelAr: String, val labelEn: String) {
    CASH(labelAr = "نقدي", labelEn = "Cash"),
    CREDIT(labelAr = "آجل", labelEn = "Credit");

    companion object {
        /**
         * Bucket the stored payment method into the two voucher buckets: CREDIT (deferred / ذمم)
         * prints آجل; every immediate settlement (CASH, CHEQUE, TRANSFER, CARD, or unknown/null)
         * prints نقدي.
         */
        fun fromPaymentMethod(method: String?): PaymentType =
            if (method.equals("CREDIT", ignoreCase = true)) CREDIT else CASH
    }
}
