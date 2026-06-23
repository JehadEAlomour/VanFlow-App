# Offers — Tester Guide (FlowVan app)

How to test the **Offers** feature in a SALE. The cart is **server-fed**: on every cart
change the app calls `/offers/evaluate` and **displays the server's computed result** (per-line
discounts, free/gift lines, and the totals) as the cart. The **server is authoritative** and
re-applies on upload, so the on-screen cart matches the posted voucher. Offline, the app falls
back to on-device totals so the rep can still sell.

## Payment-first flow (NEW)

- The SALE screen opens with a **blocking Cash/Credit chooser**. The rep **cannot add items**
  until a payment method is chosen — this guarantees offers (esp. payment-method discounts)
  evaluate correctly from the **first** item.
- The chosen method is shown later as a small **Cash/Credit toggle at the top of the cart**
  and stays **editable**; changing it re-evaluates offers (the toggle is part of the evaluate
  key alongside cart + gift picks).
- At save there is no second payment prompt — only a confirm dialog showing the method + total.

## No manual discounts (NEW)

- The manual **line discount** (in the add-item sheet) and the **voucher/invoice discount**
  inputs in the summary have been **removed**. All discounts now come from **offers** only,
  computed by the server. The local save passes `discountAmount = 0`.

## Server-fed cart (NEW)

- **Online** (`offersFromServer = true`): the cart lines, per-line discounts, and the totals
  card (subtotal / offers discount / tax / final total) are **exactly what the server returned**
  — not on-device math. Each line shows its server unit price × qty, net, and a green
  "- discount" chip when the offer discounted it.
- **Offline** (`offersFromServer = false`, evaluate failed): an amber **"غير متصل"** banner
  appears; the cart + totals fall back to the on-device `InvoiceTaxCalculator` (no offers).
  The sale is never blocked. The server re-applies offers authoritatively on sync.

## Preconditions

- Backend reachable (Settings → خادم النظام) and you are logged in.
- The backend has active offers configured for the test customer / store. The app calls:
  - `POST /api/v1/offers/evaluate` on every cart change (debounced ~300 ms).
  - `GET /api/v1/offers/active` for offline caching.
- Open a customer → **بيع / SALE**, **choose Cash or Credit** in the opening chooser, then add
  items and switch to the **cart view** (cart icon, top-left) — the Cash/Credit toggle, offer
  banners, FREE lines, server-fed lines, and totals card all render there.

## What "applied" looks like

- **Applied-offers banner** (green, 🎁 "العروض المطبقة") above the cart lines, one chip per
  applied offer with its name + summary.
- **FREE lines**: a green card with a **"هدية / FREE"** badge, the original unit price
  **struck through**, qty shown, **net 0** (no qty editor, read-only).
- **Totals card** (tap to expand): the **subtotal**, a green **"العروض"** discount row
  (= the server's `totalDiscount`), the **tax**, and the **final total** — all read from the
  server's `totals` when online (from the on-device calculator when offline). The final total
  never goes below 0.
- A brief "…" appears next to the banner title while an evaluation is in flight
  (`isEvaluatingOffers`).

> The displayed cart + totals are the **server's** result (display layer). The local save still
> uploads the raw cart (itemNumber/qty/unitPrice) + paymentMethod + chosenFreeItems with
> `discountAmount = 0`; the server re-applies the offer and is the final arbiter.

---

## Type-by-type test matrix

### 1. ITEM_QTY_DISCOUNT — "buy ≥ N of A, A gets a discount"
1. Add item **A** below the threshold qty → no offer.
2. Increase A to **≥ N**.
3. **Expect:** within ~300 ms, the banner shows the offer; the **العروض** row appears in
   the totals with A's line discount; final total drops accordingly.
4. Reduce A below N → discount and banner chip **auto-removed**.

### 2. BUY_X_GET_Y_FREE — "buy ≥ N of A → free B"
1. Add item **A** to **≥ N**.
2. **Expect:** a **FREE line for B** appears (green "هدية / FREE" badge, price struck,
   net 0) and a banner chip. Cart total is unchanged by the free item (net 0).
3. Reduce A below N → the FREE line **disappears** automatically.

### 3. BASKET_THRESHOLD — invoice discount **or** "choose your free item"
1. Add items until the basket reaches the threshold.
2a. **Discount variant:** banner chip + **العروض** invoice discount row; total drops.
2b. **Choice variant:** a **"اختر هديتك" bottom sheet** appears listing the choices.
   - Tap a choice → it is **selected** (✓, highlighted) and recorded as a gift pick. The
     pick is sent to the server as `chosenFreeItems`; on the next re-eval the chosen item
     comes back as a **FREE line / net 0**. It is **NOT** added as a normal cart line.
   - Tap a selected choice again → it deselects.
   - Cancel/Done → sheet dismissed; you can re-open by editing the cart.

> Note: gift picks are tracked in state as a flat `chosenFreeItems` list and sent on both
> evaluate and upload. The server validates them against the offer's gift pool and is the
> final arbiter.

### 4. ITEM_SET_THRESHOLD — selected items X/Y/Z reach qty (any/all)
1. Add the set items to the configured quantities.
2. **Expect:** discount (العروض row) or a FREE line, plus a banner chip — same rendering
   as types 1/2 depending on the reward.
3. Break the set (remove/reduce an item) → reward auto-removed.

### 5. LOYALTY_FIRST_PURCHASE — new customer's first sale
1. Use a customer with **no prior posted sales**.
2. Add any qualifying cart.
3. **Expect:** the banner shows the loyalty offer (invoice discount in the العروض row,
   or a FREE line). On a returning customer, nothing applies.

### 6. ITEM_QTY_REWARD — "buy N of selected items → a GIFT you pick (or a % discount)"
This offer has two reward variants:

**GIFT variant (rep picks the gift):**
1. Add the selected trigger item(s) to the configured qty **N**.
2. **Expect:** the **"اختر هديتك" bottom sheet** opens listing the gift pool. If the offer
   grants more than one gift, the header shows **"اختر K (picked/K)"** — pick up to **K**
   items (multi-select). Selected items show a ✓ and a highlighted border; tapping a
   selected item deselects it. Picking past the quota evicts your oldest pick for that offer.
3. Each picked gift comes back from the server as a **FREE line / net 0** (green
   "هدية / FREE" card), plus a banner chip. The trigger lines are charged normally.
4. Reduce the trigger below N (or empty the pool) → the gift entitlement disappears and any
   now-invalid picks are **auto-pruned**.
5. **Sync:** the picks ride along on upload (see Save/sync) so the posted voucher carries
   the same gifts the preview showed.

**% discount variant:**
1. Add the selected trigger item(s) to qty **N**.
2. **Expect:** no gift sheet — instead the selected lines get a per-line discount that lands
   in the **العروض** row (same rendering as type 1). No app-side gift handling needed.

---

## Totals / rounding check

- Expand the totals card. **Online:** verify `subtotal − offersDiscount + tax = final total`,
  where every number is the server's (`totals.subtotalFils` … `totals.grandTotalFils`,
  converted to JOD). Free lines do not move the total (full price + 100% discount = net 0).
- **Offline:** the same arithmetic holds but is computed on-device by `InvoiceTaxCalculator`
  with **no offers** applied.

## Offline behaviour

- Turn off connectivity. Editing the cart still works; the evaluate call fails silently. The
  state flips to `offersFromServer = false`:
  - the offer banner / free lines / offers row **clear**,
  - an amber **"غير متصل"** banner appears,
  - the cart lines + totals card switch to **on-device** values (no offers, no crash, sale
    never blocked).
- Re-enabling connectivity and editing the cart re-evaluates and restores the server-fed cart.
- **TODO (not yet implemented):** full offline preview from the cached `GET /offers/active`
  list. Currently offline shows the local no-offers total until back online; the server remains
  the final arbiter at promotion on sync.

## Save / sync

- Save the voucher as usual. The cart **plus the rep's gift picks** (`chosenFreeItems`) is
  what posts; the **server applies the offer**, adds the free lines, records the redemption,
  and returns the authoritative voucher. Gift picks are persisted locally on the invoice
  (`invoices.chosenFreeItemsCsv`, DB v6) so they survive an offline save and ride the
  `POST /sync/vouchers` body when connectivity returns.
- If an offer no longer qualifies at post time, the posted voucher won't include it (the app
  reconciles on the next catalog/voucher refresh).

> **Verify a GIFT sale end-to-end:** pick a gift, save, then confirm on the server side that
> the posted voucher contains the chosen item as a free line and a redemption was recorded.
