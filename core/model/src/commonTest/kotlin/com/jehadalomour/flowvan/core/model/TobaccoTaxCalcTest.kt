package com.jehadalomour.flowvan.core.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Proves the Kotlin tobacco engine matches the backend/ERP engine
 * (cash-van-dashboard/src/modules/vouchers/tobacco-tax-calc.ts) for the canonical
 * spec fixture. Keep in sync with docs/SPEC-tobacco-tax.md.
 */
class TobaccoTaxCalcTest {

    // Spec example: qty=10, unitPrice=2000, consumerPrice=2500, salesTax=13% on the
    // consumer base, special=600/unit fixed, withheld=400/unit fixed.
    private val specProfile = TobaccoTaxProfile(
        id = "p1",
        taxBase = TobaccoTaxBase.CONSUMER_PRICE,
        salesTaxEnabled = true,
        salesTaxRate = 13,
        specialTaxEnabled = true,
        specialTaxCalculationType = SpecialTaxCalcType.FIXED_PER_UNIT,
        specialTaxBase = SpecialTaxBase.QUANTITY,
        specialTaxRate = null,
        specialTaxFixedAmount = 600,
        withheldTaxEnabled = true,
        withheldTaxCalculationType = WithheldTaxCalcType.FIXED_PER_UNIT,
        withheldTaxBase = WithheldTaxBase.SALE_PRICE,
        withheldTaxAmount = 400,
        withheldTaxRate = null,
    )

    @Test
    fun spec_fixture_matches_backend() {
        val r = TobaccoTaxCalc.calc(
            quantity = 10.0,
            unitPriceFils = 2000,
            consumerPriceFils = 2500,
            profile = specProfile,
        )
        assertEquals(3250L, r.salesTaxFils)   // 13% × (2500×10)
        assertEquals(6000L, r.specialTaxFils) // 600 × 10
        assertEquals(4000L, r.withheldTaxFils) // 400 × 10
        assertEquals(9250L, r.grossTaxFils)   // 3250 + 6000
        assertEquals(5250L, r.netTaxFils)     // 9250 − 4000
    }

    @Test
    fun sale_price_base_uses_discounted_price_rate_special() {
        // taxBase SALE_PRICE, sales 10%, special RATE 5% on sale, no withheld.
        val p = specProfile.copy(
            taxBase = TobaccoTaxBase.SALE_PRICE,
            salesTaxRate = 10,
            specialTaxCalculationType = SpecialTaxCalcType.RATE,
            specialTaxBase = SpecialTaxBase.SALE_PRICE,
            specialTaxRate = 5,
            specialTaxFixedAmount = null,
            withheldTaxEnabled = false,
        )
        // saleBase = 1000 × 4 = 4000 → sales 400, special 200, net 600.
        val r = TobaccoTaxCalc.calc(quantity = 4.0, unitPriceFils = 1000, consumerPriceFils = 9999, profile = p)
        assertEquals(400L, r.salesTaxFils)
        assertEquals(200L, r.specialTaxFils)
        assertEquals(600L, r.netTaxFils)
    }

    @Test
    fun consumer_price_base_is_offer_immune_via_invoice_calc() {
        // A tobacco line in the cart: consumer-price base → tax independent of the sale
        // discount. 10 base pieces, MSRP 2500 fils, sales 13%, special 600/u, withheld 400/u.
        val line = CartLine(
            productId = "x", sku = "SK", nameAr = "دخان",
            unitPrice = 2.000, qty = 10.0, discountPct = 0.20, // 20% offer
            unitConversionQty = 1.0, taxRate = 0.16, lineTaxType = LineTaxType.TAXABLE,
            isTobacco = true, tobaccoProfile = specProfile, consumerPriceFils = 2500,
        )
        val summary = InvoiceTaxCalculator.calculateInvoice(listOf(line))
        // Tobacco tax stays 5.250 JOD (net) regardless of the 20% discount, because the
        // profile's base is the fixed consumer price. Only the line net (16.000) shrinks.
        assertEquals(5.250, summary.totalTax, 0.0005)
    }
}
