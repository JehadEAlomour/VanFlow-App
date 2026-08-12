package com.jehadalomour.flowvan.core.domain.ledger

import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * What a statement is allowed to charge for.
 *
 * These exist because the rule had two copies that disagreed — an order moved the
 * printed balance by its full value and the screen's by nothing — and because every
 * case here is a figure a shopkeeper is asked to pay.
 */
class CustomerStatementTest {

    private fun voucher(
        type: String,
        total: Double = 100.0,
        paymentMethod: String? = "CREDIT",
        status: String = "CONFIRMED",
    ) = InvoiceEntity(
        id = "id-$type-$total-$paymentMethod-$status",
        number = "V-1",
        type = type,
        status = status,
        customerId = "cust-1",
        salesmanId = "rep-1",
        createdAt = 0L,
        linesJson = "[]",
        subtotal = total,
        discountAmount = 0.0,
        taxAmount = 0.0,
        total = total,
        paymentMethod = paymentMethod,
        notes = null,
        syncedAt = null,
    )

    // ── Orders ────────────────────────────────────────────────────────────────

    @Test
    fun `an order is not on the statement at all`() {
        assertFalse(CustomerStatement.isLedgerEntry(voucher(CustomerStatement.TYPE_ORDER)))
    }

    @Test
    fun `an order moves nothing even if it reaches the balance`() {
        assertEquals(0.0, CustomerStatement.movement(voucher(CustomerStatement.TYPE_ORDER)))
    }

    @Test
    fun `an order with no payment method is still excluded`() {
        // Orders carry no payment: an order is not paid when it is written. The old
        // rule kept null-method vouchers, which is exactly how orders got in.
        assertFalse(
            CustomerStatement.isLedgerEntry(
                voucher(CustomerStatement.TYPE_ORDER, paymentMethod = null),
            ),
        )
    }

    // ── Sales ─────────────────────────────────────────────────────────────────

    @Test
    fun `a credit sale is a debit`() {
        val sale = voucher(CustomerStatement.TYPE_SALE, total = 45.5)
        assertTrue(CustomerStatement.isLedgerEntry(sale))
        assertEquals(45.5, CustomerStatement.movement(sale))
        assertFalse(CustomerStatement.isCredit(sale))
    }

    @Test
    fun `a cash sale never reaches the ledger`() {
        // Paid at the counter, so it created no receivable. Listing it would bill the
        // customer for money they already handed over.
        assertFalse(
            CustomerStatement.isLedgerEntry(
                voucher(CustomerStatement.TYPE_SALE, paymentMethod = "CASH"),
            ),
        )
    }

    @Test
    fun `a cheque sale never reaches the ledger`() {
        assertFalse(
            CustomerStatement.isLedgerEntry(
                voucher(CustomerStatement.TYPE_SALE, paymentMethod = "CHEQUE"),
            ),
        )
    }

    @Test
    fun `a sale with no payment method is kept`() {
        // Legacy rows predate the column. Hiding them would silently drop real debt,
        // which is the one error worse than showing too much.
        assertTrue(
            CustomerStatement.isLedgerEntry(
                voucher(CustomerStatement.TYPE_SALE, paymentMethod = null),
            ),
        )
    }

    // ── Returns ───────────────────────────────────────────────────────────────

    @Test
    fun `a credit return is a credit`() {
        val ret = voucher(CustomerStatement.TYPE_RETURN, total = 12.25)
        assertTrue(CustomerStatement.isLedgerEntry(ret))
        assertEquals(-12.25, CustomerStatement.movement(ret))
        assertTrue(CustomerStatement.isCredit(ret))
    }

    @Test
    fun `a cash-refunded return never reaches the ledger`() {
        assertFalse(
            CustomerStatement.isLedgerEntry(
                voucher(CustomerStatement.TYPE_RETURN, paymentMethod = "CASH"),
            ),
        )
    }

    // ── Cancelled ─────────────────────────────────────────────────────────────

    @Test
    fun `a cancelled sale never reaches the ledger`() {
        assertFalse(
            CustomerStatement.isLedgerEntry(
                voucher(CustomerStatement.TYPE_SALE, status = "CANCELLED"),
            ),
        )
    }

    @Test
    fun `a rejected sale never reaches the ledger`() {
        assertFalse(
            CustomerStatement.isLedgerEntry(
                voucher(CustomerStatement.TYPE_SALE, status = "REJECTED"),
            ),
        )
    }

    // ── The column has to add up ──────────────────────────────────────────────

    @Test
    fun `an unrecognised voucher type moves nothing`() {
        // The running balance beside each line and the totals at the foot both come
        // from movement(). A type nobody anticipated must not make them disagree.
        assertEquals(0.0, CustomerStatement.movement(voucher("SOMETHING_NEW")))
    }

    @Test
    fun `the ledger of a mixed period nets to sales minus returns`() {
        val vouchers = listOf(
            voucher(CustomerStatement.TYPE_SALE, total = 100.0),
            voucher(CustomerStatement.TYPE_SALE, total = 50.0, paymentMethod = "CASH"),
            voucher(CustomerStatement.TYPE_RETURN, total = 30.0),
            voucher(CustomerStatement.TYPE_ORDER, total = 500.0),
            voucher(CustomerStatement.TYPE_SALE, total = 70.0, status = "CANCELLED"),
        )
        val net = vouchers
            .filter(CustomerStatement::isLedgerEntry)
            .sumOf(CustomerStatement::movement)
        assertEquals(70.0, net)
    }
}
