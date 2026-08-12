# Stitch — العملاء (customer list)

Companion to `stitch-design-brief.md`. That file's §1 still applies; this expands
its §3.3 into the prompts that actually produce the screen.

Generate these **in order**. §2 is the screen everything else varies from, so get
it right before asking for anything else — Stitch anchors on its last good
result, and a weak base propagates.

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

## 2. The list — default state

> Design the **العملاء** (customers) screen.
>
> **Top bar, 56px.** Title "العملاء" at the right in 18 Bold. At the left, two
> 40px square outline icon buttons: filter and sort. A back chevron pointing
> **right** at the far right of the bar.
>
> **Search field, pinned below the bar and always visible.** Full width, 6px
> radius, white with a `#D9E1EE` border, a line search icon at the start edge and
> the placeholder "ابحث باسم المحل أو رقمه". It does not scroll away — a rep
> looking for one shop should never have to scroll up first.
>
> **Filter chips, one horizontal row, scrollable.** Flat rectangles, 6px radius,
> 32px tall: `الكل` · `على المسار` · `عليه ذمم` · `تجاوز الحد` · `لم تتم زيارته`.
> Selected chip is solid `#1B5FD9` with white text; unselected is white with a
> `#D9E1EE` border and `#4A5A73` text. **No pill shapes, no icons inside chips.**
>
> **A thin result count line** beneath the chips, right-aligned, 11sp tertiary:
> "٤٨ عميل" — Arabic word, Western digits.
>
> **The list.** Rows, **not cards** — flush to both screen edges, separated by a
> 1px `#D9E1EE` divider, so more shops fit per screen. Each row is 72px tall:
>
> - A **4px vertical status bar at the start (right) edge**, full row height,
>   carrying state by colour.
> - Then the content, right-aligned: **shop name** in 15 Bold `#0B1626` on the
>   first line; on the second line, in 11 `#6E7C93`, the code and area separated
>   by a middot — `C-1042 · الزرقاء`.
> - At the **end (left)** of the row, right-aligned as a column: the **balance**
>   in 15 Bold, and beneath it an 11sp label saying what that number is.
>
> Use real content: سوبرماركت السلام · C-1042 · الزرقاء · 128.750 ·
> بقالة الأمانة · C-0977 · الرصيفة · 0.000 · ماركت النور · C-1130 · الهاشمية ·
> 45.500 · مؤسسة أبو أحمد · C-0812 · الزرقاء · 312.000.
>
> **A full-width rectangular button fixed to the bottom of the screen**, flat
> `#1B5FD9`, 8px radius, 52px tall, white 15 Bold text: "عميل جديد". **Not** a
> circular floating action button, and not floating over the last row — the list
> ends above it.

---

## 3. Row anatomy — generate this as a detail sheet

> Design a **specification sheet** showing the five states of a single customer
> row from that screen, stacked vertically at full width with a small Arabic
> caption above each. Same flat style, Almarai, light only.
>
> 1. **عادي** — no balance. Status bar `#D9E1EE`. Balance reads `0.000` in
>    `#6E7C93`, label beneath: "لا يوجد رصيد".
> 2. **عليه ذمم** — owes money. Status bar `#C42F2F`. Balance `128.750` in
>    `#C42F2F` Bold, label: "الرصيد".
> 3. **تمت الزيارة اليوم** — visited today. Status bar `#0B8F58`. Balance in its
>    normal colour, and a small green check line icon before the shop name.
> 4. **تجاوز الحد الائتماني** — over the credit limit. Status bar `#9A5B00`, and
>    the row's background a very faint amber `#FDF6EA`. Balance `312.000` in
>    `#9A5B00` Bold, label: "تجاوز الحد" — this rep must not sell on credit here,
>    so the whole row is tinted rather than just the figure.
> 5. **غير نشط** — inactive shop. Everything at 45% opacity, status bar
>    `#D9E1EE`, and a small "غير نشط" label where the balance label goes.
>
> No gradients, no rounded row corners, no shadows — these are flush list rows.

---

## 4. Filter — a full screen, not a sheet

> Because this app uses **no bottom sheets**, tapping the filter icon opens its
> own screen. Design **تصفية العملاء**.
>
> Top bar: "تصفية العملاء" at the right, and "إلغاء" as a text button at the
> left. Below, grouped sections, each with a small 12sp Bold Arabic header and a
> bordered white block of rows:
>
> - **الحالة** — checkbox rows: عليه ذمم · تجاوز الحد الائتماني · لم تتم زيارته
>   اليوم · غير نشط.
> - **المنطقة** — checkbox rows for areas: الزرقاء · الرصيفة · الهاشمية ·
>   الأزرق, each with its shop count at the end in `#6E7C93`.
> - **المسار** — radio rows: كل العملاء · على مسار اليوم فقط.
> - **الترتيب** — radio rows: الاسم · الأعلى رصيداً · الأحدث زيارة · الأقدم
>   زيارة.
>
> Checkboxes and radios are square 20px with a 4px radius, accent `#1B5FD9` when
> selected. **Two full-width buttons fixed at the bottom, side by side**:
> outlined "مسح الكل" and filled blue "عرض النتائج (٤٨)" — the count updates with
> the selection, so the rep knows before committing.

---

## 5. Search in progress

> Same screen with the search field focused and "سلام" typed. The filter chip row
> is **hidden** while searching — it competes with the query. Results are the same
> rows, with the matched part of each shop name in `#1B5FD9`. A "إلغاء" text
> button appears at the end of the search field.

---

## 6. The four states — generate each separately

Ask for these one at a time. Requested together, Stitch merges them into one
screen and you lose the distinction that matters.

> **A. جارٍ التحميل** — the screen with the top bar and search field in place,
> and in the body six **skeleton rows**: flat `#E8EDF5` rectangles matching the
> row layout, no spinner, no shimmer gradient. The chrome stays put so the screen
> does not jump when content lands.

> **B. لا يوجد عملاء** — no customers at all. Centred in the body: a 48px line
> icon of a shopfront in `#D9E1EE`, then one sentence in 13 `#4A5A73`: "لا يوجد
> عملاء في قائمتك بعد." Then the "عميل جديد" button. No card, no border, no
> illustration.

> **C. لا توجد نتائج** — the search found nothing. **Visibly different from B**:
> the search field stays filled with "سلام", and the body shows one sentence —
> "لا توجد نتائج لـ «سلام»" — plus a text button "مسح البحث". No icon. A rep must
> be able to tell "you have no customers" from "your search matched nothing".

> **D. تعذّر التحميل** — the load failed. A bordered block with a 1px `#9A5B00`
> border and a faint amber `#FDF6EA` fill, containing a warning line icon, the
> sentence "تعذّر تحميل العملاء. تحقق من الاتصال." and a filled blue "إعادة
> المحاولة" button. **This must not look like B or C** — a failure that reads as
> emptiness is how a rep concludes a shop was deleted.

---

## 7. Rejecting what Stitch will try

| It will return | Say this |
|---|---|
| Cards with shadows for each customer | "Use flush list rows with a 1px divider, no cards, no shadow." |
| A circular FAB | "Replace the floating action button with a full-width rectangular button fixed at the bottom." |
| A bottom sheet for filters | "Replace the bottom sheet with a full screen that has its own top bar and a cancel button." |
| Pill-shaped chips | "Chips are rectangles with a 6px radius, not pills." |
| Avatar circles with initials | "Remove the avatars — shop identity is the name and code, not a coloured circle." |
| A dark variant | "There is no dark theme in this product. Light only." |
| Gradients on the status bar or button | "Solid fills only. No gradients anywhere." |
| Latin placeholder names | "Use the real Arabic shop names given in the prompt." |

---

## 8. Why the row is built this way

Worth knowing when judging what comes back, since these are the parts that look
arbitrary but are not:

- **Rows, not cards.** A card costs ~16px of padding and a shadow per shop. On a
  round of 48 shops that is several screens of scrolling bought for decoration.
- **The status bar is at the start edge**, which in RTL is the right — where the
  eye lands first. Colour is doing the scanning, and it must be the first thing
  reached, not the last.
- **Balance has a label under it.** A bare number in a list is ambiguous: is it
  what they owe, what they bought, their limit? One 11sp word removes a question
  the rep would otherwise answer by tapping in.
- **Over-limit tints the whole row.** Every other state colours one element,
  because every other state is information. This one is a prohibition — the rep
  must not sell on credit here — and prohibitions should be impossible to miss
  while scrolling fast.
