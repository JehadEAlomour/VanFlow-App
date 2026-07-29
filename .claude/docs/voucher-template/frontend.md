# Voucher Template — Frontend Guide

For the **admin web frontend** that edits a tenant's [`VoucherTemplate`](voucher-template.contract.md), and as a reference for how the **mobile app** consumes it. The base template is the starting point for every tenant.

---

## 1. TypeScript type (shared)

```ts
// voucher-template.ts
export interface VoucherTemplate {
  /** Currency symbol appended to amounts. JOD = "د.أ" (AR) / "JD" (Latin). */
  currency: string;
  /** Decimal places for money. JOD = 3 (fils). 0–4. */
  amountDecimals: number;
  /** Default GST % for new taxable items (per-line rates override). 0–100. */
  defaultTaxPct: number;
  /** true → receipt renders pure black/white. Forced true for Jordan. */
  monochrome: boolean;
  /** Caption under the tax QR. JoFotara/ISTD — never "ZATCA". */
  qrCaption: string;
  /** Master toggle for the payment-type field. */
  showPaymentType: boolean;
  /** Show payment type in the header block (outlined box). */
  paymentTypeInHeader: boolean;
  /** Show payment type in the totals/footer block (plain bold). */
  paymentTypeInFooter: boolean;
}

/** Jordan base template — use as the form's initial values / "Reset to default". */
export const BASE_VOUCHER_TEMPLATE: VoucherTemplate = {
  currency: 'د.أ',
  amountDecimals: 3,
  defaultTaxPct: 16.0,
  monochrome: true,
  qrCaption: 'الرمز الضريبي (JoFotara - ISTD)',
  showPaymentType: true,
  paymentTypeInHeader: true,
  paymentTypeInFooter: true,
};
```

---

## 2. Fetch / save

```ts
// GET resolved template (base + tenant overrides, merged server-side)
const template: VoucherTemplate = await api.get('/api/voucher-template').then(r => r.data);

// PUT full or partial; server merges, validates, returns resolved
const saved: VoucherTemplate = await api.put('/api/voucher-template', form).then(r => r.data);

// Reset to base
await api.put('/api/voucher-template', {}); // empty body => inherit base
```

The `GET` always returns a complete object — no client-side merge needed. Render the editor directly from it.

---

## 3. Editor form (field UI)

| Field | Control | Notes |
|-------|---------|-------|
| `currency` | text input | RTL; placeholder `د.أ`. Max 64. |
| `amountDecimals` | number / stepper | integer 0–4. |
| `defaultTaxPct` | number input | 0–100, suffix `%`. |
| `monochrome` | toggle | **Locked ON for Jordan tenants** — disable with a tooltip "Required for this market". |
| `qrCaption` | text input | RTL; warn if it contains "ZATCA". Max 64. |
| `showPaymentType` | toggle | When OFF, disable the two below. |
| `paymentTypeInHeader` | toggle | Disabled when `showPaymentType` is OFF. |
| `paymentTypeInFooter` | toggle | Disabled when `showPaymentType` is OFF. |

Validation must match the contract §2 (mirror the JSON Schema). Show a **"Reset to default"** button that loads `BASE_VOUCHER_TEMPLATE`.

---

## 4. Live preview (how the values map to the printed voucher)

The mobile receipt renders these effects — replicate in the admin preview if you build one:

- **`monochrome=true`** → entire receipt is pure black on white; emphasis only via weight/size/boxes/rules, no color.
- **`currency` + `amountDecimals`** → totals & line amounts render as `123.450 د.أ` (3 dp).
- **Payment type:**
  - `showPaymentType && paymentTypeInHeader` → outlined box (1.5px black border, no fill, bold) in the header, after the customer block, value = `نقدي` / `آجل`.
  - `showPaymentType && paymentTypeInFooter` → plain bold row in the totals block, after the **عدد الأصناف** (item count) row.
  - `showPaymentType=false` → field hidden in both spots, no blank gap.
- **`qrCaption`** → caption text under the tax-QR slot.
- **Tax footer label** (not a template field — derived from the voucher's lines): `إجمالي الضريبة (١٦٪)` when all taxed lines share one rate, else `إجمالي الضريبة`.

---

## 5. Gotchas

- Send numbers as numbers (`amountDecimals: 3`), not strings.
- Don't add unknown keys — the backend rejects them on `PUT` (`forbidNonWhitelisted`).
- Arabic fields are RTL; render inputs with `dir="rtl"`.
- The mobile app ignores fields it doesn't know yet, so the backend/admin can introduce a new field before the app ships support — but it won't render until the app updates.
