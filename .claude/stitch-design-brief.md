# FlowVan — design brief for Stitch

Paste **§1 Global style** into Stitch first so it holds the palette and the RTL
rule, then paste one screen block from §3 at a time. Each block is written to
stand alone — Stitch loses context between generations, so every one repeats the
constraints that matter rather than saying "as before".

Two things to say up front and repeat often, because they are the two Stitch
gets wrong on a Gulf/Levant app: **Arabic is the primary language, not a
translation**, and **the layout is right-to-left**.

---

## 1. Global style

> Design a mobile app called **FlowVan** — an Arabic-first field sales app for
> van salesmen ("مندوب") who sell FMCG stock door-to-door to small shops in
> Jordan. The user is standing in a shop or sitting in a van, one-handed, often
> in bright sunlight, frequently on a bad connection.
>
> **Language and direction: Arabic (RTL) is primary.** All labels, headings and
> body text in Arabic. The entire layout mirrors right-to-left: back arrows point
> right, text aligns right, rows read right-to-left. Numbers and currency use
> Western digits (1 2 3, never ٠١٢) and stay left-to-right inside the Arabic
> text. Currency is Jordanian Dinar shown to **three** decimals — `45.500`.
>
> **Style:** clean, dense, utilitarian. A working tool, not a consumer app. Flat
> cards on a soft blue-grey ground, generous rounded corners, thin hairline
> borders, restrained shadows. No illustrations, no gradients on large surfaces,
> no decorative imagery. Small colour gradients are allowed only on icon tiles.
>
> **Light and dark themes, both designed.** Dark is not an inversion — keep the
> accent legible on a dark ground.
>
> **Colours (light theme):**
> - Page background `#F4F6FB`
> - Card surface `#FFFFFF`
> - Raised surface `#E6EBF4`
> - Hairline border `#E1E6F0`
> - Primary text `#0F1A2E`
> - Secondary text `#5A6A85`
> - Tertiary text `#6E7C93`
> - Accent / primary action `#2C6FE4`
> - Success, cash, positive `#0FA968`
> - Warning, credit, pending `#B36C00`
> - Danger, returns, debt `#D63B3B`
> - Teal for stock and quantities `#0E9E91`
> - Purple for reports and analysis `#7757D4`
>
> **Colours (dark theme):**
> - Page background `#0B1220`, card surface `#121B2C`, raised `#16202F`
> - Border `#22304A`, primary text `#E8EDF7`, secondary `#97A5BC`
> - Accent `#5B94F5`, success `#34C48A`, warning `#D89434`, danger `#EE6A6A`
>
> **Type scale** (Arabic sans, e.g. IBM Plex Sans Arabic or Noto Sans Arabic):
> - Screen title 17sp Semibold
> - Card title 14sp ExtraBold
> - Body 13sp Regular
> - Label / caption 11sp Regular
> - Money 13–15sp Bold, tabular figures
> - Big money 22sp ExtraBold
>
> **Spacing:** 4 / 8 / 16 / 24 only. **Radii:** 12 for cards, 10 for chips and
> buttons, 8 for inputs. **Touch targets:** minimum 48×48.
>
> **Semantic colour is never decoration.** Green means cash or positive, amber
> means credit or waiting, red means a return or a debt. Do not use them to make
> a screen look lively.

---

## 2. Shared components

Ask Stitch for these once, then reference them by name in screen prompts.

| Component | Description to give Stitch |
|---|---|
| **Top bar** | Right-aligned title with optional subtitle underneath, back chevron pointing right at the far right, optional action icon at the far left. No elevation, sits on the page background. |
| **Stat card** | White card, 12 radius, hairline border. A 36×36 rounded-square icon tile with a small two-stop gradient, then a title and a caption stacked, then a value or chevron at the end. |
| **Summary pill** | Small tile inside a card: 11sp label on top, bold value beneath, a 3px coloured bar or tinted background carrying the semantic colour. |
| **Filter chip row** | Horizontally scrollable pills. Selected = accent fill, white text. Unselected = raised surface, secondary text. |
| **Transaction row** | Card with a coloured type badge at the start (بيع green, مرتجع red, طلب teal, تحصيل blue), document number and date stacked in the middle, amount and a settlement label (نقدي / آجل) at the end. |
| **Date range bar** | A single row control showing "من … إلى …" with both dates, tapping opens a range picker. Presets: هذا الشهر, الشهر الماضي, آخر ٣٠ يوم. |
| **Empty state** | Centred: a light line icon, one sentence in secondary text. No button. |
| **Error state** | Card with amber text explaining what failed and what to do, and an "إعادة المحاولة" button. **Must look different from the empty state** — a rep has to be able to tell "no transactions" from "the request failed". |
| **Loading** | Centred circular indicator in accent colour, nothing else. |

---

## 3. Screens

### 3.1 Login — تسجيل الدخول

> An Arabic RTL mobile login screen for a field sales app. Dark navy page with a
> subtle radial glow behind the centre. Centred logo mark, then the title
> "تسجيل الدخول" and subtitle "لوحة تحكم العمليات". A single card holding two
> fields — "رقم المستخدم" and "كلمة المرور" with a show/hide eye at the start of
> the field — and a full-width mint-green primary button reading "دخول". A small
> company name in muted text at the bottom. No social login, no sign-up link:
> accounts are created by the office.

### 3.2 Home — الرئيسية

> Arabic RTL home screen for a van salesman. Top: a greeting with the rep's name
> and today's date, and a connection/sync status chip. Then a 2×2 grid of stat
> cards: مبيعات اليوم, التحصيلات, عدد الزيارات, رصيد المركبة — each with a
> gradient icon tile, a big number in Western digits and a small caption. Below,
> a "مسار اليوم" card showing progress through today's route as a horizontal bar
> with "١٢ من ١٨ عميل". Then a row of large action tiles: عميل جديد, طلب بضاعة,
> تقفيل اليوم, التقارير. Bottom navigation with 4 items, labels in Arabic.

### 3.3 Customer list — العملاء

> Arabic RTL searchable customer list. Sticky search field at top with a filter
> icon. Chips for فلترة: الكل, على المسار, عليه ذمم, لم تتم زيارته. Each row is a
> card: customer name in bold, shop code and area beneath in secondary text, and
> at the end the outstanding balance — red when they owe money, muted grey when
> zero. A small green dot on rows already visited today. Floating action button
> at the bottom start corner for "عميل جديد".

### 3.4 Customer details — صفحة العميل

> Arabic RTL customer detail screen. Header card: shop name, code, phone with a
> call button, and address with a map button. Then two stat cards side by side —
> إجمالي المبيعات in blue and التحصيلات in green. Then a row of four circular
> action buttons: بيع, مرتجع, طلب, تحصيل. Then a stacked list of report cards,
> each with a gradient icon tile, title and one-line description, and a chevron:
> كشف الحساب, تقرير الحركات, التقرير المفصل للحركات, تقرير السندات, تقرير
> الدفعات. Dense but calm — this screen is the hub the rep returns to.

### 3.5 New customer — عميل جديد

> Arabic RTL form screen. **At the very top, before any field**, an information
> banner in amber: "هذا العميل يحتاج موافقة الإدارة — سيُرسل للمراجعة عند الحفظ،
> ولا يمكن البيع له قبل الاعتماد." Then fields: اسم العميل, رقم الهاتف, and a
> location card with a "التقاط الموقع الحالي" button showing captured
> coordinates once taken. Then a required document section titled "وثيقة العميل
> (إلزامي)" with two side-by-side buttons — camera and gallery — and a green
> "✓ تم إرفاق الوثيقة" confirmation once attached. Full-width blue "حفظ العميل"
> button, disabled until name and document are present.
>
> Also design the waiting state: after saving, the form is replaced by a card
> with a spinner and "بانتظار موافقة المشرف…" and the line "ابقَ على هذه الشاشة،
> سيتم إعلامك فور الاعتماد."

### 3.6 Sale — سند بيع

> Arabic RTL point-of-sale cart screen. Top bar with the customer name. A search
> field for adding items. Cart lines as cards: item name in bold, unit and price
> beneath, a quantity stepper (− number +) and the line total at the end. Swipe
> or a small red icon to remove. A sticky summary panel at the bottom above the
> action button: المجموع, الخصم, الضريبة, and الإجمالي in large bold. Payment
> method as a segmented control: نقدي / آجل / شيك. Full-width green "حفظ السند"
> button. Show a low-stock warning chip in amber on any line exceeding van stock.

### 3.7 Collection — تحصيل

> Arabic RTL payment collection screen. Customer name and current balance at the
> top in a red-tinted card. A large numeric amount field with the currency
> suffix. Method as three big selectable tiles: نقداً, شيك, حوالة — each with an
> icon, the selected one filled with the accent colour. When شيك is selected,
> reveal extra fields: رقم الشيك, البنك, تاريخ الاستحقاق, and a "تصوير الشيك"
> button. Full-width green "تسجيل التحصيل" button.

### 3.8 Account statement — كشف الحساب

> Arabic RTL account statement screen. Top bar with title, customer name
> underneath, and a print icon at the far left. A date range bar. A summary card
> with two pills — مدين in red and دائن in green — a divider, then الرصيد in
> large bold, red when the customer owes. Then a chronological list of entries,
> each a card with a type badge, document number and date, and the amount with
> "مدين" or "دائن" beneath it.

### 3.9 Transaction report — تقرير الحركات

> Arabic RTL report screen. Top bar with a print icon. Date range bar, then a
> filter chip row: الكل, المبيعات, المرتجعات, التحصيلات. Then a totals card
> holding four summary pills in a 2×2 grid — إجمالي المبيعات green, إجمالي
> المرتجعات red, إجمالي التحصيلات blue, المبيعات النقدية teal — a divider, then
> two emphasised rows: صافي الحركات and إجمالي الآجل in amber. Then the
> transaction rows. Include the error state: an amber card reading "تعذّر جلب
> الحركات من الخادم" with an "إعادة المحاولة" button.

### 3.10 Detailed transaction report — التقرير المفصل للحركات

> Same as the transaction report, plus expandable vouchers. Each voucher card can
> be tapped to reveal its item lines underneath, separated by a divider: item
> name in bold, then "٣ كرتون × 12.500" in secondary text, and the line value at
> the end. Two small buttons above the list: توسيع الكل and طيّ الكل. Collapsed
> by default.

### 3.11 Print preview — thermal receipt

> Arabic RTL print preview screen. Dark navy top bar. Below it a row of two
> buttons: a filled blue "طباعة حرارية" and an outlined "مشاركة PDF", then a
> small centred printer status line in green when connected. The rest of the
> screen is a light grey backdrop holding a **white paper receipt** with torn
> zigzag edges top and bottom, floating with a soft shadow.
>
> The receipt itself is **pure black on white — no colour at all**, because it
> prints on a 1-bit thermal head. Hierarchy comes from weight, size and rules
> only. Centred company logo, company name large and bold, tax number, then the
> document title reversed out white-on-black in a full-width bar. Then
> label/value lines, a bordered table of movements, and the final total reversed
> out white-on-black. Signature lines at the bottom: توقيع العميل and توقيع
> المندوب.

### 3.12 End of day — تقفيل اليوم

> Arabic RTL daily settlement screen. A summary card with the day's figures:
> المبيعات النقدية, المبيعات الآجلة, التحصيلات النقدية, الشيكات, and المتوقع في
> الصندوق in large bold. A field for النقد المسلَّم with a live difference
> indicator that turns green at zero, red when short and amber when over. A list
> of the day's vouchers, collapsible. A full-width primary "تقفيل اليوم" button,
> and a secondary "طباعة التقرير" beside it.

### 3.13 Reports hub — التقارير

> Arabic RTL grid of report entry points, two columns. Each tile: a gradient icon
> square, a title, and a one-line description. Entries: المبيعات, الدفعات,
> الزيارات, التدفق النقدي, مبيعات الأصناف, الذمم, الأهداف, ملخص السندات. Purple
> and blue gradients, calm and evenly weighted — no tile should look more
> important than the others.

### 3.14 Van stock — تحميل المركبة

> Arabic RTL van inventory screen. Search field, then item rows: item name and
> code, and at the end the quantity with a coloured chip — green when healthy,
> amber when low, red when out. A summary bar at the top with عدد الأصناف and
> قيمة المخزون. A primary button "طلب بضاعة" at the bottom.

### 3.15 Settings — الإعدادات

> Arabic RTL settings screen, grouped sections with small uppercase-style Arabic
> section headers. Groups: الحساب (name, role, sign out), الطابعة (connect,
> paired device, test print), المزامنة (last sync time, sync now, pending count),
> التطبيق (language toggle Arabic/English, theme toggle light/dark/system),
> عن التطبيق (version, server address). Rows are label at the start, value or
> switch at the end, chevrons pointing left.

---

## 4. Prompting notes

**Repeat the direction every time.** Stitch drifts back to LTR after a few
generations. Start each prompt with "Arabic RTL mobile screen" rather than
assuming it remembers.

**Give it real content, not placeholders.** Use "سوبرماركت السلام", "45.500",
"S-101-000123". Lorem or English placeholders produce layouts that break the
moment real Arabic goes in — Arabic runs longer than English at the same
meaning, so a design proved on placeholders will overflow.

**Ask for both themes explicitly** by pasting the dark palette in a follow-up:
"now the same screen in the dark theme using these colours…".

**Do not accept centred layouts.** Stitch likes centring things; this app is a
scanning tool and almost everything should be aligned to the reading edge.

**Regenerate the states separately.** Ask for the empty, loading and error
version of any list screen as its own generation, or you will only ever get the
happy path — and the empty-versus-error distinction is the one this app is
currently missing.

---

## 5. Carry the audit's fixes in

Four decisions from the design audit are already folded into §1, so anything
generated from this brief lands on the corrected side of them:

1. **Tertiary text is `#6E7C93`**, not the app's current `#A8B3C6` — the old
   value is 2.1:1 on white and unreadable in sunlight.
2. **Both themes are specified**, so nothing generated here assumes light only.
3. **One type scale and one spacing scale**, rather than per-screen sizes.
4. **Empty, loading and error are named components**, so they stop being
   redesigned per screen.
