# Discount & Tax Calculation Reference (for Node.js POS)

This document describes how **discounts** and **taxes** are calculated in FlowVan, with
ready-to-use JavaScript so the same rules can be imported into a Node.js POS.

All formulas below match the Kotlin source of truth:

- `core/model/.../CartLine.kt` — per-line discount & tax
- `core/model/.../InvoiceTaxCalculator.kt` — invoice-level discount & tax
- `core/network/.../Money.kt` — money/percent conversions

> **Money convention:** the backend stores money as integer **fils** (`1 JOD = 1000 fils`).
> Convert at the API boundary only. In-app calculations use `Double` JOD values.
> **Tax rate convention:** rates are stored as decimals — `0.16` means **16%**.
> **Line discount convention:** `discountPct` is a decimal — `0.05` means **5%**.

---

## 1. Discount Types

There are **two levels** of discount, applied in order: line first, then invoice.

### 1.1 Line-level discount

| Type | Backend code | Meaning |
|------|--------------|---------|
| Percentage | `PERCENTAGE` | A percentage of the line's gross total. |
| Value (fixed) | `FIXED_AMOUNT` | A fixed amount off the line (UI `VALUE`). |

> In the current sync layer, **line discounts are sent as `PERCENTAGE`** (`discountPct * 100`).
> The calculator itself treats the line discount as a percentage (`discountPct`).

### 1.2 Invoice-level discount

```
None      → no invoice discount
Percent   → percentage applied to the net AFTER all line discounts
Fixed     → fixed JOD amount applied to the net AFTER all line discounts
```

Both are **clamped** so the discount can never be negative and never exceed the net it
applies to: `clamped = max(0, min(raw, base))`.

---

## 2. Tax Types

Each line carries one of three tax types. The rate (`taxRate`, e.g. `0.16`) is per-line.

| Type | Meaning | Tax formula | Customer pays |
|------|---------|-------------|---------------|
| `TAXABLE` | Price **excludes** tax; tax added on top. | `net × rate` | `net + tax` |
| `INCLUSIVE` | Price **already includes** tax; tax is extracted for reporting. | `net × (rate / (1 + rate))` | `net` (unchanged) |
| `EXEMPT` | No tax. | `0` | `net` |

An app-level setting `TaxType { INCLUDED_TAX, EXCLUDED_TAX }` decides which line tax type
new lines get stamped with (`INCLUSIVE` vs `TAXABLE`).

---

## 3. Per-Line Formulas

For a single line with `unitPrice`, `qty`, `discountPct` (decimal), `taxRate` (decimal),
`lineTaxType`:

```
grossLineTotal = unitPrice × qty
lineDiscount   = grossLineTotal × clamp(discountPct, 0, 1)
lineNet        = grossLineTotal − lineDiscount

TAXABLE   → lineTax = lineNet × rate           ; lineTotal = lineNet + lineTax
INCLUSIVE → lineTax = lineNet × rate/(1+rate)  ; lineTotal = lineNet
EXEMPT    → lineTax = 0                         ; lineTotal = lineNet
```

---

## 4. Invoice Calculation Order (the important part)

The invoice-level discount must be applied **before** tax is finalized, and it must be
distributed **proportionally** across the tax-type groups so each group's tax is correct.

1. **Line totals** — sum `grossLineTotal` (subtotal) and `lineDiscount` over all lines.
2. **Group nets by tax type** — sum `lineNet` into `TAXABLE`, `INCLUSIVE`, `EXEMPT`.
   `sumNetAll = sumNetTaxable + sumNetInclusive + sumNetExempt`.
3. **Invoice discount** — `invDisc = computeDiscount(sumNetAll, input)` (clamped).
4. **Distribute discount proportionally** to each group by its share of `sumNetAll`:
   `discGroup = invDisc × (sumNetGroup / sumNetAll)`, then `finalGroup = sumNetGroup − discGroup`.
5. **Re-calculate tax per line** on the discounted nets, scaling each line by
   `finalGroup / sumNetGroup`, using each line's own rate.

```
totalTax   = taxOnTaxable + taxInInclusive
grandTotal = finalTaxable + taxOnTaxable + finalInclusive + finalExempt
```

> `taxInInclusive` is **informational** (already inside the price) — it is NOT added to the
> grand total. Only `taxOnTaxable` is added on top.

---

## 5. Node.js Implementation

Drop this into your POS (e.g. `invoiceCalc.js`). Pure functions, no dependencies.

```js
// invoiceCalc.js

// ── Money / rate helpers (backend stores integer fils; 1 JOD = 1000 fils) ──
const filsToJod = (fils) => fils / 1000;
const jodToFils = (jod) => Math.round(jod * 1000);
/** Format a JOD/qty value as a 3-decimal string ("1.250") for the voucher API. */
const toAmountString = (v) => {
  const milli = Math.round(v * 1000);
  const sign = milli < 0 ? '-' : '';
  const a = Math.abs(milli);
  return `${sign}${Math.floor(a / 1000)}.${String(a % 1000).padStart(3, '0')}`;
};
/** Format a rate (0.16) as a whole-number percent string ("16"). */
const toPercentString = (rate) => String(Math.round(rate * 100));

const clamp = (v, lo, hi) => Math.max(lo, Math.min(v, hi));

// ── Tax types ──
const LineTaxType = Object.freeze({
  TAXABLE: 'TAXABLE',
  INCLUSIVE: 'INCLUSIVE',
  EXEMPT: 'EXEMPT',
});

// ── Invoice discount input ──
const InvoiceDiscount = {
  none: () => ({ kind: 'NONE' }),
  percent: (pct) => ({ kind: 'PERCENT', pct }), // pct is a whole number, e.g. 10 = 10%
  fixed: (amount) => ({ kind: 'FIXED', amount }), // amount in JOD
};

/**
 * A cart line.
 * @typedef {Object} CartLine
 * @property {number} unitPrice   - JOD per unit
 * @property {number} qty
 * @property {number} discountPct - decimal, 0.05 = 5%
 * @property {number} taxRate     - decimal, 0.16 = 16%
 * @property {string} lineTaxType - LineTaxType.*
 */

// ── Per-line helpers ──
const grossLineTotal = (l) => l.unitPrice * l.qty;
const lineDiscount = (l) => grossLineTotal(l) * clamp(l.discountPct ?? 0, 0, 1);
const lineNet = (l) => grossLineTotal(l) - lineDiscount(l);

function lineTax(l) {
  switch (l.lineTaxType) {
    case LineTaxType.TAXABLE:   return lineNet(l) * l.taxRate;
    case LineTaxType.INCLUSIVE: return lineNet(l) * (l.taxRate / (1 + l.taxRate));
    case LineTaxType.EXEMPT:    return 0;
    default:                    return lineNet(l) * l.taxRate;
  }
}

function lineTotal(l) {
  return l.lineTaxType === LineTaxType.TAXABLE ? lineNet(l) + lineTax(l) : lineNet(l);
}

// ── Invoice-level discount (clamped to [0, base]) ──
function computeDiscount(base, input) {
  let raw = 0;
  if (input.kind === 'PERCENT') raw = base * (input.pct / 100);
  else if (input.kind === 'FIXED') raw = input.amount;
  return Math.max(0, Math.min(raw, base));
}

const sumNetByType = (cart, type) =>
  cart.filter((l) => l.lineTaxType === type).reduce((s, l) => s + lineNet(l), 0);

const proportional = (amount, part, whole) => (whole > 0 ? amount * (part / whole) : 0);

/**
 * Calculate a full invoice summary.
 * @param {CartLine[]} cart
 * @param {{kind:string, pct?:number, amount?:number}} invoiceDiscount
 */
function calculateInvoice(cart, invoiceDiscount = InvoiceDiscount.none()) {
  if (!cart || cart.length === 0) return ZERO_SUMMARY();

  // Step 1: line totals
  const subtotal = cart.reduce((s, l) => s + grossLineTotal(l), 0);
  const totalLineDiscounts = cart.reduce((s, l) => s + lineDiscount(l), 0);

  // Step 2: nets grouped by tax type
  const sumNetTaxable = sumNetByType(cart, LineTaxType.TAXABLE);
  const sumNetInclusive = sumNetByType(cart, LineTaxType.INCLUSIVE);
  const sumNetExempt = sumNetByType(cart, LineTaxType.EXEMPT);
  const sumNetAll = sumNetTaxable + sumNetInclusive + sumNetExempt;

  // Step 3: invoice-level discount
  const invDisc = computeDiscount(sumNetAll, invoiceDiscount);

  // Step 4: distribute discount proportionally by type
  const finalTaxable = sumNetTaxable - proportional(invDisc, sumNetTaxable, sumNetAll);
  const finalInclusive = sumNetInclusive - proportional(invDisc, sumNetInclusive, sumNetAll);
  const finalExempt = sumNetExempt - proportional(invDisc, sumNetExempt, sumNetAll);

  // Step 5: re-calculate tax per line on discounted nets
  const taxOnTaxable =
    sumNetTaxable > 0
      ? cart
          .filter((l) => l.lineTaxType === LineTaxType.TAXABLE)
          .reduce((s, l) => s + lineNet(l) * (finalTaxable / sumNetTaxable) * l.taxRate, 0)
      : 0;

  const taxInInclusive =
    sumNetInclusive > 0
      ? cart
          .filter((l) => l.lineTaxType === LineTaxType.INCLUSIVE)
          .reduce(
            (s, l) =>
              s +
              lineNet(l) * (finalInclusive / sumNetInclusive) * (l.taxRate / (1 + l.taxRate)),
            0,
          )
      : 0;

  const totalTax = taxOnTaxable + taxInInclusive;
  const grandTotal = finalTaxable + taxOnTaxable + finalInclusive + finalExempt;

  return {
    subtotalBeforeDiscounts: subtotal,
    totalLineDiscounts,
    netAfterLineDiscounts: sumNetAll,
    invoiceDiscountAmount: invDisc,
    netTaxable: finalTaxable,
    netInclusive: finalInclusive,
    netExempt: finalExempt,
    taxOnTaxable,    // added on top of the total
    taxInInclusive,  // informational only — already inside the price
    totalTax,
    grandTotal,
  };
}

function ZERO_SUMMARY() {
  return {
    subtotalBeforeDiscounts: 0, totalLineDiscounts: 0, netAfterLineDiscounts: 0,
    invoiceDiscountAmount: 0, netTaxable: 0, netInclusive: 0, netExempt: 0,
    taxOnTaxable: 0, taxInInclusive: 0, totalTax: 0, grandTotal: 0,
  };
}

module.exports = {
  LineTaxType, InvoiceDiscount, calculateInvoice,
  grossLineTotal, lineDiscount, lineNet, lineTax, lineTotal,
  filsToJod, jodToFils, toAmountString, toPercentString,
};
```

---

## 6. Worked Examples

### Example A — TAXABLE, line + invoice percentage discount

```js
const { calculateInvoice, InvoiceDiscount, LineTaxType } = require('./invoiceCalc');

const cart = [
  { unitPrice: 10, qty: 2, discountPct: 0.05, taxRate: 0.16, lineTaxType: LineTaxType.TAXABLE },
];

// gross = 20, lineDiscount = 1.00, lineNet = 19.00
// invoice 10% off net → invDisc = 1.90, finalTaxable = 17.10
// tax = 17.10 × 0.16 = 2.736, grandTotal = 17.10 + 2.736 = 19.836
const s = calculateInvoice(cart, InvoiceDiscount.percent(10));
console.log(s.totalTax.toFixed(3));    // 2.736
console.log(s.grandTotal.toFixed(3));  // 19.836
```

### Example B — INCLUSIVE (tax already in price)

```js
const cart = [
  { unitPrice: 11.6, qty: 1, discountPct: 0, taxRate: 0.16, lineTaxType: LineTaxType.INCLUSIVE },
];
// net = 11.6, extracted tax = 11.6 × 0.16/1.16 = 1.60
// grandTotal = net = 11.6 (tax NOT added on top)
const s = calculateInvoice(cart);
console.log(s.taxInInclusive.toFixed(3)); // 1.600
console.log(s.grandTotal.toFixed(3));     // 11.600
```

### Example C — EXEMPT

```js
const cart = [
  { unitPrice: 5, qty: 3, discountPct: 0, taxRate: 0.16, lineTaxType: LineTaxType.EXEMPT },
];
const s = calculateInvoice(cart, InvoiceDiscount.fixed(2));
// net = 15, fixed discount = 2 → finalExempt = 13, tax = 0, grandTotal = 13
console.log(s.totalTax);              // 0
console.log(s.grandTotal.toFixed(3)); // 13.000
```

---

## 7. Mapping to the Backend (sync)

When creating an invoice request:

| Field | Value |
|-------|-------|
| `unitPrice` | `jodToFils(line.unitPrice)` (integer fils) |
| `lineDiscountType` | `"PERCENTAGE"` |
| `lineDiscountValue` | `line.discountPct * 100` (e.g. `0.05 → 5.0`) |
| `invoiceDiscountType` | `"FIXED_AMOUNT"` if a discount amount exists, else `null` |
| `invoiceDiscountValue` | `jodToFils(discountAmount)` if it exists, else `null` |

The stored invoice keeps: `discountAmount = totalLineDiscounts + invoiceDiscountAmount`,
`taxAmount = totalTax`, `total = grandTotal`.

---

## 8. Edge Cases & Rounding

- **Clamping:** line `discountPct` clamped to `[0, 1]`; invoice discount clamped to `[0, base]`.
- **Empty cart:** returns an all-zero summary.
- **Division guards:** proportional/tax steps skip a group when its net sum is `0`.
- **Rounding:** keep full `Double`/`number` precision through the calculation; round only at
  display (3 decimals for JOD) and at the fils boundary (`Math.round(jod * 1000)`).
