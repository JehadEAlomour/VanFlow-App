package com.jehadalomour.flowvan.core.data.repository

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.core.data.connectivity.ConnectivityObserver
import com.jehadalomour.flowvan.core.network.api.CollectionApi
import com.jehadalomour.flowvan.core.network.api.VoucherApi

/** What one line of the transaction report is. */
enum class TxnKind { SALE, RETURN, ORDER, COLLECTION }

/**
 * One movement on the customer's account, from the SERVER.
 *
 * [total] is the document's value. [credit] is the slice of it left on account:
 * equal to [total] on a fully-credit sale, zero on a cash one, and zero on a
 * collection — money coming back is not credit extended.
 */
data class CustomerTxn(
    val id: String,
    val number: String,
    val kind: TxnKind,
    /** yyyy-MM-dd as the server reported it; blank when it sent nothing. */
    val date: String,
    val total: Double,
    val credit: Double,
    /** cash / cheque / transfer for a collection; null for a voucher. */
    val method: String? = null,
) {
    /** True when the whole document was settled at the counter. */
    val isCash: Boolean get() = kind != TxnKind.COLLECTION && credit <= 0.0
}

/** The report for one customer over one date range. */
data class TransactionReport(
    val rows: List<CustomerTxn> = emptyList(),
    /** True when this came off the network; false means it could not be built. */
    val isLive: Boolean = false,
) {
    val sales: List<CustomerTxn> get() = rows.filter { it.kind == TxnKind.SALE }
    val returns: List<CustomerTxn> get() = rows.filter { it.kind == TxnKind.RETURN }
    val collections: List<CustomerTxn> get() = rows.filter { it.kind == TxnKind.COLLECTION }

    val salesTotal: Double get() = sales.sumOf { it.total }
    val returnsTotal: Double get() = returns.sumOf { it.total }
    val collectionsTotal: Double get() = collections.sumOf { it.total }

    /** What the customer actually took, net of what came back. */
    val netTotal: Double get() = salesTotal - returnsTotal

    /**
     * How much of the movement was left ON ACCOUNT. A return's credit reduces it,
     * because a credit sale reversed never became a debt.
     */
    val creditTotal: Double get() =
        sales.sumOf { it.credit } - returns.sumOf { it.credit }

    /** The rest of the sales — settled at the counter, owed by nobody. */
    val cashTotal: Double get() = netTotal - creditTotal
}

/**
 * Builds the customer transaction report (تقرير الحركات) from the server.
 *
 * Server-only on purpose. The local database holds what THIS handset synced, so
 * a report built from it quietly omits anything sold to the shop by another van
 * or entered at the office — and this is the one screen where the rep and the
 * shopkeeper are comparing the same account from opposite sides. A report that
 * is merely probably complete is worse than one that admits it cannot be built.
 */
class TransactionReportRepository(
    private val vouchers: VoucherApi,
    private val collections: CollectionApi,
    private val connectivity: ConnectivityObserver,
) {
    private val log = Logger.withTag("TxnReport")

    /**
     * @param customerNumber the customer's CODE — what vouchers are keyed by.
     * @param customerId the customer's UUID — what collections are keyed by.
     *   The two endpoints identify a customer differently; passing the wrong one
     *   returns an empty list rather than an error, so they are separate params.
     * @param from inclusive, yyyy-MM-dd
     * @param to inclusive, yyyy-MM-dd
     */
    suspend fun load(
        customerNumber: String,
        customerId: String,
        from: String,
        to: String,
    ): TransactionReport {
        if (!connectivity.isOnline()) return TransactionReport(isLive = false)

        val voucherRows = runCatching {
            vouchers.customerTransactions(customerNumber, from, to)
        }.onFailure { log.w("voucher fetch failed: ${it.message}") }.getOrNull()

        val collectionRows = runCatching {
            // A shop with a lot of history still fits: the page cap is generous
            // and a single customer's collections over a month are dozens, not
            // thousands.
            collections.list(customerId = customerId, from = from, to = to, limit = 200).items
        }.onFailure { log.w("collection fetch failed: ${it.message}") }.getOrNull()

        // Either call failing makes the report wrong rather than short — a
        // statement missing its payments reads as unpaid debt.
        if (voucherRows == null || collectionRows == null) {
            return TransactionReport(isLive = false)
        }

        val rows = buildList {
            voucherRows.forEach { v ->
                val kind = when (v.transKind.uppercase()) {
                    "SALE" -> TxnKind.SALE
                    "RETURN" -> TxnKind.RETURN
                    else -> TxnKind.ORDER
                }
                add(
                    CustomerTxn(
                        id = v.id,
                        number = v.voucherNumber,
                        kind = kind,
                        date = (v.inDate ?: v.createdAt).orEmpty().take(10),
                        total = v.netTotal.toDoubleOrNull() ?: 0.0,
                        credit = v.creditTotal.toDoubleOrNull() ?: 0.0,
                    ),
                )
            }
            collectionRows.forEach { c ->
                add(
                    CustomerTxn(
                        id = c.id,
                        number = c.id.take(8),
                        kind = TxnKind.COLLECTION,
                        date = c.collectedAt.orEmpty().take(10),
                        // Collections are stored in FILS; vouchers in major units.
                        // Mixing the two would inflate a payment a thousandfold.
                        total = c.amount / 1000.0,
                        credit = 0.0,
                        method = c.method,
                    ),
                )
            }
        }.sortedBy { it.date }

        return TransactionReport(rows = rows, isLive = true)
    }
}
