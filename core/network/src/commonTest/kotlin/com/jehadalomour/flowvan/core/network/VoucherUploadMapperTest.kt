package com.jehadalomour.flowvan.core.network

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.model.InvoiceLine
import com.jehadalomour.flowvan.core.network.dto.CreatedVoucherTxn
import com.jehadalomour.flowvan.core.network.dto.SyncVoucherResult
import com.jehadalomour.flowvan.core.network.mapper.toAdoptedInvoice
import com.jehadalomour.flowvan.core.network.mapper.toVoucherRequest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Guards the offline invoice fix: an offer-applied SALE stores the OFFER-APPLIED totals in its
 * primary fields (so the saved/printed invoice matches the cart, even offline) but must UPLOAD the
 * RAW cart — the backend re-applies offers on POST, so posting the offer discount would double-count.
 */
class VoucherUploadMapperTest {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // SPRITE-330 from the bug report: 0.450 × 20 = 9.000 gross, 16% tax.
    private fun line(discountPct: Double) = InvoiceLine(
        productId = "P1", sku = "SPRITE-330", nameAr = "سبرايت", qty = 20.0,
        unitPrice = 0.450, discountPct = discountPct, lineTotal = 0.0,
        taxType = "TAXABLE", taxAmount = 0.0, unit = "حبة", unitConversionQty = 1.0, taxRate = 0.16,
    )

    private fun entity(
        linesJson: String,
        discountAmount: Double,
        total: Double,
        uploadLinesJson: String? = null,
        uploadDiscountAmount: Double? = null,
    ) = InvoiceEntity(
        id = "id1", number = "INV-1", type = "SALE", status = "CONFIRMED",
        customerId = "C1", salesmanId = "S1", createdAt = 0L,
        linesJson = linesJson, subtotal = 9.0, discountAmount = discountAmount,
        taxAmount = 0.0, total = total, paymentMethod = "CASH", notes = null, syncedAt = null,
        uploadLinesJson = uploadLinesJson, uploadDiscountAmount = uploadDiscountAmount,
    )

    @Test
    fun offerSaleUploadsRawCartNotTheOfferDiscount() {
        // Primary (display) lines carry the 2.000-off offer (22.2%); the upload snapshot is raw (0%).
        val primary = json.encodeToString(listOf(line(discountPct = 2.0 / 9.0)))
        val raw = json.encodeToString(listOf(line(discountPct = 0.0)))
        val req = entity(
            linesJson = primary, discountAmount = 2.0, total = 8.120,
            uploadLinesJson = raw, uploadDiscountAmount = 0.0,
        ).toVoucherRequest(userCode = "U1", customerNumber = "CUST1", json = json)

        // The posted line carries NO discount — the server re-applies the offer (no double count).
        assertNull(req.transactions[0].discountValue)
        assertNull(req.totalDiscountValue)
        // The payment is the offer-applied total the customer actually pays.
        assertEquals("8.120", req.payments[0].amount)
    }

    @Test
    fun plainSaleWithManualDiscountUploadsFromPrimaryFields() {
        // No offer (uploadLinesJson null) → the manual 10% line discount is posted as before.
        val primary = json.encodeToString(listOf(line(discountPct = 0.10)))
        val req = entity(linesJson = primary, discountAmount = 0.90, total = 8.100)
            .toVoucherRequest("U1", "CUST1", json)

        // 9.000 gross × 10% = 0.900 posted per line; no voucher-level remainder.
        assertEquals("0.900", req.transactions[0].discountValue)
        assertNull(req.totalDiscountValue)
    }

    // ── adopting the server's computed invoice on sync ──────────────────────

    @Test
    fun adoptsServerComputedTotalsForTheSale() {
        // Server response for the bug case: SPRITE-330 with a 2.000 offer discount applied.
        val res = SyncVoucherResult(
            id = "id1", voucherNumber = "INV-VAN-1",
            netTotal = "8.120", totalTax = "1.120", totalDiscountValue = "0",
            transactions = listOf(
                CreatedVoucherTxn(
                    itemNumber = "SPRITE-330", itemName = "سبرايت", itemQty = "20",
                    unitPrice = "0.450", taxPercentage = "16", discountValue = "2.000",
                    total = "7.000", netTotal = "8.120", unitName = "حبة", unitBaseQty = 1,
                ),
            ),
        )
        val a = res.toAdoptedInvoice()
        assertNotNull(a)
        assertEquals(9.0, a.subtotal, 0.0005)      // 0.450 × 20 gross
        assertEquals(2.0, a.discountAmount, 0.0005) // the offer
        assertEquals(1.12, a.taxAmount, 0.0005)     // 16% of the 7.000 net
        assertEquals(8.12, a.total, 0.0005)         // grand total the customer pays
        val line = a.lines.single()
        assertEquals(2.0 / 9.0, line.discountPct, 0.0005)
        assertEquals(8.12, line.lineTotal, 0.0005)
        assertEquals(1.12, line.taxAmount, 0.0005)
    }

    @Test
    fun adoptionExcludesGiftLinesAndReturnsNullWhenNoPaidLines() {
        // A gift/free line nets to 0 → not a paid line; the paid SPRITE line is kept.
        val withGift = SyncVoucherResult(
            netTotal = "8.120", totalTax = "1.120",
            transactions = listOf(
                CreatedVoucherTxn(itemNumber = "SPRITE-330", itemQty = "20", unitPrice = "0.450",
                    discountValue = "2.000", total = "7.000", netTotal = "8.120", taxPercentage = "16"),
                CreatedVoucherTxn(itemNumber = "GIFT-1", itemQty = "1", unitPrice = "0.500",
                    discountValue = "0.500", total = "0", netTotal = "0", discountPercentage = "100"),
            ),
        )
        assertEquals(1, withGift.toAdoptedInvoice()?.lines?.size) // gift excluded

        // No paid lines (older backend returns just a number) → null, on-device calc stands.
        assertNull(SyncVoucherResult(voucherNumber = "INV-1").toAdoptedInvoice())
    }
}
