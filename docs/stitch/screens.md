# FlowVan Mobile — Screen Prompts for Stitch

One section per screen. Each has a **Stitch Prompt** (copy-paste, **Mobile** mode) and a spec.
All screens are Arabic-RTL, dark-default, with the shared bars/components from
[`design-system.md`](design-system.md). Money = JOD 3-decimals monospace.

**Index**
[1. Splash](#1-splash) · [2. Login](#2-login) · [3. Home](#3-home) · [4. Start Shift](#4-start-shift) ·
[5. Route / Journey](#5-route--journey) · [6. End of Day](#6-end-of-day) · [7. Settings](#7-settings) ·
[8. Customer List](#8-customer-list) · [9. Customer 360](#9-customer-360) · [10. Account Statement](#10-account-statement) ·
[11. Sale Voucher](#11-sale-voucher) · [12. Return Voucher](#12-return-voucher) · [13. Request / Order](#13-request--order) ·
[14. Collection](#14-collection-payment) · [15. Van Stock](#15-van-stock) · [16. Barcode Scan](#16-barcode-scan) ·
[17. Cart / Checkout](#17-cart--checkout) · [18. Reports Hub](#18-reports-hub) · [19. Voucher Detail](#19-voucher-detail) ·
[20. Receipt / Print](#20-receipt--print-preview) · [21. Printer Connect](#21-printer-connect) ·
[22. AI Assistant](#22-ai-assistant) · [23. Map Navigation](#23-map-navigation)

---

## 1. Splash
**Stitch Prompt:**
> Design an Arabic-RTL mobile **splash screen** for “فان فلو / FlowVan”, a cash-van sales app.
> Deep navy gradient background, a centered “V” / van monogram in cyan-blue, the wordmark
> “فان فلو”, a thin progress indicator, and a tiny version label at the bottom. Premium, calm.

---

## 2. Login
**Stitch Prompt:**
> Design an Arabic-RTL mobile **login** screen, dark navy. Top: brand monogram + “فان فلو” + tagline
> “مبيعات المندوبين”. A card with a “رقم المستخدم / User number” field and a “كلمة المرور / Password”
> field (show/hide eye), a big full-width “دخول / Sign in” primary button, and a small “وضع تجريبي / Demo”
> hint. Inline error “فشل تسجيل الدخول. تحقق من بياناتك.” A server-URL gear link in the corner for backend config.
> Bottom: language toggle. Large touch targets, one-hand friendly.

**Spec:** `POST /auth/login {userNumber,password}` → JWT + rep id. Demo mode logs in by phone offline. Backend URL editable in settings.

---

## 3. Home
Salesman daily cockpit.

**Stitch Prompt:**
> Design an Arabic-RTL mobile **home/dashboard** for a cash-van salesman, dark navy. Top app bar:
> greeting “صباح الخير، {name}”, a sync chip (green “مُزامن”), and a profile/logout icon.
> A prominent **shift banner**: if no active shift, a big “ابدأ الوردية / Start shift” CTA card; if active,
> show “وردية نشطة” with elapsed time + a live tracking dot.
> A **2×2 KPI grid**: مبيعات اليوم (mono JOD), تحصيلات اليوم (JOD), زيارات (count), إنجاز المسار (%, ring).
> A “مسار اليوم / Today’s route” card listing the next 5 customers (name, mono number, distance, status dot)
> with a “عرض المسار / View route” link. Quick-action tiles: بيع / مرتجع / تحصيل / مسح باركود (Sale/Return/Collect/Scan)
> as big icon buttons. Bottom nav (Home/Customers/Orders/Reports/More). Offline-first; values from local cache.

**Spec:** KPIs from local + `/reps/me/kpis`; route top-5 from journey plan; start-shift triggers GPS tracking. Quick actions deep-link to flows.

---

## 4. Start Shift
**Stitch Prompt:**
> Design an Arabic-RTL mobile **start-shift** confirmation sheet (bottom sheet over Home). Title
> “بدء الوردية”, a short note that location tracking will run during the shift (with a small map/pin),
> current van + opening cash field (mono JOD keypad), and a big “ابدأ / Start” button. Mention a persistent
> tracking notification. Reassuring, simple.

**Spec:** Creates a Shift, starts foreground GPS tracking. Opening float optional.

---

## 5. Route / Journey
**Stitch Prompt:**
> Design an Arabic-RTL mobile **route / journey plan** screen. Top: date + a small **map** showing ordered
> customer pins (1→2→3…) connected by a path and the salesman’s live van marker. Below, an **ordered stop list**:
> each row = stop number badge, customer Arabic name + mono number, distance + ETA, status pill
> (قادم / تمت الزيارة / تخطّي — upcoming/visited/skipped), and a chevron. Tapping a stop opens actions
> (Navigate, Visit/Sell, Skip with reason). A top progress strip: زيارات {visited}/{total}, نسبة الإنجاز %.
> Sticky bottom: “الوجهة التالية / Next stop” button that opens map navigation.

**Spec:** Journey plan / `/routes`. Visit/skip posts back; deviation tracked. Reorder allowed.

---

## 6. End of Day
**Stitch Prompt:**
> Design an Arabic-RTL mobile **end-of-day / close-shift** summary screen, dark navy. A celebratory header
> “ملخص اليوم”. KPI cards: إجمالي المبيعات (mono JOD), التحصيلات (cash vs cheque split bar), عدد الفواتير,
> المرتجعات, الزيارات, المسافة المقطوعة (km), عدد العملاء. A cash-reconciliation card: opening float + cash collected
> − deposits = expected cash drawer (mono), with a “count” input. A list of unsynced items with a “مزامنة الكل / Sync all”
> button (amber if queued). Big bottom CTA “إنهاء الوردية / End shift”. Print/share day report icon.

**Spec:** Aggregates the shift; forces sync of pending; ends shift + stops tracking; optional printed Z-report.

---

## 7. Settings
**Stitch Prompt:**
> Design an Arabic-RTL mobile **settings** screen, grouped list sections: الحساب (name, user number mono, sign out),
> اللغة (AR/EN), المظهر (dark/light), خادم النظام (backend URL field), الطابعة (printer connect status),
> التتبع (location tracking on/off + permission status), المزامنة (last sync time, “مزامنة الآن”), حول التطبيق (version).
> Clean Material list rows with trailing controls (switches, chevrons, value labels). RTL.

**Spec:** multiplatform-settings; backend URL drives all APIs; tracking + printer + sync controls.

---

## 8. Customer List
**Stitch Prompt:**
> Design an Arabic-RTL mobile **customers** list. Top app bar “العملاء” + search + filter icon.
> Sticky search field “ابحث بالاسم أو الرقم”. Filter chips: الكل / مدين (debtors) / قريب مني (nearby) / حسب المنطقة.
> A scrollable list of **customer cards**: avatar/initial, Arabic name, mono number, phone (mono),
> a balance badge (red “عليه 1,250.000” debt / green credit), type pill (نقدي/آجل), and distance “1.2 كم”.
> A FAB “+ عميل” to add a customer. Pull-to-refresh; offline cache. Big tap targets.

**Spec:** `/customers` (offset/limit/q) + local. Sort by distance/debt. Tap → Customer 360.

---

## 9. Customer 360
**Stitch Prompt:**
> Design an Arabic-RTL mobile **customer detail (360)** screen. Header card: customer name, mono number,
> phone (call button), address + a small map pin, type pill, and big balance figures (debt red / credit green, mono JOD),
> credit limit + payment terms. An **AI strip** (violet): churn risk %, recommended products with confidence.
> A horizontal tab row: ملخص / مبيعات / مرتجعات / طلبات / تحصيلات / زيارات. Body shows the selected list
> (transactions: date, ref mono, amount mono, status pill). A **sticky bottom action bar** with four big buttons:
> بيع / مرتجع / طلب / تحصيل (Sale/Return/Order/Collect). Visit-log timeline with “had sale” markers.

**Spec:** local + `/customers/{id}` (insights, visits). Actions deep-link to selling flows pre-filled with this customer.

---

## 10. Account Statement
**Stitch Prompt:**
> Design an Arabic-RTL mobile **account statement** for a customer. Header: customer name + date-range chips
> (هذا الشهر / 90 يوم / الكل). A running-balance **ledger list**: each row date (mono), type (بيع/تحصيل/مرتجع),
> ref (mono), debit/credit (mono, credit green), and a bold running balance (mono). A pinned footer with
> “الرصيد الختامي / Closing balance” big mono JOD. Buttons: “مشاركة PDF / Share” and “طباعة / Print”.
> Clean, financial, monospace numerics, easy to read in sunlight.

**Spec:** Built from vouchers + payments. Export PDF / print via thermal printer.

---

## 11. Sale Voucher
The core selling flow.

**Stitch Prompt:**
> Design an Arabic-RTL mobile **new sale (invoice)** screen for a van salesman. Top app bar “بيع جديد” + customer
> chip (selected customer name + mono number, tap to change). A prominent **“مسح الباركود / Scan” button** plus a
> product search field. A **cart list** of line items: product Arabic name + mono SKU, a qty stepper (− 1 +), unit
> selector (حبة/كرتون), unit price (mono), line total (mono), swipe-to-delete. A discount field (value/percent toggle).
> A **totals card** pinned above the bottom bar: subtotal, discount, tax 16%, **الإجمالي / Total** big mono JOD.
> Sticky bottom action bar: “إضافة دفعة / Add payment” + primary “تأكيد البيع / Confirm sale”. On confirm, a success
> sheet with print/share. Stock guard: warn if qty exceeds van stock. Offline-first (saves + queues).

**Spec:** Builds a SALE voucher (lines from item_units/barcode, price quote), optional payment lines, tax 16%, van-stock check; posts to `/vouchers`, queues offline.

---

## 12. Return Voucher
**Stitch Prompt:**
> Design an Arabic-RTL mobile **return** screen, same shape as the sale screen but tinted with amber accents.
> Title “مرتجع”, customer chip, and an option to **return against an original invoice** (pick a recent invoice →
> prefilled returnable lines with max-qty caps) or free return via scan/search. Cart with qty steppers (capped),
> reason chips (تالف/منتهي/خطأ — damaged/expired/wrong), totals card, sticky “تأكيد المرتجع / Confirm return”.

**Spec:** RETURN voucher, optional `referenceVoucherNumber`, returnable caps, reasons. Goods go back to van stock.

---

## 13. Request / Order
**Stitch Prompt:**
> Design an Arabic-RTL mobile **order/request** screen (advance order, not immediate sale), blue accent.
> Title “طلب / Order”, customer chip, product picker + qty steppers, a requested **delivery date** field, notes,
> totals (no immediate payment). Sticky “تأكيد الطلب / Confirm order”. A note that stock is reserved until fulfilled.

**Spec:** ORDER voucher (`isFulfilled=false`), reserves stock, fulfilled later.

---

## 14. Collection (Payment)
**Stitch Prompt:**
> Design an Arabic-RTL mobile **collection / payment** screen. Title “تحصيل”, customer chip with current balance
> shown big (mono red debt). A large **amount keypad** with the entered amount formatted live as JOD (mono).
> Method selector segmented control: نقدًا / شيك / تحويل (Cash/Cheque/Transfer). If **cheque**: extra fields —
> bank, cheque number (mono), due date, and a “صورة الشيك / Photo” capture button (OCR-assisted). A “الرصيد بعد الدفع”
> preview. Sticky primary “تأكيد التحصيل / Confirm” → success sheet with print receipt. Offline-first.

**Spec:** Creates a payment/collection; cheque captures bank+number+due+image (OCR). Prints receipt. Posts to `/collections`.

---

## 15. Van Stock
**Stitch Prompt:**
> Design an Arabic-RTL mobile **van stock** screen. Title “مخزون المركبة”. A summary header (total SKUs, total units,
> est. value mono JOD). A searchable list of on-van items: product name + mono SKU, on-hand qty (mono, amber if low /
> red if zero), unit. Tabs or segmented control: المخزون / تحميل / إرجاع (Stock / Load / Return). Load/Return build a
> line list with qty steppers and a confirm bottom bar. A “مسح الباركود” quick add.

**Spec:** `/reps/{id}/van-stock` (+ load/return). Reflects sales/returns. Low-stock highlighting.

---

## 16. Barcode Scan
**Stitch Prompt:**
> Design an Arabic-RTL mobile **barcode scanner** overlay. Full-screen camera with a centered scan frame
> (animated line), a dimmed surround, a torch toggle, and a manual “إدخال الرقم / Enter code” fallback.
> On a hit, a bottom sheet slides up with the resolved product: image, Arabic name, mono barcode, unit + price (mono JOD),
> a qty stepper, and an “إضافة / Add” button. Fast, one-handed.

**Spec:** Resolves via `/item-units/barcode/{barcode}` → product + unit + price; adds to the active cart.

---

## 17. Cart / Checkout
**Stitch Prompt:**
> Design an Arabic-RTL mobile **cart / checkout review** sheet shared by sale/return/order. A clean line-item list
> (name, mono SKU, qty × unit price, line total mono, edit/remove), a discount row, a totals block (subtotal, discount,
> tax 16%, total big mono JOD), a payment summary (paid vs remaining if partial), and customer + date header.
> Sticky bottom: “تعديل / Edit” + primary “تأكيد / Confirm”. Then a **success screen**: green check, voucher number (mono),
> total, and buttons طباعة / مشاركة / تم (Print/Share/Done).

**Spec:** Final review before posting; success returns voucher number; offers print/share.

---

## 18. Reports Hub
**Stitch Prompt:**
> Design an Arabic-RTL mobile **reports hub**. Title “التقارير” + a date-range selector (اليوم/الأسبوع/الشهر/مخصص).
> A grid of report tiles: المبيعات (Sales), التحصيلات (Payments), التدفق النقدي (Cash Flow), الحركات (Transactions),
> المرتجعات (Returns), أفضل الأصناف (Best items), الزيارات (Visits), رحلاتي (My trips). Each tile = icon + title + a headline
> mono number for the range. Tapping opens a report screen: a small chart (bar/line) + a summary strip + a list/table
> (date, ref mono, amount mono, status). Export/share per report. Keep it scannable, monospace numerics.

**Spec:** 8–11 reports off local data + `/reports/*`. “رحلاتي” mirrors the dashboard trips report for this salesman.

---

## 19. Voucher Detail
**Stitch Prompt:**
> Design an Arabic-RTL mobile **voucher detail** screen. Header: kind pill (بيع/مرتجع/طلب), voucher number (mono),
> date/time (mono), customer name + number, posted/synced status chips. A line-item list (name, mono SKU, qty, unit price,
> line total — all mono). A totals card (subtotal, discount, tax, total mono JOD). Payment lines if any. Sticky bottom:
> “طباعة / Print”, “مشاركة PDF / Share”, and (if draft) “ترحيل / Post”.

**Spec:** Read view of a saved voucher; print/share/post.

---

## 20. Receipt / Print Preview
**Stitch Prompt:**
> Design an Arabic-RTL mobile **thermal receipt preview** (58/80mm look) on a phone. A white receipt mockup centered on a
> dark backdrop: company logo + name + tax number, voucher number + date (mono), customer, a monospace line-item table
> (item, qty, price, total), totals + tax, a thank-you line, and a barcode/QR at the bottom. Controls: printer status chip,
> paper-width toggle (58/80), copies stepper, and big buttons “طباعة / Print” + “مشاركة PDF”. Monospace, print-accurate.

**Spec:** Renders the receipt for the XPrinter thermal SDK; PDF share fallback.

---

## 21. Printer Connect
**Stitch Prompt:**
> Design an Arabic-RTL mobile **printer connect** bottom sheet. Title “توصيل الطابعة”. Segmented connection type:
> Bluetooth / USB / شبكة (Network). For Bluetooth: a scan button + a list of discovered devices (name, mono MAC, signal)
> with connect buttons and a connected-state check. A “طباعة اختبارية / Test print” button. Clear status (connected green /
> searching amber / error red). Permissions hint if Bluetooth is off.

**Spec:** XPrinter SDK pairing; persists last printer; test print.

---

## 22. AI Assistant
**Stitch Prompt:**
> Design an Arabic-RTL mobile **AI assistant** chat screen, violet AI accent. Top bar “المساعد الذكي” with a sparkle icon.
> A chat thread: assistant bubbles (left in RTL) with a subtle violet gradient edge + confidence/“why” chips, user bubbles
> (right). Suggestion chips above the input (“أفضل العملاء اليوم”, “ما الأصناف الناقصة؟”, “لخّص ورديتي”). A bottom input bar
> with mic + send. Streaming typing indicator. Keep AI elements glowing violet; everything else calm.

**Spec:** Claude-backed streaming; suggestion chips run canned queries; references local data (customers, stock, KPIs).

---

## 23. Map Navigation
**Stitch Prompt:**
> Design an Arabic-RTL mobile **turn-by-turn navigation** screen to a customer. Full-screen map with the route path,
> the salesman’s van marker, and the destination customer pin. A top **maneuver banner** (next turn arrow + “بعد 200م انعطف يمينًا”
> + street). A bottom **trip card**: customer name + mono number, distance + ETA (mono), and big buttons “بدء / Start”,
> “وصلت / Arrived” (→ opens the sell/visit flow), and “تخطّي / Skip”. Re-center FAB. High contrast for driving.

**Spec:** Navigates to the selected route stop; “Arrived” opens the visit/sell flow; integrates with route compliance.

---

### Notes for the redesign
- Keep flows **short**: scan → qty → confirm. Default the customer from context (route/customer screen).
- Surface **sync & offline** state on every write screen.
- Reuse the sale screen skeleton for return/order (recolor + caps/fields) so the system feels consistent.
- All money/qty/barcodes/times monospace LTR; all labels Arabic-RTL.
