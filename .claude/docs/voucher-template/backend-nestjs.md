# Voucher Template — Backend (NestJS) Implementation Guide

Implements the per-tenant store + API for the [`VoucherTemplate` contract](voucher-template.contract.md). The mobile app fetches this once per session and renders the receipt from it; if the request fails, the app keeps its built-in Jordan defaults.

---

## 1. API

### `GET /api/voucher-template`
Returns the resolved template for the authenticated tenant. Always 200 with a **complete** object (custom overrides merged onto the base template) so the client never has to merge.

**Auth:** tenant resolved from the bearer token (same as other endpoints).

**200 response**
```json
{
  "currency": "د.أ",
  "amountDecimals": 3,
  "defaultTaxPct": 16.0,
  "monochrome": true,
  "qrCaption": "الرمز الضريبي (JoFotara - ISTD)",
  "showPaymentType": true,
  "paymentTypeInHeader": true,
  "paymentTypeInFooter": true
}
```

### `PUT /api/voucher-template` (admin only)
Upserts the tenant's overrides. Body = partial or full `VoucherTemplate`. Validate against the JSON Schema; merge onto base; persist; return the resolved object.

> Keep it idempotent. A `PUT` with `{}` resets the tenant to the base template.

---

## 2. DTO + validation (`class-validator`)

```ts
// voucher-template.dto.ts
import { IsBoolean, IsInt, IsNumber, IsString, Length, Max, Min, IsOptional } from 'class-validator';

export class VoucherTemplateDto {
  @IsOptional() @IsString() @Length(1, 64)
  currency = 'د.أ';

  @IsOptional() @IsInt() @Min(0) @Max(4)
  amountDecimals = 3;

  @IsOptional() @IsNumber() @Min(0) @Max(100)
  defaultTaxPct = 16.0;

  @IsOptional() @IsBoolean()
  monochrome = true;

  @IsOptional() @IsString() @Length(1, 64)
  qrCaption = 'الرمز الضريبي (JoFotara - ISTD)';

  @IsOptional() @IsBoolean()
  showPaymentType = true;

  @IsOptional() @IsBoolean()
  paymentTypeInHeader = true;

  @IsOptional() @IsBoolean()
  paymentTypeInFooter = true;
}
```

Enable global validation with `whitelist: true` + `forbidNonWhitelisted: true` so unknown keys are rejected on write (storage stays clean), and `transform: true` so defaults apply:

```ts
app.useGlobalPipes(new ValidationPipe({ whitelist: true, forbidNonWhitelisted: true, transform: true }));
```

---

## 3. The canonical base template (single source)

```ts
// voucher-template.base.ts
import { VoucherTemplateDto } from './voucher-template.dto';

/** Jordan rollout base template. Custom tenant overrides merge ON TOP of this. */
export const BASE_VOUCHER_TEMPLATE: VoucherTemplateDto = {
  currency: 'د.أ',
  amountDecimals: 3,
  defaultTaxPct: 16.0,
  monochrome: true,
  qrCaption: 'الرمز الضريبي (JoFotara - ISTD)',
  showPaymentType: true,
  paymentTypeInHeader: true,
  paymentTypeInFooter: true,
};
```

---

## 4. Persistence

Store **only the override delta** per tenant (a JSON column), so changing the base template later automatically reaches tenants that never customized.

### Prisma
```prisma
model VoucherTemplate {
  tenantId  String   @id
  overrides Json     @default("{}")   // partial VoucherTemplate
  updatedAt DateTime @updatedAt
}
```

### TypeORM (alternative)
```ts
@Entity('voucher_template')
export class VoucherTemplateEntity {
  @PrimaryColumn() tenantId: string;
  @Column('jsonb', { default: {} }) overrides: Partial<VoucherTemplateDto>;
  @UpdateDateColumn() updatedAt: Date;
}
```

---

## 5. Service + controller

```ts
// voucher-template.service.ts
@Injectable()
export class VoucherTemplateService {
  constructor(private readonly repo: VoucherTemplateRepo) {}

  async resolve(tenantId: string): Promise<VoucherTemplateDto> {
    const row = await this.repo.findByTenant(tenantId);
    return { ...BASE_VOUCHER_TEMPLATE, ...(row?.overrides ?? {}) };
  }

  async upsert(tenantId: string, patch: VoucherTemplateDto): Promise<VoucherTemplateDto> {
    // Persist only keys that differ from the base, keeping storage minimal.
    const overrides = Object.fromEntries(
      Object.entries(patch).filter(([k, v]) => (BASE_VOUCHER_TEMPLATE as any)[k] !== v),
    );
    await this.repo.upsert(tenantId, overrides);
    return this.resolve(tenantId);
  }
}
```

```ts
// voucher-template.controller.ts
@Controller('api/voucher-template')
@UseGuards(JwtAuthGuard)
export class VoucherTemplateController {
  constructor(private readonly svc: VoucherTemplateService) {}

  @Get()
  get(@Tenant() tenantId: string) {
    return this.svc.resolve(tenantId);
  }

  @Put()
  @UseGuards(AdminGuard)
  upsert(@Tenant() tenantId: string, @Body() dto: VoucherTemplateDto) {
    return this.svc.upsert(tenantId, dto);
  }
}
```

---

## 6. Contract guarantees the app relies on

- **Always return all fields** on `GET` (merge done server-side). The app *can* tolerate missing fields, but a complete object avoids surprises.
- **Numbers as JSON numbers**, not strings (`amountDecimals: 3`, not `"3"`).
- **`monochrome` must stay `true` for Jordan tenants.** A colored market flips it later; the app already has the color path behind the flag.
- **`qrCaption` must never contain "ZATCA"** for Jordan — it is JoFotara/ISTD.
- Keep keys stable; add-only evolution (see contract §5).

---

## 7. Seed

On tenant creation, **do not** write a full row — leave `overrides = {}` so the tenant inherits the base template and benefits from future base changes. Only persist a row when an admin customizes via `PUT`.
