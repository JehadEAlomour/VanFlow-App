# FlowVan Mobile — Design Foundations

Shared visual language for every screen. Tokens come from the live app
(`core/design-system`). Two surfaces: a **deep-navy dark** (default field theme) and a
clean **light** theme. Brand = navy + cyan; accents mirror the web dashboard.

---

## <a id="stitch-style-block"></a>Stitch Style Block (paste this first)

> **Style:** Premium Arabic-RTL mobile app for FMCG cash-van salesmen. Field-grade, calm,
> high-contrast. Deep navy brand (#02192B) with a bright cyan secondary (#02A1C5) and an
> electric-blue primary action (#4B8FF6). Default **dark** theme on navy surfaces
> (#1A2232 cards on a near-black ground), plus a clean light theme (white cards on #F7F9F8).
> **Typography:** Arabic UI in a friendly humanist sans (Tajawal / IBM Plex Sans Arabic);
> Latin in Plus Jakarta Sans; all numbers, money, barcodes and times in IBM Plex Mono,
> always left-to-right. **Layout:** mobile, RTL, big thumb-friendly targets (≥48dp), rounded
> 16px cards, 12px controls, soft shadows, generous spacing. Bottom nav + bottom action bars
> for primary CTAs. **Color discipline:** neutral surfaces; color only for status — green
> #1DC97A (success/paid), amber #F5A41A (pending/sync), red #F04F4F (error/overdue),
> blue primary, cyan brand, violet #9B7FEA for AI. Money is Jordanian Dinar (JOD), 3 decimals.

---

## Color tokens

### Brand & accents (shared)
| Token | Hex | Use |
|------|-----|-----|
| Primary (navy) | `#02192B` | Brand, headers, dark ground |
| Primary-400 | `#4C6F90` | Muted navy text/icons |
| Secondary (cyan) | `#02A1C5` | Brand secondary, highlights, links |
| Accent Blue | `#4B8FF6` | Primary buttons / CTAs |
| Accent Green | `#1DC97A` | Success, paid, online, positive |
| Accent Amber | `#F5A41A` | Pending, sync-queued, warning |
| Accent Red | `#F04F4F` | Error, overdue, offline, destructive |
| Accent Purple | `#9B7FEA` | AI features |
| Accent Teal | `#22D3C2` | Secondary category |
| Star Gold | `#FFB800` | Ratings / featured |

### Dark theme (default, field use)
Ground near-black navy; `FvSurface #1A2232` (cards), `FvSurfaceHigh #1E2A3A` (raised),
`FvSurfaceTop #243044` (active). Text near-white, muted `#A5ADBA`.

### Light theme
Background `#F7F9F8`, Surface `#FFFFFF`, SurfaceHigh `#F4F6FB`, text `#242424`,
muted `#798797` / `#A5ADBA`, hairline borders `#DDE8F5` / `#D9DEE4`.

### Semantic tints (each at ~10–15% for chips)
Success `#13B97D` (+`#B1FFE3`), Info `#55B8FF` (+`#EEF8FF`), Warning `#FFBA55` (+`#FFF8EE`),
Error `#EE534F` (+`#FFEEEE`).

---

## Typography
- **Arabic UI:** Tajawal / IBM Plex Sans Arabic (400/600/700). Default in RTL.
- **Latin UI:** Plus Jakarta Sans.
- **Numerics:** IBM Plex Mono — money, qty, barcodes, receipt #, timestamps, coordinates. LTR always.
- Scale (Material3-ish): display 45–57 · headline 24–32 · title 16–22/700 · body 14–16 · label 11–13/600.
- Money emphasized: large mono, e.g. **`12.500` JOD** with a smaller muted “JOD/د.أ”.

---

## Spacing, shape, motion
- 4dp grid. Screen padding 16dp. Card padding 16dp. List row ~56–64dp.
- Radius: cards **16dp**, controls/inputs **12dp**, chips full, FAB/CTA 14dp.
- Elevation: subtle (1–3dp); bottom sheets and action bars sit above content.
- Motion: quick, springy; sheet slide-ups for create flows; toast/snackbar for sync results.

---

## RTL & field rules
- Mirror everything; sidebar/back chevrons flip. Numbers/money/barcodes stay LTR mono.
- **Minimize typing.** Prefer: barcode scanner, product pickers, qty steppers (− 1 +), amount keypad, customer search, map-pick.
- Always show **sync state**: a small chip — green “مُزامن / Synced”, amber “بانتظار المزامنة / Queued (3)”, red “غير متصل / Offline”. Offline-first: actions save locally and sync later.
- Shift state visible: “وردية نشطة / Shift active” banner; tracking notification implied.

---

## Core components (reuse across screens)
- **Top app bar:** screen title (RTL), back chevron (flips), optional actions (search, filter, sync chip, profile). Brand navy in dark theme.
- **Bottom nav** (4–5 tabs): الرئيسية / العملاء / الطلبات / التقارير / المزيد (Home/Customers/Orders/Reports/More) with icons; active = accent.
- **Bottom action bar:** sticky primary CTA(s) for the screen (e.g. “تأكيد البيع / Confirm sale”), full-width, large.
- **KPI tile:** label + big mono value + tinted icon; 2-up grid on phone.
- **List row / card:** leading status dot or avatar/initial, Arabic title + mono sub (number/phone), trailing value (mono JOD) + chevron.
- **Customer card:** name, mono number, balance (red debt / green credit), type pill, distance/last-visit.
- **Line-item row:** product name + mono SKU, qty stepper, unit, line total (mono). Swipe-to-delete.
- **Amount keypad:** big numeric pad for collections/prices, live formatted JOD.
- **Product picker / scanner:** search field + “مسح الباركود / Scan” button → camera scan → resolved product + unit + price.
- **Status pill / chip:** tone-tinted (paid/pending/overdue/draft/posted/synced).
- **Map:** customer pins + the salesman’s live van marker (truck glyph) + route path.
- **Empty/offline/loading:** friendly illustration + line; skeletons for lists.
- **Bottom sheet:** for pickers, filters, confirm dialogs, payment method.

## Money & numbers
JOD, **3 decimals**, grouped (`1,250.000`). Backend mixes fils (×1000) and major JOD — display only, mono. Qty integer/3dp, barcodes mono, dates `YYYY-MM-DD`, times `HH:mm`.

## Iconography
Rounded line icons (lucide/Material). Home, Users, ShoppingCart, Receipt, BarChart, Truck,
ScanLine/Barcode, MapPin, Wallet/Banknote, Printer, Sparkles (AI), Settings, Cloud/Sync.
