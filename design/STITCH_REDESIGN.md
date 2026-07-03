# FlowVan — Stitch Redesign Brief

> Paste-ready prompts for **Google Stitch** to regenerate the FlowVan UI with a new
> Cobalt + Aqua-Mint palette. Covers **30 screens** across 8 modules, each with
> **mobile + tablet** variants and **RTL / Arabic-first** notes.
>
> **How to use:** In Stitch, paste **§1 (Global Style)** once as your project style
> context, then paste any screen's prompt block from §3. Generate mobile first, then
> tap "tablet" / paste the tablet variant. Repeat per screen.

---

## 1. Global Style (paste this first into Stitch)

```
DESIGN SYSTEM — FlowVan (field van-sales / cashflow app)

Brand personality: modern fintech, crisp, data-dense but breathable, trustworthy.
Direction: RTL — Arabic-first. All layouts mirror right-to-left. Numbers stay LTR.
Typography: clean geometric sans (Cairo / IBM Plex Sans Arabic for Arabic, Inter for
  Latin/numbers). Generous line height. Bold weights for KPIs and money values.
Corners: rounded 16px cards, 12px inputs/buttons, 999px pills/chips.
Elevation: soft shadows on light theme, subtle inner borders on dark theme.
Spacing: 16px screen padding, 12px between cards, 8px inside cards.
Motion cue: active/positive elements glow with the Aqua accent.

COLOR PALETTE
Primary  (Cobalt Blue)  #0047AB   — brand, headers, primary buttons, links
Accent   (Aqua Mint)    #00FFC8   — CTAs, active states, positive money, highlights, focus rings
Ink      (Charcoal)     #333333   — primary text
Surface  (White)        #FFFFFF   — cards / sheets

Derived tokens:
  Primary 700 (pressed)  #003079
  Primary 50  (tint bg)  #E8EEF8
  Accent 700  (on-light) #00B894   — use this for accent text/buttons on white (mint #00FFC8 is too bright for fills on white; reserve pure mint for dark theme + glows)
  Text secondary         #6B7280
  Text disabled          #9CA3AF
  Border / divider       #E5E7EB
  Surface alt / row      #F5F7FA
  Background (light)      #F7F9FC

Status:
  Success  #12B886   Warning  #F59E0B   Error  #EF4444   Info  #0EA5E9

DARK THEME (app is dark-first in production — generate both):
  Background deepest #08111E   Background #0C1A2B   Surface #122438   Surface high #1A3150
  Border (dark)      #24405F   Text high #EAF1FB   Text mid #93A4C0   Text low #5A6B85
  Primary on dark    #2D6BE0 (brighter cobalt)   Accent on dark #00FFC8 (pure mint pops here)

USAGE RULES
- Money / positive deltas / "paid" / progress fills → Aqua accent (#00B894 light, #00FFC8 dark).
- Overdue / returns / errors → Error red. Warnings (low stock) → Amber.
- Primary buttons → Cobalt fill, white text. Secondary buttons → Aqua outline or Aqua fill on charcoal.
- Tier badges A/B/C → A = Cobalt, B = Aqua, C = neutral gray.
- Customer overdue amounts → red; balances in good standing → ink/green.
- Sale flow = Aqua green, Return flow = Error red, Request/Order = Cobalt — keep this semantic across the app.
```

---

## 2. Responsive Rules (mobile vs tablet)

| | Mobile (360–430dp) | Tablet (≥720dp / landscape) |
|---|---|---|
| **Nav** | Bottom flow; back arrows in top bar | Persistent **right-side rail** (RTL) or master-detail |
| **Lists** | 1 column | 2-column grid, or **list + detail** split (40/60) |
| **Stat/KPI grid** | 2×2 | 1×4 row, or 4 across |
| **Quick actions** | 2–3 columns | 4–6 columns |
| **Forms / dialogs** | Full-width / bottom sheet | Centered modal max-width 560dp, or side panel |
| **Voucher screen** | Catalog ↔ Cart toggle | Catalog **+** Cart side-by-side (no toggle) |
| **Reports** | Stacked rows | Wider rows, more columns, sticky filter bar |

RTL note for every screen: top-bar back arrow on the **right**, primary actions on the
**left**, text right-aligned, list chevrons point left, progress bars fill right→left.

---

## 3. Screen Prompts

> Each block: **Purpose**, **Mobile**, **Tablet**, **Color mapping**, and a copy-paste
> **Stitch prompt**. Prefix every prompt with the §1 style if Stitch loses context.

---

### 3.1 — Login  ·  `feature/auth/LoginScreen.kt`

**Purpose:** Phone + password authentication, dark branded entry screen.
**Mobile:** Centered column — logo badge, app name, phone field, password field with
eye toggle, full-width primary button, inline error chip.
**Tablet:** Split — left 50% brand panel (gradient + logo + tagline), right 50% centered
form card max-width 420dp.
**Color mapping:** Dark cobalt→navy gradient background; logo badge Aqua-on-cobalt;
button Cobalt fill (Aqua glow on focus); error chip red.

```
Design a LOGIN screen for an Arabic RTL field-sales app, dark theme.
Background: deep gradient from cobalt #0047AB into near-black navy #08111E.
Center: a rounded square logo badge with an aqua #00FFC8 gradient and app monogram,
app name "FlowVan" below in white bold, small Arabic subtitle in muted blue-gray.
Form card (translucent navy #122438, 16px corners): phone-number input with leading
icon, password input with show/hide eye toggle, both with aqua focus ring.
Primary button full width, cobalt #0047AB fill, white text, subtle aqua glow.
Below: a red error chip state. All text right-aligned, RTL layout, numbers LTR.
Generate mobile portrait AND a tablet landscape version with a left brand panel + right form.
```

---

### 3.2 — Home Dashboard  ·  `feature/home/HomeScreen.kt`

**Purpose:** Daily KPIs, quick actions, route preview.
**Mobile:** Top bar (settings + logout) → hero greeting card (date, shift status, sync
chip) → 2×2 stat grid (Sales, Collections, Returns, Visits) → visit-progress bar card →
2-col quick-action grid (Route, Customers, Van Stock, End of Day, Reports, AI) → route
preview card. Pull-to-refresh.
**Tablet:** Hero full-width; stats as 1×4 row; quick actions 6-across; route preview as
a right-side panel beside stats.
**Color mapping:** Hero = cobalt gradient, white text, Aqua sync dot. Stat cards white
with colored icon chips (Sales=Aqua, Collections=Cobalt, Returns=Red, Visits=Amber).
Progress bar fills Aqua. Quick-action tiles white, cobalt icons, Aqua on press.

```
Design a HOME DASHBOARD for an Arabic RTL van-sales app, light theme (also output dark).
Top bar with settings and logout icon buttons. A hero greeting card with a cobalt
#0047AB gradient: Arabic greeting + name, today's date, a shift-status pill, and an
aqua #00FFC8 "synced" indicator dot. Below, a 2x2 grid of KPI stat cards (white, soft
shadow, 16px corners) for Sales, Collections, Returns, Visits — each with a colored
icon chip (Sales aqua, Collections cobalt, Returns red, Visits amber) and a bold money
value. A "today's visits" progress card with an aqua progress bar (fills right-to-left).
A quick-actions grid (2 cols) of rounded tiles: Route, Customers, Van Stock, End of Day,
Reports, AI Assistant — cobalt icons. A route-preview card listing the next customers.
RTL, right-aligned text, numbers LTR. Output mobile portrait AND tablet landscape
(stats in one row of 4, quick actions 6 across, route preview as a side panel).
```

---

### 3.3 — Today's Route  ·  `feature/home/TodayRouteScreen.kt`

**Purpose:** Ordered customer stops with todo / mark-done tracking.
**Mobile:** Top bar with back + "done/total" progress badge → vertical list of stop
cards (customer name, order #, area, optional admin note, todo status, map button,
mark-done button with spinner).
**Tablet:** Two columns of stop cards, or list-left + map-right.
**Color mapping:** Progress badge Aqua. Completed stop = Aqua check + dimmed; pending =
cobalt accent. Map button cobalt outline; mark-done button Aqua fill.

```
Design a TODAY'S ROUTE screen, Arabic RTL, light + dark. Top bar: back arrow (right),
title, and an aqua #00FFC8 progress badge "done/total". A scrollable list of stop cards
(white, 16px corners): customer name (bold), order number, area with a pin icon, an
optional highlighted admin-note strip, a todo row with a check state, a cobalt #0047AB
outlined "navigate" button and an aqua-filled "mark done" button (with a loading spinner
state). Completed stops show an aqua check and reduced opacity. RTL, numbers LTR.
Mobile portrait AND tablet (two-column stop grid, or list + map split).
```

---

### 3.4 — Route (all customers)  ·  `feature/home/RouteScreen.kt`

**Purpose:** Full route list with search and tier badges.
**Mobile:** Sticky top bar w/ progress → search field → customer rows (tier badge,
overdue amount, area, navigate + view buttons).
**Tablet:** 2-column rows or list/detail split; persistent search.
**Color mapping:** Tier A=Cobalt, B=Aqua, C=gray gradient badges. Overdue=red.

```
Design a ROUTE customer-list screen, Arabic RTL, light + dark. Sticky top bar with a
progress indicator. A search field with leading magnifier. List of customer rows (white
cards): circular tier badge (A=cobalt #0047AB, B=aqua #00FFC8, C=gray), customer name,
area, an overdue amount in red when present, plus a navigate icon button (cobalt) and a
view button. RTL, numbers LTR. Mobile portrait AND tablet two-column / master-detail.
```

---

### 3.5 — End of Day  ·  `feature/home/EndOfDayScreen.kt`

**Purpose:** Shift-close summary + settlement.
**Mobile:** Scroll of section cards: Sales summary (sales, returns, net, collections),
Cash settlement breakdown, Cheque/Transfer details → confirm/logout button.
**Tablet:** Two-column card layout; summary on left, settlement on right.
**Color mapping:** Net sales = Aqua; returns = red; collections = cobalt. Confirm button
cobalt; values bold.

```
Design an END OF DAY summary screen, Arabic RTL, light + dark. Stacked section cards
(white, 16px): "Sales Summary" with KPI rows (Sales, Returns in red, Net Sales in aqua
#00FFC8/#00B894, Collections in cobalt), "Cash Settlement" breakdown, "Cheque/Transfer"
details. Each row label right, bold value left, numbers LTR. A final full-width cobalt
#0047AB "Close Shift" button. Mobile portrait AND tablet two-column.
```

---

### 3.6 — Settings  ·  `feature/home/SettingsScreen.kt`

**Purpose:** Preferences (theme, language, tax mode, AI key).
**Mobile:** Grouped setting cards with toggles + segmented controls: Theme
(System/Light/Dark), Language (AR/EN), Tax mode, AI API key field. Snackbar feedback.
**Tablet:** Two-column settings groups.
**Color mapping:** Active segment = Cobalt fill; toggles Aqua when on; section headers
cobalt.

```
Design a SETTINGS screen, Arabic RTL, light + dark. Grouped cards (white, 16px) with
section headers in cobalt #0047AB. Controls: a Theme segmented control (System/Light/
Dark, selected segment cobalt fill white text), a Language toggle (Arabic/English),
a Tax-mode selector, and an "AI Assistant API key" masked input with save. Switches use
aqua #00FFC8 when enabled. A bottom snackbar confirmation. RTL. Mobile AND tablet
two-column.
```

---

### 3.7 — Home Placeholder  ·  `feature/home/HomePlaceholderScreen.kt`

**Purpose:** Phase placeholder.
**Mobile/Tablet:** Centered greeting card, progress indicator, logout.
**Color mapping:** Cobalt card, Aqua progress.

```
Design a minimal PLACEHOLDER home screen, Arabic RTL, light + dark: a centered welcome
card (cobalt #0047AB accent), an aqua #00FFC8 circular progress indicator, a short
"coming soon" message, and a logout button. Mobile AND tablet (just centered, wider).
```

---

### 3.8 — Customer List  ·  `feature/customer/CustomerListScreen.kt`

**Purpose:** Browse/search all customers with filters.
**Mobile:** Sticky top bar → search field → horizontal filter chips (Tier, Segment) →
customer cards (name, ID, tier badge, overdue, area, map + view buttons).
**Tablet:** 2-column card grid or list/detail split; filters in a sticky sidebar.
**Color mapping:** Tier badges A/B/C (cobalt/aqua/gray); overdue red; active chip cobalt.

```
Design a CUSTOMER LIST screen, Arabic RTL, light + dark. Sticky top bar with back and
title. A search field, then a horizontal row of filter chips (Tier, Segment) — selected
chip cobalt #0047AB fill. Customer cards (white, 16px): name + ID, a tier badge
(A cobalt / B aqua #00FFC8 / C gray), area with pin, an overdue amount in red when
present, and map + view icon buttons. RTL, numbers LTR. Mobile portrait AND tablet
two-column grid with a sticky filter sidebar.
```

---

### 3.9 — Customer Dashboard  ·  `feature/customer/CustomerDashboardScreen.kt`

**Purpose:** 360° customer view + action launchpad.
**Mobile:** Hero card (name, balance, tier) → account summary card (balance, credit
limit, overdue) → report-tiles grid (Vouchers, Payments, Statement, Transaction,
Receivables) → action buttons (Sale, Return, Request, Collection, AI) → transaction
history list. Visit dialog on back.
**Tablet:** Hero + summary across top; reports grid left, history right; action bar
docked.
**Color mapping:** Hero cobalt gradient; balance Aqua if positive / red if overdue;
Sale=Aqua, Return=red, Request=cobalt, Collection=cobalt-outline, AI=aqua.

```
Design a CUSTOMER DASHBOARD, Arabic RTL, light + dark. A hero card with cobalt #0047AB
gradient: customer name, tier badge, and current balance (large; aqua #00FFC8 if in good
standing, red if overdue). An account-summary card with rows: current balance, credit
limit, overdue (red). A grid of report tiles (Vouchers, Payments, Account Statement,
Transaction, Receivables) with colored icons. A horizontal action bar of buttons: Sale
(aqua), Return (red), Request (cobalt), Collection (cobalt outline), AI (aqua). A
transaction-history list with date, description, and color-coded amount. RTL, numbers
LTR. Mobile portrait AND tablet (hero across top, reports left + history right, docked
action bar).
```

---

### 3.10 — Account Statement  ·  `feature/customer/AccountStatementScreen.kt`

**Purpose:** Dated ledger of debits/credits.
**Mobile:** Date-range bar → summary pills (balance, collected) → statement rows (date,
description, amount) clickable to invoice/receipt.
**Tablet:** Wider table-like rows with running-balance column.
**Color mapping:** Debit red, credit Aqua/green; summary pills cobalt + aqua.

```
Design an ACCOUNT STATEMENT screen, Arabic RTL, light + dark. A date-range selector bar
at top. Two summary pills: current balance (cobalt #0047AB) and collected (aqua
#00FFC8). A ledger list of rows: date, description, and amount color-coded (debits red,
credits green/aqua), each row tappable. RTL, numbers LTR. Mobile portrait AND tablet
with a wider table layout including a running-balance column.
```

---

### 3.11 — Voucher (Sale / Return / Request)  ·  `feature/voucher/VoucherScreen.kt`

**Purpose:** Build a sale, return, or order. The app's most complex screen.
**Mobile:** Top bar with type toggle + save → toggle between **Catalog** (category chips,
product grid w/ image, price, qty; product detail modal) and **Cart** (line items w/ qty
steppers, unit dropdown, discount input, payment-method selector, totals w/ tax, save).
**Tablet:** **No toggle** — Catalog (left 60%) and Cart (right 40%) side-by-side.
**Color mapping:** Save button gradient is **type-semantic**: Sale=Aqua, Return=red,
Request/Order=cobalt. Qty steppers cobalt; selected category chip cobalt; totals bold;
discount field aqua focus.

```
Design a VOUCHER / CART screen for sales, Arabic RTL, light + dark. Top bar with a
segmented type toggle (Sale / Return / Request) and a save button whose color matches
the type: Sale = aqua #00FFC8, Return = red #EF4444, Request = cobalt #0047AB.
Two areas: (1) PRODUCT CATALOG — a horizontal row of category chips (selected cobalt),
a grid of product cards with image, name, price, and an add/quantity stepper; tapping a
product opens a detail modal. (2) CART — line-item rows with quantity steppers, a unit
dropdown, a per-line discount input (amount or %), a payment-method selector
(Cash / Cheque / Transfer / Credit), and a totals summary (subtotal, tax, total in bold)
with the type-colored save button. RTL, numbers LTR.
Mobile: toggle between Catalog and Cart. Tablet: show Catalog (60%) and Cart (40%)
side-by-side with no toggle.
```

---

### 3.12 — Van Stock  ·  `feature/voucher/VanStockScreen.kt`

**Purpose:** On-van inventory levels.
**Mobile:** Top bar → search → category chips → product cards (image, name, SKU, qty,
stock-status badge, unit price); detail bottom sheet.
**Tablet:** 2–3 column product grid; detail as side panel.
**Color mapping:** In-stock=Aqua/green, Low=amber, Out=red badges; selected chip cobalt.

```
Design a VAN STOCK inventory screen, Arabic RTL, light + dark. Top bar with back + title,
a search field, a horizontal row of category chips (selected cobalt #0047AB). Product
cards (white, 16px): product image, name, SKU, current quantity, a stock-status badge
(In Stock = aqua/green, Low = amber, Out = red), and unit price. Tapping opens a detail
bottom sheet. RTL, numbers LTR. Mobile portrait AND tablet 3-column grid with a side
detail panel.
```

---

### 3.13 — Collection (payment)  ·  `feature/voucher/CollectionScreen.kt`

**Purpose:** Record a customer payment.
**Mobile:** Payment-method tabs (Cash / Cheque / Transfer) → amount field → conditional
date picker (cheque) → bank dropdown (cheque/transfer) → reference field → tax toggle →
submit → breakdown summary.
**Tablet:** Two-column: form left, live breakdown summary right.
**Color mapping:** Active method tab cobalt; amount field aqua focus; submit cobalt;
breakdown total Aqua.

```
Design a COLLECTION (record payment) screen, Arabic RTL, light + dark. A segmented tab
row of payment methods (Cash / Cheque / Transfer) with the active tab cobalt #0047AB.
An amount input (large, aqua #00FFC8 focus ring), a conditional date picker (for cheque),
a bank dropdown (for cheque/transfer), a reference-number field, and a tax
inclusive/exclusive toggle. A payment-breakdown summary card (total in aqua) and a
cobalt submit button. RTL, numbers LTR. Mobile portrait AND tablet two-column (form left,
breakdown right).
```

---

### 3.14 — Reports Hub  ·  `feature/reports/ReportsHubScreen.kt`

**Purpose:** Entry grid to all reports.
**Mobile:** Header with back → 2-col grid of 6 report cards (icon, title, subtitle):
All Sales, All Payments, Visit, Cash Flow, Items Sales, Receivables.
**Tablet:** 3-col grid.
**Color mapping:** Each card a distinct colored icon chip (Sales=cobalt, Payments=aqua,
Visit=teal, Cash Flow=amber, Items=purple, Receivables=red) on white.

```
Design a REPORTS HUB screen, Arabic RTL, light + dark. Header with back arrow + title.
A grid of report cards (white, 16px, soft shadow): All Sales (cobalt #0047AB icon),
All Payments (aqua #00FFC8 icon), Visit Report (teal), Cash Flow (amber), Items Sales
(purple), Receivables (red) — each card with icon chip, title, and subtitle. RTL.
Mobile 2-column AND tablet 3-column.
```

---

### 3.15 — All Sales Report  ·  `feature/reports/AllSalesReportScreen.kt`

**Purpose:** Company-wide sales.
**Mobile:** Date-range bar → type filter chips (All/Sales/Returns/Requests) → summary
pills (Total Sales, Total Returns) → entry rows (customer, date, amount, type badge).
**Tablet:** Sticky filter + table rows with more columns.
**Color mapping:** Sales=Aqua, Returns=red type badges; pills cobalt/red; active chip cobalt.

```
Design an ALL SALES REPORT screen, Arabic RTL, light + dark. A sticky date-range bar and
type filter chips (All / Sales / Returns / Requests, selected cobalt #0047AB). Two summary
pills: Total Sales (aqua #00FFC8) and Total Returns (red). A list of sales entries:
customer name, date, amount, and a type badge (Sale aqua, Return red, Request cobalt),
each tappable. RTL, numbers LTR. Mobile portrait AND tablet wide-table with sticky filters.
```

---

### 3.16 — All Payments Report  ·  `feature/reports/AllPaymentsReportScreen.kt`

**Purpose:** Company-wide payments.
**Mobile:** Date-range bar → method filter chips → summary pill (Total Collected) → rows
(customer, method, amount, date) → tap to receipt.
**Tablet:** Wide table.
**Color mapping:** Total Aqua; method badges cobalt/teal/amber; active chip cobalt.

```
Design an ALL PAYMENTS REPORT screen, Arabic RTL, light + dark. Date-range bar, payment-
method filter chips (selected cobalt #0047AB), a "Total Collected" summary pill in aqua
#00FFC8, and a list of payment rows (customer, method badge, amount, date) tappable to a
receipt. RTL, numbers LTR. Mobile portrait AND tablet wide-table.
```

---

### 3.17 — Voucher Report (per customer)  ·  `feature/reports/VoucherReportScreen.kt`

**Purpose:** A customer's invoice history.
**Mobile:** Date range → type filters (Sale/Return/Request) + payment-kind filters
(Cash/Cheque/Transfer/Credit) → voucher rows (date, amount, status, type badge) → detail.
**Tablet:** Filters in sidebar; table rows.
**Color mapping:** Type badges semantic; status pills (paid=Aqua, due=amber, overdue=red).

```
Design a VOUCHER REPORT screen (per customer), Arabic RTL, light + dark. Date-range bar,
type filter chips (Sale/Return/Request) and payment-kind chips (Cash/Cheque/Transfer/
Credit), selected chips cobalt #0047AB. Voucher rows: date, amount, a type badge (Sale
aqua #00FFC8 / Return red / Request cobalt), and a status pill (Paid aqua, Due amber,
Overdue red). Tappable to detail. RTL, numbers LTR. Mobile AND tablet (filter sidebar +
table).
```

---

### 3.18 — Transaction Report (per customer)  ·  `feature/reports/TransactionReportScreen.kt`

**Purpose:** Customer transaction stream.
**Mobile:** Date-range bar → type filter chips → transaction rows (description, amount,
date).
**Tablet:** Wide rows + running balance.
**Color mapping:** Debit red / credit Aqua; active chip cobalt.

```
Design a TRANSACTION REPORT screen (per customer), Arabic RTL, light + dark. Date-range
bar, type filter chips (selected cobalt #0047AB), and a list of transactions: description,
date, and amount color-coded (debit red, credit aqua #00FFC8). RTL, numbers LTR. Mobile
AND tablet wide-table with running balance.
```

---

### 3.19 — Payment Report (per customer)  ·  `feature/reports/PaymentReportScreen.kt`

**Purpose:** Customer payment history.
**Mobile:** Date-range bar → method filter chips → payment rows (method, amount, date) →
tap to receipt.
**Tablet:** Wide table.
**Color mapping:** Method badges; total Aqua; active chip cobalt.

```
Design a PAYMENT REPORT screen (per customer), Arabic RTL, light + dark. Date-range bar,
payment-method filter chips (Cash/Cheque/Transfer, selected cobalt #0047AB), and payment
rows: method badge, amount (aqua #00FFC8), date, tappable to a receipt. RTL, numbers LTR.
Mobile AND tablet wide-table.
```

---

### 3.20 — Receivables Report  ·  `feature/reports/ReceivablesReportScreen.kt`

**Purpose:** Outstanding balances by customer.
**Mobile:** Summary pills (Total Balance, Total Overdue) + customer-count → customer rows
(name, area, balance, overdue in red).
**Tablet:** Wide table sortable by overdue.
**Color mapping:** Overdue red; total-balance pill cobalt; overdue pill red.

```
Design a RECEIVABLES REPORT screen, Arabic RTL, light + dark. Two summary pills: Total
Balance (cobalt #0047AB) and Total Overdue (red), plus a customer-count indicator. A list
of customer rows: name, area, current balance, and overdue amount highlighted in red.
RTL, numbers LTR. Mobile AND tablet wide-table sorted by overdue.
```

---

### 3.21 — Visit Report  ·  `feature/reports/VisitReportScreen.kt`

**Purpose:** Daily visit tracking.
**Mobile:** Date-range bar → summary card (visited/planned) → visited-customer list with
dates.
**Tablet:** Summary + map split.
**Color mapping:** Visited=Aqua check; planned-not-visited=amber; progress Aqua.

```
Design a VISIT REPORT screen, Arabic RTL, light + dark. Date-range bar, a summary card
showing visited vs planned with an aqua #00FFC8 progress ring, and a list of visited
customers with visit dates and an aqua check. Not-yet-visited entries show amber. RTL.
Mobile AND tablet (summary + map split).
```

---

### 3.22 — Cash Flow Report  ·  `feature/reports/CashFlowReportScreen.kt`

**Purpose:** Cash & receivables movement.
**Mobile:** Date-range bar → entries (type invoice/payment/return, amount, method, date)
→ tap to detail.
**Tablet:** Wide table + optional trend chart.
**Color mapping:** Inflow Aqua, outflow/return red, invoice cobalt.

```
Design a CASH FLOW REPORT screen, Arabic RTL, light + dark. Date-range bar and a list of
movement entries: a type tag (Invoice cobalt #0047AB, Payment aqua #00FFC8, Return red),
amount, method, and date, tappable to detail. RTL, numbers LTR. Mobile AND tablet with a
wide table plus a small inflow/outflow trend chart.
```

---

### 3.23 — Items Sales Report  ·  `feature/reports/ItemsSalesReportScreen.kt`

**Purpose:** Product-level analytics.
**Mobile:** Date-range bar → summary pills (units sold, revenue) → item rows (name, SKU,
qty sold, total, % of total bar).
**Tablet:** Wide table + bar chart of top items.
**Color mapping:** Revenue pill Aqua; % bars cobalt→aqua gradient.

```
Design an ITEMS SALES REPORT screen, Arabic RTL, light + dark. Date-range bar, two summary
pills (Units Sold cobalt #0047AB, Revenue aqua #00FFC8), and item rows: product name, SKU,
quantity sold, total amount, and a small share-of-total bar (cobalt-to-aqua gradient).
RTL, numbers LTR. Mobile AND tablet wide-table with a top-items bar chart.
```

---

### 3.24 — Voucher Print  ·  `feature/print/VoucherPrintScreen.kt`

**Purpose:** Thermal-receipt preview + print controls.
**Mobile:** Receipt-preview canvas (58/80mm look) → printer-connection dialog → print
button w/ device selector → share-PDF. Horizontal scroll for wide preview.
**Tablet:** Preview left, controls panel right.
**Color mapping:** Preview is monochrome receipt (it prints B/W). Chrome around it uses
cobalt buttons + Aqua "connected" status. Note Arabic prints as bitmap.

```
Design a VOUCHER PRINT preview screen, Arabic RTL, light + dark. Center: a realistic
thermal-receipt preview (narrow 80mm paper, monochrome, store header, line items, totals,
footer) on a neutral backdrop, horizontally scrollable. App chrome around it: a printer-
connection status chip (aqua #00FFC8 when connected, gray when not), a "select device"
dialog, a cobalt #0047AB "Print" button, and a "Share PDF" secondary button. RTL chrome,
the receipt itself stays monochrome. Mobile portrait AND tablet (preview left, controls
panel right).
```

---

### 3.25 — Voucher Detail  ·  `feature/print/VoucherDetailScreen.kt`

**Purpose:** Read-only invoice view.
**Mobile:** Invoice header (date, type, customer) → line-item list (name, unit, qty, unit
price, total) → summary (subtotal, tax, total) → payment-method info.
**Tablet:** Header + summary across top; items table below.
**Color mapping:** Type badge semantic; total bold; tax muted; reprint button cobalt.

```
Design a VOUCHER DETAIL (read-only invoice) screen, Arabic RTL, light + dark. An invoice
header card with date, a type badge (Sale aqua #00FFC8 / Return red / Request cobalt
#0047AB) and customer. A line-items list: product name, unit, quantity, unit price, line
total. A summary section: subtotal, tax (muted), total (bold). A payment-method info row.
A cobalt "Reprint" button. RTL, numbers LTR. Mobile portrait AND tablet (header + summary
top, items table below).
```

---

### 3.26 — Receipt Detail  ·  `feature/print/ReceiptDetailScreen.kt`

**Purpose:** Read-only payment receipt.
**Mobile:** Payment header (date, method, customer) → details (amount, reference) →
linked-invoice (if any) → status indicator.
**Tablet:** Two-column card.
**Color mapping:** Amount Aqua; status pill (cleared=Aqua, pending=amber); method badge.

```
Design a RECEIPT DETAIL (read-only payment) screen, Arabic RTL, light + dark. A payment
header card: date, method badge, customer. Detail rows: amount (large, aqua #00FFC8),
reference number, and a linked-invoice chip when applicable. A status pill (Cleared aqua,
Pending amber). A cobalt #0047AB "Reprint" button. RTL, numbers LTR. Mobile portrait AND
tablet two-column.
```

---

### 3.27 — Map Navigation  ·  `feature/map/MapNavigationScreen.kt`

**Purpose:** GPS routing to a customer.
**Mobile:** Full-bleed map → top customer-info card → bottom sheet with distance,
duration, and a "Navigate" button (opens native maps). GPS loading state.
**Tablet:** Map + persistent customer/route panel on the side.
**Color mapping:** Route line cobalt; destination pin Aqua; navigate button cobalt;
ETA value Aqua.

```
Design a MAP NAVIGATION screen, Arabic RTL, light + dark. A full-bleed map with a cobalt
#0047AB route line and an aqua #00FFC8 destination pin. A top customer-info card
(name, address). A bottom sheet with distance, ETA (aqua value), and a cobalt "Navigate"
button that launches the native maps app, plus a GPS-loading state. RTL chrome.
Mobile portrait AND tablet (map + side route/customer panel).
```

---

### 3.28 — AI Assistant  ·  `feature/ai/AiAssistantScreen.kt`

**Purpose:** Chat assistant (Claude-powered), optionally customer-scoped.
**Mobile:** Header with AI icon + connection status → chat list (user bubbles right,
assistant left in RTL) → quick-suggestion chips → input field with send button. Streaming
indicator. API-key dialog.
**Tablet:** Wider chat column centered; suggestion chips in a side rail.
**Color mapping:** Assistant bubbles surface-gray, user bubbles cobalt; AI icon + send
button Aqua; streaming dots Aqua; connected status Aqua.

```
Design an AI ASSISTANT chat screen, Arabic RTL, light + dark. Header with an aqua #00FFC8
sparkle AI icon and a connection-status dot (aqua when connected). A chat list: user
messages in cobalt #0047AB bubbles, assistant messages in light surface bubbles
(dark-surface in dark theme), aligned for RTL. A row of quick-suggestion chips above the
input. A text input with an aqua send button and a streaming "typing" indicator. An
API-key configuration dialog state. RTL, numbers LTR. Mobile portrait AND tablet (wider
centered chat with suggestions in a side rail).
```

---

## 4. Component Cheat-Sheet (reuse across screens)

| Component | Fill | Text/Icon | Notes |
|---|---|---|---|
| Primary button | Cobalt `#0047AB` | White | Aqua glow on focus |
| Accent / CTA button | Aqua `#00B894` (light) / `#00FFC8` (dark) | Charcoal/Navy | "Save sale", "Collect" |
| Secondary button | Transparent, Cobalt border | Cobalt | |
| Destructive button | Red `#EF4444` | White | Return / delete |
| Chip (selected) | Cobalt `#0047AB` | White | Filters, categories |
| Chip (idle) | `#F5F7FA` | `#6B7280` | |
| Tier badge A / B / C | Cobalt / Aqua / Gray | White | |
| Money positive | — | `#00B894` | Net sales, collected |
| Money overdue/return | — | `#EF4444` | |
| Status: paid/cleared | Aqua tint | `#00B894` | |
| Status: due/pending | Amber tint `#F59E0B` | | |
| Status: overdue/error | Red tint `#EF4444` | | |
| Progress bar / ring | Aqua | track `#E5E7EB` | Fills RTL |
| Card | White (`#122438` dark) | Ink `#333333` | 16px, soft shadow |
| Input focus ring | Aqua `#00FFC8` | | |

---

## 5. After Stitch — wiring it back to code

Once you have Stitch designs you like, update these token files (do **not** hand-edit
each screen):

- `core/design-system/.../ColorTokens.kt` — base color constants
- `core/design-system/.../LightColorScheme.kt` — Material 3 light mapping
- `core/design-system/.../DarkColorScheme.kt` — Material 3 dark mapping
- `core/design-system/.../FvColors.kt` — FlowVan light palette
- `core/design-system/.../theme/ExtendedColors.kt` — success/info/warning/etc.

Suggested Material 3 mapping for the new palette:

```
primary            = #0047AB   onPrimary           = #FFFFFF
secondary          = #00B894   onSecondary         = #06231C   (dark: #00FFC8 / #062019)
tertiary           = #00FFC8   onTertiary          = #062019
background (light)  = #F7F9FC   surface (light)     = #FFFFFF   onSurface = #333333
background (dark)   = #08111E   surface (dark)      = #122438   onSurface = #EAF1FB
error              = #EF4444   success(ext) #12B886  warning(ext) #F59E0B  info(ext) #0EA5E9
```

> Tell me when your Stitch designs are ready and I can apply this mapping to the token
> files for you.
