package com.jehadalomour.flowvan.core.domain.usecase

import com.jehadalomour.flowvan.core.database.entity.PaymentEntity
import com.jehadalomour.flowvan.core.data.location.LocationProvider
import com.jehadalomour.flowvan.core.data.repository.CustomerRepository
import com.jehadalomour.flowvan.core.data.repository.PaymentRepository
import com.jehadalomour.flowvan.core.model.PaymentMethod
import com.jehadalomour.flowvan.core.domain.sync.SyncScheduler
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class CollectionValidationException(val messageAr: String) : Exception(messageAr)

class RecordCollectionUseCase(
    private val payments: PaymentRepository,
    private val customers: CustomerRepository,
    private val syncScheduler: SyncScheduler,
    private val location: LocationProvider,
) {
    @OptIn(ExperimentalTime::class)
    suspend operator fun invoke(
        customerId: String,
        salesmanId: String,
        amount: Double,
        method: PaymentMethod,
        chequeNumber: String?,
        chequeBank: String?,
        chequeDate: Long?,
        transferRef: String?,
        notes: String?,
    ): Result<PaymentEntity> = runCatching {
        if (amount <= 0.0) throw CollectionValidationException("المبلغ يجب أن يكون أكبر من صفر")
        when (method) {
            PaymentMethod.CHEQUE -> {
                if (chequeNumber.isNullOrBlank()) throw CollectionValidationException("رقم الشيك مطلوب")
                if (chequeBank.isNullOrBlank()) throw CollectionValidationException("اسم البنك مطلوب")
            }
            PaymentMethod.TRANSFER -> {
                if (transferRef.isNullOrBlank()) throw CollectionValidationException("رقم الحوالة مطلوب")
            }
            else -> Unit
        }

        val number = VoucherNumber.next("RCP")
        val now = Clock.System.now().toEpochMilliseconds()
        val loc = location.lastLocation()
        val entity = PaymentEntity(
            id = "PMT-$number",
            number = number,
            customerId = customerId,
            salesmanId = salesmanId,
            amount = amount,
            method = method.name,
            status = "CONFIRMED",
            createdAt = now,
            chequeNumber = chequeNumber,
            chequeBank = chequeBank,
            chequeDate = chequeDate,
            transferRef = transferRef,
            notes = notes,
            syncedAt = null,
            repLat = loc?.lat,
            repLng = loc?.lng,
        )
        payments.save(entity)
        customers.adjustBalance(customerId, -amount)
        syncScheduler.syncNow()
        entity
    }
}
