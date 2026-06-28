# FlowVan (CashFlow) Mobile — Stitch Redesign Brief

Ready-to-use design brief for **[Google Stitch](https://stitch.withgoogle.com)** to redesign
the **FlowVan** salesman app — a Kotlin Multiplatform (Compose) cash-van field-sales app for
FMCG salesmen in Jordan. Offline-first; talks to the same NestJS backend as the dashboard.

## What's here
| File | Use it for |
|------|-----------|
| [`design-system.md`](design-system.md) | Shared mobile visual language — paste **once** as your global style/theme before generating screens. |
| [`screens.md`](screens.md) | One **copy-paste Stitch prompt per screen** + full layout/component/data/state specs. |

## How to drive Stitch
1. Open Stitch → **Mobile** mode (phone canvas, ~390px).
2. Paste the **Style Block** from [`design-system.md`](design-system.md#stitch-style-block) first to lock the theme.
3. Generate one screen at a time from [`screens.md`](screens.md); each prompt restates the must-haves so screens stay consistent.
4. Refine with short deltas (“bigger primary CTA”, “sticky bottom action bar”, “number pad keypad for amount”).

## Non-negotiables (tell Stitch every time)
- **Arabic-first, RTL.** Primary language Arabic; mirror layout right-to-left. Numbers, money (JOD, 3 decimals), barcodes, IDs, times stay **LTR + monospace**.
- **One-hand field use.** Big tap targets (≥48dp), thumb-reachable primary actions, **bottom action bars**, minimal typing, lots of pickers/steppers/scanner.
- **Works in sunlight & offline.** High contrast, clear status colors, obvious sync state (a small cloud/queue indicator). Default theme is a **deep-navy dark** surface; also support a clean light theme.
- **Money is JOD with 3 decimals** (`12.500`), monospace, never mirrored.
- **Status colors fixed:** green = success/online/paid, amber = pending/idle/sync-queued, red = error/offline/overdue, blue = primary/info, cyan = brand secondary, violet = AI.

## Screen index (~22)
Auth: Splash · Login
Home: Home/Dashboard · Start-Shift · Route (Journey) · End-of-Day · Settings
Customers: Customer List · Customer 360 · Account Statement
Selling: Sale Voucher · Return Voucher · Request/Order · Collection (Payment) · Van Stock · Barcode Scan · Cart/Checkout
Reports: Reports Hub · (Sales / Payments / Cash Flow / Transactions)
Output: Voucher Detail · Receipt / Print Preview · Printer Connect
Assist: AI Assistant · Map Navigation

See [`screens.md`](screens.md) for each.
