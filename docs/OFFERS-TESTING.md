# Offers — Tester Guide (FlowVan app)

How to test the **Offers** feature in a SALE. The app shows a **live preview** of offers
on every cart change; the **server is authoritative** and re-applies on upload, so the
on-screen preview must match the posted voucher.

## Preconditions

- Backend reachable (Settings → خادم النظام) and you are logged in.
- The backend has active offers configured for the test customer / store. The app calls:
  - `POST /api/v1/offers/evaluate` on every cart change (debounced ~300 ms).
  - `GET /api/v1/offers/active` for offline caching.
- Open a customer → **بيع / SALE** to reach the sale screen. Add items, then switch to the
  **cart view** (cart icon, top-left) — offer banners, FREE lines, and the offers discount
  row all render in the cart view + totals card.

## What "applied" looks like

- **Applied-offers banner** (green, 🎁 "العروض المطبقة") above the cart lines, one chip per
  applied offer with its name + summary.
- **FREE lines**: a green card with a **"هدية / FREE"** badge, the original unit price
  **struck through**, qty shown, **net 0** (no qty editor, read-only).
- **Totals card** (tap to expand): an extra green **"العروض"** row = per-line offer
  discounts + invoice-level offer discount. The **final total** never goes below 0.
- A brief "…" appears next to the banner title while an evaluation is in flight
  (`isEvaluatingOffers`).

> Offer discounts are **display-only**. They are NOT written into the local voucher as a
> manual discount; the cart (incl. any chosen free item) is what syncs, and the server
> re-applies the offer.

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
   - Tap a choice → the sheet closes, the chosen item is **added to the cart as a normal
     line (qty 1)**, and a re-evaluation runs. The server treats the chosen item as free,
     so after re-eval it shows as a FREE line / net 0.
   - Cancel → sheet dismissed; you can re-trigger by editing the cart.

> Note: the device is **stateless** — picking a free item just adds a cart line. No
> separate `chosenFreeItems` field is sent; the server re-evaluates from the cart lines.

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

---

## Totals / rounding check

- Expand the totals card. Verify:
  `subtotal − (line discounts + voucher discount + offer line discounts + offer invoice
  discount) + tax = final total`, and the final total equals what the server returns on
  post. Free lines do not move the total (full price + 100% discount = net 0).

## Offline behaviour

- Turn off connectivity. Editing the cart still works; the evaluate call fails silently —
  the offer banner/free lines/offers row **clear** (no crash, sale never blocked). The
  spinner clears. Re-enabling connectivity and editing the cart re-evaluates.
- **TODO (not yet implemented):** full offline preview from the cached `GET /offers/active`
  list. Currently offline simply shows no offers until back online; the server remains the
  final arbiter at promotion on sync.

## Save / sync

- Save the voucher as usual. The cart (including any chosen free item line) is what posts;
  the **server applies the offer** and returns the authoritative voucher. If an offer no
  longer qualifies at post time, the posted voucher won't include it (the app reconciles on
  the next catalog/voucher refresh).
