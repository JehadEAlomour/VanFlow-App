# Stitch Prompt — Selected Item Bottom Sheet (FlowVan SALE/Cart)

> Paste the **"Stitch Prompt"** block below into Google Stitch. The sections above it are
> the design reference (tokens, data, layout) the prompt is built from — keep them for editing.

---

## 1. Context

A **mobile bottom sheet** for a van-sales POS app. It opens when a salesman taps a product
to add it to (or edit it in) the cart. Single screen, **Arabic-first, RTL layout**, Jordanian
Dinar currency (JOD, suffix **د.أ**). Material 3, rounded, card-based, soft shadows.

The sheet is a full item editor: pick **unit**, set **quantity** (live stock check), optionally
edit **unit price** and apply a **line discount**, see a **live line total**, then **Add / Update / Delete**.

---

## 2. Design Tokens (`Fv`)

| Role | Color | Hex |
|---|---|---|
| Screen bg (behind sheet) | Deep | `#F4F6FB` |
| Sheet / card surface | White | `#FFFFFF` |
| Pills / steppers bg | Surface Top | `#E6EBF4` |
| Primary text | Dark navy | `#0F1A2E` |
| Secondary text | Muted | `#5A6A85` |
| Placeholder | Light | `#A8B3C6` |
| Primary / links | Blue | `#2C6FE4` |
| Success / add | Green | `#0FA968` |
| Warning | Amber | `#B36C00` |
| Error / delete / discount | Red | `#D63B3B` |
| Divider / border | Border | `#E1E6F0` |

**Gradients**
- Hero header: `#EEF4FF → #E6F1FB → #E1F5EE` (subtle, top-to-bottom)
- Add-to-cart button: `#185FA5 → #0C447C`
- Save / confirm button: `#1D9E75 → #0F6E56`
- Line-total card: blue gradient, white text

**Shape:** sheet top corners 28dp; cards 16dp; pills/buttons 12dp. Body font Cairo/Tajawal (Arabic).

---

## 3. Data shown (real fields)

- **Product:** `nameAr`, `sku` (barcode), base `unit`, `salePrice`, `vanStock`, `imageUrl?`, `taxRate` (0.16)
- **Units:** list of `ProductUnit { name, price, conversionQty }` — e.g. حبة ×1, كرتونة ×12
- **Cart line:** `qty`, selected `unit`, `unitPrice`, `discountPct`, live `lineTotal`
- **Stock rule:** `qty × conversionQty` must be ≤ `vanStock` when stock enforcement is on (SALE)

---

## 4. Layout (top → bottom, RTL)

1. **Drag handle** — centered 40×4dp rounded bar, `#E1E6F0`.
2. **Hero header** (gradient `#EEF4FF→E6F1FB→E1F5EE`, rounded):
   - Product image, or 80dp rounded box with the product-name initial on a tinted fill.
   - **`nameAr`** centered, bold, `#0F1A2E`.
   - Blue badge: **price / base unit** → `د.أ 2.50 / حبة`.
   - SKU badge pinned top-start (RTL top-right): small pill, muted.
3. **Quantity stepper** — label **"الكمية"**. Row: circular **−**, large bold qty, circular **+**.
   `+` disabled (grayed) when next qty would exceed stock.
4. **Unit selector** — dropdown. Selected shows `name ×conversionQty` when conversion ≠ 1
   (e.g. **كرتونة ×12**). Opens list of all units. Changing unit reseeds the price.
5. **Stock panel** (SALE only) — thin card: "المتوفر بالمركبة" vs requested base units.
   Green when OK, **red** with warning text when requested exceeds `vanStock`.
6. **Unit price field** — suffix **د.أ**.
   - Editable input if salesman has price permission.
   - Otherwise read-only blue badge showing the unit's price.
7. **Line discount** (only if discount permission) — segmented toggle **% | د.أ**, then an
   amount input whose suffix matches the toggle. Tint accents in **red** `#D63B3B`.
8. **Line total card** — blue gradient, full width, white text. Label **"الإجمالي"** + big
   amount **`د.ا 25.00`**. Recalculates live from qty × price − discount (+ tax per line).
9. **Action row** (bottom, sticky):
   - **Delete** (red, only when editing an existing line) — trash icon.
   - **Cancel** (gray outline).
   - **Add / Update** (green gradient, cart icon) — text **"أضف للسلة"** / **"تحديث البند"**.
     Disabled/grayed when qty = 0 or stock exceeded.

---

## 5. States to generate

- **Add (new line)** — no Delete button, button reads "أضف للسلة".
- **Edit (existing line)** — Delete shown, button reads "تحديث البند".
- **Stock exceeded** — red stock panel, disabled Add button.
- **Multi-unit** — unit dropdown open showing حبة ×1 / كرتونة ×12.
- **Discount applied** — % toggle active, total reflects the discount.

---

## 6. Stitch Prompt (copy this)

```
Design a mobile bottom sheet for an Arabic-first (RTL) van-sales POS app, Material 3 style,
currency Jordanian Dinar with the suffix "د.أ". The sheet adds/edits a product line in a cart.

Layout top to bottom:
1. Centered drag handle.
2. Gradient hero header (#EEF4FF to #E6F1FB to #E1F5EE) with an 80dp rounded product image
   placeholder, the Arabic product name "حليب نيدو 1 كغ" centered in bold dark navy (#0F1A2E),
   a blue pill showing "د.أ 2.50 / حبة", and a small SKU pill "SKU 100245" in the top-right.
3. A quantity row labeled "الكمية" with a circular minus button, a large bold number, and a
   circular plus button. Stepper background #E6EBF4.
4. A unit selector dropdown showing "كرتونة ×12".
5. A thin stock card: "المتوفر بالمركبة: 48" on the right, requested "24 حبة" on the left, in
   green when within stock.
6. A unit price input field with suffix "د.أ" and value 2.50.
7. A line discount control: a segmented toggle "% | د.أ" (percent selected, accent red #D63B3B)
   and an amount input with suffix "%".
8. A full-width blue gradient (#185FA5 to #0C447C) total card with white text: label "الإجمالي"
   on one side and "د.ا 25.00" big on the other.
9. A bottom action row: a red outline "حذف" button with a trash icon, a gray outline "إلغاء"
   button, and a wide green gradient (#1D9E75 to #0F6E56) "أضف للسلة" button with a cart icon.

Colors: surface white #FFFFFF, screen bg #F4F6FB, primary text #0F1A2E, secondary #5A6A85,
primary blue #2C6FE4, success green #0FA968, error red #D63B3B, border #E1E6F0. Rounded corners:
sheet 28dp, cards 16dp, buttons 12dp. Soft shadows. Arabic font (Cairo/Tajawal), full RTL.

Generate two variants: (a) "Add to cart" state, and (b) "Edit line" state where the primary
button reads "تحديث البند" and the delete button is visible.
```
