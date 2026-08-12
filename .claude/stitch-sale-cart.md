# Stitch — بيع · the sale screen and its cart

Companion to `stitch-design-brief.md`. Paste §1 of that file first, or the
condensed reminder in §1 below.

This screen is not a shop checkout, and prompting it as one produces the wrong
thing. It is a **van salesman writing a voucher at a counter**: the stock is in
the vehicle behind him, the price may be overridden, offers apply automatically,
some lines are free, and the same screen writes three different document types.
Everything below exists because the code already does it.

Generate in this order — §3 is the base, §4 varies from it, §5–§8 are states.

---

## 1. Paste this first, every session

> Arabic **RTL** mobile screen for **FlowVan**, a field sales app for van
> salesmen in Jordan. **Almarai** font. **Light theme only — never a dark
> version.** Flat and functional: solid fills, 1px `#D9E1EE` borders, 8px radius
> on cards, 6px on controls. **No gradients, no glassmorphism, no blur, no glow,
> no emoji, no illustrations, no purple, no bottom sheets, no modals.**
> Single-weight 24px line icons.
>
> Colours: page `#F2F5FA` · surface `#FFFFFF` · border `#D9E1EE` · text `#0B1626`
> · secondary `#4A5A73` · tertiary `#6E7C93` · accent `#1B5FD9` · success/cash
> `#0B8F58` · warning/credit `#9A5B00` · danger/return `#C42F2F` · stock
> `#0B7E74`.
>
> Numbers are Western (1 2 3, never ٠١٢), left-to-right inside Arabic text.
> Currency is JOD to three decimals: `45.500`. Quantities have no trailing
> zeros: `3`, not `3.0`.

---

## 2. The shape of the screen

One screen, two views, switched by a control in the top bar — **not two
destinations and not a sheet**:

| View | What it is |
|---|---|
| **الأصناف** | the product picker: search, filters, product rows, add to cart |
| **السلة** | the cart: lines, offers, discount, totals, payment, save |

A counter on the toggle shows how many lines are in the cart. The rep flips
between the two many times per visit, so switching must be one tap and must not
lose scroll position or the search query.

The same screen writes **three document types** — بيع, مرتجع, طلب — and differs
per type. Design بيع first, then §7.

---

## 3. View A — الأصناف · the product picker

> Design the product picker view of an Arabic RTL sale screen.
>
> **Top bar, 56px.** Back chevron pointing right at the far right. Then the
> customer's name in 15 Bold with "بيع" beneath it in 11 secondary. At the far
> left, a **view toggle**: a flat bordered button reading "السلة" with a count
> badge — a small solid `#1B5FD9` square with white Western digits.
>
> **Search field, pinned, always visible.** Full width, 6px radius, white,
> `#D9E1EE` border, line search icon at the start edge, placeholder
> "ابحث عن صنف أو باركود". A barcode-scan line icon at the end edge of the field.
>
> **Two filter buttons in a row beneath it**, each a flat bordered rectangle with
> a label and a small chevron: "كل الفئات" and "كل المخزون". They are dropdown
> **anchors** — the menu opens as a panel attached to the button, not a sheet.
> When a filter is active the button fills solid `#1B5FD9` with white text.
>
> **Product rows** — flush list rows with a 1px divider, not cards. Each 76px:
> - item name in 14 Bold on the first line
> - on the second, in 11 `#6E7C93`: the item code and the unit — `10234 · كرتون`
> - at the end, stacked and right-aligned: the unit price in 14 Bold, and beneath
>   it a **stock badge**
> - a flat square 32px "+" button at the far end to add one to the cart
>
> **Stock badge, three states**, a small rectangle with 4px radius:
> - in stock — text `#0B7E74`, faint teal fill, e.g. `48 كرتون`
> - low — text `#9A5B00`, faint amber fill, e.g. `3 كرتون`
> - out of stock — text `#C42F2F`, faint red fill, "غير متوفر", and the whole row
>   drops to 45% opacity with its "+" disabled
>
> **A summary bar fixed at the bottom**, above the system bar: on the right
> "٤ أصناف" and on the left the running total in 17 ExtraBold, then a
> full-width-feeling flat blue button "عرض السلة". It is a readout plus one
> action, not a floating card.
>
> Real content: مياه غدير ٦×١.٥ لتر · 10234 · كرتون · 4.500 · 48 كرتون ·
> شيبس تشيبسي كبير · 10891 · كرتون · 12.000 · 3 كرتون · عصير راني برتقال ·
> 10455 · كرتون · 8.750 · غير متوفر.

---

## 4. View B — السلة · the cart

> Design the cart view of the same screen. Same top bar, with the toggle now
> reading "الأصناف".
>
> **Cart lines** — flush rows with dividers, taller than the picker's because
> each carries a stepper:
> - item name in 14 Bold; beneath it in 11 secondary the unit and unit price,
>   `كرتون · 4.500`
> - a **quantity stepper**: two flat 32px squares with 6px radius and a
>   `#D9E1EE` border holding − and +, with the quantity between them in 15 Bold
>   Western digits
> - the line total at the end in 15 Bold
> - a small red outline trash icon at the far end
>
> **Offer rows attach underneath their line**, indented, in 11sp: a small tag
> line icon, the offer name, and the discount it took off in `#0B8F58` —
> "خصم كمية ٥٪ − 2.250". Several offers can stack on one line. These are applied
> automatically, so they are stated, never editable.
>
> **A free-item line** looks like a normal line with a small "هدية" tag in
> `#0B8F58` where the price would be, and its total showing `0.000`.
>
> **Voucher discount section** — only for reps allowed to give one. A bordered
> block with a label "خصم السند", a small numeric input, and two flat segments
> choosing نسبة % or مبلغ. Absent entirely when the rep lacks the permission —
> not greyed out, absent.
>
> **Totals block**, bordered, label/value rows: المجموع, خصم السند, الخصومات
> والعروض in `#0B8F58`, الضريبة — then a divider and **الإجمالي in 24
> ExtraBold**. This is the number the shopkeeper is told, and the only large
> figure on the screen.
>
> **Payment method**: two flat bordered segments, نقدي and آجل, selected one
> solid `#1B5FD9` with white text.
>
> **Save button** fixed at the bottom, full width, flat `#0B8F58`, 8px radius,
> 52px, "حفظ السند". Disabled and grey when the cart is empty.

---

## 5. Replace the two sheets with screens

The code has two bottom sheets. Both must become full screens with their own top
bar and back chevron.

> **A. اختيار صنف** — the quick-add sheet becomes a full screen: search field,
> product rows identical to §3, and a "تم" button. It exists so a rep can add
> several items without leaving the cart, so it returns to the cart on close.

> **B. اختيار الهدية** — choosing which free item an offer grants. A full screen
> titled "اختر الهدية", a line of explanatory text — "عرض: اشترِ ١٠ واحصل على ١
> مجاناً" — then the eligible products as selectable rows with a radio square at
> the start edge, and a flat blue "تأكيد" button fixed at the bottom. A rep must
> be able to see what they are choosing between; a half-height sheet over a cart
> cannot show four options and their stock.

---

## 6. States — generate each separately

> **A. سلة فارغة** — the cart with nothing in it: one sentence in 13 secondary,
> "السلة فارغة. أضف أصنافاً من قائمة الأصناف." and a flat blue button
> "الأصناف". No illustration.

> **B. لا توجد نتائج** — the picker with a query matching nothing: the search
> field keeps its text, one sentence "لا توجد أصناف مطابقة", and a text button
> "مسح البحث".

> **C. غير متصل** — a full-width amber strip directly under the top bar:
> "أنت غير متصل. سيُحفظ السند ويُرسل عند عودة الاتصال." Bordered, faint amber
> fill, no icon-only version — the sentence matters more than the symbol. The
> screen stays fully usable: selling offline is normal, not an error.

> **D. تجاوز الحد الائتماني** — the customer is over their credit limit: the آجل
> payment segment is disabled and an amber strip above the payment row explains
> "تجاوز العميل الحد الائتماني — البيع نقداً فقط." Cash stays selectable. This is
> a prohibition, not a warning, and it must not be dismissible.

---

## 7. The other two document types

> **مرتجع (return)** — same cart, three differences: the top bar subtitle reads
> "مرتجع" and every total is shown in `#C42F2F`; a **required reason** row of
> flat selectable segments — تالف, منتهي الصلاحية, خطأ في الطلب, أخرى; and a
> **source invoice banner** at the top reading "مرتجع من السند: S-101-000123"
> with a "تغيير" text button, or "اختر السند الأصلي" when none is chosen yet.

> **طلب (order)** — same cart, plus a **delivery date field**: a flat bordered
> row with a calendar line icon reading "تاريخ التسليم: ١٥/٠٨/٢٠٢٦", tapping it
> opens a full date screen. No payment segments at all — an order is not paid
> when it is written.

---

## 8. Rejecting what Stitch will try

| It returns | Say this |
|---|---|
| A shop-style product grid with images | "No product images. These are FMCG SKUs identified by name, code and unit — use list rows." |
| A bottom sheet for the item picker | "Replace the sheet with a full screen with its own top bar and back button." |
| Cards for cart lines | "Flush list rows with a 1px divider. No cards, no shadows." |
| A circular FAB for the cart | "The cart is a labelled toggle in the top bar with a count badge." |
| Pill-shaped payment switch | "Two flat bordered rectangles, 6px radius, the selected one solid-filled." |
| Offers as promotional banners | "Offers are stated as small rows under the line they discounted, not advertised." |
| A dark variant | "There is no dark theme in this product." |
| Latin placeholder items | "Use the real Arabic item names in the prompt." |

---

## 9. Why it is built this way

- **The picker and the cart are one screen, not two.** A rep adds an item, checks
  the running total, adds another — dozens of times per visit. Two destinations
  would put a navigation transition inside that loop.
- **Stock is on the product row, not behind a tap.** The van's stock is the hard
  limit on the sale; a rep who has to open an item to discover it is out has
  already promised it to the shopkeeper.
- **Offers are stated, never editable.** They are evaluated by the server and
  frozen onto the voucher. Presenting them as adjustable would imply a control
  the rep does not have.
- **The voucher discount is absent, not disabled,** for reps without the
  permission. A greyed field invites the shopkeeper to ask for a discount the rep
  cannot give, which is a conversation the design should not start.
- **Offline is not an error state.** Most of the round happens without signal.
  The banner informs; it never blocks.
