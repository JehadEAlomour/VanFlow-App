package com.jehadalomour.flowvan.feature.print

import kotlin.math.ceil
import kotlin.math.floor

/**
 * How a statement is cut into A4 pages.
 *
 * A thermal roll has no pages — it is one strip as long as the account needs, and
 * the rep tears it off. A4 does have pages, and pretending otherwise is what makes
 * a shared statement unreadable: fitting a hundred movements onto one sheet shrinks
 * the type until nobody can read the figure the page exists to state.
 *
 * The split is by ROW COUNT rather than by measuring the laid-out document, because
 * every row on this table is exactly one line — a date, a short label, a document
 * number and three amounts, none of which wrap at A4 width. That makes the page
 * break arithmetic that can be reasoned about and tested, instead of a measurement
 * pass that can only be eyeballed.
 *
 * The budget below is in dp and mirrors StatementA4Document. It is an ESTIMATE:
 * the real height of a line of Arabic depends on the font actually resolved on the
 * handset, so [SAFETY_ROWS] is left unspent rather than filling the sheet to its
 * last millimetre. Erring short costs a little white space at the foot of a page;
 * erring long silently clips movements off the bottom of the paper.
 */

// ── The height budget (dp), mirroring StatementA4Document ────────────────────

/** A4 at 96 dpi. */
private const val PAGE_HEIGHT = 1123f
private const val PAGE_MARGIN = 36f

/** A movement's fixed line, plus the hairline drawn under it. */
private const val ROW_HEIGHT = 26f + 0.8f

/** Logo, company, title rule, the customer/period grid and the opening balance. */
private const val FULL_MASTHEAD = 305f

/** A continuation sheet's single identifying line and its rule. */
private const val CONTINUATION_MASTHEAD = 29f

/** The table's column headings, repeated on every sheet. */
private const val TABLE_HEADING = 39f

/** The identifying strip every sheet ends with. */
private const val PAGE_FOOTER = 19f

/**
 * Totals, the boxed closing balance, the signature strip and the thanks line —
 * measured at its tallest, which is a customer in credit, since that adds a line.
 */
private const val TRAILER_HEIGHT = 226f

/** Deliberately unspent. See the note above about estimated text heights. */
private const val SAFETY_ROWS = 1

private const val CONTENT_HEIGHT = PAGE_HEIGHT - PAGE_MARGIN * 2

private fun rowsFitting(headerHeight: Float): Int =
    floor((CONTENT_HEIGHT - headerHeight - TABLE_HEADING - PAGE_FOOTER) / ROW_HEIGHT).toInt() -
        SAFETY_ROWS

// ── What the sheets hold ─────────────────────────────────────────────────────

/** Movements on the FIRST sheet, which gives most of its height to the masthead. */
internal val FIRST_PAGE_ROWS: Int = rowsFitting(FULL_MASTHEAD)

/** Movements on a continuation sheet, which repeats only a slim header. */
internal val CONTINUATION_PAGE_ROWS: Int = rowsFitting(CONTINUATION_MASTHEAD)

/**
 * Rows' worth of height the closing block needs.
 *
 * The closing balance must never be separated from the movements that produce it —
 * a sheet ending mid-table, with the total on a page that got lost, is exactly how
 * a statement becomes an argument.
 */
internal val TRAILER_ROWS: Int = ceil(TRAILER_HEIGHT / ROW_HEIGHT).toInt()

internal data class StatementPagePlan(
    /** Index of this page's first movement in the full list. */
    val firstRow: Int,
    val rowCount: Int,
    /** The full masthead — logo, company, customer, period. Page one only. */
    val showsHeader: Boolean,
    /** Totals, the closing balance and the signatures. Last page only. */
    val showsTrailer: Boolean,
) {
    val lastRowExclusive: Int get() = firstRow + rowCount
}

/**
 * Cut [rowCount] movements into pages.
 *
 * Always returns at least one page: a period with no movement still prints, and
 * still has to state the opening and closing balance — "nothing happened" is a
 * real answer to give a shopkeeper.
 */
internal fun planStatementPages(rowCount: Int): List<StatementPagePlan> {
    val pages = mutableListOf<StatementPagePlan>()
    var placed = 0
    var first = true

    // Lay the movements out, page after page, ignoring the closing block for now.
    do {
        val capacity = if (first) FIRST_PAGE_ROWS else CONTINUATION_PAGE_ROWS
        val take = minOf(capacity, rowCount - placed)
        pages += StatementPagePlan(
            firstRow = placed,
            rowCount = take,
            showsHeader = first,
            showsTrailer = false,
        )
        placed += take
        first = false
    } while (placed < rowCount)

    // Then find room for the closing block. It goes at the foot of the last page
    // when that page has the space, and on a page of its own when it does not —
    // never squeezed in on top of rows it would overlap.
    val last = pages.last()
    val lastCapacity = if (last.showsHeader) FIRST_PAGE_ROWS else CONTINUATION_PAGE_ROWS
    pages[pages.lastIndex] =
        if (last.rowCount + TRAILER_ROWS <= lastCapacity) {
            last.copy(showsTrailer = true)
        } else {
            pages += StatementPagePlan(
                firstRow = rowCount,
                rowCount = 0,
                showsHeader = false,
                showsTrailer = true,
            )
            last
        }
    return pages
}

/**
 * The height a planned page actually asks for, in dp — the check that the budget
 * above is self-consistent, and the thing a test can hold the constants to when
 * somebody later widens the masthead or adds a row to the closing block.
 */
internal fun plannedPageHeight(plan: StatementPagePlan): Float =
    PAGE_MARGIN * 2 +
        (if (plan.showsHeader) FULL_MASTHEAD else CONTINUATION_MASTHEAD) +
        TABLE_HEADING +
        plan.rowCount * ROW_HEIGHT +
        (if (plan.showsTrailer) TRAILER_HEIGHT else 0f) +
        PAGE_FOOTER

/** A4 at 96 dpi — what [plannedPageHeight] must not exceed. */
internal const val A4_HEIGHT_DP = PAGE_HEIGHT
