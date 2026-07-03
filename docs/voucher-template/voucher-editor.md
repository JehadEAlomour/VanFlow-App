# Voucher Template — Web Editor Base + App ↔ Website Integration

**Purpose:** the single base contract for (a) the **web voucher editor** that lets a tenant design their printed voucher, and (b) the **connection between the website and the mobile app** that renders it. The mobile app already renders from a `VoucherTemplate`; this document is the canonical, expanded schema the editor edits and the backend serves.

**Market default:** Jordan — JOD (3 dp), 16% GST, JoFotara/ISTD tax QR, pure black-on-white thermal print.

> Companion docs: [`voucher-template.contract.md`](./voucher-template.contract.md) (wire contract), [`backend-nestjs.md`](./backend-nestjs.md) (NestJS store/serve), [`frontend.md`](./frontend.md) (TS types). **This file supersedes them as the editor base** and lists the new fields added since (logo, footer, signature, columns, discount, free items, digit locale).

---

## 1. Architecture — who owns what

```
┌────────────────────┐   PUT /voucher-template    ┌────────────────────┐
│  Web Voucher Editor │ ─────────────────────────▶ │   NestJS Backend    │
│  (design + preview) │ ◀───────────────────────── │  (store overrides)  │
└────────────────────┘   GET /voucher-template     └─────────┬──────────┘
                                                              │ GET /voucher-template
                                                              │ GET /company-info
                                                              ▼
                                                   ┌────────────────────┐
                                                   │   Mobile App (KMP)  │
                                                   │  renders the voucher│
                                                   │  → thermal / PDF    │
                                                   └────────────────────┘
```

- **Editor (website):** edits the `VoucherTemplate` + tenant company profile + logo. Renders a live preview that mirrors the app's renderer.
- **Backend:** stores per-tenant template **overrides** (delta over the base), serves the **resolved** template, owns company profile + logo storage.
- **App:** fetches the resolved template (server-first, DB cache fallback) and renders. The app is the source of truth for *layout* rules; the template only toggles/parameterizes them.

**Division of responsibility:** the editor controls **what shows and the values** (toggles, text, currency, rates, logo). It does **not** control pixel layout — the app owns positions, fonts, RTL shaping, and the monochrome rule. This keeps one renderer and avoids the editor and app drifting.

---

## 2. The base template (canonical JSON)

JSON keys are the wire contract — never rename; add-only. Fields marked **(live)** exist in the app today; **(proposed)** are the editor expansion (app must read them before the editor exposes them — see §9 versioning).

```json
{
  "schemaVersion": 2,

  "currency": "د.أ",
  "amountDecimals": 3,
  "forceLatinDigits": true,

  "defaultTaxPct": 16.0,
  "showTaxColumn": true,
  "taxColumnShowsPercent": true,

  "monochrome": true,

  "logo": {
    "show": true,
    "source": "SERVER_THEN_DEFAULT",
    "heightDp": 54
  },

  "companyHeader": {
    "showName": true,
    "showTaxNumber": true,
    "showBranch": true,
    "showAddress": false,
    "showPhone": false
  },

  "columns": ["QTY", "UNIT", "TAX", "PRICE", "DISCOUNT", "TOTAL"],

  "showPaymentType": true,
  "paymentTypeInHeader": true,
  "paymentTypeInFooter": true,

  "showItemCount": true,
  "showLineDiscount": true,
  "showTotalDiscount": true,
  "showFreeItems": true,

  "qr": {
    "show": false,
    "caption": "الرمز الضريبي (JoFotara - ISTD)",
    "source": "SERVER"
  },

  "signature": {
    "showRecipient": true,
    "showStamp": false
  },

  "footer": {
    "thanksText": "شكراً لتعاملكم معنا",
    "poweredByText": "Powered by 7Software",
    "showPoweredBy": true
  }
}
```

---

## 3. Field reference (grouped by editor section)

### 3.1 Money & numbers
| Key | Type | Default | Status | Effect |
|-----|------|---------|--------|--------|
| `currency` | string | `"د.أ"` | live | Symbol appended to amounts. |
| `amountDecimals` | int 0–4 | `3` | live | Decimal places (JOD = 3, fils). |
| `forceLatinDigits` | bool | `true` | live\* | All numbers render Latin `0-9` even under Arabic. (App forces this today; field makes it explicit.) |

### 3.2 Tax
| Key | Type | Default | Status | Effect |
|-----|------|---------|--------|--------|
| `defaultTaxPct` | number 0–100 | `16.0` | live | Default rate for new taxable items. Per-line rate still wins. |
| `showTaxColumn` | bool | `true` | proposed | Show the per-line tax column. |
| `taxColumnShowsPercent` | bool | `true` | live | Tax column shows the rate (`16%`), not the tax type. Footer label is `إجمالي الضريبة (16%)` when all lines share one rate. |

### 3.3 Theme
| Key | Type | Default | Status | Effect |
|-----|------|---------|--------|--------|
| `monochrome` | bool | `true` | live | Pure black/white render (forced for Jordan/thermal). Hard constraint; the editor should lock this ON for thermal tenants. |

### 3.4 Logo
| Key | Type | Default | Status | Effect |
|-----|------|---------|--------|--------|
| `logo.show` | bool | `true` | live | Show the logo above the company name. |
| `logo.source` | enum `SERVER_THEN_DEFAULT` \| `SERVER_ONLY` \| `DEFAULT_ONLY` | `SERVER_THEN_DEFAULT` | proposed | Where the logo comes from. App bundles a default; server provides `logoUrl`. |
| `logo.heightDp` | int 24–96 | `54` | proposed | Rendered logo height. |

Logo is tinted **solid black** when `monochrome` (1-bit thermal). See §5.

### 3.5 Company header
The values (name, tax number, branch, address, phone) come from the **company profile** (`/company-info`), not the template. These toggles only show/hide each line.
| Key | Type | Default | Status |
|-----|------|---------|--------|
| `companyHeader.showName` | bool | `true` | live |
| `companyHeader.showTaxNumber` | bool | `true` | live |
| `companyHeader.showBranch` | bool | `true` | live |
| `companyHeader.showAddress` | bool | `false` | proposed |
| `companyHeader.showPhone` | bool | `false` | proposed |

### 3.6 Item table
| Key | Type | Default | Status | Effect |
|-----|------|---------|--------|--------|
| `columns` | string[] | `["QTY","UNIT","TAX","PRICE","DISCOUNT","TOTAL"]` | proposed | Ordered list of columns to render. Allowed: `QTY, UNIT, TAX, PRICE, DISCOUNT, TOTAL`. Fewer columns = more width per cell (helps thermal legibility). |
| `showFreeItems` | bool | `true` | live | Render gift/free items (ITEM_QTY_REWARD picks) with a `(مجاني)`/`(Free)` tag; price/total show `Free`. |

### 3.7 Payment type
| Key | Type | Default | Status |
|-----|------|---------|--------|
| `showPaymentType` | bool | `true` | live |
| `paymentTypeInHeader` | bool | `true` | live (outlined box) |
| `paymentTypeInFooter` | bool | `true` | live (plain bold) |

Payment buckets: `CREDIT → آجل`, everything else → `نقدي`.

### 3.8 Totals & discounts (footer block)
| Key | Type | Default | Status | Effect |
|-----|------|---------|--------|--------|
| `showItemCount` | bool | `true` | live | `عدد الأصناف` row. |
| `showLineDiscount` | bool | `true` | live | `خصم الأصناف` row (sum of per-line discounts) when > 0. |
| `showTotalDiscount` | bool | `true` | live | `إجمالي الخصم` row (overall discount) when > 0. |

### 3.9 Tax QR
| Key | Type | Default | Status | Effect |
|-----|------|---------|--------|--------|
| `qr.show` | bool | `false` | live | Render the QR block. Currently false (no payload generator yet); the block is omitted entirely when there's no QR data. |
| `qr.caption` | string | `"الرمز الضريبي (JoFotara - ISTD)"` | live | Caption under the QR. Never "ZATCA". |
| `qr.source` | enum `SERVER` \| `LOCAL` | `SERVER` | proposed | Where the QR payload comes from (JoFotara differs from Saudi TLV). |

### 3.10 Signature
| Key | Type | Default | Status |
|-----|------|---------|--------|
| `signature.showRecipient` | bool | `true` | live (`توقيع المستلم`) |
| `signature.showStamp` | bool | `false` | live (stamp `الختم` currently removed) |

### 3.11 Footer
| Key | Type | Default | Status |
|-----|------|---------|--------|
| `footer.thanksText` | string | `"شكراً لتعاملكم معنا"` | proposed (string is localized in app today) |
| `footer.poweredByText` | string | `"Powered by 7Software"` | live |
| `footer.showPoweredBy` | bool | `true` | proposed |

---

## 4. Company profile (separate from the template)

Edited in the website, served by `GET /company-info`, **cached in the app DB** (`app_settings`) so the header prints offline. The app pulls it server-first when online, else from cache.

```jsonc
// GET /company-info  (existing CompanyInfoDto)
{
  "companyNameAr": "شركتي للتجارة",
  "companyNameEn": "My Trading Co.",
  "sellerTin": "1234567890",        // → الرقم الضريبي
  "sellerAddress": "عمّان - الأردن",
  "sellerPhone": "+962 7 ...",
  "logoUrl": "https://cdn/.../logo.png",
  "taxCalcMethod": "EXCLUSIVE",     // INCLUSIVE | EXCLUSIVE — authoritative tax mode (mirrors ERP)
  "timezone": "Asia/Amman",
  "locale": "ar"
}
```

**Editor split:** template = *layout/toggles*; company profile = *identity values*. The editor app may present both in one screen but they POST to two endpoints.

---

## 5. Logo handling

- **Default:** the app bundles a placeholder vector logo (replaceable). Renders tinted **black** under `monochrome`.
- **Server logo:** `company-info.logoUrl` (PNG/JPG). To render it the app needs a **multiplatform image loader (Coil 3 KMP)** — NOT yet in the project. Until added, `logo.source` effectively falls back to the bundled default.
- **Monochrome rule:** any colored logo is **thresholded to 1-bit** black-on-white before drawing (so it rasterizes cleanly on the thermal head). The editor preview must show the same 1-bit version when `monochrome` is on.
- **Upload:** website uploads the logo → backend stores → returns `logoUrl`. Recommend square or wide transparent PNG ≥ 256 px tall; the app scales to `logo.heightDp`.

---

## 6. App ↔ Website connection

### 6.1 Endpoints
| Method | Path | Who | Body / returns |
|--------|------|-----|----------------|
| `GET` | `/voucher-template` | app + editor | Resolved template (base + tenant overrides), **complete object**. |
| `PUT` | `/voucher-template` | editor (admin) | Partial or full template → merged, validated, stored; returns resolved. `{}` resets to base. |
| `GET` | `/company-info` | app + editor | Company profile (above). |
| `PUT` | `/company-info` | editor (admin) | Update company profile. |
| `POST` | `/company-info/logo` | editor (admin) | Multipart upload → returns `{ logoUrl }`. |

Auth: same bearer token as the rest of the API; tenant resolved from the token.

### 6.2 App fetch & caching (already implemented for company-info; same pattern for template)
- **Server-first when online**, persist to local DB, render from it.
- **Offline / failure → DB cache.** Never blocks printing.
- Template fetch should happen on session start and/or when opening the print screen.

### 6.3 When edits reach the app
1. Admin saves in the editor → `PUT /voucher-template`.
2. App picks up the change on its next online template fetch (session start / print open). No push needed for v1; a future `templateUpdatedAt` can drive cache invalidation.

### 6.4 Storage (backend)
Persist **only the override delta** per tenant (a JSON column), merged onto the base server-side — so changing the base later reaches every uncustomized tenant automatically. See [`backend-nestjs.md`](./backend-nestjs.md).

---

## 7. Editor UX → preview mapping

The editor should render a live preview that mirrors these effects:

- `monochrome=true` → entire voucher pure black/white; emphasis via weight/size/boxes, never color.
- `currency` + `amountDecimals` → amounts as `123.450 د.أ`; `forceLatinDigits` keeps digits `0-9`.
- `logo.*` → black logo above the company name at `heightDp`.
- `companyHeader.*` → show/hide name, tax number, branch, address, phone (values from company profile).
- `columns` → the item grid columns, in order.
- payment toggles → outlined box in header, plain bold in footer (`نقدي`/`آجل`).
- discount toggles → `خصم الأصناف` then `إجمالي الخصم` rows when > 0.
- `showFreeItems` → gift lines with `(مجاني)`/`(Free)`.
- `qr.show` + `qr.caption` → QR block + caption (only when a payload exists).
- `signature.*` → recipient and/or stamp signature lines.
- `footer.*` → thanks line + `Powered by 7Software`.

Provide a **"Reset to default"** that loads the base template, and lock `monochrome` ON for thermal tenants.

---

## 8. JSON Schema (Draft 2020-12)

```json
{
  "$schema": "https://json-schema.org/draft/2020-12/schema",
  "$id": "https://flowvan/schemas/voucher-template.v2.json",
  "title": "VoucherTemplate",
  "type": "object",
  "additionalProperties": false,
  "properties": {
    "schemaVersion":          { "type": "integer", "const": 2, "default": 2 },
    "currency":               { "type": "string", "minLength": 1, "maxLength": 64, "default": "د.أ" },
    "amountDecimals":         { "type": "integer", "minimum": 0, "maximum": 4, "default": 3 },
    "forceLatinDigits":       { "type": "boolean", "default": true },
    "defaultTaxPct":          { "type": "number", "minimum": 0, "maximum": 100, "default": 16.0 },
    "showTaxColumn":          { "type": "boolean", "default": true },
    "taxColumnShowsPercent":  { "type": "boolean", "default": true },
    "monochrome":             { "type": "boolean", "default": true },
    "logo": {
      "type": "object", "additionalProperties": false,
      "properties": {
        "show":     { "type": "boolean", "default": true },
        "source":   { "enum": ["SERVER_THEN_DEFAULT", "SERVER_ONLY", "DEFAULT_ONLY"], "default": "SERVER_THEN_DEFAULT" },
        "heightDp": { "type": "integer", "minimum": 24, "maximum": 96, "default": 54 }
      }
    },
    "companyHeader": {
      "type": "object", "additionalProperties": false,
      "properties": {
        "showName":      { "type": "boolean", "default": true },
        "showTaxNumber": { "type": "boolean", "default": true },
        "showBranch":    { "type": "boolean", "default": true },
        "showAddress":   { "type": "boolean", "default": false },
        "showPhone":     { "type": "boolean", "default": false }
      }
    },
    "columns": {
      "type": "array",
      "items": { "enum": ["QTY", "UNIT", "TAX", "PRICE", "DISCOUNT", "TOTAL"] },
      "uniqueItems": true,
      "default": ["QTY", "UNIT", "TAX", "PRICE", "DISCOUNT", "TOTAL"]
    },
    "showPaymentType":       { "type": "boolean", "default": true },
    "paymentTypeInHeader":   { "type": "boolean", "default": true },
    "paymentTypeInFooter":   { "type": "boolean", "default": true },
    "showItemCount":         { "type": "boolean", "default": true },
    "showLineDiscount":      { "type": "boolean", "default": true },
    "showTotalDiscount":     { "type": "boolean", "default": true },
    "showFreeItems":         { "type": "boolean", "default": true },
    "qr": {
      "type": "object", "additionalProperties": false,
      "properties": {
        "show":    { "type": "boolean", "default": false },
        "caption": { "type": "string", "minLength": 1, "maxLength": 64, "default": "الرمز الضريبي (JoFotara - ISTD)" },
        "source":  { "enum": ["SERVER", "LOCAL"], "default": "SERVER" }
      }
    },
    "signature": {
      "type": "object", "additionalProperties": false,
      "properties": {
        "showRecipient": { "type": "boolean", "default": true },
        "showStamp":     { "type": "boolean", "default": false }
      }
    },
    "footer": {
      "type": "object", "additionalProperties": false,
      "properties": {
        "thanksText":    { "type": "string", "maxLength": 120, "default": "شكراً لتعاملكم معنا" },
        "poweredByText": { "type": "string", "maxLength": 60, "default": "Powered by 7Software" },
        "showPoweredBy": { "type": "boolean", "default": true }
      }
    }
  }
}
```

> `additionalProperties: false` is for **backend write-validation**. The app is lenient (ignores unknown keys), so the editor can add a field before the app ships support for it.

---

## 9. Versioning & rollout rules

1. **Never rename/repurpose a key.** Add-only; every new key carries a default equal to current behavior.
2. **`schemaVersion`** tracks editor-visible shape. The app reads what it knows and ignores the rest, so the editor can lead an app release — but a new control should only be *exposed* once the app reads it (else admins toggle something that does nothing).
3. **`(proposed)` fields** in §3 are not yet honored by the app renderer. Sequence: (a) app reads the field, (b) backend validates/stores it, (c) editor exposes the control.
4. Keep market names out of keys (`qr.caption`, not `joFotaraCaption`) so one codebase serves multiple markets.
5. `monochrome` stays `true` and `qr.caption` never says "ZATCA" for Jordan tenants.

---

## 10. Work needed to fully connect (checklist)

**Backend (NestJS)**
- [ ] `GET/PUT /voucher-template` with delta storage + merge (see backend doc).
- [ ] `PUT /company-info`, `POST /company-info/logo` (upload → `logoUrl`).
- [ ] Validate against the §8 schema on write.

**Website (editor)**
- [ ] Form bound to the §2 base template + company profile.
- [ ] Live preview mirroring §7.
- [ ] Logo upload; "Reset to default"; lock `monochrome` for thermal tenants.

**App (KMP)**
- [ ] `VoucherTemplateRepository` (server-first, DB cache) + fetch on print/session — mirrors the existing `CompanyInfoRepository`.
- [ ] Expand the in-app `VoucherTemplate` to the v2 fields and honor the `(proposed)` toggles in the renderer.
- [ ] Add **Coil 3 KMP** to load `company-info.logoUrl` + 1-bit threshold it.

**Current app state (already done):** monochrome render, payment type (header+footer), free items `(Free)`, line+total discount, tax-as-percent, Latin digits, company header from `/company-info` (server-first + DB cache), bundled default logo.
