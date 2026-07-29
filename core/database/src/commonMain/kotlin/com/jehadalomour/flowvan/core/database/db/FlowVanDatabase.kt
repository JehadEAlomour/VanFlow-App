package com.jehadalomour.flowvan.core.database.db

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.jehadalomour.flowvan.core.database.dao.AiMessageDao
import com.jehadalomour.flowvan.core.database.dao.AppSettingsDao
import com.jehadalomour.flowvan.core.database.dao.CustomerDao
import com.jehadalomour.flowvan.core.database.dao.InvoiceDao
import com.jehadalomour.flowvan.core.database.dao.LocationPointDao
import com.jehadalomour.flowvan.core.database.dao.OfferDao
import com.jehadalomour.flowvan.core.database.dao.PaymentDao
import com.jehadalomour.flowvan.core.database.dao.PriceListItemDao
import com.jehadalomour.flowvan.core.database.dao.ProductDao
import com.jehadalomour.flowvan.core.database.dao.ProductUnitDao
import com.jehadalomour.flowvan.core.database.dao.RouteStopDao
import com.jehadalomour.flowvan.core.database.dao.ShiftDao
import com.jehadalomour.flowvan.core.database.dao.TobaccoTaxProfileDao
import com.jehadalomour.flowvan.core.database.dao.UserDao
import com.jehadalomour.flowvan.core.database.entity.AiMessageEntity
import com.jehadalomour.flowvan.core.database.entity.AppSettingsEntity
import com.jehadalomour.flowvan.core.database.entity.CustomerEntity
import com.jehadalomour.flowvan.core.database.entity.InvoiceEntity
import com.jehadalomour.flowvan.core.database.entity.LocationPointEntity
import com.jehadalomour.flowvan.core.database.entity.OfferEntity
import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.database.entity.PriceListItemEntity
import com.jehadalomour.flowvan.core.database.entity.ProductEntity
import com.jehadalomour.flowvan.core.database.entity.ProductUnitEntity
import com.jehadalomour.flowvan.core.database.entity.RouteStopEntity
import com.jehadalomour.flowvan.core.database.entity.ShiftEntity
import com.jehadalomour.flowvan.core.database.entity.TobaccoTaxProfileEntity
import com.jehadalomour.flowvan.core.database.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        ProductUnitEntity::class,
        InvoiceEntity::class,
        PaymentEntity::class,
        LocationPointEntity::class,
        ShiftEntity::class,
        AiMessageEntity::class,
        RouteStopEntity::class,
        AppSettingsEntity::class,
        OfferEntity::class,
        PriceListItemEntity::class,
        TobaccoTaxProfileEntity::class,
    ],
    version = 16,
    exportSchema = true,
    autoMigrations = [
        AutoMigration(from = 1, to = 2),
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        // v6: invoices.chosenFreeItemsCsv (nullable) for ITEM_QTY_REWARD gift picks.
        AutoMigration(from = 5, to = 6),
        // v7: app_settings company profile columns (companyNameAr/En, companyTaxNumber).
        AutoMigration(from = 6, to = 7),
        // v8: products.imageUrl (nullable) for catalog/cart thumbnails.
        AutoMigration(from = 7, to = 8),
        // v9: offers cache table + customers.category/regionId/repId for offline offer eligibility.
        AutoMigration(from = 8, to = 9),
        // v10: invoices/payments repLat/repLng (nullable) — sale-time GPS for the
        // per-rep location lock (customers.requireProximity).
        AutoMigration(from = 9, to = 10),
        // v11: price_list_items cache table + customers.priceListId (nullable) for
        // per-customer price-list pricing (from GET /price-lists/full).
        AutoMigration(from = 10, to = 11),
        // v12: invoices.uploadLinesJson/uploadDiscountAmount (nullable) — raw upload
        // snapshot so offer-applied invoices display correctly offline yet upload the
        // raw cart (the server re-applies offers, avoiding a double discount).
        AutoMigration(from = 11, to = 12),
        // v13: app_settings.companyLogo — the company logo (base64 data URI) cached from
        // /company-info so the printed voucher header shows it, even offline.
        AutoMigration(from = 12, to = 13),
        // v14: tobacco_tax_profiles cache table + products.isTobacco/tobaccoProfileId/
        // consumerPriceFils — so tobacco items apply the excise/special tax at point of sale.
        AutoMigration(from = 13, to = 14),
        // v15: tobacco_tax_profiles.taxIncludedInConsumerPrice — extract the sales tax
        // from a tax-inclusive consumer price (÷(100+rate)) to match the ERP engine.
        AutoMigration(from = 14, to = 15),
        // v16: invoices.appliedOffersJson (nullable) — offers applied at sale time, frozen for
        // the printed receipt (per-offer name + discount value) so the footer can itemize them.
        AutoMigration(from = 15, to = 16),
    ],
)
@ConstructedBy(FlowVanDatabaseConstructor::class)
abstract class FlowVanDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun productUnitDao(): ProductUnitDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun shiftDao(): ShiftDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun routeStopDao(): RouteStopDao
    abstract fun aiMessageDao(): AiMessageDao
    abstract fun appSettingsDao(): AppSettingsDao
    abstract fun offerDao(): OfferDao
    abstract fun priceListItemDao(): PriceListItemDao
    abstract fun tobaccoTaxProfileDao(): TobaccoTaxProfileDao
}

@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object FlowVanDatabaseConstructor : RoomDatabaseConstructor<FlowVanDatabase> {
    override fun initialize(): FlowVanDatabase
}

const val FLOW_VAN_DB_NAME: String = "flowvan.db"

/** Folder (inside the Documents dir) where dated database backups are written. */
const val BACKUP_DIR: String = "van-flow-backups"