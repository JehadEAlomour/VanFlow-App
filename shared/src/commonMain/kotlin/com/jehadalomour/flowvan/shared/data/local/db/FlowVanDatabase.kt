package com.jehadalomour.flowvan.shared.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.jehadalomour.flowvan.shared.data.local.dao.AiMessageDao
import com.jehadalomour.flowvan.shared.data.local.dao.CustomerDao
import com.jehadalomour.flowvan.shared.data.local.dao.InvoiceDao
import com.jehadalomour.flowvan.shared.data.local.dao.LocationPointDao
import com.jehadalomour.flowvan.shared.data.local.dao.PaymentDao
import com.jehadalomour.flowvan.shared.data.local.dao.ProductDao
import com.jehadalomour.flowvan.shared.data.local.dao.RouteStopDao
import com.jehadalomour.flowvan.shared.data.local.dao.ShiftDao
import com.jehadalomour.flowvan.shared.data.local.dao.UserDao
import com.jehadalomour.flowvan.shared.data.local.entity.AiMessageEntity
import com.jehadalomour.flowvan.shared.data.local.entity.CustomerEntity
import com.jehadalomour.flowvan.shared.data.local.entity.InvoiceEntity
import com.jehadalomour.flowvan.shared.data.local.entity.LocationPointEntity
import com.jehadalomour.flowvan.shared.data.local.entity.PaymentEntity
import com.jehadalomour.flowvan.shared.data.local.entity.ProductEntity
import com.jehadalomour.flowvan.shared.data.local.entity.RouteStopEntity
import com.jehadalomour.flowvan.shared.data.local.entity.ShiftEntity
import com.jehadalomour.flowvan.shared.data.local.entity.UserEntity

@Database(
    entities = [
        UserEntity::class,
        CustomerEntity::class,
        ProductEntity::class,
        InvoiceEntity::class,
        PaymentEntity::class,
        LocationPointEntity::class,
        ShiftEntity::class,
        AiMessageEntity::class,
        RouteStopEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(FlowVanDatabaseConstructor::class)
abstract class FlowVanDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun customerDao(): CustomerDao
    abstract fun productDao(): ProductDao
    abstract fun invoiceDao(): InvoiceDao
    abstract fun paymentDao(): PaymentDao
    abstract fun shiftDao(): ShiftDao
    abstract fun locationPointDao(): LocationPointDao
    abstract fun routeStopDao(): RouteStopDao
    abstract fun aiMessageDao(): AiMessageDao
}

@Suppress("KotlinNoActualForExpect", "NO_ACTUAL_FOR_EXPECT")
expect object FlowVanDatabaseConstructor : RoomDatabaseConstructor<FlowVanDatabase> {
    override fun initialize(): FlowVanDatabase
}

const val FLOW_VAN_DB_NAME: String = "flowvan.db"