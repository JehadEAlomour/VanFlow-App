# FlowVan — design brief for Stitch (v2)

Supersedes the first brief. The direction changed on five points, and all five
are things Stitch will undo unless they are repeated in every prompt:

| | |
|---|---|
| **Font** | **Almarai (المراعي)** everywhere. Not Cairo, not Tajawal, not Noto. |
| **Theme** | **Light only. No dark mode, ever.** |
| **Navigation** | **No bottom sheets. No modals.** Every action is its own screen. |
| **Dashboard** | **A 3-column grid of every function, first thing on the screen.** |
| **Look** | Flat and functional. **No gradients, no glassmorphism, no AI-styling.** |

Paste **§1** first, then one screen block at a time from §3.

---

## 1. Global style

> Design a mobile app called **FlowVan** — an Arabic field sales app for van
> salesmen ("مندوب") selling FMCG stock to small shops in Jordan and the Gulf.
> The user is standing in a shop doorway, one-handed, in **bright morning
> sunlight**, on a poor connection, and is trying to start a task in under two
> seconds.
>
> ### Language and direction
> **Arabic is the only interface language and the layout is right-to-left.** All
> labels, titles and body text in Arabic. Back chevrons point right, text aligns
> right, rows read right-to-left, lists indent from the right.
> **Numbers stay Western (1 2 3, never ٠١٢) and left-to-right** inside the Arabic
> text. Currency is Jordanian Dinar to three decimals — `45.500`.
>
> ### Typeface
> **Almarai** for everything — the Arabic geometric sans by Boutros. Use its four
> weights deliberately: Light 300 nowhere, Regular 400 for body, Bold 700 for
> titles and figures, ExtraBold 800 for the one number that matters on a screen.
> Do not substitute Cairo, Tajawal, Dubai or Noto Sans Arabic.
>
> ### Theme: light only
> **There is no dark mode.** These users work mornings in direct sun; the design
> is optimised for a bright screen washed out by daylight, which means **high
> contrast and solid fills**, not subtlety. Never propose a dark variant.
>
> ### Colours
> - Page background `#F2F5FA`
> - Card / tile surface `#FFFFFF`
> - Border `#D9E1EE` — visible, not a whisper. Sunlight eats hairlines.
> - Primary text `#0B1626`
> - Secondary text `#4A5A73`
> - Tertiary text `#6E7C93` — the lightest text permitted anywhere
> - Accent, primary action `#1B5FD9`
> - Cash, success, positive `#0B8F58`
> - Credit, warning, pending `#9A5B00`
> - Return, debt, danger `#C42F2F`
> - Stock, quantity `#0B7E74`
>
> All body text sits at 4.5:1 or better against its background. There is no
> pale-grey-on-white anywhere.
>
> ### The look: flat and functional
> This is a working tool, closer to a ledger or a POS terminal than a consumer
> app. Build it from **solid fills, clear borders and honest rectangles**.
>
> **Do not use:** gradients of any kind, glassmorphism, blur, soft coloured
> shadows, glow, neumorphism, pill-shaped everything, oversized rounded corners,
> emoji as icons, decorative illustrations, centred hero sections, or purple.
>
> **Do use:** flat solid colour, 8px corner radius on tiles and cards, 6px on
> inputs and chips, 1px `#D9E1EE` borders, a single hairline divider between
> list rows, and a shadow only where something genuinely floats.
>
> Icons are **single-weight line icons in one colour**, sized 24. No two-tone,
> no filled-and-outlined mixing, no gradient icon tiles.
>
> ### Structure
> - Spacing: 4 / 8 / 12 / 16 / 24 only.
> - Radius: 8 tiles and cards, 6 controls, 0 for full-bleed bars.
> - Touch targets: 48×48 minimum. Grid tiles are far larger.
> - Type scale: title 18 Bold · card title 15 Bold · body 13 Regular ·
>   label 11 Regular · money 15 Bold · hero money 24 ExtraBold.
>
> ### Navigation: no sheets, no modals
> **Never use a bottom sheet, a modal dialog, or a popup menu.** Every action
> opens a full screen with its own top bar and back chevron. The only permitted
> overlay is a destructive confirmation — and only for delete.
>
> The reason: a rep taps with one thumb while holding a crate. A sheet that
> covers half the screen and can be dismissed by a stray drag loses their work.

---

## 2. The rule that shapes every screen

> **Actions come before information.**
>
> The old dashboard opened on statistics and buried the buttons below the fold, so
> the rep had to scroll before they could start working. Invert it: the first
> thing on every screen is **what you can do**, laid out as a grid. Numbers come
> after, and the rep scrolls to *read*, never to *act*.
>
> Nothing a rep needs to tap in a shop doorway may require scrolling.

---

## 3. Screens

### 3.1 Dashboard — الرئيسية · the main screen

> Arabic RTL mobile home screen for a field sales app, **light theme only**,
> **Almarai** font, flat and functional with no gradients.
>
> **Top strip, compact — 64px tall, not a hero.** On the right the rep's name and
> today's Gregorian date in small text; on the left a sync status chip
> (green "متصل" or amber "غير متصل") and a notifications icon. That is all: no
> avatar circle, no greeting card, no illustration.
>
> **Immediately below, filling the screen: a 3-column grid of every function.**
> Twelve square tiles, three across, four rows. Each tile is a white square with
> an 8px radius and a 1px `#D9E1EE` border, containing a 24px line icon in the
> tile's semantic colour and an Arabic label in 12sp Bold beneath it, both
> centred. Nothing else — no counts, no descriptions, no gradient behind the
> icon.
>
> The twelve tiles in order:
> 1. العملاء — customers (accent blue)
> 2. مسار اليوم — today's route (accent blue)
> 3. بيع — sale (green)
> 4. مرتجع — return (red)
> 5. طلب — order (teal)
> 6. تحصيل — collection (green)
> 7. عميل جديد — new customer (accent blue)
> 8. تحميل المركبة — van stock (teal)
> 9. طلب بضاعة — stock request (teal)
> 10. التقارير — reports (dark slate)
> 11. تقفيل اليوم — end of day (amber)
> 12. الإعدادات — settings (dark slate)
>
> **Below the grid, and only below it,** a compact figures strip the rep scrolls
> down to read: four cells in a single bordered block, two per row — مبيعات
> اليوم, التحصيلات, عدد الزيارات, رصيد المركبة. Each is a small Arabic label with
> a bold Western-digit figure. No cards, no icons, no gradients — this is a
> readout, not a feature.
>
> **No bottom navigation bar.** The grid is the navigation.

### 3.2 Customer page — صفحة العميل

> Arabic RTL customer detail screen, **light only**, **Almarai**, flat, no
> gradients, no bottom sheet.
>
> **Identity block at the top, compact.** Shop name in 18 Bold, code and area
> beneath in secondary text. On the left of that row, two 40px square outline
> buttons: call and map. Then a single bordered strip showing الرصيد — the
> outstanding balance — in 24 ExtraBold, red when they owe and green when clear.
> This is the one number that decides what the rep does next, so it is the only
> large figure on the screen.
>
> **Then, immediately, a 3-column grid of everything you can do with this
> customer** — same tile design as the dashboard, nine tiles, three rows:
> 1. بيع (green) · 2. مرتجع (red) · 3. طلب (teal)
> 4. تحصيل (green) · 5. كشف الحساب (blue) · 6. تقرير الحركات (blue)
> 7. التقرير المفصل (blue) · 8. تقرير السندات (slate) · 9. الموقع (slate)
>
> **Below the grid**, the readout: آخر زيارة, آخر عملية, إجمالي المبيعات,
> التحصيلات — a bordered block of label/value rows, no cards.
>
> The rep must be able to open a sale, a collection or the statement **without
> scrolling at all**.

### 3.3 Customer list — العملاء

> Arabic RTL list screen, light only, Almarai. Search field pinned at the top
> with a filter icon at its start. A single row of filter chips beneath:
> الكل, على المسار, عليه ذمم, لم تتم زيارته — flat rectangles with a 6px radius,
> selected one filled solid `#1B5FD9` with white text, unselected white with a
> `#D9E1EE` border.
>
> Rows are **not cards** — they are list rows separated by a 1px divider, flush
> to the screen edges, which fits more shops per screen. Each row: shop name in
> 15 Bold, code and area beneath in 11 secondary, and at the end the balance in
> 15 Bold, red when owing and grey `#6E7C93` when zero. A 4px vertical green bar
> at the start edge of rows already visited today.
>
> A rectangular "عميل جديد" button fixed at the bottom, full width, flat accent
> blue, 8px radius — **not** a circular floating action button.

### 3.4 Sale — بيع

> Arabic RTL point-of-sale screen, light only, Almarai, **no bottom sheet at any
> point**. Top bar with the customer name and balance. A search field for adding
> an item; results appear as a full list on the same screen, not in a sheet.
>
> Cart lines are list rows with dividers: item name 14 Bold, unit and unit price
> beneath, a quantity stepper (− 3 +) with square 32px buttons, and the line total
> at the end in Bold. A red outline icon at the far end removes the line.
>
> A **fixed totals panel** above the bottom of the screen, always visible without
> scrolling: المجموع, الخصم, الضريبة as small label/value rows, then الإجمالي in
> 24 ExtraBold. Payment method as three flat rectangular segments — نقدي / آجل /
> شيك — the selected one filled solid. Full-width green "حفظ السند" button.

### 3.5 Collection — تحصيل

> Arabic RTL collection screen, light only, Almarai, no sheets. The customer's
> outstanding balance in a bordered strip at the top, red. A large amount input,
> 28 ExtraBold, with "د.أ" as a suffix. Method as three flat square tiles in a
> row — نقداً, شيك, حوالة — each an icon and a label, selected one filled solid
> accent blue with white content.
>
> When شيك is selected, the cheque fields appear **inline below, on the same
> screen** — رقم الشيك, البنك, تاريخ الاستحقاق, and a "تصوير الشيك" button — never
> in a sheet or dialog. Full-width green "تسجيل التحصيل" button.

### 3.6 Reports — التقارير

> Arabic RTL screen, light only, Almarai. A 3-column grid of report tiles
> matching the dashboard exactly: المبيعات, الدفعات, الزيارات, التدفق النقدي,
> مبيعات الأصناف, الذمم, الأهداف, ملخص السندات, تقفيل اليوم. Flat white squares,
> line icon, Arabic label. No descriptions under the labels — the label is the
> description.

### 3.7 Statement / transaction reports — كشف الحساب · تقرير الحركات

> Arabic RTL report screen, light only, Almarai, no sheets. Top bar with a print
> icon at the far end. A **date range bar as a full-width flat control** reading
> "من ٠١/٠٨ إلى ١١/٠٨" — tapping it opens a full date-range screen, never a
> picker sheet.
>
> A bordered totals block, two columns of label/value pairs: إجمالي المبيعات,
> إجمالي المرتجعات, إجمالي التحصيلات, المبيعات النقدية — then a divider, then
> صافي الحركات and إجمالي الآجل in 18 Bold with the amber colour on the credit
> figure.
>
> Then the entries as list rows with dividers: a 3px coloured bar at the start
> edge encoding the type (green sale, red return, teal order, blue collection),
> the document number and date stacked, and the amount with نقدي or آجل beneath
> it at the end.
>
> Also produce the **error state** as its own screen: a bordered amber block
> reading "تعذّر جلب الحركات من الخادم" with an "إعادة المحاولة" button — visibly
> different from the empty state, which is a single grey sentence and no button.

### 3.8 New customer — عميل جديد

> Arabic RTL form, light only, Almarai. **Before any field**, a bordered amber
> notice: "هذا العميل يحتاج موافقة الإدارة — سيُرسل للمراجعة عند الحفظ، ولا يمكن
> البيع له قبل الاعتماد."
>
> Fields stacked full width: اسم العميل, رقم الهاتف, then a location block with a
> "التقاط الموقع الحالي" button showing the captured coordinates once taken. Then
> "وثيقة العميل (إلزامي)" with two flat square buttons side by side — camera and
> gallery — and a green "✓ تم إرفاق الوثيقة" line once attached. Full-width blue
> "حفظ العميل" button, greyed until name and document are present.

### 3.9 Print preview — معاينة الطباعة

> Arabic RTL print screen, light only, Almarai for the interface. A dark slate
> top bar. Two flat rectangular buttons in a row: filled blue "طباعة حرارية" and
> outlined "مشاركة PDF". A small printer status line beneath in green or grey.
>
> The rest is a grey backdrop holding a **white paper receipt** with torn zigzag
> edges, floating on a soft shadow. The receipt is **pure black on white, no
> colour whatsoever** — it prints on a 1-bit thermal head where any hue dithers
> into a stipple. Hierarchy comes from weight, size and rules only. Company logo
> centred, company name large and bold, then the document title reversed out
> white-on-black in a full-width bar, label/value lines, a ruled table, and the
> final total reversed out white-on-black.

### 3.10 End of day — تقفيل اليوم

> Arabic RTL settlement screen, light only, Almarai, no sheets. A bordered block
> of the day's figures as label/value rows: المبيعات النقدية, المبيعات الآجلة,
> التحصيلات النقدية, الشيكات — then a divider and المتوقع في الصندوق in 24
> ExtraBold. A single input for النقد المسلَّم, and beneath it a live difference
> row that reads الفرق and turns green at zero, red when short, amber when over.
> Two full-width buttons: green "تقفيل اليوم" and outlined "طباعة التقرير".

### 3.11 Settings — الإعدادات

> Arabic RTL settings, light only, Almarai. Grouped sections with a small bold
> Arabic header above each group and a bordered block containing the rows.
> Groups: الحساب, الطابعة, المزامنة, التطبيق, عن التطبيق. Each row is a label at
> the start and a value, switch or left-pointing chevron at the end.
> **No theme toggle** — there is only one theme.

---

## 4. Prompting notes

**Repeat the five constraints in every prompt.** Almarai, light-only, no sheets,
3-column grid, no gradients. Stitch reverts to a dark hero with a purple gradient
within three generations if you let it.

**Say "flat" and "no gradient" explicitly, every time.** It is the single
strongest signal against the generated-looking default.

**Reject anything centred.** This is a scanning tool; content aligns to the
reading edge. The only centred things are the grid tile contents.

**Use real Arabic content, never placeholders.** "سوبرماركت السلام",
"45.500", "S-101-000123". Arabic runs longer than English at the same meaning, so
a layout proved on Latin placeholder text overflows the moment it is translated.

**Ask for the empty, loading and error state of any list as separate
generations,** or you will only ever receive the happy path.

**If it returns a bottom sheet, say so plainly:** "replace the bottom sheet with
a full screen that has its own top bar and back button". It will comply, but only
if told each time.

---

## 5. What changed from v1, and why

| Decision | Reason |
|---|---|
| Almarai everywhere | One family, four weights, drawn for Arabic UI rather than adapted from Latin. |
| Light theme only | These reps sell in the morning, in sun. A dark UI is unreadable there, and maintaining a second theme buys nothing for this audience. |
| No bottom sheets | One thumb, a crate in the other arm. A sheet dismissed by a stray drag loses work. A screen with a back button does not. |
| 3-column grid of everything | Twelve functions reachable without scrolling. A launcher, not a feed. |
| Actions above information | The rep opens the app to *do* something. Statistics are what you read afterwards, and they move below the fold. |
| Flat, no gradients | Gradient icon tiles and soft shadows are the current generated-design signature. Solid fills and honest borders also survive sunlight better. |
