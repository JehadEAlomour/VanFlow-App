package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.InvoiceRepository
import com.jehadalomour.flowvan.core.data.repository.ProductRepository
import com.jehadalomour.flowvan.core.data.repository.ProductUnitRepository
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.InvoiceDiscountInput
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.model.InvoiceTaxCalculator
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.network.api.ApprovalApi
import com.jehadalomour.flowvan.core.network.dto.ApprovalTypes
import com.jehadalomour.flowvan.core.network.mapper.toVoucherRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * F10 — blocking return approval (when `vouchers.return.approval` is on).
 *
 * The salesman cannot print until a manager approves. Instead of saving the
 * return locally and printing, we build the SAME voucher payload and file it as
 * an approval request. Nothing touches local stock/balance yet — the return is
 * only committed (locally) once the manager approves, so a rejected request
 * leaves no trace. The poll/cancel use cases drive the pending UI.
 */

/** Builds the transient return entity used to derive the approval payload (never saved as-is). */
@OptIn(ExperimentalTime::class)
private fun buildReturnEntity(
    number: String,
    customerId: String,
    salesmanId: String,
    cart: List<CartLine>,
    reason: String,
    paymentMethod: PaymentMethod,
    extraNotes: String?,
    referenceInvoiceId: String?,
    referenceNumber: String?,
    status: String,
    syncedAt: Long?,
    taxExempt: Boolean,
    taxExemptionNumber: String?,
    json: Json,
): InvoiceEntity {
    // Return "as it was": an exempt source sale is returned tax-free, so the stored
    // total/lines carry no tax and reports reading them show none.
    val storedCart = if (taxExempt) InvoiceTaxCalculator.exemptCartLines(cart) else cart
    val summary = InvoiceTaxCalculator.calculateInvoice(
        cart = storedCart,
        invoiceDiscount = InvoiceDiscountInput.None,
    )
    val now = Clock.System.now().toEpochMilliseconds()
    val invoiceLines = storedCart.map {
        InvoiceLine(
            productId = it.productId,
            sku = it.sku,
            nameAr = it.nameAr,
            qty = it.qty,
            unitPrice = it.unitPrice,
            discountPct = it.discountPct,
            lineTotal = it.lineTotal,
            taxType = it.lineTaxType.name,
            taxAmount = it.lineTax,
            unit = it.unit,
            unitId = it.unitId,
            unitConversionQty = it.unitConversionQty,
            taxRate = it.taxRate,
        )
    }
    return InvoiceEntity(
        id = newVoucherClientRef(),   // unique clientRef; server assigns the real number
        number = number,
        type = "RETURN",
        status = status,
        customerId = customerId,
        salesmanId = salesmanId,
        createdAt = now,
        linesJson = json.encodeToString(invoiceLines),
        isTaxExempt = taxExempt,
        taxExemptionNumber = if (taxExempt) taxExemptionNumber else null,
        subtotal = summary.displaySubtotal,
        discountAmount = summary.totalLineDiscounts,
        taxAmount = summary.totalTax,
        total = summary.grandTotal,
        paymentMethod = paymentMethod.name,
        notes = "سبب: $reason${extraNotes?.let { " — $it" } ?: ""}",
        syncedAt = syncedAt,
        referenceInvoiceId = referenceInvoiceId,
        referenceNumber = referenceNumber,
    )
}

/** Files the return as a manager approval request. Returns the request id to poll. */
class RequestReturnApprovalUseCase(
    private val approvalApi: ApprovalApi,
    private val voucherNumbers: VoucherNumberGenerator,
    private val json: Json,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        salesmanId: String,
        userCode: String,
        customerNumber: String?,
        cart: List<CartLine>,
        reason: String,
        paymentMethod: PaymentMethod,
        extraNotes: String?,
        referenceInvoiceId: String?,
        referenceNumber: String?,
        taxExempt: Boolean = false,
        taxExemptionNumber: String? = null,
    ): Result<String> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()
        require(reason.isNotBlank()) { "reason required" }

        // The local counter only advances when a voucher is SAVED locally, and the
        // approval flow never saves until approved — so a pre-generated number would
        // be identical for every request and collide on the server's direct create
        // ("Voucher RET-… already exists"). Instead we send an EMPTY voucherNumber so
        // the server assigns a guaranteed-unique serial on approval, and commit the
        // local copy using that returned number. `number` here is only a scratch id
        // for building the line payload. clientRef is made unique for replay hygiene.
        val number = voucherNumbers.next("RET", "RETURN")
        val entity = buildReturnEntity(
            number = number,
            customerId = customerId,
            salesmanId = salesmanId,
            cart = cart,
            reason = reason,
            paymentMethod = paymentMethod,
            extraNotes = extraNotes,
            referenceInvoiceId = referenceInvoiceId,
            referenceNumber = referenceNumber,
            status = "PENDING_APPROVAL",
            syncedAt = null,
            taxExempt = taxExempt,
            taxExemptionNumber = taxExemptionNumber,
            json = json,
        )
        val payload = entity.toVoucherRequest(userCode, customerNumber, json).copy(
            voucherNumber = null,   // omitted in JSON → server assigns a unique serial on approve
            clientRef = "$number-${Clock.System.now().toEpochMilliseconds()}",
        )
        val dto = approvalApi.create(
            type = ApprovalTypes.RETURN_VOUCHER,
            payload = payload,
            note = entity.notes,
            customerNumber = customerNumber,
        )
        dto.id
    }
}

/** Decision of a polled approval request, normalized for the UI. */
data class ApprovalDecision(
    val status: String,             // pending | approved | rejected | cancelled
    val resultVoucher: String?,
    val decisionNote: String?,
)

/** Polls a single approval request for its current decision. */
class PollApprovalUseCase(
    private val approvalApi: ApprovalApi,
) {
    suspend operator fun invoke(id: String): Result<ApprovalDecision> = runCatching {
        // Salesman-safe: reads from `GET /approvals/mine` (the :id route is manager-only).
        // A missing id (not yet visible / aged out) is treated as still pending.
        val dto = approvalApi.one(id)
            ?: return@runCatching ApprovalDecision(status = "pending", resultVoucher = null, decisionNote = null)
        ApprovalDecision(
            status = dto.status,
            resultVoucher = dto.resultVoucher,
            decisionNote = dto.decisionNote,
        )
    }
}

/** Cancels the salesman's own still-pending request (e.g. they left the screen). */
class CancelApprovalUseCase(
    private val approvalApi: ApprovalApi,
) {
    suspend operator fun invoke(id: String): Result<Unit> = runCatching {
        approvalApi.cancel(id)
        Unit
    }
}

/**
 * Commits an approved return LOCALLY: the server already created the voucher
 * (number = [approvedNumber]), so we save the salesman's copy marked synced (no
 * re-push) and adjust van stock + customer balance. Idempotent on the number.
 */
class CommitApprovedReturnUseCase(
    private val invoices: InvoiceRepository,
    private val products: ProductRepository,
    private val productUnits: ProductUnitRepository,
    private val customers: CustomerRepository,
    private val json: Json,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        approvedNumber: String,
        customerId: String,
        salesmanId: String,
        cart: List<CartLine>,
        reason: String,
        paymentMethod: PaymentMethod,
        extraNotes: String?,
        referenceInvoiceId: String?,
        referenceNumber: String?,
        taxExempt: Boolean = false,
        taxExemptionNumber: String? = null,
    ): Result<InvoiceEntity> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = buildReturnEntity(
            number = approvedNumber,
            customerId = customerId,
            salesmanId = salesmanId,
            cart = cart,
            reason = reason,
            paymentMethod = paymentMethod,
            extraNotes = extraNotes,
            referenceInvoiceId = referenceInvoiceId,
            referenceNumber = referenceNumber,
            status = "CONFIRMED",
            syncedAt = now,                 // server already has it — never re-push
            taxExempt = taxExempt,
            taxExemptionNumber = taxExemptionNumber,
            json = json,
        )
        invoices.save(entity)
        // Returns credit van stock back; the server derived the same from the posted voucher.
        for (line in cart) {
            applyVanStockDelta(products, productUnits, line, line.stockQty.toInt())
        }
        // Only a CREDIT return (credit note) reduces the customer's balance; a CASH return
        // refunds cash and leaves the balance unchanged.
        if (paymentMethod == PaymentMethod.CREDIT) {
            customers.adjustBalance(customerId, -entity.total)
        }
        entity
    }
}
