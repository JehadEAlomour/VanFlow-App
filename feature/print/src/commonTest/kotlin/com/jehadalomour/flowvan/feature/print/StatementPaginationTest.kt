package com.jehadalomour.flowvan.feature.print

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The page break is arithmetic, so it can be checked rather than eyeballed.
 *
 * What matters on a statement handed to a shopkeeper: every movement appears
 * exactly once, in order, and the closing balance is never on a page of its own
 * by accident — someone reading a sheet that ends mid-table has to be able to
 * turn over and find the total.
 */
class StatementPaginationTest {

    /** Every row lands on exactly one page, in order, with none invented or lost. */
    private fun assertCoversAllRows(rowCount: Int) {
        val pages = planStatementPages(rowCount)
        var expected = 0
        pages.forEach { page ->
            assertEquals(expected, page.firstRow, "page starts where the last one ended")
            expected = page.lastRowExclusive
        }
        assertEquals(rowCount, expected, "every movement is printed")
    }

    @Test
    fun an_empty_period_still_prints_one_page() {
        val pages = planStatementPages(0)
        assertEquals(1, pages.size)
        assertEquals(0, pages[0].rowCount)
        // "Nothing happened this month" is a real answer, and it still has to
        // carry the opening and closing balance.
        assertTrue(pages[0].showsHeader)
        assertTrue(pages[0].showsTrailer)
    }

    @Test
    fun a_short_statement_is_a_single_sheet() {
        val pages = planStatementPages(5)
        assertEquals(1, pages.size)
        assertTrue(pages[0].showsHeader && pages[0].showsTrailer)
    }

    @Test
    fun only_the_first_page_carries_the_masthead() {
        val pages = planStatementPages(200)
        assertTrue(pages.first().showsHeader)
        // Repeating the logo and the customer block on every sheet wastes a third
        // of each one; continuation pages get a slim header instead.
        assertTrue(pages.drop(1).none { it.showsHeader })
    }

    @Test
    fun only_the_last_page_carries_the_closing_block() {
        val pages = planStatementPages(200)
        assertTrue(pages.last().showsTrailer)
        assertTrue(pages.dropLast(1).none { it.showsTrailer })
    }

    @Test
    fun no_movement_is_dropped_or_repeated_at_any_length() {
        // Around each boundary, where an off-by-one would silently eat a row.
        listOf(0, 1, 13, 14, 15, 25, 26, 27, 49, 50, 51, 62, 63, 99, 100, 500)
            .forEach(::assertCoversAllRows)
    }

    @Test
    fun the_closing_block_never_overlaps_the_rows_above_it() {
        (0..300).forEach { n ->
            val last = planStatementPages(n).last()
            val capacity = if (last.showsHeader) FIRST_PAGE_ROWS else CONTINUATION_PAGE_ROWS
            assertTrue(
                last.rowCount + TRAILER_ROWS <= capacity,
                "closing block does not fit on the last page of a $n-row statement",
            )
        }
    }

    @Test
    fun a_full_page_of_rows_pushes_the_closing_block_to_its_own_sheet() {
        // Exactly a full first page: the totals cannot also fit, so they get a
        // sheet rather than being drawn over the last rows.
        val pages = planStatementPages(FIRST_PAGE_ROWS)
        assertEquals(2, pages.size)
        assertEquals(FIRST_PAGE_ROWS, pages[0].rowCount)
        assertEquals(0, pages[1].rowCount)
        assertTrue(pages[1].showsTrailer)
    }

    @Test
    fun the_last_page_keeps_its_rows_when_the_closing_block_fits_beneath_them() {
        val n = FIRST_PAGE_ROWS - TRAILER_ROWS
        val pages = planStatementPages(n)
        assertEquals(1, pages.size)
        assertEquals(n, pages[0].rowCount)
        assertTrue(pages[0].showsTrailer)
    }

    @Test
    fun the_masthead_costs_the_first_page_rows() {
        // The first sheet carries the logo, the company block, the customer and the
        // period; a continuation sheet carries a single slim line. Giving page one
        // a continuation page's worth of rows runs the table off the bottom of the
        // paper, and nothing about the output says so until it is printed.
        val pages = planStatementPages(CONTINUATION_PAGE_ROWS)
        assertTrue(
            pages[0].rowCount < CONTINUATION_PAGE_ROWS,
            "the first page must hold fewer rows than a continuation page",
        )
        assertTrue(pages.size > 1)
    }

    @Test
    fun no_planned_page_is_taller_than_the_paper() {
        // The constants and the row counts are one budget, and this is what holds
        // them together. Widening the masthead or adding a line to the closing
        // block without re-deriving the counts pushes movements off the bottom of
        // the sheet, where nothing about the PDF says they are missing.
        (0..300).forEach { n ->
            planStatementPages(n).forEachIndexed { i, page ->
                val height = plannedPageHeight(page)
                assertTrue(
                    height <= A4_HEIGHT_DP,
                    "page ${i + 1} of a $n-row statement wants ${height}dp of a ${A4_HEIGHT_DP}dp sheet",
                )
            }
        }
    }

    @Test
    fun a_full_sheet_uses_most_of_the_paper() {
        // The other side of the previous test: a budget that is merely SAFE could be
        // satisfied by printing three rows a page. A full continuation sheet should
        // be within a couple of rows of the bottom.
        val page = planStatementPages(500)[1]
        assertTrue(page.rowCount == CONTINUATION_PAGE_ROWS)
        assertTrue(
            plannedPageHeight(page) > A4_HEIGHT_DP - 80f,
            "a full sheet leaves ${A4_HEIGHT_DP - plannedPageHeight(page)}dp unused",
        )
    }

    @Test
    fun page_count_grows_with_the_account_rather_than_the_type_shrinking() {
        // The whole reason pagination exists: a year of movement must not be
        // squeezed onto one sheet.
        assertTrue(planStatementPages(400).size > planStatementPages(40).size)
    }
}
