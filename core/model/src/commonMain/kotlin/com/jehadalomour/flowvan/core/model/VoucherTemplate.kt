package com.jehadalomour.flowvan.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Data-driven configuration for the printed sales / return voucher. Defaults target the
 * Jordan rollout: 16% GST, Jordanian Dinar (3-dp fils), JoFotara/ISTD tax-QR labeling, and
 * pure black-on-white printing.
 *
 * Every field carries [SerialName] + a default so partial or older JSON — and the upcoming
 * server-fed template — deserializes without breaking. Keeping these here (rather than
 * hardcoded in the renderer) preserves the push-an-update-without-recompile approach.
 */
@Serializable
data class VoucherTemplate(
    /** Currency symbol appended to amounts. Jordanian Dinar: "د.أ" (AR), "JD" elsewhere. */
    @SerialName("currency") val currency: String = "د.أ",
    /** Decimal places for amounts. JOD uses 3 (fils). */
    @SerialName("amountDecimals") val amountDecimals: Int = 3,
    /** Default GST rate (percent) for new taxable items. Per-line rates still win. */
    @SerialName("defaultTaxPct") val defaultTaxPct: Double = 16.0,
    /** When true the renderer draws pure black/white, ignoring any color constants. */
    @SerialName("monochrome") val monochrome: Boolean = true,
    /** Caption under the tax QR. JoFotara/ISTD for Jordan — never ZATCA. */
    @SerialName("qrCaption") val qrCaption: String = "الرمز الضريبي (JoFotara - ISTD)",
    /** Master toggle for the payment-type field in both header and footer. */
    @SerialName("showPaymentType") val showPaymentType: Boolean = true,
    /** Show the payment type in the header info block (outlined box). */
    @SerialName("paymentTypeInHeader") val paymentTypeInHeader: Boolean = true,
    /** Show the payment type in the totals/footer block (plain bold). */
    @SerialName("paymentTypeInFooter") val paymentTypeInFooter: Boolean = true,
)
