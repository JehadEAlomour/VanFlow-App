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
    return CreateVoucherRequest(
        voucherNumber = id,                 // local id → unique + lets a retry hit a 409 (treated as synced)
        transKind = kind,
        userCode = userCode,
        customerNumber = customerNumber,
        isPosted = true,
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
