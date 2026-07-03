# Voucher Template — Shared Contract (Base Template)

**Status:** Canonical source of truth for the printed sales / return voucher configuration.
**Owner of the model:** mobile app (`core/model/VoucherTemplate.kt`).
**Consumers:** NestJS backend (serves per-tenant templates), admin frontend (edits them), mobile app (renders them).
**Market default:** Jordan (16% GST, JOD, JoFotara/ISTD, pure black-on-white).

> The mobile app decodes this object with `kotlinx.serialization`. **Every field has a default**, so any field the backend omits falls back to the Jordan base value below. Sending `null` for a field is treated as "use default" (the app runs `explicitNulls=false`). Unknown extra fields are ignored — safe to add fields ahead of an app release.

---

## 1. The base template (canonical JSON)

This is the exact default the app ships with. The backend must return **this object** for a Jordan tenant that has no custom overrides. JSON keys are the wire contract — do not rename.

```json
{
  "currency": "د.أ",
  "amountDecimals": 3,
  "defaultTaxPct": 16.0,
  "monochrome": true,
  "qrCaption": "الرمز الضريبي (JoFotara - ISTD)",
  "showPaymentType": true,
  "paymentTypeInHeader": true,
  "paymentTypeInFooter": true
}
```

---

## 2. Field reference

| Key | Type | Base value | Required on wire | Meaning |
|-----|------|-----------|------------------|---------|
| `currency` | string | `"د.أ"` | no (defaults) | Currency symbol appended to amounts. JOD = `د.أ` (AR) / `JD` (Latin). |
| `amountDecimals` | int (0–4) | `3` | no | Decimal places for money. JOD = 3 (fils). |
| `defaultTaxPct` | number | `16.0` | no | Default GST % for **new taxable items**. Per-line rates still override. Display/seed only — not applied retroactively. |
| `monochrome` | boolean | `true` | no | `true` → renderer draws pure black/white, ignoring all color. Forced `true` for Jordan. |
| `qrCaption` | string | `"الرمز الضريبي (JoFotara - ISTD)"` | no | Caption under the tax QR. **Never** "ZATCA". |
| `showPaymentType` | boolean | `true` | no | Master toggle for the payment-type field (header + footer). |
| `paymentTypeInHeader` | boolean | `true` | no | Show payment type in the header block (outlined box). |
| `paymentTypeInFooter` | boolean | `true` | no | Show payment type in the totals/footer block (plain bold). |

### Validation rules (backend must enforce)
- `amountDecimals`: integer, `0 <= x <= 4`.
- `defaultTaxPct`: number, `0 <= x <= 100`.
- `currency`, `qrCaption`: non-empty strings, max 64 chars.
- Booleans: strict booleans, not `"true"`/`0`.
- If `showPaymentType=false`, the two `paymentType*` flags are ignored by the renderer (still store them).

---

## 3. PaymentType (related enum — informational)

Payment is captured per-voucher (not part of the template) and printed via this bucketing. The backend that creates vouchers sends a `paymentMethod`/`paymentType` string; the app collapses it to two buckets for display:

| Stored value(s) | Bucket | `labelAr` | `labelEn` |
|-----------------|--------|-----------|-----------|
| `CREDIT` | CREDIT | `آجل` | `Credit` |
| `CASH`, `CHEQUE`, `TRANSFER`, `CARD`, null/unknown | CASH | `نقدي` | `Cash` |

Rule: `CREDIT` (case-insensitive) → `آجل`; **everything else** → `نقدي`.

---

## 4. JSON Schema (Draft 2020-12)

Use to validate stored/served templates on the backend and in the admin UI.

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://flowvan/schemas/voucher-template.json",
  "title": "VoucherTemplate",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "currency":            { "type": "string", "minLength": 1, "maxLength": 64, "default": "د.أ" },
    "amountDecimals":      { "type": "integer", "minimum": 0, "maximum": 4, "default": 3 },
    "defaultTaxPct":       { "type": "number", "minimum": 0, "maximum": 100, "default": 16.0 },
    "monochrome":          { "type": "boolean", "default": true },
    "qrCaption":           { "type": "string", "minLength": 1, "maxLength": 64, "default": "الرمز الضريبي (JoFotara - ISTD)" },
    "showPaymentType":     { "type": "boolean", "default": true },
    "paymentTypeInHeader": { "type": "boolean", "default": true },
    "paymentTypeInFooter": { "type": "boolean", "default": true }
  }
}
```

> Note: `additionalProperties: false` is for **backend storage validation**. The mobile app itself is lenient (ignores unknowns) so the schema can grow before the app catches up.

---

## 5. Versioning & evolution rules

1. **Never rename or repurpose a key.** Add a new key with a default instead.
2. **Every new key must have a default** equal to current behavior, so old apps that don't send/read it are unaffected.
3. Keep market-specific naming out of keys (e.g. `qrCaption`, not `joFotaraCaption`) — the same field serves other markets.
4. Color belongs to the template (`monochrome`), never to the payment/domain model.
5. Breaking changes require an app release first; coordinate via the `templateSchemaVersion` field if/when introduced.

See [`backend-nestjs.md`](./backend-nestjs.md) and [`frontend.md`](./frontend.md) for implementation.
