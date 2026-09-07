package com.jehadalomour.flowvan.feature.voucher

import com.jehadalomour.flowvan.core.model.CartLine
import com.jehadalomour.flowvan.core.model.FreeLine
import com.jehadalomour.flowvan.core.model.Product
import com.jehadalomour.flowvan.core.model.ProductUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The van-stock gate on a SALE, with the offers' gifts counted.
 *
 * A gift costs nothing but it still leaves the van, so it has to be counted against
 * stock like a sold piece. The case that used to slip through: a rep holding 6 of an
 * item sells all 6, "buy 6 get 1 free" adds a seventh, and nothing on the device
 * objected — the backend refused it later, after the goods were gone.
 */
class VoucherStockGateTest {

    private fun product(sku: String, vanStock: Int) = Product(
        id = "p-$sku",
        sku = sku,
        nameAr = "صنف $sku",
        nameEn = sku,
        category = "",
        unit = "حبة",
        salePrice = 1.0,
        costPrice = 0.5,
        vanStock = vanStock,
        minStock = 0,
        expiryDate = null,
        brand = null,
    )

    private fun line(sku: String, qty: Double, conversionQty: Double = 1.0, unitId: String = "") =
        CartLine(
            productId = "p-$sku",
            sku = sku,
            nameAr = "صنف $sku",
            unitPrice = 1.0,
            qty = qty,
            unitId = unitId,
            unitConversionQty = conversionQty,
        )

    private fun gift(sku: String, qty: Double) =
        FreeLine(itemNumber = sku, qty = qty, unitPriceJod = 1.0, offerId = "o1")

    private fun sale(
        products: List<Product>,
        cart: List<CartLine>,
        freeLines: List<FreeLine> = emptyList(),
        productUnits: Map<String, List<ProductUnit>> = emptyMap(),
    ) = VoucherState(
        type = VoucherType.SALE,
        products = products,
        productUnits = productUnits,
        cart = cart,
        freeLines = freeLines,
    )

    @Test
    fun `six sold plus one gift out of six in the van is a shortage`() {
        val state = sale(
            products = listOf(product("GEL", vanStock = 6)),
            cart = listOf(line("GEL", 6.0)),
            freeLines = listOf(gift("GEL", 1.0)),
        )
        val shortage = state.stockShortages.single()
        assertEquals(6, shortage.available)
        assertEquals(7, shortage.requested)
        assertFalse(state.canSave, "a sale the van cannot cover must not be saveable")
    }

    @Test
    fun `five sold plus one gift out of six in the van fits`() {
        val state = sale(
            products = listOf(product("GEL", vanStock = 6)),
            cart = listOf(line("GEL", 5.0)),
            freeLines = listOf(gift("GEL", 1.0)),
        )
        assertTrue(state.stockShortages.isEmpty())
        assertTrue(state.canSave)
    }

    @Test
    fun `a gift of another item is charged to that item's stock`() {
        val state = sale(
            products = listOf(product("GEL", vanStock = 10), product("SOAP", vanStock = 0)),
            cart = listOf(line("GEL", 6.0)),
            freeLines = listOf(gift("SOAP", 1.0)),
        )
        val shortage = state.stockShortages.single()
        assertEquals("SOAP", shortage.sku)
        assertEquals(0, shortage.available)
        assertEquals(1, shortage.requested)
    }

    @Test
    fun `the stepper reserves the gifted pieces`() {
        val state = sale(
            products = listOf(product("GEL", vanStock = 6)),
            cart = listOf(line("GEL", 5.0)),
            freeLines = listOf(gift("GEL", 1.0)),
        )
        // 6 in the van, one of them promised free → 5 is as far as the sold line can go.
        assertEquals(5.0, state.maxQtyFor(state.cart.single()))
    }

    @Test
    fun `a packaging unit's cap counts the gift in base pieces`() {
        val state = sale(
            products = listOf(product("GEL", vanStock = 24)),
            cart = listOf(line("GEL", 1.0, conversionQty = 12.0, unitId = "u-carton")),
            freeLines = listOf(gift("GEL", 1.0)),
            productUnits = mapOf(
                "p-GEL" to listOf(
                    ProductUnit(
                        id = "u-carton",
                        productId = "p-GEL",
                        name = "كرتونة",
                        price = 12.0,
                        conversionQty = 12.0,
                    ),
                ),
            ),
        )
        // 24 pieces less the gifted one = 23 → one whole carton of 12 is all that fits.
        assertEquals(1.0, state.maxQtyFor(state.cart.single()))
    }

    @Test
    fun `two lines of one item are pooled against the same stock`() {
        val state = sale(
            products = listOf(product("GEL", vanStock = 12)),
            cart = listOf(
                line("GEL", 1.0, conversionQty = 12.0, unitId = "u-carton"),
                line("GEL", 1.0),
            ),
        )
        val shortage = state.stockShortages.single()
        assertEquals(12, shortage.available)
        assertEquals(13, shortage.requested)
    }

    @Test
    fun `a variant unit has its own pool, which gifts never touch`() {
        val state = sale(
            products = listOf(product("GEL", vanStock = 6)),
            cart = listOf(line("GEL", 6.0, unitId = "u-red")),
            freeLines = listOf(gift("GEL", 1.0)),
            productUnits = mapOf(
                "p-GEL" to listOf(
                    ProductUnit(
                        id = "u-red",
                        productId = "p-GEL",
                        name = "أحمر",
                        price = 1.0,
                        conversionQty = 1.0,
                        vanStock = 6,
                        isStockUnit = true,
                    ),
                ),
            ),
        )
        // The variant's own 6 cover the sold line; the gift comes out of the base pool,
        // which holds 6 and is untouched by the sale.
        assertTrue(state.stockShortages.isEmpty())
        assertEquals(6.0, state.maxQtyFor(state.cart.single()))
    }

    @Test
    fun `a RETURN is never gated on van stock`() {
        val state = VoucherState(
            type = VoucherType.RETURN,
            products = listOf(product("GEL", vanStock = 0)),
            cart = listOf(line("GEL", 6.0)),
        )
        assertTrue(state.stockShortages.isEmpty())
    }

    @Test
    fun `the gift sheet shows what the van can still spare`() {
        val state = sale(
            products = listOf(product("GEL", vanStock = 6)),
            cart = listOf(line("GEL", 6.0)),
        )
        assertEquals(0, state.giftRoomFor("GEL"))
        assertEquals(4, state.copy(cart = listOf(line("GEL", 2.0))).giftRoomFor("GEL"))
    }
}
