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
 * Blocking SALE-discount approval (when `vouchers.discount.approval` is on and the
 * salesman lacks direct-discount). Same pattern as the return-approval flow: build
 * the SALE payload (with the discount), file it as a VOUCHER_DISCOUNT request, and
 * only commit the sale locally once a manager approves. Nothing touches stock /
 * balance until then, so a rejected request leaves no trace.
 */

@OptIn(ExperimentalTime::class)
private fun buildSaleEntity(
    number: String,
    customerId: String,
    salesmanId: String,
    cart: List<CartLine>,
    discountAmount: Double,
    paymentMethod: PaymentMethod,
    notes: String?,
    status: String,
    syncedAt: Long?,
    taxExempt: Boolean,
    taxExemptionNumber: String?,
    json: Json,
): InvoiceEntity {
    // Exempt customer → store the SALE tax-free (material price + total) so the
    // sales/voucher reports reading this document's total show no tax.
    val storedCart = if (taxExempt) InvoiceTaxCalculator.exemptCartLines(cart) else cart
    val summary = InvoiceTaxCalculator.calculateInvoice(
        cart = storedCart,
        invoiceDiscount = if (discountAmount > 0.0) InvoiceDiscountInput.Fixed(discountAmount)
        else InvoiceDiscountInput.None,
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
        type = "SALE",
        status = status,
        customerId = customerId,
        salesmanId = salesmanId,
        createdAt = now,
        linesJson = json.encodeToString(invoiceLines),
        isTaxExempt = taxExempt,
        taxExemptionNumber = if (taxExempt) taxExemptionNumber else null,
        subtotal = summary.displaySubtotal,
        discountAmount = summary.totalLineDiscounts + summary.invoiceDiscountAmount,
        taxAmount = summary.totalTax,
        total = summary.grandTotal,
        paymentMethod = paymentMethod.name,
        notes = notes,
        syncedAt = syncedAt,
    )
}

/** Files the discounted SALE as a manager approval request. Returns the request id. */
class RequestDiscountApprovalUseCase(
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
        discountAmount: Double,
        paymentMethod: PaymentMethod,
        notes: String?,
        taxExempt: Boolean = false,
        taxExemptionNumber: String? = null,
    ): Result<String> = runCatching {
        if (cart.isEmpty()) throw EmptyCartException()
        val number = voucherNumbers.next("INV", "SALE")
        val entity = buildSaleEntity(
            number = number,
            customerId = customerId,
            salesmanId = salesmanId,
            cart = cart,
            discountAmount = discountAmount,
            paymentMethod = paymentMethod,
            notes = notes,
            status = "PENDING_APPROVAL",
            syncedAt = null,
            taxExempt = taxExempt,
            taxExemptionNumber = taxExemptionNumber,
            json = json,
        )
        // Empty voucherNumber → server assigns a unique serial on approve; unique clientRef.
        val payload = entity.toVoucherRequest(userCode, customerNumber, json).copy(
            voucherNumber = null,
            clientRef = "$number-${Clock.System.now().toEpochMilliseconds()}",
        )
        val dto = approvalApi.create(
            type = ApprovalTypes.VOUCHER_DISCOUNT,
            payload = payload,
            note = notes,
            customerNumber = customerNumber,
        )
        dto.id
    }
}

/**
 * Commits an approved discounted SALE locally: the server already created the
 * voucher (number = [approvedNumber]); save the salesman's copy marked synced
 * (no re-push), decrement van stock, and add to the customer balance on CREDIT.
 */
class CommitApprovedSaleUseCase(
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
        discountAmount: Double,
        paymentMethod: PaymentMethod,
        notes: String?,
        taxExempt: Boolean = false,
        taxExemptionNumber: String? = null,
    ): Result<InvoiceEntity> = runCatching {
        val now = Clock.System.now().toEpochMilliseconds()
        val entity = buildSaleEntity(
            number = approvedNumber,
            customerId = customerId,
            salesmanId = salesmanId,
            cart = cart,
            discountAmount = discountAmount,
            paymentMethod = paymentMethod,
            notes = notes,
            status = "CONFIRMED",
            syncedAt = now, // server already has it — never re-push
            taxExempt = taxExempt,
            taxExemptionNumber = taxExemptionNumber,
            json = json,
        )
        invoices.save(entity)
        for (line in cart) {
            applyVanStockDelta(products, productUnits, line, -line.stockQty.toInt())
        }
        if (paymentMethod == PaymentMethod.CREDIT) {
            customers.adjustBalance(customerId, entity.total)
        }
        entity
    }
}
