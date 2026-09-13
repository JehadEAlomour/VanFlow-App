package com.jehadalomour.flowvan.core.model.ledger

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * A counter sale is one transaction. The ERP posts it as an invoice plus a receipt
 * that names the same document, and the statement must not show the shop being
 * billed and then credited for money that never became a debt.
 */
class CashVoucherCollapseTest {

    private val day = 86_400_000L
    private val noon = day * 20_000 + 43_200_000L      // some day, midday
    private val nextNoon = noon + day

    private fun sale(number: String, amount: Double, at: Long = noon) = StatementMovement(
        id = "inv-$number", number = number, createdAt = at,
        docType = StatementDocType.SALE, debit = amount,
    )

    private fun receipt(ref: String, amount: Double, at: Long = noon, id: String = "rcp-$ref") =
        StatementMovement(
            id = id, number = ref, createdAt = at,
            docType = StatementDocType.PAYMENT, credit = amount, method = "CASH",
        )

    @Test
    fun `a sale settled the same day for its full value becomes one row`() {
        val out = CashVoucherCollapse.apply(listOf(sale("INV-1", 100.0), receipt("INV-1", 100.0)))
        assertEquals(1, out.size)
        val row = out.single()
        assertEquals(StatementDocType.SALE, row.docType)
        assertEquals(100.0, row.debit)
        assertEquals(100.0, row.credit)
        // It was paid at the counter, so it moved the account by nothing.
        assertEquals(0.0, row.movement)
        // and the row says how, because the shopkeeper asks.
        assertEquals("CASH", row.method)
    }

    @Test
    fun `a receipt naming the sale joins its line whatever day it was taken`() {
        // The rule is the reference, as the ERP states it: a receipt that names this
        // document belongs on this document's line.
        val out = CashVoucherCollapse.apply(
            listOf(sale("INV-1", 100.0), receipt("INV-1", 100.0, at = nextNoon)),
        )
        assertEquals(1, out.size)
        assertEquals(0.0, out.single().movement)
    }

    @Test
    fun `a PART payment joins the line and leaves the balance it really leaves`() {
        val out = CashVoucherCollapse.apply(listOf(sale("INV-1", 100.0), receipt("INV-1", 40.0)))
        assertEquals(1, out.size)
        assertEquals(100.0, out.single().debit)
        assertEquals(40.0, out.single().credit)
        // Still owed: 60. Folding changed the row count, never the money.
        assertEquals(60.0, out.sumOf { it.movement })
    }

    @Test
    fun `a payment on account, naming no sale here, keeps its own row`() {
        val out = CashVoucherCollapse.apply(listOf(sale("INV-1", 100.0), receipt("INV-9", 30.0)))
        assertEquals(2, out.size)
        assertEquals(70.0, out.sumOf { it.movement })
    }

    @Test
    fun `a receipt naming a different document is left alone`() {
        val out = CashVoucherCollapse.apply(listOf(sale("INV-1", 100.0), receipt("INV-2", 100.0)))
        assertEquals(2, out.size)
    }

    @Test
    fun `the receipt is dropped even when it is listed before its invoice`() {
        // The two share a posting date, so the ERP may order them either way.
        val out = CashVoucherCollapse.apply(listOf(receipt("INV-1", 100.0), sale("INV-1", 100.0)))
        assertEquals(1, out.size)
        assertEquals(StatementDocType.SALE, out.single().docType)
    }

    @Test
    fun `two cash sales of the same value each take their own receipt`() {
        val out = CashVoucherCollapse.apply(
            listOf(
                sale("INV-1", 100.0), sale("INV-2", 100.0),
                receipt("INV-1", 100.0), receipt("INV-2", 100.0),
            ),
        )
        assertEquals(2, out.size)
        assertTrue(out.all { it.movement == 0.0 })
    }

    @Test
    fun `a credit sale with no receipt is untouched`() {
        val out = CashVoucherCollapse.apply(listOf(sale("INV-1", 100.0)))
        assertEquals(1, out.size)
        assertEquals(100.0, out.single().movement)
    }

    @Test
    fun `the closing balance is identical either way — folding moves no money`() {
        val rows = listOf(
            sale("INV-1", 100.0), receipt("INV-1", 100.0),      // cash sale
            sale("INV-2", 250.0),                                // on account
            receipt("INV-2", 50.0, at = nextNoon),               // part paid later
        )
        assertEquals(rows.sumOf { it.movement }, CashVoucherCollapse.apply(rows).sumOf { it.movement })
        assertEquals(200.0, CashVoucherCollapse.apply(rows).sumOf { it.movement })
        // Two sales, each carrying its own receipt.
        assertEquals(2, CashVoucherCollapse.apply(rows).size)
    }

    @Test
    fun `a JOURNAL row is left exactly as the ERP sent it`() {
        val journal = StatementMovement(
            id = "jrn-1", number = "JV-9", createdAt = noon,
            docType = StatementDocType.JOURNAL, debit = 15.0,
        )
        val out = CashVoucherCollapse.apply(listOf(journal, receipt("INV-1", 5.0)))
        assertEquals(2, out.size)
        assertEquals(StatementDocType.JOURNAL, out.first().docType)
    }

    @Test
    fun `a statement with no receipts at all is returned unchanged`() {
        val rows = listOf(sale("INV-1", 100.0), sale("INV-2", 20.0))
        assertEquals(rows, CashVoucherCollapse.apply(rows))
    }

    @Test
    fun `a JOURNAL that names a receipt's reference can absorb it too`() {
        // Any debit row is a document that can be paid — the ERP decides what the
        // receipt names, and this only honours it.
        val journal = StatementMovement(
            id = "jrn-1", number = "JV-9", createdAt = noon,
            docType = StatementDocType.JOURNAL, debit = 20.0,
        )
        val out = CashVoucherCollapse.apply(listOf(journal, receipt("JV-9", 20.0)))
        assertEquals(1, out.size)
        assertEquals(0.0, out.single().movement)
    }
}
