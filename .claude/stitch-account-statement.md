# Stitch — كشف الحساب (customer account statement)

Companion to `stitch-design-brief.md`. That file's §1 still applies; this expands
its §3.7 into the prompts that actually produce the screen, and settles one thing
§3.7 left open: **كشف الحساب is a balance document, not a transaction list.**
تقرير الحركات already lists movement. This screen exists to answer *how much does
he owe me right now, and how did it get there* — so it carries an opening
balance, a running balance per row, and a closing balance. That single decision
drives most of the layout below.

Generate **in order**. §2 is the base; §3–§6 vary from it.

---

## 1. Paste this first, every session

> Arabic **RTL** mobile screen for **FlowVan**, a field sales app used by van
> salesmen in Jordan. **Almarai** font throughout. **Light theme only — never
> propose a dark version.** Flat and functional: solid fills, 1px `#D9E1EE`
> borders, 8px radius on cards, 6px on controls. **No gradients, no glassmorphism,
> no blur, no glow, no emoji, no illustrations, no purple, no bottom sheets, no
> modals.** Single-weight 24px line icons in one colour.
>
> Colours: page `#F2F5FA` · surface `#FFFFFF` · border `#D9E1EE` · primary text
> `#0B1626` · secondary `#4A5A73` · tertiary `#6E7C93` · accent `#1B5FD9` ·
> success `#0B8F58` · warning `#9A5B00` · danger `#C42F2F`.
>
> Numbers are Western (1 2 3, never ٠١٢) and left-to-right inside Arabic text.
> Currency is JOD to three decimals: `45.500`.

---

## 2. The statement — default state

> Design the **كشف الحساب** (account statement) screen for one customer.
>
> **Top bar, 56px.** Title "كشف الحساب" at the right in 18 Bold, and directly
> beneath it in 11sp `#6E7C93` the shop name "سوبرماركت السلام". A back chevron
> pointing **right** at the far right. At the **left**, two 40px outline icon
> buttons: a **print** icon and a **share** icon — both line icons, `#1B5FD9`,
> no labels, no filled circles.
>
> **Date range bar**, full width, flat white with a `#D9E1EE` border, 6px radius,
> 44px tall, a calendar line icon at the start edge and the range reading
> "من 01/08 إلى 12/08". Tapping it opens a **full date screen**, never a picker
> sheet.
>
> **The balance block** — the most important element on the screen, and it sits
> above the entries, not below them. A white bordered block, 8px radius:
>
> - First row: label "الرصيد السابق" at the right in 12sp `#4A5A73`, value
>   `210.000` at the left in 14 Bold `#0B1626`.
> - Then "إجمالي المديونية" — `486.250` in `#C42F2F`.
> - Then "إجمالي الدفعات" — `348.500` in `#0B8F58`.
> - A 1px `#D9E1EE` divider across the full block.
> - Then **"الرصيد المستحق"** in 13 Bold, and its value `347.750` in **20
>   ExtraBold `#C42F2F`** — the largest number anywhere on the screen.
>
> These are **label/value rows in one column, not a 2×2 grid of pills.** The four
> figures are read against each other and must share an edge to be comparable.
>
> **The entries.** Flush list rows, **not cards**, edge to edge, separated by a
> 1px `#D9E1EE` divider. Newest first. Each row is 72px:
>
> - A **4px vertical bar at the start (right) edge**, full height, coloured by
>   type: sale `#C42F2F`, return `#0B8F58`, collection `#1B5FD9`, order `#0B7E74`.
> - Then, right-aligned: the **document number** in 14 Bold `#0B1626` on the
>   first line, and beneath it in 11sp `#6E7C93` the date and time —
>   `12/08 · 09:42`.
> - At the **end (left)**, right-aligned as a column: the **amount** in 15 Bold
>   in the type's colour, and beneath it in 11sp `#6E7C93` the **running balance
>   after this document** — `الرصيد 347.750`.
>
> **A small flat rectangular badge** carrying the type sits between the status bar
> and the number: `فاتورة` · `مرتجع` · `سند قبض` · `طلب`. 6px radius, 10sp Bold,
> the type's colour on a 12% tint of it. No pills, no icons inside badges.
>
> Use real content, newest first:
> `INV-2291` · 12/08 · فاتورة · 128.750 · الرصيد 347.750 —
> `RCP-0884` · 11/08 · سند قبض · 200.000 · الرصيد 219.000 —
> `RET-0143` · 10/08 · مرتجع · 18.500 · الرصيد 419.000 —
> `INV-2264` · 08/08 · فاتورة · 227.500 · الرصيد 437.500.
>
> **A full-width rectangular button fixed to the bottom**, flat `#1B5FD9`, 8px
> radius, 52px tall, white 15 Bold: "طباعة كشف الحساب". Not a floating action
> button, and it does not float over the last row — the list ends above it.

---

## 3. Row anatomy — generate as a spec sheet

> Design a **specification sheet** showing the four statement row types stacked
> vertically at full width, each with a small Arabic caption above it. Same flat
> style, Almarai, light only, no cards.
>
> 1. **فاتورة (مدين)** — start bar `#C42F2F`, badge `فاتورة`, amount `128.750` in
>    `#C42F2F`, caption beneath the amount: "الرصيد 347.750".
> 2. **سند قبض (دائن)** — start bar `#1B5FD9`, badge `سند قبض` and a second
>    smaller badge beside it for the method: `نقدي` / `شيك` / `حوالة`. Amount
>    `200.000` in `#0B8F58` with a **minus sign before it** — a collection reduces
>    the balance and the row must say so without being read twice.
> 3. **مرتجع (دائن)** — start bar `#0B8F58`, badge `مرتجع`, amount `18.500` in
>    `#0B8F58`, also with a minus sign.
> 4. **شيك مؤجل** — a collection by cheque that has not cleared. Start bar
>    `#9A5B00`, row background a faint amber `#FDF6EA`, badge `شيك — لم يُحصّل`,
>    and the running balance caption reads "لا يُحتسب في الرصيد". A cheque in hand
>    is not money received, and a rep who reads it as paid will refuse the next
>    collection.
>
> No rounded row corners, no shadows, no avatars.

---

## 4. The date range — a full screen, not a picker

> Because this app uses **no bottom sheets and no modals**, tapping the range bar
> opens its own screen. Design **الفترة**.
>
> Top bar: "الفترة" at the right, "إلغاء" as a text button at the left. Then a
> row of flat preset chips, 6px radius, 32px tall: `هذا الشهر` · `الشهر الماضي` ·
> `آخر ٣٠ يوم` · `من البداية` · `مخصص`. Selected is solid `#1B5FD9` with white
> text.
>
> Below, two bordered white blocks labelled "من" and "إلى", each showing the
> chosen date in 16 Bold with a calendar line icon at the start edge. Beneath
> them, an inline month calendar — flat, no shadow, weekday initials in 11sp
> `#6E7C93`, the selected range filled `#1B5FD9` at its two ends and tinted
> `#E8EFFC` between them. **Two full-width buttons fixed at the bottom**, side by
> side: outlined "إعادة تعيين" and filled blue "عرض".

---

## 5. Print preview — معاينة الطباعة

> Design the print preview reached from the "طباعة كشف الحساب" button. It is a
> **screen**, not a dialog.
>
> Top bar "معاينة كشف الحساب" with a back chevron at the right. The body shows the
> paper itself on the page background: a **white sheet centred with a 1px
> `#D9E1EE` border**, proportioned like an 80mm thermal roll — narrow and tall —
> so the rep sees the real output, not a reflow of the screen.
>
> On the sheet, all centred except the table: the company logo as a black-and-white
> block at the top, the title "كشف حساب" in Bold, then a small label/value list —
> العميل, رقم العميل, الهاتف, الفترة, المندوب. A dashed rule. Then the table with
> four column headers — التاريخ · السند · مدين · دائن — and rows beneath, with the
> **document number on its own line reading left-to-right** under each row's
> amounts. A dashed rule. Then الرصيد السابق, إجمالي المديونية, إجمالي الدفعات,
> and **الرصيد المستحق** in the largest weight on the sheet. At the foot, two
> signature lines side by side: "توقيع العميل" and "توقيع المندوب", then
> "تاريخ الطباعة".
>
> Everything on the sheet is **pure black on white** — no colour, no grey text, no
> tints. It is printed on a 1-bit thermal head, and grey dithers into noise.
>
> **Two full-width buttons fixed at the bottom**: outlined "مشاركة" with a share
> line icon, and filled blue "طباعة" with a printer line icon.

---

## 6. The four states — generate each separately

Ask one at a time. Requested together, Stitch merges them into one screen and the
distinction that matters is lost.

> **A. جارٍ التحميل** — top bar, range bar and balance block **in place** with the
> figures replaced by flat `#E8EDF5` rectangles, and six skeleton rows below. No
> spinner, no shimmer gradient. The chrome stays put so nothing jumps when the
> data lands.

> **B. لا توجد حركات** — the range is valid but empty. The balance block **still
> shows**, with الرصيد السابق and الرصيد المستحق equal, because an empty month
> does not mean a zero balance. Below it, one grey sentence: "لا توجد حركات في
> هذه الفترة." and a text button "توسيع الفترة". No icon, no card, no border.

> **C. تعذّر التحميل** — the load failed. A bordered block, 1px `#9A5B00`, fill
> `#FDF6EA`, with a warning line icon, the sentence "تعذّر جلب كشف الحساب من
> الخادم." and a filled blue "إعادة المحاولة" button. **The balance block is
> hidden entirely in this state** — a stale balance shown beside a failed fetch is
> worse than no balance, because the rep will collect against it.

> **D. الرصيد لصالح العميل** — the customer has overpaid. Same screen, but
> الرصيد المستحق reads `-42.500` in `#0B8F58`, and the label beneath it changes to
> "الرصيد لصالح العميل". The figure must not stay red — a credit balance shown in
> the debt colour is read as debt at a glance.

---

## 7. Rejecting what Stitch will try

| It will return | Say this |
|---|---|
| A 2×2 grid of coloured summary pills | "Use single-column label/value rows sharing one right edge. No pills." |
| Cards with shadows for each entry | "Flush list rows with a 1px divider. No cards, no shadow." |
| The balance block below the entries | "The closing balance goes above the list. It is the reason the screen is opened." |
| Dropping the running balance per row | "Every row shows the balance after that document." |
| A bottom sheet for the date range | "Replace the sheet with a full screen with its own top bar and cancel button." |
| A colourful print preview | "The sheet is pure black on white — it is a 1-bit thermal print." |
| An A4-proportioned preview | "The paper is an 80mm thermal roll: narrow and tall." |
| A circular FAB for print | "Full-width rectangular button fixed at the bottom." |
| A dark variant | "There is no dark theme in this product. Light only." |
| Latin placeholder data | "Use the real Arabic names and the document numbers given." |

---

## 8. Why it is built this way

The parts that look arbitrary but are not:

- **Opening → entries → closing.** A statement whose figures do not reconcile to a
  balance is just a filtered list. The rep hands this to a shopkeeper who will
  argue with it, so it has to be followable line by line.
- **The running balance is per row, not just at the foot.** The argument is never
  about the total; it is about one invoice. The number the shopkeeper disputes has
  to be next to the document he disputes.
- **Collections carry a minus sign** even though the colour already says credit.
  Colour fails in sunlight and fails for the ~8% of men with a red-green
  deficiency. The sign does not.
- **Uncleared cheques are excluded from the running balance and say so.** This is
  the single most expensive misreading available on this screen — a rep who counts
  an uncleared cheque as collected will skip the follow-up.
- **The error state hides the balance.** Everywhere else a stale number beats no
  number. Not here: this figure is what the rep collects against, and collecting
  against a stale balance costs real money.
- **The preview shows the roll's proportions.** Reps print in doorways and a
  surprise at the printer costs a reprint on paper they may not have.
