# ERP ⇄ Cash-Van Integration — **Mobile (FlowVan) Spec**

> TL;DR: the app needs **almost no changes**. It already syncs to the cash-van backend; when ERP
> mode is on, the BE simply *sources* the catalog/stock from the ERP and *relays* the app's
> vouchers/collections onward. The phone never talks to the ERP.
> Hub spec: `cash-van-dashboard/docs/ERP-SYNC.md` · ERP spec: `ERP/docs/CASHVAN-INTEGRATION.md`.

## Why it's transparent
- **Catalog/units/stock** already arrive via the app's existing catalog refresh (`RefreshCatalogUseCase` → products, units, stock from the BE). When ERP mode is on, those rows are just ERP-sourced — same shape, same endpoints, **no new sync path**.
- **Vouchers/collections** already post through `POST /sync/vouchers` / collections to the BE. The BE's ERP **outbox** forwards them to the ERP. The app's posting path is unchanged.
- The app's voucher number is kept end-to-end (server no longer reassigns), so the number the salesman sees is the one that reaches the ERP. (See `returns-reference-original-sale` note.)

## What to confirm / small adds
1. **Read-only catalog when ERP-managed.** The salesman must not create/edit items or units locally (the ERP owns them). Confirm the "add item / edit unit" actions are gated by the existing permission (`canAddItems`) and simply stay hidden when the admin hasn't granted it. No ERP-specific flag is needed on the phone — the BE stops serving local item-write endpoints when ERP mode is on (returns 409); the app should surface that gracefully if ever hit.
2. **Stock is ERP-authoritative.** Van stock shown in the app comes from the BE (which mirrors ERP). Local stock decrement on sale stays as an **offline display** convenience; the truth re-syncs on the next catalog refresh. Ensure `enforceStock`/stock badges read the synced figure, and a refresh after sync updates them.
3. **Salesman summary & end-of-day already exist** (`EndOfDayScreen`, home KPIs). They feed the BE; the BE pushes the end-of-day reconciliation to the ERP. No app change required. *(Optional nicety: a small "synced to ERP ✓" indicator on the end-of-day screen, driven by a BE flag.)*
4. **Offline still works.** Create sales/returns/collections offline as today; they queue and post when back online; the BE then relays to the ERP. The app needs no awareness of the ERP.

## Net change to the app
- **Code:** none required for the sync itself. At most: graceful handling of a 409 "managed by ERP" on any item/unit write, and (optional) an end-of-day "synced" badge.
- **Behaviour:** when the admin turns ERP mode on, new ERP items/units appear in the app's catalog (so they're usable on a voucher) on the next refresh, and stock reflects ERP values.

## Acceptance
- With ERP mode on, an item added in the ERP shows up in the app's product picker after a catalog refresh and can be sold.
- A sale/return/collection created on the phone still posts via the existing sync and is not blocked.
- No item/unit creation is offered to a salesman without `canAddItems`; a server 409 (ERP-managed) is shown as a friendly message, not a crash.
