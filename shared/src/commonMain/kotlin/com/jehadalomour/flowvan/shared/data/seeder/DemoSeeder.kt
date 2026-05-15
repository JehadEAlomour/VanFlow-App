package com.jehadalomour.flowvan.shared.data.seeder

import co.touchlab.kermit.Logger
import com.jehadalomour.flowvan.shared.data.local.db.FlowVanDatabase
import com.jehadalomour.flowvan.shared.data.local.entity.CustomerEntity
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import com.jehadalomour.flowvan.shared.data.local.entity.ProductEntity
import com.jehadalomour.flowvan.shared.data.local.entity.ShiftEntity
import com.jehadalomour.flowvan.shared.data.local.entity.UserEntity
import com.jehadalomour.flowvan.shared.data.settings.SettingsKeys
import com.jehadalomour.flowvan.shared.domain.model.InvoiceLine
import com.russhwolf.settings.Settings
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DemoSeeder(
    private val db: FlowVanDatabase,
    private val settings: Settings,
    private val json: Json,
) {
    private val log = Logger.withTag("DemoSeeder")

    suspend fun seedIfNeeded() {
        if (settings.getBoolean(SettingsKeys.DEMO_SEEDED, false)) {
            log.d { "demo data already seeded — skipping" }
            return
        }
        seed()
        settings.putBoolean(SettingsKeys.DEMO_SEEDED, true)
        log.i { "demo data seeded" }
    }

    suspend fun reset() {
        settings.remove(SettingsKeys.DEMO_SEEDED)
    }

    @OptIn(ExperimentalTime::class)
    private suspend fun seed() {
        val tz = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toEpochMilliseconds()
        val today = Instant.fromEpochMilliseconds(now).toLocalDateTime(tz).date
        val startOfToday = today.atStartOfDayIn(tz)
        val eightAmTodayMs = startOfToday.plus(8, DateTimeUnit.HOUR).toEpochMilliseconds()
        val oneDayMs = 24L * 60 * 60 * 1000

        db.userDao().upsertAll(seedUsers())
        db.customerDao().upsertAll(seedCustomers())
        db.productDao().upsertAll(seedProducts())
        db.invoiceDao().upsertAll(seedInvoices(now, oneDayMs))
        db.paymentDao().upsertAll(seedPayments(now, oneDayMs))
        db.shiftDao().upsert(
            ShiftEntity(
                id = "SHF-DEMO-1",
                userId = "USR-001",
                startedAt = eightAmTodayMs,
                endedAt = null,
                status = "ACTIVE",
                startLat = 31.9539,
                startLng = 35.9106,
                endLat = null,
                endLng = null,
            ),
        )
    }

    private fun seedUsers(): List<UserEntity> = listOf(
        UserEntity(
            id = "USR-001",
            nameAr = "أحمد المصري",
            nameEn = "Ahmed Al-Masri",
            phone = "0791234567",
            passwordHash = "demo_hash_1234",
            role = "SALESMAN",
            token = null,
            lastLoginAt = null,
            lastLoginLat = null,
            lastLoginLng = null,
        ),
        UserEntity(
            id = "USR-002",
            nameAr = "محمد الخالد",
            nameEn = "Mohammed Al-Khaled",
            phone = "0799876543",
            passwordHash = "demo_hash_1234",
            role = "SALESMAN",
            token = null, lastLoginAt = null, lastLoginLat = null, lastLoginLng = null,
        ),
        UserEntity(
            id = "USR-003",
            nameAr = "فيصل النمر",
            nameEn = "Faisal Al-Nimer",
            phone = "0795551234",
            passwordHash = "demo_hash_1234",
            role = "SUPERVISOR",
            token = null, lastLoginAt = null, lastLoginLat = null, lastLoginLng = null,
        ),
        UserEntity(
            id = "USR-004",
            nameAr = "سارة الأحمد",
            nameEn = "Sara Al-Ahmad",
            phone = "0797778899",
            passwordHash = "demo_hash_1234",
            role = "SALESMAN",
            token = null, lastLoginAt = null, lastLoginLat = null, lastLoginLng = null,
        ),
    )

    private fun seedCustomers(): List<CustomerEntity> {
        // 10 on-route + 5 off-route — Amman neighbourhoods.
        val onRoute = listOf(
            cust("CUS-001", "بقالة الأمل",       "Al-Amal Grocery",     "وسط البلد",       "A", "CHAMPIONS",  0.10, 120.500,   0.000, 5000.000, 1, 31.9514, 35.9239,  isOnRoute = true),
            cust("CUS-002", "سوبرماركت السعادة", "Saada Supermarket",   "جبل عمان",        "A", "LOYAL",      0.18, 850.250, 200.000, 8000.000, 2, 31.9577, 35.9244,  isOnRoute = true),
            cust("CUS-003", "بقالة النور",       "Al-Noor Store",       "الصويفية",         "B", "AT_RISK",    0.65, 450.000, 450.000, 3000.000, 3, 31.9420, 35.8760,  isOnRoute = true),
            cust("CUS-004", "محل أبو علي",       "Abu Ali Mart",        "سحاب",            "C", "PROMISING",  0.40,  60.000,   0.000, 1500.000, 4, 31.8689, 36.0010,  isOnRoute = true),
            cust("CUS-005", "بقالة الشمس",       "Sun Grocery",         "الزرقاء",          "B", "REGULAR",    0.30, 220.750,   0.000, 4000.000, 5, 32.0728, 36.0876,  isOnRoute = true),
            cust("CUS-006", "سوبرماركت الرابية", "Al-Rabiyeh Market",   "الرابية",          "A", "CHAMPIONS",  0.08, 980.000,   0.000, 9000.000, 6, 31.9856, 35.8634,  isOnRoute = true),
            cust("CUS-007", "بقالة الجبيهة",     "Jubeiha Store",       "الجبيهة",          "C", "DORMANT",    0.82, 750.000, 750.000, 2000.000, 7, 32.0273, 35.8744,  isOnRoute = true),
            cust("CUS-008", "محل الدوار السابع", "7th Circle Mart",     "الدوار السابع",    "B", "LOYAL",      0.20, 310.000, 100.000, 5000.000, 8, 31.9563, 35.8483,  isOnRoute = true),
            cust("CUS-009", "بقالة الشميساني",   "Shmeisani Grocery",   "الشميساني",        "A", "PROMISING",  0.35, 180.000,   0.000, 6000.000, 9, 31.9698, 35.8990,  isOnRoute = true),
            cust("CUS-010", "محل الجاردنز",      "Gardens Store",       "الجاردنز",         "B", "AT_RISK",    0.70, 540.000, 240.000, 4500.000,10, 31.9852, 35.8520,  isOnRoute = true),
        )
        val offRoute = listOf(
            cust("CUS-011", "بقالة المدينة",     "Al-Madina Store",     "وسط البلد",       "C", "REGULAR",    0.25,   0.000,   0.000, 2000.000, 99, null, null, isOnRoute = false),
            cust("CUS-012", "محل الياسمين",      "Yasmeen Mart",        "خلدا",            "B", "REGULAR",    0.30,  90.500,   0.000, 3000.000, 99, null, null, isOnRoute = false),
            cust("CUS-013", "سوبرماركت العائلة","Family Supermarket",  "تلاع العلي",      "A", "LOYAL",      0.12, 410.000,   0.000, 7000.000, 99, null, null, isOnRoute = false),
            cust("CUS-014", "بقالة الفرحة",      "Al-Farha Grocery",    "ماركا",           "C", "DORMANT",    0.78,   0.000,   0.000, 1500.000, 99, null, null, isOnRoute = false),
            cust("CUS-015", "محل النخبة",        "Elite Mart",          "عبدون",           "A", "CHAMPIONS",  0.05, 200.000,   0.000,10000.000, 99, null, null, isOnRoute = false),
        )
        return onRoute + offRoute
    }

    private fun cust(
        id: String, ar: String, en: String, area: String,
        tier: String, segment: String,
        churn: Double, balance: Double, overdue: Double, creditLimit: Double,
        order: Int, lat: Double?, lng: Double?, isOnRoute: Boolean,
    ) = CustomerEntity(
        id = id,
        code = id.removePrefix("CUS-"),
        nameAr = ar,
        nameEn = en,
        phone = "07" + (90000000 + id.takeLast(3).toInt() * 137).toString().take(8),
        area = area,
        addressAr = "$area - $ar",
        tier = tier,
        segment = segment,
        churnRisk = churn,
        balance = balance,
        overdueAmount = overdue,
        creditLimit = creditLimit,
        taxNumber = null,
        isOnRoute = isOnRoute,
        visitOrder = order,
        lat = lat,
        lng = lng,
    )

    private fun seedProducts(): List<ProductEntity> {
        val raw = listOf(
            // sku, ar, en, category, unit, sale, cost, vanStock, minStock, brand
            P("SKU-001", "عصير تروبيكانا برتقال 1لتر", "Tropicana Orange 1L",   "Beverages",      "carton", 1.250,  0.900, 80, 20, "Tropicana"),
            P("SKU-002", "بيبسي 330مل علبة",          "Pepsi 330ml Can",       "Beverages",      "carton", 0.350,  0.220, 200, 50, "Pepsi"),
            P("SKU-003", "حليب يونيفريش 1لتر",        "Unifresh Milk 1L",      "Dairy",          "carton", 1.100,  0.800, 60, 25, "Unifresh"),
            P("SKU-004", "لبن يونيفريش 500غ",         "Unifresh Yogurt 500g",  "Dairy",          "pack",   0.900,  0.620, 5,  20, "Unifresh"),  // low
            P("SKU-005", "شيبس ليز ملح 25غ",          "Lays Salt 25g",         "Snacks",         "box",    0.250,  0.150, 300, 80, "Lays"),
            P("SKU-006", "برينجلز 165غ ساور كريم",    "Pringles 165g",         "Snacks",         "carton", 1.500,  1.050, 45, 15, "Pringles"),
            P("SKU-007", "تويكس 50غ",                  "Twix 50g",              "Confectionery",  "box",    0.500,  0.300, 250, 60, "Twix"),
            P("SKU-008", "اريال مسحوق 3كغ",            "Ariel Powder 3kg",      "Cleaning",       "box",    8.500,  6.200, 25, 10, "Ariel"),
            P("SKU-009", "فيم سائل تنظيف 750مل",      "Vim Liquid 750ml",      "Cleaning",       "bottle", 1.800,  1.250, 40, 15, "Vim"),
            P("SKU-010", "هاينز كاتشب 460غ",          "Heinz Ketchup 460g",    "Canned",         "bottle", 2.250,  1.600, 30, 12, "Heinz"),
            P("SKU-011", "ماجي مرق دجاج 24 مكعب",     "Maggi Cubes 24",        "Canned",         "box",    3.100,  2.300, 22, 10, "Maggi"),
            P("SKU-012", "نسكافيه 200غ",               "Nescafe 200g",          "Hot Beverages",  "jar",    7.250,  5.400, 18,  8, "Nescafe"),
            P("SKU-013", "ليبتون شاي 100 كيس",        "Lipton Tea 100",        "Hot Beverages",  "box",    4.000,  2.900, 35, 12, "Lipton"),
            P("SKU-014", "أرز بركة 5كغ",               "Baraka Rice 5kg",       "Dry Goods",      "bag",    9.500,  7.000, 28, 10, "Baraka"),
            P("SKU-015", "سكر تحمير 1كغ",              "Brown Sugar 1kg",       "Dry Goods",      "bag",    1.000,  0.700, 60, 20, null),
            P("SKU-016", "مياه معدنية 1.5لتر",         "Water 1.5L",            "Water",          "carton", 0.350,  0.180, 400,100, null),
            P("SKU-017", "هيد آند شولدرز شامبو 400مل","Head&Shoulders 400ml",  "Personal Care",  "bottle", 4.750,  3.300, 24, 10, "Head & Shoulders"),
            P("SKU-018", "دوف صابون 100غ",            "Dove Soap 100g",        "Personal Care",  "box",    0.850,  0.550, 80, 30, "Dove"),
            P("SKU-019", "مناديل ورقية 200 منديل",    "Tissues 200",           "Household",      "box",    1.150,  0.700, 100, 40, null),
            P("SKU-020", "ورق ألمنيوم 30م",           "Aluminum Foil 30m",     "Household",      "roll",   2.000,  1.350, 18,  8, null),
            P("SKU-021", "زيت زيتون 1لتر",            "Olive Oil 1L",          "Cooking Oils",   "bottle", 6.500,  4.800, 12,  6, null),
            P("SKU-022", "زيت دوار شمس 3لتر",         "Sunflower Oil 3L",      "Cooking Oils",   "bottle", 4.250,  3.100, 32, 10, null),
            P("SKU-023", "كولا دايت 330مل",           "Diet Cola 330ml",       "Beverages",      "carton", 0.400,  0.250, 150, 40, "Pepsi"),
            P("SKU-024", "جبنة كرافت 200غ",           "Kraft Cheese 200g",     "Dairy",          "pack",   2.100,  1.450,  3, 12, "Kraft"),  // low
            P("SKU-025", "بطاطا ليز شواء 50غ",        "Lays BBQ 50g",          "Snacks",         "box",    0.450,  0.280, 220, 60, "Lays"),
            P("SKU-026", "كيت كات 45غ",               "Kit Kat 45g",           "Confectionery",  "box",    0.500,  0.310, 180, 50, null),
            P("SKU-027", "سائل جلي بريل 750مل",       "Pril 750ml",            "Cleaning",       "bottle", 1.450,  1.000, 38, 15, null),
            P("SKU-028", "تونة رياسة 170غ",           "Rayyan Tuna 170g",      "Canned",         "can",    1.250,  0.850, 70, 25, null),
            P("SKU-029", "قهوة عربية 250غ",           "Arabic Coffee 250g",    "Hot Beverages",  "bag",    3.750,  2.700,  4, 10, null),  // low
            P("SKU-030", "تمر مجدول 500غ",            "Medjool Dates 500g",    "Special Items",  "pack",   5.500,  4.000, 20,  8, null),
        )
        return raw.mapIndexed { i, p ->
            ProductEntity(
                id = "PRD-${(i + 1).toString().padStart(3, '0')}",
                sku = p.sku,
                nameAr = p.nameAr,
                nameEn = p.nameEn,
                category = p.category,
                unit = p.unit,
                salePrice = p.sale,
                costPrice = p.cost,
                vanStock = p.vanStock,
                minStock = p.minStock,
                expiryDate = null,
                brand = p.brand,
            )
        }
    }

    private data class P(
        val sku: String, val nameAr: String, val nameEn: String, val category: String, val unit: String,
        val sale: Double, val cost: Double, val vanStock: Int, val minStock: Int, val brand: String?,
    )

    private fun seedInvoices(now: Long, oneDay: Long): List<InvoiceEntity> {
        return listOf(
            // 3 sales today
            mkInv("INV-T-0001", "SALE", "CONFIRMED", "CUS-001", now - 3 * 60 * 60 * 1000, "CASH",
                listOf(Triple("SKU-001", 5.0, 1.250), Triple("SKU-005", 10.0, 0.250))),
            mkInv("INV-T-0002", "SALE", "CONFIRMED", "CUS-002", now - 2 * 60 * 60 * 1000, "CHEQUE",
                listOf(Triple("SKU-008", 3.0, 8.500), Triple("SKU-014", 2.0, 9.500))),
            mkInv("INV-T-0003", "SALE", "CONFIRMED", "CUS-006", now - 60 * 60 * 1000, "CREDIT",
                listOf(Triple("SKU-012", 2.0, 7.250), Triple("SKU-013", 4.0, 4.000))),
            // 1 return today
            mkInv("RET-T-0001", "RETURN", "CONFIRMED", "CUS-003", now - 90 * 60 * 1000, null,
                listOf(Triple("SKU-004", 2.0, 0.900))),
            // 3 historical sales
            mkInv("INV-H-0001", "SALE", "CONFIRMED", "CUS-005", now - 1 * oneDay, "CASH",
                listOf(Triple("SKU-002", 24.0, 0.350))),
            mkInv("INV-H-0002", "SALE", "CONFIRMED", "CUS-009", now - 2 * oneDay, "TRANSFER",
                listOf(Triple("SKU-016", 30.0, 0.350))),
            mkInv("INV-H-0003", "SALE", "CONFIRMED", "CUS-010", now - 3 * oneDay, "CREDIT",
                listOf(Triple("SKU-021", 4.0, 6.500))),
            // 1 request today
            mkInv("REQ-T-0001", "REQUEST", "CONFIRMED", "CUS-008", now - 30 * 60 * 1000, null,
                listOf(Triple("SKU-024", 6.0, 2.100), Triple("SKU-029", 4.0, 3.750))),
        )
    }

    private fun mkInv(
        number: String, type: String, status: String, customerId: String, createdAt: Long,
        paymentMethod: String?, items: List<Triple<String, Double, Double>>,
    ): InvoiceEntity {
        val subtotal = items.sumOf { (_, q, p) -> q * p }
        val tax = subtotal * 0.16
        val total = subtotal + tax
        val linesJson = json.encodeToString(
            items.mapIndexed { idx, (sku, qty, price) ->
                InvoiceLine(
                    productId = "PRD-${(idx + 1).toString().padStart(3, '0')}",
                    sku = sku,
                    nameAr = "بند $sku",
                    qty = qty,
                    unitPrice = price,
                    discountPct = 0.0,
                    lineTotal = qty * price,
                )
            },
        )
        return InvoiceEntity(
            id = "INV-$number",
            number = number,
            type = type,
            status = status,
            customerId = customerId,
            salesmanId = "USR-001",
            createdAt = createdAt,
            linesJson = linesJson,
            subtotal = subtotal,
            discountAmount = 0.0,
            taxAmount = tax,
            total = total,
            paymentMethod = paymentMethod,
            notes = null,
            syncedAt = null,
        )
    }

    private fun seedPayments(now: Long, oneDay: Long): List<PaymentEntity> = listOf(
        PaymentEntity(
            id = "PMT-001", number = "RCP-T-0001",
            customerId = "CUS-001", salesmanId = "USR-001",
            amount = 50.000, method = "CASH", status = "CONFIRMED",
            createdAt = now - 2 * 60 * 60 * 1000,
            chequeNumber = null, chequeBank = null, chequeDate = null,
            transferRef = null, notes = null, syncedAt = null,
        ),
        PaymentEntity(
            id = "PMT-002", number = "RCP-Y-0001",
            customerId = "CUS-002", salesmanId = "USR-001",
            amount = 250.000, method = "CHEQUE", status = "CONFIRMED",
            createdAt = now - 1 * oneDay,
            chequeNumber = "100234", chequeBank = "Arab Bank", chequeDate = now - oneDay,
            transferRef = null, notes = null, syncedAt = null,
        ),
        PaymentEntity(
            id = "PMT-003", number = "RCP-T-0002",
            customerId = "CUS-009", salesmanId = "USR-001",
            amount = 180.500, method = "TRANSFER", status = "CONFIRMED",
            createdAt = now - 4 * 60 * 60 * 1000,
            chequeNumber = null, chequeBank = null, chequeDate = null,
            transferRef = "TRF-554433", notes = null, syncedAt = null,
        ),
        PaymentEntity(
            id = "PMT-004", number = "RCP-Y-0002",
            customerId = "CUS-003", salesmanId = "USR-001",
            amount = 120.000, method = "CHEQUE", status = "BOUNCED",
            createdAt = now - 2 * oneDay,
            chequeNumber = "100100", chequeBank = "Bank of Jordan", chequeDate = now - 2 * oneDay,
            transferRef = null, notes = "ارتجع لعدم كفاية الرصيد", syncedAt = null,
        ),
    )

}