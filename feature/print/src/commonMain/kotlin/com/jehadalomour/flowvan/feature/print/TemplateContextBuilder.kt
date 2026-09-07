package com.jehadalomour.flowvan.feature.print

import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.PaymentType
import com.jehadalomour.flowvan.core.model.print.TemplateContext
import com.jehadalomour.flowvan.core.model.print.TemplateLine
import com.jehadalomour.flowvan.core.model.print.TemplateTokens
import com.jehadalomour.flowvan.core.model.print.VoucherKinds
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Gathers everything a designed template can print (spec §4) from the print screen's state.
 *
 * The money figures follow the built-in receipt's own footer so the two never disagree on a
 * counter: the subtotal is Σ(qty × price) including the gift lines' notional value, the
 * discount is what the receipt would itemise (offers when applied, else line + invoice
 * discount + the gifts), and a line's `total` is its gross — discount and tax are each
 * stated once, in the totals block.
 */
internal fun VoucherPrintState.toTemplateContext(
    /** The lines to print — the compacted view when the rep chose to merge rows. */
    shownLines: List<InvoiceLine> = lines,
    /** Stamp text for `{{invoice.taxExempt}}`; a resource string, so it comes from the UI. */
    taxExemptStamp: String,
    /** Column labels and the payment-type word follow the app locale. */
    isArabic: Boolean = true,
): TemplateContext {
    val t = template
    val freeGross = freeLines.sumOf { it.qty * it.unitPrice }
    val lineDiscount = shownLines.sumOf { it.qty * it.unitPrice * it.discountPct }
    val discount = if (appliedOffers.isNotEmpty()) {
        appliedOffers.sumOf { it.discountAmount }
    } else {
        lineDiscount + freeGross + discountAmount
    }
    val paymentType = PaymentType.fromPaymentMethod(paymentMethod)
    val paymentWord = if (isArabic) paymentType.labelAr else paymentType.labelEn
    // The app records one settlement per voucher and no tendered amount, so "paid" is the
    // total for an immediate settlement and nothing for a credit sale; change is always 0.
    val paid = if (paymentType == PaymentType.CREDIT) 0.0 else total
    val (date, time) = createdAt.toTemplateDateTime()
    val ctx = TemplateContext(
        companyNameAr = companyNameAr,
        companyNameEn = companyNameEn,
        companyTaxNumber = companyTaxNumber,
        companyPhone1 = companyPhone,
        companyLogo = companyLogo,
        branchName = branch,
        invoiceNumber = number,
        invoiceKind = VoucherKinds.documentTypeFor(type),
        invoiceDate = date,
        invoiceTime = time,
        cashier = salesmanNameAr,
        customerName = customerNameAr,
        customerNumber = customerCode,
        customerTaxNumber = customerTaxNumber.orEmpty(),
        store = branch,
        note = notes.orEmpty(),
        lines = shownLines.map { it.toTemplateLine(isGift = false) } +
            freeLines.map { it.toTemplateLine(isGift = true) },
        subtotal = subtotal + freeGross,
        discount = discount,
        taxTotal = taxAmount,
        total = total,
        paid = paid,
        change = 0.0,
        paymentType = paymentWord,
        isTaxExempt = isTaxExempt,
        taxExemptStamp = taxExemptStamp,
        taxExemptionNumber = taxExemptionNumber.orEmpty(),
        qrData = qrData.orEmpty(),
        decimals = t.amountDecimals,
        currency = t.currency,
    )
    // "CASH: 6.500 JOD" — one settlement, in the stored method's own code.
    val methodCode = paymentMethod?.uppercase()?.ifBlank { null } ?: paymentType.name
    return ctx.copy(payments = "$methodCode: ${TemplateTokens.money(paid, ctx)}")
}

/**
 * A gift is billed as a normal item at its real price with a 100 % discount, so it prints
 * a 0 price and 0 total with the whole gross as its discount — the same figures the
 * built-in receipt shows, so the two copies foot identically.
 */
private fun InvoiceLine.toTemplateLine(isGift: Boolean): TemplateLine {
    val gross = qty * unitPrice
    return TemplateLine(
        name = nameAr,
        sku = sku,
        barcode = "",
        qty = qty,
        unit = unit,
        price = if (isGift) 0.0 else unitPrice,
        taxRate = taxRate,
        discount = if (isGift) gross else gross * discountPct,
        tax = taxAmount,
        total = if (isGift) 0.0 else gross,
        isGift = isGift || discountPct >= 1.0,
    )
}

/** `YYYY-MM-DD` and `HH:mm` in the device zone, Latin digits. */
internal fun Long.toTemplateDateTime(): Pair<String, String> {
    val dt = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault())
    val date = "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')}"
    val time = "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}"
    return date to time
}
