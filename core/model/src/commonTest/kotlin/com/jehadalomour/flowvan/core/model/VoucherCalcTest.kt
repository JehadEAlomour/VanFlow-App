package com.jehadalomour.flowvan.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Mirrors the backend's canonical fixtures (voucher-calc.spec.ts) to prove the
 * Kotlin engine produces byte-for-fil identical results. Keep in sync with
 * cash-van-dashboard/docs/VOUCHER-CALC-SPEC.md.
 */
class VoucherCalcTest {

    @Test
    fun exclusive_single_line_no_discount() {
        val r = VoucherCalc.calc(
            CalcInput(taxMode = TaxMode.EXCLUSIVE, lines = listOf(
                CalcLineInput(unitPriceFils = 1000, qty = 2.0, taxRatePct = 16.0),
            )),
        )
        assertEquals(2000, r.lines[0].netFils)
        assertEquals(320, r.lines[0].taxFils)
        assertEquals(2320, r.lines[0].totalFils)
        assertEquals(2000, r.totalNetFils)
        assertEquals(320, r.totalTaxFils)
        assertEquals(2320, r.grandTotalFils)
    }

    @Test
    fun inclusive_single_line_tax_extracted() {
        val r = VoucherCalc.calc(
            CalcInput(taxMode = TaxMode.INCLUSIVE, lines = listOf(
                CalcLineInput(unitPriceFils = 1160, qty = 1.0, taxRatePct = 16.0),
            )),
        )
        assertEquals(1000, r.lines[0].taxableFils)
        assertEquals(160, r.lines[0].taxFils)
        assertEquals(1160, r.lines[0].totalFils)
    }

    @Test
    fun exclusive_line_percent_discount() {
        val r = VoucherCalc.calc(
            CalcInput(taxMode = TaxMode.EXCLUSIVE, lines = listOf(
                CalcLineInput(unitPriceFils = 1000, qty = 1.0, lineDiscountPct = 10.0, taxRatePct = 16.0),
            )),
        )
        assertEquals(100, r.lines[0].lineDiscountFils)
        assertEquals(900, r.lines[0].netFils)
        assertEquals(144, r.lines[0].taxFils)
        assertEquals(1044, r.lines[0].totalFils)
    }

    @Test
    fun exclusive_line_percent_plus_fixed_stacked() {
        val r = VoucherCalc.calc(
            CalcInput(taxMode = TaxMode.EXCLUSIVE, lines = listOf(
                CalcLineInput(unitPriceFils = 1000, qty = 1.0, lineDiscountPct = 10.0, lineDiscountFils = 50, taxRatePct = 16.0),
            )),
        )
        assertEquals(150, r.lines[0].lineDiscountFils)
        assertEquals(850, r.lines[0].netFils)
        assertEquals(136, r.lines[0].taxFils)
        assertEquals(986, r.lines[0].totalFils)
    }

    @Test
    fun exclusive_header_percent_distributed() {
        val r = VoucherCalc.calc(
            CalcInput(taxMode = TaxMode.EXCLUSIVE, headerDiscountPct = 10.0, lines = listOf(
                CalcLineInput(unitPriceFils = 1000, qty = 1.0, taxRatePct = 16.0),
                CalcLineInput(unitPriceFils = 2000, qty = 1.0, taxRatePct = 16.0),
            )),
        )
        assertEquals(300, r.headerDiscountFils)
        assertEquals(100, r.lines[0].headerShareFils)
        assertEquals(900, r.lines[0].netFils)
        assertEquals(144, r.lines[0].taxFils)
        assertEquals(200, r.lines[1].headerShareFils)
        assertEquals(1800, r.lines[1].netFils)
        assertEquals(288, r.lines[1].taxFils)
        assertEquals(2700, r.totalNetFils)
        assertEquals(432, r.totalTaxFils)
        assertEquals(3132, r.grandTotalFils)
    }

    @Test
    fun exclusive_header_fixed_remainder_on_last_line() {
        val r = VoucherCalc.calc(
            CalcInput(taxMode = TaxMode.EXCLUSIVE, headerDiscountFils = 100, lines = listOf(
                CalcLineInput(unitPriceFils = 1000, qty = 1.0, taxRatePct = 16.0),
                CalcLineInput(unitPriceFils = 1000, qty = 1.0, taxRatePct = 16.0),
                CalcLineInput(unitPriceFils = 1000, qty = 1.0, taxRatePct = 16.0),
            )),
        )
        assertEquals(listOf(33L, 33L, 34L), r.lines.map { it.headerShareFils })
        assertEquals(100, r.headerDiscountFils)
        assertEquals(2900, r.totalNetFils)
        assertEquals(465, r.totalTaxFils)
        assertEquals(3365, r.grandTotalFils)
    }

    @Test
    fun inclusive_with_header_discount() {
        val r = VoucherCalc.calc(
            CalcInput(taxMode = TaxMode.INCLUSIVE, headerDiscountFils = 160, lines = listOf(
                CalcLineInput(unitPriceFils = 1160, qty = 1.0, taxRatePct = 16.0),
            )),
        )
        assertEquals(1000, r.lines[0].netFils)
        assertEquals(862, r.lines[0].taxableFils)
        assertEquals(138, r.lines[0].taxFils)
        assertEquals(1000, r.lines[0].totalFils)
    }

    @Test
    fun grand_total_equals_net_plus_tax_invariant() {
        val r = VoucherCalc.calc(
            CalcInput(taxMode = TaxMode.EXCLUSIVE, headerDiscountPct = 7.0, lines = listOf(
                CalcLineInput(unitPriceFils = 1234, qty = 3.0, lineDiscountPct = 5.0, lineDiscountFils = 20, taxRatePct = 16.0),
                CalcLineInput(unitPriceFils = 555, qty = 2.0, taxRatePct = 16.0),
            )),
        )
        assertEquals(r.totalNetFils + r.totalTaxFils, r.grandTotalFils)
    }
}
