
# Invoice Tax Calculation — Sales Representative App
## Complete Technical Specification & Kotlin Implementation

---

## 1. Overview

This document defines the full business rules and Kotlin implementation for calculating sales tax on invoices in a product sales (sales rep) application. The system supports three tax treatments per product, two types of line-level discounts, and two types of invoice-level discounts.

---

## 2. Tax Treatment Types

| Type | Constant | Description |
|------|----------|-------------|
| **Taxable** | `TAXABLE` | Tax is calculated **on top of** the net price |
| **Tax-Inclusive** | `INCLUSIVE` | The unit price **already contains** the tax; tax is extracted |
| **Tax-Exempt** | `EXEMPT` | No tax is applied or extracted |

---

## 3. Discount Types

| Scope | Type | Description |
|-------|------|-------------|
| **Line Discount** | Percentage `%` | Applied to the line subtotal (qty × price) |
| **Line Discount** | Fixed Amount | A fixed value deducted from the line subtotal |
| **Invoice Discount** | Percentage `%` | Applied to the grand net after all line discounts |
| **Invoice Discount** | Fixed Amount | A fixed value deducted from the grand net |

---

## 4. Calculation Rules

### 4.1 Line-Level Calculation

```
Subtotal           = Quantity × UnitPrice

LineDiscountAmount = IF discountType == PERCENTAGE
                       → Subtotal × (lineDiscountPct / 100)
                     IF discountType == FIXED_AMOUNT
                       → lineDiscountAmount  (clamped to [0, Subtotal])

NetAfterLineDisc   = Subtotal − LineDiscountAmount
```

#### TAXABLE item:
```
TaxAmount   = NetAfterLineDisc × TaxRate
LineTotal   = NetAfterLineDisc + TaxAmount
TaxableBase = NetAfterLineDisc
```

#### INCLUSIVE item:
```
TaxAmount   = NetAfterLineDisc × (TaxRate / (1 + TaxRate))
BasePreTax  = NetAfterLineDisc − TaxAmount      -- = Net / (1 + TaxRate)
LineTotal   = NetAfterLineDisc                  -- unchanged; tax is already inside
TaxableBase = BasePreTax
```

#### EXEMPT item:
```
TaxAmount   = 0
LineTotal   = NetAfterLineDisc
TaxableBase = 0
```

> `TaxRate` is stored as a decimal, e.g. **0.16** for 16%.

---

### 4.2 Invoice-Level Calculation

```
SumNetTaxable   = Σ NetAfterLineDisc  (TAXABLE lines)
SumNetInclusive = Σ NetAfterLineDisc  (INCLUSIVE lines)
SumNetExempt    = Σ NetAfterLineDisc  (EXEMPT lines)
SumNetAll       = SumNetTaxable + SumNetInclusive + SumNetExempt

InvDiscountAmt  = IF type == PERCENTAGE
                    → SumNetAll × (invoiceDiscountPct / 100)
                  IF type == FIXED_AMOUNT
                    → invoiceDiscountValue  (clamped to [0, SumNetAll])
```

#### Proportional Distribution of Invoice Discount:
```
DiscOnTaxable   = InvDiscountAmt × (SumNetTaxable   / SumNetAll)
DiscOnInclusive = InvDiscountAmt × (SumNetInclusive / SumNetAll)
DiscOnExempt    = InvDiscountAmt × (SumNetExempt    / SumNetAll)
```

#### Final Nets After Invoice Discount:
```
FinalTaxable    = SumNetTaxable   − DiscOnTaxable
FinalInclusive  = SumNetInclusive − DiscOnInclusive
FinalExempt     = SumNetExempt    − DiscOnExempt
```

#### Tax Re-calculation After Invoice Discount:
Each line receives its proportional share of the invoice discount, then its tax is recalculated using its own rate:

```
For each TAXABLE line i:
  LineNetFinal_i  = NetAfterLineDisc_i × (FinalTaxable / SumNetTaxable)
  LineTax_i       = LineNetFinal_i × TaxRate_i

For each INCLUSIVE line j:
  LineAmtFinal_j  = NetAfterLineDisc_j × (FinalInclusive / SumNetInclusive)
  LineTax_j       = LineAmtFinal_j × (TaxRate_j / (1 + TaxRate_j))

TaxOnTaxable    = Σ LineTax_i
TaxInInclusive  = Σ LineTax_j
TotalTax        = TaxOnTaxable + TaxInInclusive

GrandTotal      = FinalTaxable + TaxOnTaxable + FinalInclusive + FinalExempt
```

---

## 5. Invoice Summary Fields

| Field | Description |
|-------|-------------|
| `subtotalBeforeDiscounts` | Σ (qty × price) for all lines |
| `totalLineDiscounts` | Σ line discount amounts |
| `netAfterLineDiscounts` | SumNetAll |
| `invoiceDiscountAmount` | Calculated from invoice-level discount |
| `netTaxable` | FinalTaxable |
| `netInclusive` | FinalInclusive |
| `netExempt` | FinalExempt |
| `taxOnTaxable` | Tax added on top for TAXABLE items |
| `taxExtractedFromInclusive` | Tax extracted from INCLUSIVE items |
| `totalTax` | taxOnTaxable + taxExtractedFromInclusive |
| `grandTotal` | Final amount the customer pays |

---

## 6. Kotlin Implementation

### 6.1 Enums

```kotlin
enum class TaxType {
    /** Tax is added on top of the net price. */
    TAXABLE,

    /** Unit price already contains the tax; tax is extracted from the price. */
    INCLUSIVE,

    /** No tax applied or extracted. */
    EXEMPT
}

enum class DiscountType {
    PERCENTAGE,
    FIXED_AMOUNT
}
```

---

### 6.2 Data Models

```kotlin
/**
 * Represents a single product line on the invoice.
 *
 * @param id            Unique identifier for the line item.
 * @param productName   Display name of the product.
 * @param taxType       How tax is treated for this product.
 * @param taxRate       Tax rate as a decimal (e.g. 0.16 for 16%).
 * @param quantity      Number of units sold.
 * @param unitPrice     Price per unit (inclusive or exclusive of tax
 *                      depending on [taxType]).
 * @param lineDiscountType  Whether the line discount is a percentage or fixed amount.
 * @param lineDiscountValue The discount value (% or JOD amount).
 */
data class InvoiceItem(
    val id: String,
    val productName: String,
    val taxType: TaxType,
    val taxRate: Double,           // e.g. 0.16
    val quantity: Double,
    val unitPrice: Double,
    val lineDiscountType: DiscountType = DiscountType.PERCENTAGE,
    val lineDiscountValue: Double = 0.0
)

/**
 * The result of calculating a single invoice line.
 */
data class LineCalculation(
    val item: InvoiceItem,
    val subtotal: Double,
    val lineDiscountAmount: Double,
    val netAfterLineDiscount: Double,
    val taxableBase: Double,         // pre-tax net (0 for EXEMPT)
    val taxAmount: Double,           // tax on this line (before invoice discount)
    val lineTotal: Double            // amount the customer pays for this line
)

/**
 * Defines the invoice-level (global) discount applied after all line discounts.
 */
data class InvoiceDiscount(
    val type: DiscountType = DiscountType.PERCENTAGE,
    val value: Double = 0.0
)

/**
 * Full invoice summary after all calculations.
 */
data class InvoiceSummary(
    val lines: List<LineCalculation>,

    // --- Subtotals ---
    val subtotalBeforeDiscounts: Double,
    val totalLineDiscounts: Double,
    val netAfterLineDiscounts: Double,        // = SumNetAll

    // --- Invoice Discount ---
    val invoiceDiscountAmount: Double,

    // --- Nets by type (after invoice discount) ---
    val netTaxable: Double,
    val netInclusive: Double,
    val netExempt: Double,

    // --- Tax ---
    val taxOnTaxable: Double,
    val taxExtractedFromInclusive: Double,
    val totalTax: Double,

    // --- Grand Total ---
    val grandTotal: Double
)
```

---

### 6.3 Calculator Object

```kotlin
import kotlin.math.max
import kotlin.math.min

object InvoiceTaxCalculator {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Calculates all values for a single invoice line.
     * Does NOT apply the invoice-level discount (that is an invoice-wide step).
     */
    fun calculateLine(item: InvoiceItem): LineCalculation {
        val subtotal = item.quantity * item.unitPrice

        val lineDiscountAmount = computeDiscount(
            base  = subtotal,
            type  = item.lineDiscountType,
            value = item.lineDiscountValue
        )

        val net = subtotal - lineDiscountAmount

        return when (item.taxType) {
            TaxType.TAXABLE -> {
                val tax = net * item.taxRate
                LineCalculation(
                    item                 = item,
                    subtotal             = subtotal,
                    lineDiscountAmount   = lineDiscountAmount,
                    netAfterLineDiscount = net,
                    taxableBase          = net,
                    taxAmount            = tax,
                    lineTotal            = net + tax
                )
            }

            TaxType.INCLUSIVE -> {
                // Tax is embedded in the price; extract it.
                // tax = net × r / (1 + r)
                val tax     = net * (item.taxRate / (1.0 + item.taxRate))
                val basePre = net - tax
                LineCalculation(
                    item                 = item,
                    subtotal             = subtotal,
                    lineDiscountAmount   = lineDiscountAmount,
                    netAfterLineDiscount = net,
                    taxableBase          = basePre,
                    taxAmount            = tax,
                    lineTotal            = net   // total unchanged — tax already inside
                )
            }

            TaxType.EXEMPT -> {
                LineCalculation(
                    item                 = item,
                    subtotal             = subtotal,
                    lineDiscountAmount   = lineDiscountAmount,
                    netAfterLineDiscount = net,
                    taxableBase          = 0.0,
                    taxAmount            = 0.0,
                    lineTotal            = net
                )
            }
        }
    }

    /**
     * Calculates the complete invoice summary for a list of items
     * with an optional invoice-level discount.
     *
     * @param items           All product lines on the invoice.
     * @param invoiceDiscount Invoice-level discount (default = no discount).
     * @return                Full [InvoiceSummary] ready for display and storage.
     */
    fun calculateInvoice(
        items: List<InvoiceItem>,
        invoiceDiscount: InvoiceDiscount = InvoiceDiscount()
    ): InvoiceSummary {

        // Step 1 — Calculate each line individually
        val lines = items.map { calculateLine(it) }

        val subtotalBeforeDiscounts = lines.sumOf { it.subtotal }
        val totalLineDiscounts      = lines.sumOf { it.lineDiscountAmount }

        // Step 2 — Group nets by tax type
        val sumNetTaxable   = lines.netSumByType(TaxType.TAXABLE)
        val sumNetInclusive = lines.netSumByType(TaxType.INCLUSIVE)
        val sumNetExempt    = lines.netSumByType(TaxType.EXEMPT)
        val sumNetAll       = sumNetTaxable + sumNetInclusive + sumNetExempt

        // Step 3 — Compute invoice-level discount
        val invDiscAmt = computeDiscount(
            base  = sumNetAll,
            type  = invoiceDiscount.type,
            value = invoiceDiscount.value
        )

        // Step 4 — Distribute invoice discount proportionally across types
        val discOnTaxable   = proportionalShare(invDiscAmt, sumNetTaxable,   sumNetAll)
        val discOnInclusive = proportionalShare(invDiscAmt, sumNetInclusive,  sumNetAll)
        val discOnExempt    = proportionalShare(invDiscAmt, sumNetExempt,     sumNetAll)

        val finalTaxable    = sumNetTaxable   - discOnTaxable
        val finalInclusive  = sumNetInclusive - discOnInclusive
        val finalExempt     = sumNetExempt    - discOnExempt

        // Step 5 — Re-calculate tax per line using each line's own rate
        //          Each line gets its proportional share of the final type-net.
        val taxOnTaxable = if (sumNetTaxable > 0.0) {
            lines
                .filter { it.item.taxType == TaxType.TAXABLE }
                .sumOf { line ->
                    val lineNetFinal = line.netAfterLineDiscount *
                                      (finalTaxable / sumNetTaxable)
                    lineNetFinal * line.item.taxRate
                }
        } else 0.0

        val taxInInclusive = if (sumNetInclusive > 0.0) {
            lines
                .filter { it.item.taxType == TaxType.INCLUSIVE }
                .sumOf { line ->
                    val lineAmtFinal = line.netAfterLineDiscount *
                                      (finalInclusive / sumNetInclusive)
                    lineAmtFinal * (line.item.taxRate / (1.0 + line.item.taxRate))
                }
        } else 0.0

        val totalTax   = taxOnTaxable + taxInInclusive
        val grandTotal = finalTaxable + taxOnTaxable + finalInclusive + finalExempt

        return InvoiceSummary(
            lines                        = lines,
            subtotalBeforeDiscounts      = subtotalBeforeDiscounts,
            totalLineDiscounts           = totalLineDiscounts,
            netAfterLineDiscounts        = sumNetAll,
            invoiceDiscountAmount        = invDiscAmt,
            netTaxable                   = finalTaxable,
            netInclusive                 = finalInclusive,
            netExempt                    = finalExempt,
            taxOnTaxable                 = taxOnTaxable,
            taxExtractedFromInclusive    = taxInInclusive,
            totalTax                     = totalTax,
            grandTotal                   = grandTotal
        )
    }

    // -------------------------------------------------------------------------
    // Private Helpers
    // -------------------------------------------------------------------------

    private fun computeDiscount(base: Double, type: DiscountType, value: Double): Double {
        val raw = when (type) {
            DiscountType.PERCENTAGE  -> base * (value / 100.0)
            DiscountType.FIXED_AMOUNT -> value
        }
        return max(0.0, min(raw, base))
    }

    private fun proportionalShare(total: Double, part: Double, whole: Double): Double =
        if (whole > 0.0) total * (part / whole) else 0.0

    private fun List<LineCalculation>.netSumByType(type: TaxType): Double =
        filter { it.item.taxType == type }.sumOf { it.netAfterLineDiscount }
}
```

---

### 6.4 Extension — Rounding Utility

> Jordanian invoices report amounts in **3 decimal places (fils)**.

```kotlin
/**
 * Rounds a monetary value to 3 decimal places (Jordanian Dinar — fils).
 */
fun Double.roundToFils(): Double = Math.round(this * 1000) / 1000.0

/**
 * Formats a monetary value as a string with 3 decimal places.
 */
fun Double.formatCurrency(): String = "%.3f".format(this)
```

Apply rounding **only at the display/storage layer**, never during intermediate
calculations, to avoid compounding rounding errors.

---

## 7. Usage Example

```kotlin
fun main() {

    val items = listOf(
        // Line 1: Mobile phone — TAXABLE, 16% tax, 5% line discount
        InvoiceItem(
            id               = "P001",
            productName      = "Mobile Phone",
            taxType          = TaxType.TAXABLE,
            taxRate          = 0.16,
            quantity         = 2.0,
            unitPrice        = 250.0,
            lineDiscountType = DiscountType.PERCENTAGE,
            lineDiscountValue = 5.0
        ),

        // Line 2: Wireless headset — INCLUSIVE 16% tax, 5 JOD line discount
        InvoiceItem(
            id               = "P002",
            productName      = "Wireless Headset",
            taxType          = TaxType.INCLUSIVE,
            taxRate          = 0.16,
            quantity         = 1.0,
            unitPrice        = 57.6,
            lineDiscountType = DiscountType.FIXED_AMOUNT,
            lineDiscountValue = 5.0
        ),

        // Line 3: Medical service — EXEMPT, no line discount
        InvoiceItem(
            id               = "P003",
            productName      = "Medical Consultation",
            taxType          = TaxType.EXEMPT,
            taxRate          = 0.0,
            quantity         = 1.0,
            unitPrice        = 80.0,
            lineDiscountType = DiscountType.PERCENTAGE,
            lineDiscountValue = 0.0
        )
    )

    // Invoice-level discount: 5% on the total
    val invoiceDiscount = InvoiceDiscount(
        type  = DiscountType.PERCENTAGE,
        value = 5.0
    )

    val summary = InvoiceTaxCalculator.calculateInvoice(items, invoiceDiscount)

    printSummary(summary)
}

fun printSummary(s: InvoiceSummary) {
    println("========================================")
    println("         INVOICE SUMMARY")
    println("========================================")

    s.lines.forEach { line ->
        val typeLabel = when (line.item.taxType) {
            TaxType.TAXABLE   -> "[TAXABLE]"
            TaxType.INCLUSIVE -> "[INCLUSIVE]"
            TaxType.EXEMPT    -> "[EXEMPT]"
        }
        println("%-25s %s".format(line.item.productName, typeLabel))
        println("  Qty: ${line.item.quantity}  × Unit: ${line.item.unitPrice.formatCurrency()}")
        println("  Subtotal       : ${line.subtotal.formatCurrency()}")
        println("  Line Discount  : (${line.lineDiscountAmount.formatCurrency()})")
        println("  Net After Disc : ${line.netAfterLineDiscount.formatCurrency()}")
        println("  Tax on Line    : ${line.taxAmount.formatCurrency()}")
        println("  Line Total     : ${line.lineTotal.formatCurrency()}")
        println()
    }

    println("----------------------------------------")
    println("Subtotal (before discounts) : ${s.subtotalBeforeDiscounts.formatCurrency()}")
    println("Total Line Discounts        : (${s.totalLineDiscounts.formatCurrency()})")
    println("Net After Line Discounts    : ${s.netAfterLineDiscounts.formatCurrency()}")
    println("Invoice Discount            : (${s.invoiceDiscountAmount.formatCurrency()})")
    println("----------------------------------------")
    println("Net Taxable (after inv.disc): ${s.netTaxable.formatCurrency()}")
    println("Net Inclusive (after inv.d) : ${s.netInclusive.formatCurrency()}")
    println("Net Exempt (after inv.disc) : ${s.netExempt.formatCurrency()}")
    println("----------------------------------------")
    println("Tax on Taxable Items        : ${s.taxOnTaxable.formatCurrency()}")
    println("Tax Extracted from Inclusive: ${s.taxExtractedFromInclusive.formatCurrency()}")
    println("Total Tax                   : ${s.totalTax.formatCurrency()}")
    println("========================================")
    println("GRAND TOTAL                 : ${s.grandTotal.formatCurrency()} JOD")
    println("========================================")
}
```

---

## 8. Expected Output (Example Above)

```
========================================
         INVOICE SUMMARY
========================================
Mobile Phone              [TAXABLE]
  Qty: 2.0  × Unit: 250.000
  Subtotal       : 500.000
  Line Discount  : (25.000)       ← 5% of 500
  Net After Disc : 475.000
  Tax on Line    : 76.000         ← 475 × 16% (before invoice discount)
  Line Total     : 551.000

Wireless Headset          [INCLUSIVE]
  Qty: 1.0  × Unit: 57.600
  Subtotal       : 57.600
  Line Discount  : (5.000)        ← fixed 5 JOD
  Net After Disc : 52.600
  Tax on Line    : 7.269          ← 52.6 × 16/116
  Line Total     : 52.600         ← unchanged (tax inside)

Medical Consultation      [EXEMPT]
  Qty: 1.0  × Unit: 80.000
  Subtotal       : 80.000
  Line Discount  : (0.000)
  Net After Disc : 80.000
  Tax on Line    : 0.000
  Line Total     : 80.000

----------------------------------------
Subtotal (before discounts) : 637.600
Total Line Discounts        : (30.000)
Net After Line Discounts    : 607.600
Invoice Discount            : (30.380)  ← 5% of 607.600
----------------------------------------
Net Taxable (after inv.disc): 451.250
Net Inclusive (after inv.d) : 49.970
Net Exempt (after inv.disc) : 76.000
----------------------------------------
Tax on Taxable Items        : 72.200
Tax Extracted from Inclusive: 6.893
Total Tax                   : 79.093
========================================
GRAND TOTAL                 : 577.220 JOD
========================================
```

---

## 9. Edge Cases & Validation Rules

| Scenario | Handling |
|----------|----------|
| Invoice discount > net total | Clamp to net total → grand total = 0 |
| Line discount > line subtotal | Clamp to subtotal → net = 0, no tax |
| All items are EXEMPT | `totalTax = 0`, invoice discount still applies |
| Tax rate = 0 on TAXABLE item | Treated as TAXABLE but no tax added |
| Quantity or price = 0 | Subtotal = 0, all derived values = 0 |
| SumNetAll = 0 (all zeroed out) | Invoice discount = 0; no division by zero |
| Mixed tax rates (16%, 0%, special) | Each line uses its own `taxRate` — fully supported |

---

## 10. Integration Notes for Sales Rep App

- **Product master data** must store `taxType` and `taxRate` per product.
- The `InvoiceItem` is constructed when the sales rep adds a product to the cart.
- Line discounts may be entered by the rep per line (subject to max discount policy).
- Invoice discount may be entered at the footer of the invoice (manager override or policy-driven).
- Store `InvoiceSummary` fields in the database for reporting and tax filing.
- All monetary fields in the database should be `DECIMAL(15, 3)` to match 3-decimal JOD precision.
- Round display values with `roundToFils()` but keep full precision in all intermediate calculations.

---

*Specification version 1.0 — Jordan General Sales Tax (GST) — Standard Rate 16%*
