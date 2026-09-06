package com.jehadalomour.flowvan.core.network.mapper

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.network.dto.CreateChequeRequest
import com.jehadalomour.flowvan.core.network.dto.CreateCollectionRequest
import com.jehadalomour.flowvan.core.network.dto.CreateInvoiceLine
import com.jehadalomour.flowvan.core.network.dto.CreateInvoiceRequest
import com.jehadalomour.flowvan.core.network.dto.CreateVoucherRequest
import com.jehadalomour.flowvan.core.network.dto.SyncVoucherResult
import com.jehadalomour.flowvan.core.network.dto.VoucherPayment
import com.jehadalomour.flowvan.core.network.dto.VoucherTxn
import com.jehadalomour.flowvan.core.network.http.jodToFils
import com.jehadalomour.flowvan.core.network.http.numericStringToDouble
import com.jehadalomour.flowvan.core.network.http.toAmountString
import com.jehadalomour.flowvan.core.network.http.toPercentString
import com.jehadalomour.flowvan.core.model.isServerUnitId
import com.jehadalomour.flowvan.core.model.InvoiceLine
import kotlin.math.roundToLong
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.datetime.Instant
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
    // Upload the RAW cart (manual discounts only): the entity's primary fields hold the
    // OFFER-APPLIED result for display, but the server re-applies offers on POST, so posting
    // the offer-applied lines/discount would double-count. uploadLinesJson/uploadDiscountAmount
    // are set only when an offer applied; null → no offer, use the primary fields as before.
    val lines = json.decodeFromString<List<InvoiceLine>>(uploadLinesJson ?: linesJson)
    val uploadDiscount = uploadDiscountAmount ?: discountAmount
    val kind = when (type) {
        "SALE" -> "SALE"
        "RETURN" -> "RETURN"
        else -> "ORDER"
    }
    val payments = if ((type == "SALE" || type == "RETURN") && paymentMethod != null) {
        // Use the payment type saved with the invoice (CASH | CHEQUE | TRANSFER | CREDIT).
        // RETURN carries it too: without a payment row the server records no return
        // payment, so the rep settlement never deducts a cash return, the credit-note
        // balance never falls, and the rep's cash box is not reduced — all three read
        // the return's CASH/CREDIT payment by (payment_type, trans_kind=RETURN).
        listOf(VoucherPayment(amount = total.toAmountString(), paymentType = paymentMethod.toString()))
    } else {
        emptyList()
    }

    // EXACT per-line discount in fils. Never convert a value discount to an integer
    // percentage (the old `discountPct.toPercentString()` rounded e.g. 5.6% → 6%,
    // corrupting the amount). We compute the discount once, in the SAME fils
    // rounding the server uses, and send it as a VALUE — so no approximation.
    fun lineDiscountFils(line: InvoiceLine): Long {
        val unitFils = (line.unitPrice * 1000.0).roundToLong()
        val grossFils = (unitFils.toDouble() * line.qty).roundToLong()
        return (grossFils.toDouble() * line.discountPct.coerceIn(0.0, 1.0)).roundToLong()
    }

    // Header (voucher-level) discount ONLY. The entity's `discountAmount` is the
    // COMBINED total (line discounts + voucher discount); line discounts are sent
    // per-line, so the header carries only the voucher-level remainder — otherwise
    // the server applies the line discounts twice (the INV-…-4 "0.75 not 1.00" bug).
    val totalLineDiscountFils = lines.sumOf { lineDiscountFils(it) }
    val voucherDiscountFils =
        ((uploadDiscount * 1000.0).roundToLong() - totalLineDiscountFils).coerceAtLeast(0L)

    return CreateVoucherRequest(
        // Do NOT send a client number: the server reserves the authoritative, collision-proof
        // number (voucher_number is UNIQUE). The old code sent the local number, which the
        // server used verbatim — so a reinstall (counter resets to 1) produced a number that
        // already existed. The server assigns; the app adopts it from the response on sync.
        voucherNumber = null,
        clientRef = id,                     // globally-unique idempotency key (UUID) → safe replays
        transKind = kind,
        userCode = userCode,
        customerNumber = customerNumber,
        referenceVoucherNumber = referenceNumber,
        // The real sale time (local save). Without it the server stamps `now()` at
        // processing, so a voucher that syncs late (e.g. after a backend outage) would
        // jump to the processing date and hide from the Operations day it was actually sold.
        inDate = Instant.fromEpochMilliseconds(createdAt).toString(),
        isPosted = true,
        // Sale-time GPS (location-locked reps) — the backend re-validates proximity
        // against these, so an offline sale is checked against where the rep was.
        repLat = repLat,
        repLng = repLng,
        totalDiscountValue = if (voucherDiscountFils > 0L) (voucherDiscountFils / 1000.0).toAmountString() else null,
        transactions = lines.map { line ->
            val discFils = lineDiscountFils(line)
            VoucherTxn(
                itemNumber = line.sku,
                itemName = line.nameAr,
                itemQty = line.qty.toAmountString(),
                unitPrice = line.unitPrice.toAmountString(),
                taxPercentage = line.taxRate.toPercentString(),
                // EXACT value, not a rounded percentage (see note above).
                discountPercentage = "0",
                discountValue = if (discFils > 0L) (discFils / 1000.0).toAmountString() else null,
                // The line's chosen unit — name + pieces-per-unit, so the server
                // moves the right base stock and the ERP gets the per-piece price.
                unitName = line.unit.ifBlank { null },
                unitCode = line.unit.ifBlank { null },
                unitBaseQty = line.unitConversionQty.roundToInt().takeIf { it > 0 },
                // The unit's real id — this is what tells red from blue, so the server
                // moves the right POOL instead of guessing from the Arabic name.
                //
                // Only a genuine server id may travel here. The BASE unit has no
                // `item_units` row — /products synthesises it from the item itself and
                // returns itemUnitId = "" — so its local id falls back to the item's
                // barcode, and posting THAT is rejected outright:
                //   400 transactions.0.itemUnitId must be a UUID
                // which fails the whole voucher and leaves it stuck on its offline number.
                // A line with no itemUnitId is the base pool, which is exactly right, and
                // the unit still travels in unitCode/unitName.
                itemUnitId = line.unitId.takeIf { it.isServerUnitId() },
            )
        },
        payments = payments,
        // GIFT picks for ITEM_QTY_REWARD offers — server adds the free lines + redemption.
        chosenFreeItems = chosenFreeItemsCsv
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList(),
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
        repLat = repLat,
        repLng = repLng,
        cheque = if (isCheque) {
            CreateChequeRequest(bankName = chequeBank, chequeNumber = chequeNumber)
        } else {
            null
        },
    )
}

/**
 * The server's authoritative invoice, adopted from a create response — the app overwrites its
 * on-device totals with these so the saved/printed invoice is exactly what the backend recorded.
 */
data class AdoptedInvoice(
    val lines: List<InvoiceLine>,
    val subtotal: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val total: Double,
)

/**
 * Map a create response's SERVER-COMPUTED voucher into the local invoice shape (money engine +
 * offers already applied on the backend). Only PAID lines (net > 0) become [AdoptedInvoice.lines]
 * — gift/free lines (net 0) keep rendering from the saved gift picks, so they aren't doubled.
 * Header totals are taken from the authoritative header fields. Returns null when the response
 * carries no computed lines (older backend / non-SALE) so the on-device calc stays in place.
 */
fun SyncVoucherResult.toAdoptedInvoice(): AdoptedInvoice? {
    val paid = transactions.filter { it.total.numericStringToDouble() > 0.0 }
    if (paid.isEmpty()) return null
    val lines = paid.map { t ->
        val qty = t.itemQty.numericStringToDouble()
        val unit = t.unitPrice.numericStringToDouble()
        val gross = unit * qty
        val disc = t.discountValue.numericStringToDouble()
        val net = t.total.numericStringToDouble()
        val grand = t.netTotal.numericStringToDouble()
        InvoiceLine(
            productId = "",                 // server-side; not needed for display
            sku = t.itemNumber,
            nameAr = t.itemName,
            qty = qty,
            unitPrice = unit,
            discountPct = if (gross > 0.0) (disc / gross).coerceIn(0.0, 1.0) else 0.0,
            lineTotal = grand,              // line grand total (with tax)
            taxType = "TAXABLE",
            taxAmount = (grand - net).coerceAtLeast(0.0),
            unit = t.unitName ?: "",
            unitConversionQty = (t.unitBaseQty ?: 1).toDouble(),
            taxRate = t.taxPercentage.numericStringToDouble() / 100.0,
        )
    }
    return AdoptedInvoice(
        lines = lines,
        subtotal = paid.sumOf { it.unitPrice.numericStringToDouble() * it.itemQty.numericStringToDouble() },
        discountAmount = paid.sumOf { it.discountValue.numericStringToDouble() },
        taxAmount = totalTax.numericStringToDouble(),
        total = netTotal.numericStringToDouble(),
    )
}

