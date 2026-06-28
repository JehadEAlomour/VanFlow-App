# CMP Mobile — VanFlow API Reference

Complete request/response reference for the **CMP mobile app** (cash-van sales reps).
Every endpoint the mobile client can call: how to call it, what to send, and what comes back.

> Backend: NestJS (VanFlow). Generated from the live controllers — keep in sync with `src/modules/**`.
> Interactive Swagger UI is served at **`/docs`** in non-production environments.

---

## Table of contents

1. [Conventions](#1-conventions) — base URL, auth, envelopes, errors, money, pagination
2. [Auth](#2-auth)
3. [Users](#3-users)
4. [Settings](#4-settings) · [Year Config](#5-year-config)
6. [Reps](#6-reps) · [Rep Locations (GPS)](#7-rep-locations-gps) · [Van Stock](#8-van-stock)
9. [Regions](#9-regions) · [Routes](#10-routes)
11. [Customers](#11-customers) · [Vendors](#12-vendors) · [Warehouses](#13-warehouses)
14. [Products](#14-products) · [Product Categories](#15-product-categories) · [Price Rules](#16-price-rules)
17. [Items](#17-items)
18. [Invoices](#18-invoices) · [Vouchers](#19-vouchers)
20. [Collections](#20-collections) · [Cheques](#21-cheques)
22. [Credit Notes](#22-credit-notes) · [JoFotara (ISTD)](#23-jofotara-istd) · [Tax Reporting](#24-tax-reporting)
25. [Audit Log](#25-audit-log) · [Notification Rules](#26-notification-rules)
27. [Health](#27-health) · [Realtime WebSocket](#28-realtime-websocket)

---

## 1. Conventions

### Base URL

```
http://<host>:3000/api/v1
```

All routes are prefixed with `/api` and URI-versioned at `v1`. Examples below use `http://localhost:3000`; replace with your server host.

### Authentication

Every endpoint **except** `POST /auth/login` and `GET /health` requires a bearer JWT:

```
Authorization: Bearer <accessToken>
```

Get the token from `POST /auth/login`. The JWT payload carries the user's `role`
(`admin | manager | supervisor | viewer`) and `permissions` flags, which drive
endpoint-level access. Endpoints below note `Roles: ...` or a required permission
when access is restricted; otherwise any authenticated user may call them.

### Success envelope

Every `2xx` JSON response is wrapped:

```json
{
  "success": true,
  "data": <PAYLOAD>,
  "timestamp": "2026-06-01T08:00:00.000Z"
}
```

The **`data`** field holds the payload documented per endpoint. `204 No Content`
responses (most `DELETE`s) have no body. A few endpoints return a **binary file**
(XLSX / CSV) instead of the envelope — flagged inline.

### Error envelope

Every non-`2xx` response:

```json
{
  "statusCode": 400,
  "message": "validation failed",
  "error": "Bad Request",
  "path": "/api/v1/...",
  "timestamp": "2026-06-01T08:00:00.000Z"
}
```

Standard codes across all endpoints:

| Code | Meaning |
|---|---|
| `400` | Validation failed / malformed request |
| `401` | Missing or invalid bearer token |
| `403` | Authenticated but not permitted (role/permission) |
| `404` | Resource not found |
| `409` | Conflict (e.g. duplicate unique value) |
| `429` | Rate-limited (login throttle) |
| `500` | Unexpected server error |

### Money & numbers

- Monetary values in **invoices, collections, products, KPIs, tax** are integer **fils** (1 JOD = 1000 fils) unless noted.
- **Vouchers, vendors, warehouses, year-config** return monetary fields as **numeric strings** (e.g. `"1.250"`).
- IDs are UUIDs unless noted (`route_stops.id`, GPS event ids and cheque/voucher line ids are bigint serialized as strings).

### Pagination

Two list styles are used:

- **offset/limit** (`?limit=&offset=`) → `data: { items: [...], total }`
- **page/limit** (`?page=&limit=&search=`) → `data: { items: [...], total, page, limit, pages }`

Each endpoint states which it uses.

---

## 2. Auth

### `POST /api/v1/auth/login`
Authenticate by `userNumber` + password; returns a JWT and the user profile. **Public** — the only endpoint with no `Authorization` header. Rate-limited.

**Request body**
```ts
{
  userNumber: string;  // 1–32 chars, e.g. "U-0001" / "admin"
  password: string;    // 6–128 chars
}
```

**Response `data`**
```ts
{
  accessToken: string;          // JWT — send as Authorization: Bearer <jwt>
  user: {
    id: string;
    userNumber: string;
    name: string;
    userType: "ADMIN" | "MANAGER" | "SALES" | "DRIVER";
    role: "admin" | "manager" | "supervisor" | "viewer";
    permissions: {
      canMakeVoucher: boolean;
      canEditVoucher: boolean;
      canAddCustomer: boolean;
      canEditCustomerCredit: boolean;
      canAddItems: boolean;
      canEditExpiry: boolean;
    };
  };
}
```

**Example**
```bash
curl -X POST http://localhost:3000/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"userNumber":"admin","password":"admin1234"}'
```

### `GET /api/v1/auth/me`
Returns the authenticated user decoded from the bearer JWT. Any authenticated user.

**Response `data`**
```ts
{
  sub: string;                  // user id
  userNumber: string;
  userType: string;
  role: string;
  permissions: Record<string, boolean>;
}
```

**Example**
```bash
curl http://localhost:3000/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
```

---

## 3. Users

All endpoints require a bearer token (no role restriction on this controller).

**`User` shape** (returned by create/get/update):
```ts
{
  id: string;
  userNumber: string;
  name: string;
  userType: "ADMIN" | "MANAGER" | "SALES" | "DRIVER";
  isActive: boolean;
  canMakeVoucher: boolean;
  canEditVoucher: boolean;
  canAddCustomer: boolean;
  canEditCustomerCredit: boolean;
  canAddItems: boolean;
  canEditExpiry: boolean;
  createdAt: string;
  updatedAt: string;
}
```

### `POST /api/v1/users`
Create a user account with login code, password and permission flags. Returns `201`.

**Request body**
```ts
{
  userNumber: string;           // required, 1–32
  name: string;                 // required, 2–120
  password: string;             // required, 6–128
  userType?: "ADMIN" | "MANAGER" | "SALES" | "DRIVER";  // default "SALES"
  isActive?: boolean;           // default true
  canMakeVoucher?: boolean;
  canEditVoucher?: boolean;
  canAddCustomer?: boolean;
  canEditCustomerCredit?: boolean;
  canAddItems?: boolean;
  canEditExpiry?: boolean;
}
```
**Response `data`** — `User`.

```bash
curl -X POST http://localhost:3000/api/v1/users \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"userNumber":"U-0002","name":"Ahmad Sales","password":"SuperSecret#1","userType":"SALES"}'
```

### `GET /api/v1/users`
Paginated list of users. **page/limit pagination.**

**Query params**
| Name | Type | Required | Notes |
|---|---|---|---|
| `page` | int | no | ≥ 1, default 1 |
| `limit` | int | no | 1–200, default 25 |
| `search` | string | no | free-text |

**Response `data`** — `{ items: User[], total, page, limit, pages }`

```bash
curl "http://localhost:3000/api/v1/users?page=1&limit=25&search=ahmad" -H "Authorization: Bearer $TOKEN"
```

### `GET /api/v1/users/:id`
Fetch one user. Path param `id` (uuid). **Response `data`** — `User`.

### `PATCH /api/v1/users/:id`
Update fields/permissions (`userNumber` and `password` not changeable here).

**Request body** (all optional)
```ts
{
  name?: string;                // 2–120
  userType?: "ADMIN" | "MANAGER" | "SALES" | "DRIVER";
  isActive?: boolean;
  canMakeVoucher?: boolean;
  canEditVoucher?: boolean;
  canAddCustomer?: boolean;
  canEditCustomerCredit?: boolean;
  canAddItems?: boolean;
  canEditExpiry?: boolean;
}
```
**Response `data`** — `User`.

### `PATCH /api/v1/users/:id/password`
Reset/change a password. Returns `204`.

**Request body** — `{ newPassword: string }` (6–128).

### `DELETE /api/v1/users/:id`
Soft-delete a user. Returns `204`.

---

## 4. Settings

Single-row company/app settings. **Admin only.** The JoFotara secret is never returned in plaintext.

### `GET /api/v1/settings`
Returns current settings (secret masked).

**Response `data`**
```ts
{
  companyNameAr: string;
  companyNameEn: string | null;
  sellerTin: string | null;
  sellerAddress: string | null;
  sellerPhone: string | null;
  sellerCityCode: string | null;   // e.g. "JO-AM"
  timezone: string;                 // default "Asia/Amman"
  locale: string;                   // default "ar"
  aiChatQuota: number;
  aiInferQuota: number;
  jofotara: { clientId: string | null; secretLast4: string | null; sandbox: boolean; isConfigured: boolean };
  updatedAt: string;
  updatedBy: string | null;
}
```

### `PATCH /api/v1/settings`
Update non-secret settings. Returns same shape as `GET`.

**Request body** (all optional)
```ts
{
  companyNameAr?: string;  // max 255
  companyNameEn?: string;  // max 255
  sellerTin?: string;      // max 64
  sellerAddress?: string;  // max 512
  sellerPhone?: string;    // max 64
  sellerCityCode?: string; // max 16
  timezone?: string;       // max 64
  locale?: string;         // max 8
  aiChatQuota?: number;    // int >= 0
  aiInferQuota?: number;   // int >= 0
}
```

### `PATCH /api/v1/settings/jofotara`
Set/rotate ISTD JoFotara credentials (secret encrypted before storage).

**Request body**
```ts
{ clientId: string; secretKey: string; sandbox?: boolean }  // sandbox default true
```
**Response `data`** — `{ clientId, secretLast4, sandbox, updatedAt }`.

---

## 5. Year Config

Fiscal-year configuration. Bearer token required (no role restriction). Monetary fields are numeric strings.

**`YearConfig` shape**
```ts
{
  id: string;
  year: number;
  accName: string;
  accValue: string;   // numeric string
  totalSale: string;
  totalD: string;     // total debit
  totalR: string;     // total credit/receipts
  createdAt: string; updatedAt: string; deletedAt: string | null; version: number;
}
```

### `POST /api/v1/year-config`
Create an entry. Returns `201`.
**Request body** — `{ year: number (1900–2999), accName: string (1–120), accValue?, totalSale?, totalD?, totalR? }` (numeric strings, default `"0"`).

### `GET /api/v1/year-config`
List all entries. **Response `data`** — `YearConfig[]`.

### `GET /api/v1/year-config/year/:year`
List entries for a specific year (path param `year`). **Response `data`** — `YearConfig[]`.

### `PATCH /api/v1/year-config/:id`
Update (`year`, `accName` not changeable). Body: `{ accValue?, totalSale?, totalD?, totalR? }`.

### `DELETE /api/v1/year-config/:id`
Delete. Returns `204`.

---

## 6. Reps

**`Rep` shape**
```ts
{
  id: string;
  userId: string | null;
  nameAr: string;
  nameEn: string | null;
  phone: string | null;
  regionId: string | null;
  vanId: string | null;
  isActive: boolean;
  hireDate: string | null;        // YYYY-MM-DD
  dailyQuotaFils: number | null;  // 1 JOD = 1000 fils
  createdAt: string; updatedAt: string; deletedAt: string | null; version: number;
}
```

### `GET /api/v1/reps`
List reps with filters. **offset/limit pagination.**

**Query params**
| name | type | required | notes |
|---|---|---|---|
| `regionId` | uuid | no | filter by region |
| `isActive` | boolean | no | filter by active status |
| `q` | string | no | substring on name_ar/name_en/phone |
| `limit` | int | no | 1–200, default 50 |
| `offset` | int | no | ≥ 0, default 0 |

**Response `data`** — `{ items: Rep[], total }`.

```bash
curl "http://localhost:3000/api/v1/reps?isActive=true&limit=50" -H "Authorization: Bearer $TOKEN"
```

### `GET /api/v1/reps/:id`
One `Rep` by id.

### `GET /api/v1/reps/:id/kpis`
KPI snapshot. **Response `data`** — `{ todayRevenueFils, routeCompletionPct, invoicesToday, customersAtRisk }`.

### `POST /api/v1/reps`
Create. Roles: `admin`, `manager`.
**Request body**
```ts
{
  nameAr: string;          // required
  nameEn?: string;
  phone?: string;
  userId?: string;
  regionId?: string;
  vanId?: string;
  isActive?: boolean;      // default true
  hireDate?: string;       // YYYY-MM-DD
  dailyQuotaFils?: number; // int >= 0
}
```

### `PATCH /api/v1/reps/:id`
Update (same fields, all optional). Roles: `admin`, `manager`.

### `DELETE /api/v1/reps/:id`
Soft-delete. Roles: `admin`. Returns `204`.

---

## 7. Rep Locations (GPS)

### `POST /api/v1/reps/:id/location`
Record a single GPS ping (foreground tracking).
**Request body**
```ts
{ lat: number; lng: number; accuracyM?: number; recordedAt?: string } // lat -90..90, lng -180..180
```
**Response `data`** — `{ id, repId, lat, lng, accuracyM, recordedAt }`.

```bash
curl -X POST http://localhost:3000/api/v1/reps/$REP/location \
  -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"lat":31.95,"lng":35.91,"accuracyM":12}'
```

### `POST /api/v1/reps/:id/location/bulk`
Bulk offline-flush, up to 500 points.
**Request body** — `{ points: { lat, lng, accuracyM?, recordedAt? }[] }` (1–500).
**Response `data`** — `{ accepted: number }`.

### `GET /api/v1/reps/locations/latest`
Latest ping per active rep (last 24h) — powers the Live Map.
**Response `data`**
```ts
{
  repId: string; nameAr: string; nameEn: string | null;
  lat: number; lng: number; accuracyM: number | null; recordedAt: string;
  status: 'online' | 'idle' | 'offline';  // online ≤5min, idle 5–30min, offline >30min
}[]
```

### `GET /api/v1/reps/:id/locations`
Replay a rep's GPS trail in a window (ASC).
**Query params** — `from?` (ISO, default now−24h), `to?` (ISO, default now), `limit?` (1–10000, default 1000).
**Response `data`** — `{ id, repId, lat, lng, accuracyM, recordedAt }[]`.

### `GET /api/v1/reps/:id/locations.geojson`
Same window as above, returned as a GeoJSON FeatureCollection (single LineString, `[lng, lat]`).
**Response `data`** — `{ type, features: [{ type, geometry: { type: 'LineString', coordinates }, properties: { repId, from, to, pointCount } }] }`.

---

## 8. Van Stock

### `GET /api/v1/reps/:repId/van-stock`
Current van stock for a rep, with stockout flags.
**Response `data`**
```ts
{
  productId: string; sku: string; nameAr: string;
  quantity: number; reorderQty: number;
  status: 'sufficient' | 'borderline' | 'stockout';
  snapshotAt: string;
}[]
```

### `POST /api/v1/reps/:repId/van-stock/load`
Load products onto a van (adds qty). Roles: `admin`, `manager`.
**Request body** — `{ items: { productId: string; quantity: number }[] }` (1–500, qty ≥ 1).
**Response `data`** — `{ updated: number }`.

### `POST /api/v1/reps/:repId/van-stock/return`
Return products from a van (subtracts, clamped at 0). Roles: `admin`, `manager`.
**Request body / Response** — same as load.

---

## 9. Regions

**`Region` shape** — `{ id, nameAr, nameEn|null, boundary: GeoJSON.Polygon|null, isActive, createdAt, updatedAt, deletedAt, version }`.

### `GET /api/v1/regions`
List regions. **offset/limit pagination.**
**Query params** — `isActive?`, `q?` (substring on nameAr/nameEn), `limit?` (1–200, default 50), `offset?` (default 0).
**Response `data`** — `{ items: Region[], total }`.

### `GET /api/v1/regions/containing`
Find the active region whose polygon contains a point (404 if none).
**Query params** — `lat` (required), `lng` (required).
**Response `data`** — one `Region`.

```bash
curl "http://localhost:3000/api/v1/regions/containing?lat=31.9539&lng=35.9106" -H "Authorization: Bearer $TOKEN"
```

### `GET /api/v1/regions/:id`
One `Region` by id.

### `POST /api/v1/regions`
Create. Roles: `admin`, `manager`.
**Request body** — `{ nameAr: string; nameEn?: string; boundary?: GeoJSON.Polygon; isActive?: boolean }`.

### `PATCH /api/v1/regions/:id`
Update (all optional; pass `boundary` to replace). Roles: `admin`, `manager`.

### `DELETE /api/v1/regions/:id`
Soft-delete (reps/users in the region get `region_id = NULL`). Roles: `admin`. Returns `204`.

---

## 10. Routes

**`RoutePlan` shape**
```ts
{
  id: string; repId: string; planDate: string; // YYYY-MM-DD
  source: 'manual' | 'ai_optimized';
  aiEstDistance: number | null; aiEstDuration: number | null; aiSavingsMin: number | null;
  acceptedAt: string | null; createdAt: string; updatedAt: string;
  stops: RouteStop[];
}
// RouteStop
{
  id: string; planId: string; customerId: string; stopOrder: number;
  estArrival: string | null; estDurationMin: number;
  actualArrival: string | null; actualDeparture: string | null;
  status: 'pending' | 'visited' | 'skipped'; skipReason: string | null;
}
```

### `GET /api/v1/routes`
List route plans (with stops). **Query params** — `date?` (YYYY-MM-DD), `repId?` (uuid). **Response `data`** — `RoutePlan[]`.

### `GET /api/v1/routes/compliance`
Stop-completion % per rep for a date.
**Query params** — `date` (required, YYYY-MM-DD).
**Response `data`** — `{ repId, planId, totalStops, visited, skipped, pending, completionPct }[]`.

### `GET /api/v1/routes/:id`
One `RoutePlan` with stops.

### `POST /api/v1/routes`
Create a manual plan. Roles: `admin`, `manager`. `409` if a plan already exists for that rep+date.
**Request body**
```ts
{
  repId: string;
  planDate: string;          // YYYY-MM-DD
  stops: { customerId: string; stopOrder?: number; estDurationMin?: number }[];  // 1–200
}
```

### `POST /api/v1/routes/generate`
Generate optimized plans (nearest-neighbor) for reps. Roles: `admin`, `manager`. Replaces existing plan per rep+date.
**Request body** — `{ repIds: string[]; planDate: string }`.
**Response `data`** — `RoutePlan[]` (`source='ai_optimized'`).

### `PATCH /api/v1/routes/:id/stops/reorder`
Reorder stops. Roles: `admin`, `manager`.
**Request body** — `{ order: { stopId: string; stopOrder: number }[] }`.

### `POST /api/v1/routes/:id/accept`
Rep accepts an AI-optimized plan (sets `acceptedAt`). No body. **Response `data`** — `RoutePlan`.

### `POST /api/v1/routes/stops/:stopId/visit`
Mark a stop visited.
**Request body** — `{ actualArrival?: string; actualDeparture?: string }` (ISO; arrival defaults to now). **Response `data`** — `RouteStop`.

### `POST /api/v1/routes/stops/:stopId/skip`
Mark a stop skipped with a reason.
**Request body** — `{ reason: string }` (1–500). **Response `data`** — `RouteStop`.

---

## 11. Customers

**`Customer` shape**
```ts
{
  id: string; customerNumber: string; customerName: string;
  nameAr: string; nameEn: string | null; phone: string | null;
  addressAr: string | null; city: string | null; cityCode: string | null;
  location: string | null; longitude: string | null; latitude: string | null;
  repId: string | null; regionId: string | null; category: string | null;
  creditLimit: string; paymentTerms: number;
  customerType: 'CASH' | 'CREDIT' | 'WHOLESALE' | 'RETAIL';
  totalDebt: string; totalCredit: string;
  tin: string | null; nin: string | null; passportNumber: string | null;
  isActive: boolean;
  createdAt: string; updatedAt: string; deletedAt: string | null; version: number;
}
// phoneHash is never returned.
```

### `GET /api/v1/customers`
List customers. **offset/limit pagination.**
**Query params**
| name | type | required | notes |
|---|---|---|---|
| `q` | string | no | substring on nameAr/nameEn/customerNumber |
| `segment` | string | no | RFM segment (requires AI profile) |
| `churnRisk` | `loyal`\|`at_risk`\|`high_risk` | no | requires AI profile |
| `regionId` | uuid | no | |
| `repId` | uuid | no | |
| `isActive` | boolean | no | |
| `limit` | int | no | 1–200, default 25 |
| `offset` | int | no | ≥ 0, default 0 |

**Response `data`** — `{ items: Customer[], total }`.

```bash
# Arabic search — URL-encode the query
curl "http://localhost:3000/api/v1/customers?q=%D9%86%D9%88%D8%B1&limit=25" -H "Authorization: Bearer $TOKEN"
```

### `GET /api/v1/customers/:id`
One `Customer` by id.

### `GET /api/v1/customers/:id/insights`
AI panel: profile, recent visits, rolled-up summaries.
**Response `data`**
```ts
{
  customer: Customer;
  aiProfile: { customerId, segment, churnScore, churnRiskLabel, ltvEstimate, shapDriversJson, modelVersion, computedAt, updatedAt } | null;
  recentVisits: CustomerVisit[];                          // last 10
  invoiceSummary: { count: number; totalFils: number };
  collectionSummary: { outstandingFils: number; overdueFils: number };
}
```

### `POST /api/v1/customers`
Create. Requires permission `canAddCustomer`. Returns `201`.
**Request body**
```ts
{
  customerNumber: string;        // required, 1–32
  customerName: string;          // required, 2–200
  nameAr?: string;               // defaults to customerName
  nameEn?: string;
  phone?: string;                // phone_hash computed automatically
  addressAr?: string; city?: string; cityCode?: string;  // e.g. "JO-AM"
  location?: string; longitude?: string; latitude?: string;
  repId?: string; regionId?: string; category?: string;
  creditLimit?: string;          // numeric string, default "0"
  paymentTerms?: number;         // int >= 0, default 30
  customerType?: 'CASH' | 'CREDIT' | 'WHOLESALE' | 'RETAIL'; // default 'CASH'
  tin?: string; nin?: string; passportNumber?: string;
  isActive?: boolean;            // default true
}
```
**Response `data`** — `Customer`.

### `PATCH /api/v1/customers/:id`
Partial update (cannot change `customerNumber`; re-hashes `phone` if given). Requires permission `canEditCustomerCredit`.
**Request body** — all create fields except `customerNumber`, optional.

### `POST /api/v1/customers/:id/reassign`
Reassign to a different rep. Roles: `admin`, `manager`.
**Request body** — `{ newRepId: string }`. **Response `data`** — updated `Customer`.

### `GET /api/v1/customers/:id/visits`
Recent visits (latest 50, DESC).
**Response `data`** — `CustomerVisit[]`
```ts
{ id, customerId, repId, visitedAt, hadSale, visitNote: string|null, lat: number|null, lng: number|null }
```

### `POST /api/v1/customers/:id/visits`
Log a visit (mobile check-in). Returns `201`.
**Request body** — `{ repId: string; visitedAt?: string; hadSale?: boolean; visitNote?: string; lat?: number; lng?: number }`.

### `POST /api/v1/customers/:id/refresh-ai`
Queue an AI-profile refresh. Roles: `admin`, `manager`. **Response `data`** — `{ queued: boolean }`.

### `POST /api/v1/customers/import`
Bulk CSV import. `multipart/form-data`, field `file`. Header row required; columns `number,name,address,phone,category`. Max 5000 rows. Roles: `admin`, `manager`.
**Response `data`** — `{ inserted: number; skipped: number; errors: { row, reason }[] }`.

```bash
curl -X POST http://localhost:3000/api/v1/customers/import \
  -H "Authorization: Bearer $TOKEN" -F "file=@customers.csv;type=text/csv"
```

### `DELETE /api/v1/customers/:id`
Soft-delete. Returns `204`.

---

## 12. Vendors

No role restrictions (any authenticated user). **`Vendor` shape** — `{ id, vendorNumber, vendorName, vendorPhone: string|null, vendorDebit: string, vendorCredit: string, createdAt, updatedAt, deletedAt }`.

### `POST /api/v1/vendors`
Create. **Request body** — `{ vendorNumber: string (1–32, unique); vendorName: string (2–200); vendorPhone?: string; vendorDebit?: string; vendorCredit?: string }` (numeric strings default `"0"`).

### `GET /api/v1/vendors`
Paginated list. **page/limit.** Query: `page?`, `limit?` (1–200, default 25), `search?`. **Response `data`** — `{ items, total, page, limit, pages }`.

### `GET /api/v1/vendors/:id`
One `Vendor`.

### `PATCH /api/v1/vendors/:id`
Update (cannot change `vendorNumber`). Body: `{ vendorName?, vendorPhone?, vendorDebit?, vendorCredit? }`.

### `DELETE /api/v1/vendors/:id`
Soft-delete. Returns `204`.

---

## 13. Warehouses

No role restrictions. **`Warehouse` shape** — `{ id, whNumber, whName, whCreditBox: string, whDebitBox: string, createdAt, updatedAt, deletedAt }`.

### `POST /api/v1/warehouses`
Create. **Request body** — `{ whNumber: string (1–32, unique); whName: string (2–200); whCreditBox?: string; whDebitBox?: string }`.

### `GET /api/v1/warehouses`
List all (no pagination). **Response `data`** — `Warehouse[]`.

### `GET /api/v1/warehouses/:id`
One `Warehouse`.

### `PATCH /api/v1/warehouses/:id`
Update (cannot change `whNumber`). Body: `{ whName?, whCreditBox?, whDebitBox? }`.

### `DELETE /api/v1/warehouses/:id`
Soft-delete. Returns `204`.

---

## 14. Products

**`Product` shape**
```ts
{
  id: string; itemNumber: string; sku: string; barcode: string;
  name: string; nameAr: string; nameEn: string | null;
  categoryId: string | null; unit: string; unitOfMeasure: string; // UN/CEFACT, e.g. "PCE"
  price: number;        // fils
  cost: number | null;  // fils
  imageUrl: string | null; isActive: boolean; reorderQty: number;
  taxType: "TAXABLE" | "INCLUSIVE" | "EXEMPT";
  taxCategory: "S" | "Z" | "E";
  taxRate: string;       // e.g. "0.1600"
  taxPercentage: string; // legacy, e.g. "16.00"
  photoUrl: string | null;
  createdAt: string; updatedAt: string; deletedAt: string | null; version: number;
}
```

### `GET /api/v1/products`
List products. **offset/limit.**
**Query params** — `q?` (sku/name_ar/item_name/barcode), `categoryId?`, `isActive?`, `limit?` (1–200, default 50), `offset?`.
**Response `data`** — `{ items: Product[], total }`.

### `GET /api/v1/products/:id`
One `Product`.

### `POST /api/v1/products/:id/quote`
Compute effective unit price at a quantity, applying matching price rules (optionally customer-specific). Returns `201`.
**Request body** — `{ qty: number (>=1); customerId?: string }`.
**Response `data`**
```ts
{
  productId: string; qty: number; segment: string | null;
  listUnitPrice: number; appliedRuleId: string | null;
  discountPct: number; finalUnitPrice: number; lineTotal: number;  // fils
}
```

### `POST /api/v1/products`
Create. Roles: `admin`, `manager`. `409` on duplicate `itemNumber`.
**Request body**
```ts
{
  itemNumber: string;        // required, 1–64, unique
  barcode: string;           // required, 1–64
  name: string;              // required, 1–200
  price: number;             // required, int >= 0 (fils)
  sku?: string;              // defaults to itemNumber
  nameAr?: string;           // defaults to name
  nameEn?: string;
  categoryId?: string;
  unit?: string;             // default "carton"
  unitOfMeasure?: string;    // default "PCE"
  cost?: number;             // fils
  imageUrl?: string;
  isActive?: boolean;        // default true
  reorderQty?: number;       // default 0
  taxType?: "TAXABLE" | "INCLUSIVE" | "EXEMPT"; // default "TAXABLE"
  taxCategory?: "S" | "Z" | "E";                // default "S"
  taxRate?: number;          // 0..1, default 0.16
}
```

### `PATCH /api/v1/products/:id`
Partial update (cannot change `itemNumber`). Roles: `admin`, `manager`.

### `DELETE /api/v1/products/:id`
Soft-delete. Roles: `admin`. Returns `204`.

---

## 15. Product Categories

**`CategoryNode`** — `{ id, nameAr, nameEn|null, parentId|null, sortOrder, createdAt, updatedAt, deletedAt, version, children: CategoryNode[] }`.

### `GET /api/v1/product-categories`
Root categories with nested children (full tree). **Response `data`** — `CategoryNode[]`.

### `POST /api/v1/product-categories`
Create. Roles: `admin`, `manager`. Body: `{ nameAr: string (1–200); nameEn?; parentId?; sortOrder? }`.

### `PATCH /api/v1/product-categories/:id`
Update subset of `{ nameAr, nameEn, parentId, sortOrder }`. Roles: `admin`, `manager`.

### `DELETE /api/v1/product-categories/:id`
Soft-delete. Roles: `admin`. Returns `204`.

---

## 16. Price Rules

**`PriceRule`** — `{ id, productId|null, customerSegment|null, minQty, discountPct, fixedPrice: number|null (fils), validFrom|null, validTo|null, createdAt, updatedAt, deletedAt, version }`.

### `GET /api/v1/price-rules`
List all rules. **Response `data`** — `PriceRule[]`.

### `POST /api/v1/price-rules`
Create. Roles: `admin`, `manager`.
**Request body** — `{ productId?; customerSegment?; minQty? (>=1, default 1); discountPct? (0–100); fixedPrice? (fils, overrides discountPct); validFrom?; validTo? }`.

### `PATCH /api/v1/price-rules/:id`
Update subset of the POST fields. Roles: `admin`, `manager`.

### `DELETE /api/v1/price-rules/:id`
Soft-delete. Roles: `admin`. Returns `204`.

---

## 17. Items

Legacy catalog/inventory model (separate from Products). Most reads are open; writes need permissions.

**`Item` shape** — same field set as `Product` plus mirrored `itemNumber`/`name`/`barcode`.

### `POST /api/v1/items`
Create a catalog item. Requires permission `canAddItems`. `409` if `itemNumber`/`barcode` exists.
**Request body** — `{ itemNumber: string (1–32, unique); name: string (1–200); barcode: string (1–64, unique); taxPercentage?: string ("0".."100"); photoUrl?: string }`.

### `GET /api/v1/items`
Paginated list. **page/limit.** Query: `page?`, `limit?` (1–200, default 25), `search?` (name/itemNumber/barcode). **Response `data`** — `{ items, total, page, limit, pages }`.

### `GET /api/v1/items/barcode/:barcode`
Look up by barcode. **Response `data`** — `Item` or `null`.

### `GET /api/v1/items/:id`
One `Item`.

### `PATCH /api/v1/items/:id`
Update subset of `{ name, barcode, taxPercentage, photoUrl }` (cannot change `itemNumber`). Requires permission `canAddItems`.

### `DELETE /api/v1/items/:id`
Soft-delete. Returns `204`.

### Unit switches (carton ↔ piece)

**`ItemSwitch`** — `{ id, itemNumber, barcode, unitQty, salePrice: string, itemName, unitName, createdAt, updatedAt, deletedAt, version }`.

- `POST /api/v1/items/switches` — create. Requires `canAddItems`. Body: `{ itemNumber; barcode (unique); unitQty (>=1); salePrice (numeric string); itemName; unitName }`.
- `GET /api/v1/items/:itemNumber/switches` — list switches for an item.
- `GET /api/v1/items/switches/barcode/:barcode` — look up by barcode (or `null`).
- `DELETE /api/v1/items/switches/:id` — delete. Returns `204`.

### Batch expiry

**`ExpiryItem`** — `{ id, itemNumber, itemName, expDate, inDate, startDate|null, storeNumber|null, createdAt, updatedAt, deletedAt, version }`.

- `POST /api/v1/items/expiry` — record a batch. Requires `canEditExpiry`. Body: `{ itemNumber; itemName; expDate (date); inDate (date); startDate?; storeNumber? }`.
- `GET /api/v1/items/expiry/list` — list all.
- `GET /api/v1/items/expiry/before/:date` — batches expiring before `YYYY-MM-DD`.
- `DELETE /api/v1/items/expiry/:id` — delete. Returns `204`.

### Item balance

- `GET /api/v1/items/balance/list` — net posted quantity per item per store. Query: `itemNumber?`, `stockNumber?`. **Response `data`** — `{ itemNumber, itemName, stockNumber: string|null, qty: string }[]`.

---

## 18. Invoices

**`Invoice` shape** (totals in fils)
```ts
{
  id: string; invoiceNumber: string;       // INV-{YYYY}-{NNNNNN}
  status: 'draft'|'confirmed'|'pending_approval'|'rejected'|'cancelled';
  jofotaraStatus: string;
  subtotal: number; totalLineDiscounts: number; invoiceDiscountAmount: number;
  netTaxable: number; netInclusive: number; netExempt: number;
  taxOnTaxable: number; taxExtractedFromInclusive: number; totalTax: number; grandTotal: number;
  // GET :id adds: lines: InvoiceLine[]
}
```

### `GET /api/v1/invoices`
List invoices. **offset/limit.**
**Query params** — `repId?`, `customerId?`, `status?` (`draft|confirmed|pending_approval|rejected|cancelled`), `from?` (ISO), `to?` (ISO), `limit?` (1–200, default 25), `offset?`.
**Response `data`** — `{ items: Invoice[], total }`.

### `GET /api/v1/invoices/export`
Export invoices + lines to **XLSX** (one row per line, JOD). Roles: `admin`, `manager`. **Binary download**, not the JSON envelope.
**Query params** — `from?` (YYYY-MM-DD), `to?`.
```bash
curl -s -o invoices.xlsx -H "Authorization: Bearer $TOKEN" \
  "http://localhost:3000/api/v1/invoices/export?from=2026-05-01&to=2026-05-31"
```

### `GET /api/v1/invoices/:id`
One invoice including `lines`.

### `GET /api/v1/invoices/:id/audit`
Approval/audit timeline. **Response `data`** — `{ id, invoiceId, action: 'submitted'|'approved'|'rejected'|'override', actorId, reason, actedAt }[]`.

### `GET /api/v1/invoices/:id/returnable`
Remaining returnable quantity per line. **Response `data`** — `{ invoiceLineId, productId, originalQty, returnableQty }[]`.

### `GET /api/v1/invoices/:id/credit-notes`
Credit notes raised against the invoice. **Response `data`** — `CreditNote[]`.

### `POST /api/v1/invoices`
Create a draft; computes per-line and invoice-level tax/totals. Returns `201`.
**Request body**
```ts
{
  customerId: string;
  repId: string;
  lines: [{                                   // 1–200
    productId: string;
    quantity: number;                         // >= 0.001 (fractional allowed)
    unitPrice?: number;                       // int fils; defaults to product.price
    lineDiscountType?: 'PERCENTAGE' | 'FIXED_AMOUNT';  // default 'PERCENTAGE'
    lineDiscountValue?: number;               // percent (0–100) or fils; default 0
  }];
  invoiceDiscountType?: 'PERCENTAGE' | 'FIXED_AMOUNT';
  invoiceDiscountValue?: number;
  paymentMethodCode?: '012' | '022';          // 012 cash | 022 receivable; default '012'
  note?: string;                              // 0–1000
  deviceId?: string;                          // 0–128
}
```

### `PATCH /api/v1/invoices/:id`
Edit a draft and recompute tax. Only drafts editable; `repId`/`customerId` immutable. Body = create minus `repId`/`customerId`, all optional.

### `POST /api/v1/invoices/:id/confirm`
Confirm a draft (`draft → confirmed`): sets `confirmedAt`, generates `jofotaraUuid`, writes `submitted` audit, emits `invoice.confirmed`. Returns `201`, the confirmed `Invoice`.

### `POST /api/v1/invoices/:id/cancel`
Cancel (idempotent). Roles: `admin`, `manager`.

### `POST /api/v1/invoices/:id/approve`
Manager approve. Roles: `admin`, `manager`. Body (optional) `{ reason? }`.

### `POST /api/v1/invoices/:id/reject`
Manager reject, returns invoice to draft. Roles: `admin`, `manager`. Body `{ reason: string }` (1–500).

### `POST /api/v1/invoices/:id/override`
Manager sets a fixed invoice-level discount (fils) and recomputes. Roles: `admin`, `manager`.
**Request body** — `{ invoiceDiscountAmount: number (fils, >= 0); reason?: string }`.

---

## 19. Vouchers

Monetary fields are numeric strings.

### `GET /api/v1/vouchers/kinds`
List transaction-kind lookups. **Response `data`** — `{ transKind, transName, sign }[]` (`sign ∈ -1|0|1`).

### `POST /api/v1/vouchers/kinds`
Create a transaction kind. Body: `{ transKind (1–32); transName (1–200); sign? (-1|0|1, default 0) }`.

### `POST /api/v1/vouchers`
Create a voucher (header + lines + payments) atomically. Requires permission `canMakeVoucher`. Returns `201`.
**Request body**
```ts
{
  voucherNumber: string;            // 1–32
  transKind: string;                // 1–32, e.g. 'SALE'
  userCode: string;
  customerNumber?: string;
  vendorNumber?: string;
  inDate?: string;                  // ISO
  totalDiscountValue?: string;      // default '0'
  totalDiscountPercentage?: string; // default '0'
  isPosted?: boolean;               // default false
  transactions: [{                  // >= 1
    itemNumber: string; itemName: string;
    itemQty: string;                // e.g. '1.000'
    unitPrice: string;              // e.g. '1.250'
    taxPercentage?: string;         // default '0'
    discountPercentage?: string;    // default '0'
    discountValue?: string;         // default '0'
    storeNumber?: string;
    transKind?: string;             // defaults to header
  }];
  payments?: [{
    amount: string;                 // e.g. '12.500'
    paymentDate?: string;
    fromAcc?: string; toAcc?: string;
    paymentType: 'CASH' | 'CHEQUE' | 'TRANSFER' | 'CARD' | 'CREDIT';  // default 'CASH'
  }];
}
```

### `GET /api/v1/vouchers`
List all vouchers. **Response `data`** — `Voucher[]`.

### `GET /api/v1/vouchers/:id`
One voucher (header + lines + payments).

### `PATCH /api/v1/vouchers/:id`
Edit an unposted voucher header. Requires permission `canEditVoucher`. Body: `{ totalDiscountValue?, totalDiscountPercentage?, customerNumber?, vendorNumber? }`.

### `PATCH /api/v1/vouchers/:id/post`
Post (immutable + applies stock effect). Requires permission `canMakeVoucher`.

### `DELETE /api/v1/vouchers/:id`
Delete an unposted voucher. Returns `204`.

### Voucher cheques

- `POST /api/v1/vouchers/cheques` — create. Body: `{ bankName; chequeNumber; chequeDate (ISO); dueDate (ISO); amount (numeric string); customerNumber?; customerName? }`.
- `GET /api/v1/vouchers/cheques/list` — list.
- `DELETE /api/v1/vouchers/cheques/:id` — delete. Returns `204`.

---

## 20. Collections

**`Collection` shape**
```ts
{
  id: string; repId: string; customerId: string;
  invoiceId: string | null; paymentId: string | null;
  amount: number;            // fils
  method: 'cash' | 'cheque';
  status: 'pending' | 'confirmed' | 'deposited' | 'bounced';
  collectedAt: string; confirmedAt: string | null; depositedAt: string | null;
  note: string | null;
  cheque?: Cheque;           // present for cheque collections
}
```

### `GET /api/v1/collections`
List collections. **offset/limit.**
**Query params** — `repId?`, `customerId?`, `method?` (`cash|cheque`), `status?` (`pending|confirmed|deposited|bounced`), `from?` (ISO), `to?` (ISO), `limit?` (1–200, default 25), `offset?`.
**Response `data`** — `{ items: Collection[], total }`.

### `GET /api/v1/collections/summary`
Daily totals. Query `date?` (YYYY-MM-DD, default today).
**Response `data`** — `{ date, totalCollectedFils, cashFils, chequeFils, pendingFils, overdueChequeFils }`.

### `GET /api/v1/collections/aging`
Uncleared-cheque aging buckets.
**Response `data`** — `{ asOf, buckets: { label: '0-7'|'8-30'|'31-60'|'60+', count, amountFils }[], totalOutstandingFils }`.

### `POST /api/v1/collections`
Record a cash/cheque collection. Returns `201`.
**Request body**
```ts
{
  repId: string;
  customerId: string;
  invoiceId?: string;
  amount: number;             // fils, int >= 1
  method: 'cash' | 'cheque';
  collectedAt?: string;       // YYYY-MM-DD or ISO; default now()
  note?: string;              // max 500
  cheque?: {                  // required when method === 'cheque'
    bankName?: string; chequeNumber?: string; payee?: string;
    amountWords?: string;     // if set & mismatched, blocks confirm
    dueDate?: string;         // YYYY-MM-DD
    ocrConfidence?: number;   // 0–1
    wordsMatch?: boolean;     // default true; false flags mismatch
    scanSource?: 'server' | 'mlkit_offline';  // default 'server'
    imagePath?: string;
  };
}
```

### `POST /api/v1/collections/batch-deposit`
Mark multiple confirmed collections deposited. Roles: `admin`, `manager`.
**Request body** — `{ collectionIds: string[] }` (min 1). **Response `data`** — `{ deposited: number; skipped: string[] }`.

### `GET /api/v1/collections/:id`
One `Collection` (with cheque if any).

### `POST /api/v1/collections/:id/confirm`
`pending → confirmed`. `409` if linked cheque has `wordsMatch=false` and isn't reconciled.

---

## 21. Cheques

**`Cheque` shape**
```ts
{
  id: string; collectionId: string;
  bankName: string | null; chequeNumber: string | null; payee: string | null;
  amount: number;            // fils
  amountWords: string | null; dueDate: string | null; // YYYY-MM-DD
  ocrConfidence: number | null; wordsMatch: boolean;
  scanSource: 'server' | 'mlkit_offline';
  status: 'pending' | 'cleared' | 'bounced' | 'cancelled';
  imagePath: string | null; scannedAt: string;
  reconciledAt: string | null; reconciledBy: string | null; paymentChequeId: string | null;
}
```

### `GET /api/v1/cheques`
List cheques. Query: `status?` (`pending|cleared|bounced|cancelled`), `dueFrom?` (YYYY-MM-DD), `dueTo?`. **Response `data`** — `Cheque[]`.

### `GET /api/v1/cheques/reconcile/queue`
Cheques needing reconciliation (`wordsMatch=false`, not reconciled). Roles: `admin`, `manager`. **Response `data`** — `Cheque[]`.

### `GET /api/v1/cheques/export/bank`
Bank clearing list as **CSV** of pending cheques. Roles: `admin`, `manager`. **File download** (`text/csv`), columns `bank_name,cheque_number,payee,amount_jod,due_date`.

### `POST /api/v1/cheques/:id/reconcile`
Confirm correct values; sets `wordsMatch=true`, `reconciledAt/By`, clears the confirm block. Roles: `admin`, `manager`.
**Request body** — `{ amount: number (fils, >= 1); amountWords?; bankName?; chequeNumber?; dueDate? }`.

### `POST /api/v1/cheques/:id/mark-cleared`
Mark cleared. Roles: `admin`, `manager`.

### `POST /api/v1/cheques/:id/mark-bounced`
Mark bounced. Roles: `admin`, `manager`.

---

## 22. Credit Notes

Returns against confirmed invoices; auto-submitted to ISTD JoFotara.

### `GET /api/v1/credit-notes`
List all credit notes. **Response `data`** — `CreditNote[]`.

### `GET /api/v1/credit-notes/:id`
One credit note with lines.
**Response `data`**
```ts
{
  id; originalInvoiceId; reason;
  subtotal; totalReturnTax; grandReturnTotal;   // fils
  jofotaraStatus;            // VALIDATED | REJECTED | ERROR | PENDING
  qrCode?; registrationNumber?;
  lines: [{ invoiceLineId, productId, returnQuantity }];
  createdAt;
}
```

### `POST /api/v1/credit-notes`
Create a return against a confirmed invoice (`returnQuantity ≤ remaining returnable`). Roles: `admin`, `manager`. Returns `201`.
**Request body**
```ts
{
  originalInvoiceId: string;
  reason: string;                          // 1–500
  lines: [{ invoiceLineId: string; returnQuantity: number }];  // 1–200, qty >= 0.001
}
```

---

## 23. JoFotara (ISTD)

Mock mode (`JOFOTARA_MOCK=true`, default) returns a deterministic `VALIDATED` + fake QR.

### `POST /api/v1/jofotara/invoices/:id/submit`
Submit/retry an invoice to ISTD now (synchronous). Roles: `admin`, `manager`.
**Response `data`** — `{ status: 'VALIDATED'|'REJECTED'|'ERROR'; qrCode?; registrationNumber?; errors?: string[] }`.

### `POST /api/v1/jofotara/credit-notes/:id/submit`
Submit/retry a credit note. Roles: `admin`, `manager`. Same response shape.

### `GET /api/v1/jofotara/submissions/:documentId/log`
ISTD submission attempt log for a document (invoice or credit-note id). Roles: `admin`, `manager`.
**Response `data`** — `{ attempt, requestUrl, requestPayload, responseStatus, responseBody, durationMs, error: string|null, createdAt }[]`.

---

## 24. Tax Reporting

Roles: `admin`, `manager` (controller-level). Money in fils.

### `GET /api/v1/tax/report`
Monthly net-output-tax report (sales tax − returns tax) from VALIDATED ledger entries.
**Query params** — `year` (required), `month` (required, 1–12).
**Response `data`**
```ts
{
  periodFrom; periodTo;
  totalSalesFils; totalSalesTaxFils;
  totalReturnsFils; totalReturnsTaxFils;  // negative
  netOutputTaxFils;                        // payable to ISTD
  invoiceCount; creditNoteCount;
}
```

### `GET /api/v1/tax/ledger`
List tax ledger entries. Query: `from?` (YYYY-MM-DD), `to?`, `entryType?` (`SALE|RETURN`). **Response `data`** — `TaxLedgerEntry[]` (returns amounts negative).

### `GET /api/v1/tax/report/export`
Monthly report as **XLSX** (JOD) for ISTD filing. Query: `year`, `month`. **Binary download.**

---

## 25. Audit Log

Roles: `admin` (controller-level). Read-only; mutations recorded automatically.

### `GET /api/v1/audit-log`
Query the audit log. **offset/limit.**
**Query params** — `entity?`, `entityId?`, `actorId?`, `from?` (ISO), `to?` (ISO), `limit?` (1–200, default 50), `offset?`.
**Response `data`** — `{ items: AuditLog[], total }` where `AuditLog = { id, actorId, entity, entityId, action, diffJson, ipAddress, userAgent, actedAt }`.

### `GET /api/v1/audit-log/:entity/:entityId`
Full change history for one record (newest first, ≤200). **Response `data`** — `AuditLog[]`.

---

## 26. Notification Rules

Roles: `admin`, `manager` (controller-level). Triggers: `anomaly_high | churn_spike | rep_offline | overdue`. Channels: `email | sms | whatsapp | push`.

### `GET /api/v1/notification-rules`
List all rules. **Response `data`** — `NotificationRule[]` (`{ id, name, trigger, channel, threshold, recipients, isActive, createdAt }`).

### `POST /api/v1/notification-rules`
Create. Returns `201`.
**Request body**
```ts
{
  name: string;                                          // 1–200
  trigger: 'anomaly_high'|'churn_spike'|'rep_offline'|'overdue';
  channel: 'email'|'sms'|'whatsapp'|'push';
  threshold?: Record<string, unknown>;
  recipients?: string[];                                 // recipient user UUIDs
  isActive?: boolean;                                    // default true
}
```

### `PATCH /api/v1/notification-rules/:id`
Partial update (all fields optional).

### `DELETE /api/v1/notification-rules/:id`
Delete. Returns `204`.

### `POST /api/v1/notification-rules/:id/test`
Fire the rule's trigger with a synthetic payload. **Response `data`** — `{ matched: number }`.

---

## 27. Health

### `GET /api/v1/health`
**Public** — no `Authorization` needed. Liveness/readiness probe.
**Response `data`** — `{ status: 'ok' | 'degraded'; db: 'up' | 'down' }`.

```bash
curl http://localhost:3000/api/v1/health
```

---

## 28. Realtime WebSocket

Not REST — a **Socket.IO** namespace that streams operational events. Single-tenant / single-instance: all events broadcast to every connected socket (no rooms).

**Connection**
- Host: `http://<host>:3000`
- Namespace: `/ws/ops`
- Transport: `['websocket']`

**Auth handshake** — JWT verified at connect. Pass it any one of:
- `auth: { token: '<jwt>' }` (preferred)
- query `?token=<jwt>`
- header `Authorization: Bearer <jwt>`

Missing/invalid token → immediate disconnect (`connect_error`), no events.

```ts
import { io } from 'socket.io-client';

const socket = io('http://<host>:3000/ws/ops', {
  auth: { token: '<jwt>' },
  transports: ['websocket'],
});

socket.on('rep.location',      (p) => { /* { rep_id, lat, lng, ts } */ });
socket.on('invoice.confirmed', (p) => { /* { invoice_id, rep_id, customer_id, total } */ });
socket.on('route.deviated',    (p) => { /* { rep_id, plan_id, deviation_m } */ });
socket.on('rep.offline',       (p) => { /* { rep_id, last_seen } */ });
socket.on('connect_error',     (e) => { /* auth failed */ });
```

**Client → server:** none — the gateway only broadcasts (plus standard Socket.IO lifecycle events).

**Server → client events**
| Event | Payload | Source |
|---|---|---|
| `rep.location` | `{ rep_id, lat, lng, ts }` | every GPS ping |
| `invoice.created` | `{ invoice_id, rep_id }` | invoice draft created |
| `invoice.confirmed` | `{ invoice_id, rep_id, customer_id, total }` | invoice confirmed |
| `route.deviated` | `{ rep_id, plan_id, deviation_m }` | rep > 500m from stops |
| `rep.offline` | `{ rep_id, last_seen }` | no ping in 2h (debounced) |
| `anomaly.flagged` | reserved | not yet emitted |
| `cheque.scanned` | reserved | not yet emitted |

---

_Generated for the CMP mobile team. Source of truth: `src/modules/**` controllers + DTOs. When the backend changes, regenerate or sync this file (and the interactive `/docs` Swagger UI)._
