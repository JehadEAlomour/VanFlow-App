package com.jehadalomour.flowvan.core.network.mapper

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.network.dto.CreateChequeRequest
import com.jehadalomour.flowvan.core.network.dto.CreateCollectionRequest
import com.jehadalomour.flowvan.core.network.dto.CreateInvoiceLine
import com.jehadalomour.flowvan.core.network.dto.CreateInvoiceRequest
import com.jehadalomour.flowvan.core.network.dto.CreateVoucherRequest
import com.jehadalomour.flowvan.core.network.dto.VoucherPayment
import com.jehadalomour.flowvan.core.network.dto.VoucherTxn
import com.jehadalomour.flowvan.core.network.http.jodToFils
import com.jehadalomour.flowvan.core.network.http.toAmountString
import com.jehadalomour.flowvan.core.network.http.toPercentString
import com.jehadalomour.flowvan.core.model.InvoiceLine
import kotlinx.serialization.json.Json

/**
 * Maps locally-saved (offline) transaction entities to the VanFlow create-request bodies.
 * Money goes JOD → fils here; [repId] is the backend rep id from the session (NOT salesmanId).
 */

fun InvoiceEntity.toCreateRequest(repId: String, json: Json): CreateInvoiceRequest {
    val lines = json.decodeFromString<List<InvoiceLine>>(linesJson)
    return CreateInvoiceRequest(
        customerId = customerId,
        repId = repId,
        lines = lines.map { line ->
            CreateInvoiceLine(
                productId = line.productId,
                quantity = line.qty,
                unitPrice = line.unitPrice.jodToFils().toLong(),
                lineDiscountType = "PERCENTAGE",
                lineDiscountValue = line.discountPct * 100.0,
            )
        },
        invoiceDiscountType = if (discountAmount > 0.0) "FIXED_AMOUNT" else null,
        invoiceDiscountValue = if (discountAmount > 0.0) discountAmount.jodToFils().toDouble() else null,
        paymentMethodCode = if (paymentMethod == "CREDIT") "022" else "012",
        note = notes,
        deviceId = id,                 // local id — lets the backend dedupe replays
    )
}

/**
 * Maps a local voucher entity (SALE / RETURN / REQUEST) to the unified `POST /vouchers` body.
 * `transKind`: SALE → SALE, RETURN → RETURN, REQUEST → ORDER. Money/qty are numeric strings.
 * Voucher transactions reference the legacy `itemNumber` (our `sku`) and the `customerNumber`.
 */
fun InvoiceEntity.toVoucherRequest(userCode: String, customerNumber: String?, json: Json): CreateVoucherRequest {
    val lines = json.decodeFromString<List<InvoiceLine>>(linesJson)
    val kind = when (type) {
        "SALE" -> "SALE"
        "RETURN" -> "RETURN"
        else -> "ORDER"
    }
    val payments = if (type == "SALE" && paymentMethod != null) {
        // Use the payment type saved with the invoice (CASH | CHEQUE | TRANSFER | CREDIT).
        listOf(VoucherPayment(amount = total.toAmountString(), paymentType = paymentMethod.toString()))
    } else {
        emptyList()
    }

    // Invoice-level (header) discount. The server applies per-line discounts from
    // `discountPercentage`, then subtracts `totalDiscountValue` from the tax-inclusive
    // gross. So the header discount we send is the POST-TAX residual that brings the
    // server's net total down to the grand total the app showed — this also folds in
    // the tax saved by the app's pre-tax invoice discount, with no double-counting.
    val backendGross = lines.sumOf { line ->
        val net = line.qty * line.unitPrice * (1.0 - line.discountPct.coerceIn(0.0, 1.0))
        net * (1.0 + line.taxRate)
    }
    val headerDiscount = (backendGross - total).coerceAtLeast(0.0)

    return CreateVoucherRequest(
        voucherNumber = id,                 // ignored by the inbox; the server assigns the real number
        clientRef = id,                     // idempotency key → safe replays + server-assigned number
        transKind = kind,
        userCode = userCode,
        customerNumber = customerNumber,
        referenceVoucherNumber = referenceNumber,
        isPosted = true,
        totalDiscountValue = if (headerDiscount > 0.005) headerDiscount.toAmountString() else null,
        transactions = lines.map { line ->
            VoucherTxn(
                itemNumber = line.sku,
                itemName = line.nameAr,
                itemQty = line.qty.toAmountString(),
                unitPrice = line.unitPrice.toAmountString(),
                taxPercentage = line.taxRate.toPercentString(),
                discountPercentage = line.discountPct.toPercentString(),
            )
        },
        payments = payments,
    )
}

fun PaymentEntity.toCreateCollectionRequest(repId: String): CreateCollectionRequest {
    val isCheque = method == "CHEQUE"
    return CreateCollectionRequest(
        repId = repId,
        customerId = customerId,
        amount = amount.jodToFils().toLong(),
        method = if (isCheque) "cheque" else "cash",     // backend supports cash | cheque
        note = notes,
        cheque = if (isCheque) {
            CreateChequeRequest(bankName = chequeBank, chequeNumber = chequeNumber)
        } else {
            null
        },
    )
}
